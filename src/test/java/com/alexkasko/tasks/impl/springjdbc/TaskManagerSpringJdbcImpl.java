package com.alexkasko.tasks.impl.springjdbc;

import com.alexkasko.tasks.impl.Stage;
import com.alexkasko.tasks.impl.Status;
import com.alexkasko.tasks.impl.TaskImpl;
import com.alexkasko.tasks.impl.TaskManagerIface;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.Collection;
import java.util.List;

import static com.alexkasko.tasks.impl.TaskImpl.MAPPER;
import static com.google.common.base.Preconditions.checkState;

/**
 * User: alexkasko
 * Date: 5/23/12
 */

@Service
public class TaskManagerSpringJdbcImpl implements TaskManagerIface {
    @Inject
    private DataSource ds;
    private NamedParameterJdbcTemplate jt;

    @PostConstruct
    private void postConstruct() {
        jt = new NamedParameterJdbcTemplate(ds);
    }

    @Override
    @Transactional
    public Collection<TaskImpl> markProcessingAndLoad() {
        // lock selected
        int updated = jt.getJdbcOperations().update("update tasks set status='SELECTED' where (status='NORMAL' and stage='CREATED') or status='RESUMED'");
        if(0 == updated) return ImmutableList.of();
        // load selected
        List<TaskImpl> tasks = jt.getJdbcOperations().query("select * from tasks where status='SELECTED'", MAPPER);
        List<Long> taskIds = Lists.transform(tasks, TaskImpl.ID_FUNCTION);
        // mark
        jt.update("update tasks set status='PROCESSING' where id in (:taskIds)", ImmutableMap.of("taskIds", taskIds));
        // returned status is not synchronized with db, but that doesn't matter
        return tasks;
    }

    @Override
    @Transactional
    public void updateStage(long taskId, String stage) {
        Stage st = Stage.valueOf(stage);
        int res = jt.update("update tasks set stage=:st where status=:processing and id=:taskId", ImmutableMap.of(
                "st", st.name(),
                "processing", Status.PROCESSING.name(),
                "taskId", taskId));
        checkState(1 == res, "Wrong updated rows count for taskId: '%s', must be 1 but was: '%s'", taskId, res);
    }

    @Override
    @Transactional
    public void updateStatusDefault(long taskId) {
        int res = jt.update("update tasks set status=:status where status=:processing and id=:taskId", ImmutableMap.of(
                "status", Status.NORMAL.name(),
                "processing", Status.PROCESSING.name(),
                "taskId", taskId));
        checkState(1 == res, "Wrong updated rows count for taskId: '%s', must be 1 but was: '%s'", taskId, res);
    }

    @Override
    @Transactional
    public void updateStatusSuspended(long taskId) {
        int res = jt.update("update tasks set status=:status where status=:processing and id=:taskId", ImmutableMap.of(
                "status", Status.SUSPENDED.name(),
                "processing", Status.PROCESSING.name(),
                "taskId", taskId));
        checkState(1 == res, "Wrong updated rows count for taskId: '%s', must be 1 but was: '%s'", taskId, res);
    }

    @Override
    @Transactional
    public void updateStatusError(long taskId, Exception e, String lastCompletedStage) {
        e.printStackTrace();
        Stage stage = Stage.valueOf(lastCompletedStage);
        int res = jt.update("update tasks set status=:status, stage=:stage where status=:processing and id=:taskId", ImmutableMap.of(
                "status", Status.ERROR.name(),
                "processing", Status.PROCESSING.name(),
                "stage", stage.name(),
                "taskId", taskId));
        checkState(1 == res, "Wrong updated rows count for taskId: '%s', must be 1 but was: '%s'", taskId, res);
    }

    @Override
    @Transactional
    public long add(TaskImpl task) {
        long id = jt.getJdbcOperations().queryForLong("select nextval('tasks_id_seq')");
        int res = jt.update("insert into tasks (id, stage, status, payload) values (:id, :stage, :status, :payload)", ImmutableMap.of(
                "id", id,
                "stage", task.getStageName(),
                "status", task.getStatus().name(),
                "payload", task.getPayload()));
        checkState(1 == res, "Wrong updated rows count on task create, must be 1 but was: '%s'", res);
        return id;
    }

    @Override
    @Transactional(readOnly = true)
    public TaskImpl load(long taskId) {
        return jt.queryForObject("select * from tasks where id=:taskId", ImmutableMap.of("taskId", taskId), MAPPER);
    }

    @Override
    public void resume(long taskId) {
        throw new UnsupportedOperationException();
    }
}
