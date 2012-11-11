package com.alexkasko.tasks.impl;

import com.alexkasko.springjdbc.named.NamedConstructorMapper;
import com.alexkasko.tasks.Task;
import com.alexkasko.tasks.TaskStageChain;
import com.google.common.base.Function;
import org.springframework.jdbc.core.RowMapper;

import javax.inject.Named;
import javax.persistence.*;

import static com.alexkasko.tasks.impl.Stage.*;

/**
* User: alexkasko
* Date: 5/22/12
*/

@Entity
@Table(name = "tasks")
@SequenceGenerator(name = "tasks_id_seq", sequenceName = "tasks_id_seq")
public class TaskImpl implements Task {
    public static final RowMapper<TaskImpl> MAPPER = NamedConstructorMapper.forClass(TaskImpl.class);
    public static final Function<TaskImpl, Long> ID_FUNCTION = new IdFunction();

    @Id
    @GeneratedValue(generator = "tasks_id_seq", strategy = GenerationType.SEQUENCE)
    @Column
    private long id;
    @Column
    @Enumerated(EnumType.STRING)
    private Stage stage;
    @Column
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column
    private long payload;

    public TaskImpl() {
    }

    public TaskImpl(long payload) {
        this.stage = CREATED;
        this.status = Status.NORMAL;
        this.payload = payload;
    }

    public TaskImpl(@Named("id") long id, @Named("stage") String stage, @Named("status") String status,
                    @Named("payload") long payload) {
        this.id = id;
        this.stage = Stage.valueOf(stage);
        this.status = Status.valueOf(status);
        this.payload = payload;
    }

    public static TaskStageChain chain() {
        return TaskStageChain.builder(CREATED)
                .add(RUNNING, DATA_LOADED, DataStageProcessor.class.getSimpleName())
                .add(REPORTS, FINISHED, ReportStageProcessor.class.getSimpleName())
                .build();
    }

    @Override
    public TaskStageChain stageChain() {
        return chain();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getStageName() {
        return stage.name();
    }

    public Status getStatus() {
        return status;
    }

    public long getPayload() {
        return payload;
    }

    public void changeStage(Stage stage) {
        this.stage = stage;
    }

    public void changeStatus(Status status) {
        this.status = status;
    }

    private static class IdFunction implements Function<TaskImpl, Long> {
        @Override
        public Long apply(TaskImpl input) {
            return input.getId();
        }
    }
}