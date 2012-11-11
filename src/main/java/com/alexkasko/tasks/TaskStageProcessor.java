package com.alexkasko.tasks;

/**
 * Interface for separate units of business oriented work on task
 *
 * @author alexkasko
 * Date: 5/17/12
 * @see TaskEngine
 * @see TaskEngine#checkSuspended(long)
 * @see Task
 * @see TaskProcessorProvider
 */
public interface TaskStageProcessor {

    /**
     * Some business oriented work to do on task.
     * {@link TaskEngine#checkSuspended(long)} method should be called periodically,
     * {@link TaskSuspendedException} must be thrown on successful suspension check
     *
     * @param taskId task id
     * @throws TaskSuspendedException task will rolled back to last completed stage and switched into 'suspended' status
     * @throws Exception task will rolled back to last completed stage and switched into 'error' status
     */
    void process(long taskId) throws Exception;
}
