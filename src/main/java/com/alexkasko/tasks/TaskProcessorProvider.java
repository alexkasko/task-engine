package com.alexkasko.tasks;

/**
 * Interface is used by engine to get processor for given stage.
 *
 * @author alexkasko
 * Date: 5/17/12
 * @see TaskStageProcessor
 * @see TaskEngine
 */
public interface TaskProcessorProvider {
    /**
     * Must provide {@link TaskStageProcessor} for given string id
     *
     * @param id processor id
     * @return stage processor
     */
    TaskStageProcessor provide(String id);
}
