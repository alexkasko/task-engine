package com.alexkasko.tasks;

/**
 * Additional operations that will be executed before/after task stage processing.
 * Listeners are separated from task processors to (among others)
 * simplify their declarative transaction management.
 *
 * @author alexkasko
 * Date: 6/30/12
 * @see TaskStageProcessor
 * @see TaskEngine
 */
public interface TaskStageListener {
    /**
     * Will be called by {@link TaskEngine} before/after stage processing
     *
     * @param taskId task ID
     */
    void fire(long taskId);
}
