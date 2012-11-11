package com.alexkasko.tasks;

import java.util.List;

/**
 * {@link TaskStageProcessor} extension to allow additional operations execution
 * before/after task stage processing.
 * Listeners are separated from task processors to (among others)
 * simplify their declarative transaction management.
 *
 * @author alexkasko
 * Date: 11/11/12
 * @see TaskEngine
 * @see TaskStageListener
 */
public interface TaskStageListenableProcessor extends TaskStageProcessor {
    /**
     * List of additional operations, that will be called by {@link TaskEngine} before stage processing
     *
     * @return before listeners
     */
    List<? extends TaskStageListener> beforeStartListeners();

    /**
     * List of additional operations, that will be called by {@link TaskEngine} after stage processing
     *
     * @return after listeners
     */
    List<? extends TaskStageListener> afterFinishListeners();
}
