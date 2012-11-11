package com.alexkasko.tasks;

/**
 * This exception will be thrown on successful suspension check
 *
 * @author alexkasko
 * Date: 5/20/12
 * @see TaskEngine
 * @see Task
 */
public class TaskSuspendedException extends RuntimeException {
    private static final long serialVersionUID = 3388511296426912332L;

    public TaskSuspendedException(long taskId) {
        super(Long.toString(taskId));
    }
}
