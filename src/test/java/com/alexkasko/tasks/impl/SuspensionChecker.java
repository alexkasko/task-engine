package com.alexkasko.tasks.impl;

/**
 * User: alexkasko
 * Date: 5/23/12
 */

import com.alexkasko.tasks.TaskEngine;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

// engine sentinel
@Service
public class SuspensionChecker {
    @Inject
    private TaskEngine engine;

    public void checkSuspended(long taskId) {
        engine.checkSuspended(taskId);
    }
}
