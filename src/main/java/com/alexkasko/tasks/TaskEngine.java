package com.alexkasko.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import static java.util.Collections.newSetFromMap;

/**
 * Engine for asynchronous multistage suspendable tasks.
 * Processes task stages one by one using provided {@link java.util.concurrent.Executor},
 * tasks stage will be updated between stages processing. Task status will be updated on error,
 * suspend or after successful processing of last stage.
 * Processors should call {@link TaskEngine#checkSuspended(long)} method periodically, it will
 * throw {@link TaskSuspendedException} on successful check.
 *
 * @author alexkasko
 * Date: 5/17/12
 * @see Task
 * @see TaskManager
 * @see TaskProcessorProvider
 * @see TaskStageChain
 * @see TaskStageProcessor
 * @see TaskSuspendedException
 */
public class TaskEngine implements Runnable {
    private static final Log logger = LogFactory.getLog(TaskEngine.class);

    private final Executor executor;
    private final TaskManager<? extends Task> manager;
    private final TaskProcessorProvider provider;
    // concurrent hash set creation
    private final Set<Long> suspended = newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
    private final Object fireLock = new Object();
    private final Object suspensionLock = new Object();

    /**
     * @param executor executor will be used to process separate stages
     * @param manager tasks DAO for all task state operations
     * @param provider stage processors provider
     */
    public TaskEngine(Executor executor, TaskManager<? extends Task> manager, TaskProcessorProvider provider) {
        if(null == executor) throw new IllegalArgumentException("Provided executor is null");
        if(null == manager) throw new IllegalArgumentException("Provided manager is null");
        if(null == provider) throw new IllegalArgumentException("Input provider is null");
        this.executor = executor;
        this.manager = manager;
        this.provider = provider;
    }

    /**
     * Init method, must be called after task manager init
     */
    public void init() {
        synchronized (suspensionLock) {
            Collection<Long> tasks = manager.loadSuspendedIds();
            if(tasks.size() > 0) {
                logger.info("Suspended tasks cached: '" + tasks + "'");
                suspended.addAll(tasks);
            }
        }
    }

    /**
     * Sends tasks provided by {@link TaskManager#markProcessingAndLoad()}
     * to execution
     *
     * @return count of tasks sent for processing
     */
    public int fire() {
        synchronized (fireLock) {
            Collection<? extends Task> tasksToFire = manager.markProcessingAndLoad();
            if(0 == tasksToFire.size()) {
                logger.debug("No tasks to fire, returning to sleep");
                return 0;
            }
            // fire tasks
            int counter = 0;
            for(Task task : tasksToFire) {
                if(null == task) throw new IllegalArgumentException("Provided task is null, task list to fire: '" + tasksToFire + "'");
                logger.debug("Firing task: '" + task + "'");
                Runnable runnable = new StageRunnable(task);
                executor.execute(runnable);
                counter += 1;
            }
            if(counter > 0 ) logger.info(counter + " tasks fired");
            return counter;
        }
    }

    /**
     * Scheduler friendly fire wrapper
     */
    @Override
    public void run() {
        fire();
    }

    /**
     * Mark task as suspended
     *
     * @param taskId task id
     * @return {@code false} if task was already suspended, {@code true} otherwise
     */
    public boolean suspend(long taskId) {
        logger.debug("Suspending task, id: '" + taskId + "'");
        synchronized (suspensionLock) {
            boolean res = suspended.add(taskId);
            if(res) manager.updateStatusSuspended(taskId);
            return res;
        }
    }

    /**
     * Throws {@link TaskSuspendedException} on successful suspension check
     *
     * @param taskId task id
     * @throws TaskSuspendedException if task was already suspended
     */
    public void checkSuspended(long taskId) {
        if(suspended.remove(taskId)) throw new TaskSuspendedException(taskId);
    }

    // Runnable instead of Callable is deliberate
    private class StageRunnable implements Runnable {
        private final Task task;

        StageRunnable(Task task) {
            if(null == task.stageChain()) throw new IllegalArgumentException("Task, id: '" + task.getId() + "' returns null stageChain");
            this.task = task;
        }

        @Override
        public void run() {
            try {
                runStages();
            } catch (Exception e) {
                logger.error("System error running task, id: '" + task.getId() + "'", e);
            }
        }

        @SuppressWarnings("unchecked")
        private void runStages() {
            final TaskStageChain chain = task.stageChain();
            TaskStageChain.Stage stage = chain.forName(task.getStageName());
            boolean markDefaultOnExit = true;
            while (chain.hasNext(stage)) {
                if (suspended.remove(task.getId())) {
                    logger.info("Task, id: '" + task.getId() + "' was suspended, terminating execution");
                    markDefaultOnExit = false;
                    break;
                }
                stage = chain.next(stage);
                logger.debug("Starting stage: '" + stage.getIntermediate() + "' for task, id: '" + task.getId() + "'");
                TaskStageProcessor processor = provider.provide(stage.getProcessorId());
                if(null == processor) throw new IllegalArgumentException("Null processor returned for id: '" + stage.getProcessorId() + "'");
                manager.updateStage(task.getId(), stage.getIntermediate());
                try {
                    fireBeforeListeners(processor);
                    processor.process(task.getId());
                    fireAfterListeners(processor);
                    logger.debug("Stage: '" + stage.getCompleted() + "' completed for task, id: '" + task.getId() + "'");
                    manager.updateStage(task.getId(), stage.getCompleted());
                } catch (TaskSuspendedException e) {
                    logger.info("Task, id: '" + task.getId() + "' was suspended on stage: '" + stage.getIntermediate() + "'");
                    manager.updateStage(task.getId(), chain.previous(stage).getCompleted());
                    markDefaultOnExit = false;
                    break;
                } catch (Exception e) {
                    manager.updateStatusError(task.getId(), e, chain.previous(stage).getCompleted());
                    markDefaultOnExit = false;
                    break;
                }
            }
            if(markDefaultOnExit) manager.updateStatusDefault(task.getId());
        }

        private void fireBeforeListeners(TaskStageProcessor processor) {
            if(processor instanceof TaskStageListenableProcessor) {
                TaskStageListenableProcessor listen = (TaskStageListenableProcessor) processor;
                for(TaskStageListener li : listen.beforeStartListeners()) {
                    li.fire(task.getId());
                }
            }
        }

        private void fireAfterListeners(TaskStageProcessor processor) {
            if(processor instanceof TaskStageListenableProcessor) {
                TaskStageListenableProcessor listen = (TaskStageListenableProcessor) processor;
                for(TaskStageListener li : listen.afterFinishListeners()) {
                    li.fire(task.getId());
                }
            }
        }
    }


}