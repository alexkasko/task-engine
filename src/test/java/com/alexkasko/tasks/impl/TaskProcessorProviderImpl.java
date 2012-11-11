package com.alexkasko.tasks.impl;

import com.alexkasko.tasks.TaskProcessorProvider;
import com.alexkasko.tasks.TaskStageProcessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * User: alexkasko
 * Date: 5/23/12
 */

@Service
public class TaskProcessorProviderImpl implements TaskProcessorProvider {
    @Inject
    private BeanFactory bf;

    @Override
    public TaskStageProcessor provide(String id) {
        return bf.getBean(id, TaskStageProcessor.class);
    }
}
