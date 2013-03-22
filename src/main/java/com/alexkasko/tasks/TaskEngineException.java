package com.alexkasko.tasks;

/**
 * Runtime exception for task-engine related errors
 *
 * @author alexkasko
 * Date: 3/22/13
 */
public class TaskEngineException extends RuntimeException {
    private static final long serialVersionUID = 3388511296426912331L;

    /**
     * Constructor
     *
     * @param message exception message
     */
    public TaskEngineException(String message) {
        super(message);
    }
}
