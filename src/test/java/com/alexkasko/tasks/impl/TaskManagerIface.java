package com.alexkasko.tasks.impl;

import com.alexkasko.tasks.TaskManager;

/**
 * User: alexkasko
 * Date: 5/23/12
 */
public interface TaskManagerIface extends TaskManager<TaskImpl> {
    public long add(TaskImpl task);

    public TaskImpl load(long taskId);

    public void resume(long taskId);
}
