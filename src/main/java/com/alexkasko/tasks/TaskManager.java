package com.alexkasko.tasks;

import java.util.Collection;

/**
 * DAO interface for tasks
 *
 * @author alexkasko
 * Date: 5/17/12
 * @see TaskEngine
 * @see Task
 */
public interface TaskManager<T extends Task> {
    /**
     * This method is used to load new and resumed tasks to run.
     * All returning tasks should be switched into 'processing' status in DB.
     * It's implementation responsibility to not return tasks that may be running in that moment.
     * Will be called from fire-caller thread.
     *
     * @return collection of tasks to run
     */
    Collection<? extends T> markProcessingAndLoad();

    /**
     * Changes task stage, will be called between stages processing
     * from stage-executor's thread only for tasks being in 'processing' status.
     *
     * @param taskId task id
     * @param stage new stage
     */
    void updateStage(long taskId, String stage);

    /**
     * Changes task status to default, will be called after successful processing of last stage
     * from stage-executor's thread only for tasks being in 'processing' status.
     *
     * @param taskId task id
     */
    void updateStatusDefault(long taskId);

    /**
     * Changes task status to from 'processing' to 'suspended', will be called on task suspension
     * from stage-executor's thread only for tasks being in 'processing' status.
     *
     * @param taskId task id
     */
    void updateStatusSuspended(long taskId);

    /**
     * Changes task status into "error" and task stage into last completed stage, will be called on processor's error
     * from stage-executor's thread only for tasks being in 'processing' status.
     *
     * @param taskId task id
     * @param e exception
     * @param lastCompletedStage name of last completed stage
     */
    void updateStatusError(long taskId, Exception e, String lastCompletedStage);
}
