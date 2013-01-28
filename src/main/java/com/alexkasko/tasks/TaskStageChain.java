package com.alexkasko.tasks;

import java.io.Serializable;
import java.util.*;

/**
 * Implementation of task stages list. Must be provided by {@link Task} instances. Thread-safe.
 *
 * @author alexkasko
 * Date: 5/22/12
 * @see TaskEngine
 * @see Task
 * @see TaskProcessorProvider
 */
public class TaskStageChain implements Serializable {
    private static final long serialVersionUID = 7486673100573364684L;
    private final Map<String, Stage> stageMap;
    private final List<Stage> stageList;

    private TaskStageChain(List<Stage> stageList) {
        this.stageList = stageList;
        this.stageMap = new LinkedHashMap<String, Stage>();
        for(Stage ts : stageList) {
            if(!ts.isStart()) stageMap.put(ts.getIntermediate(), ts);
            stageMap.put(ts.getCompleted(), ts);
        }
    }

    /**
     * @param stage stage name
     * @return {@link Stage} instance for given name
     */
    public Stage forName(String stage) {
        if(null == stage) throw new IllegalArgumentException("Null stage provided");
        Stage res = stageMap.get(stage);
        if(null == res) throw new IllegalArgumentException(
                "Unknown stage provided, valid stages are: '" + stageList + "'");
        return res;
    }

    /**
     * @param stage input stage
     * @return previous stage
     */
    public Stage previous(Stage stage) {
        int ind = index(stage);
        if(0 == ind) throw new IllegalArgumentException(
                "Start stage: '" + stage + "', has no previous stage, valid stages are: '" + stageList + "'");
        return stageList.get(ind - 1);
    }

    /**
     * @param stage input stage
     * @return next stage
     */
    public Stage next(Stage stage) {
        int ind = index(stage);
        if(stageList.size() - 1 == ind) throw new IllegalArgumentException(
                "End stage: '" + stage + "', has no next stage, valid stages are: '" + stageList + "'");
        return stageList.get(ind + 1);
    }

    /**
     * @param stage input stage
     * @return whether next stage exists for given stage
     */
    public boolean hasNext(Stage stage) {
        return stageList.size() - 1 > index(stage);
    }

    /**
     * @param startStage start stage
     * @return {@link Builder} builder for chain
     */
    public static Builder builder(Enum<?> startStage) {
        if(null == startStage) throw new IllegalArgumentException("Null startStage provided");
        return builder(startStage.name());
    }

    /**
     * @param startStage start stage name
     * @return {@link Builder} builder for chain
     */
    public static Builder builder(String startStage) {
        if(null == startStage) throw new IllegalArgumentException("Null startStage provided");
        return new Builder(startStage);
    }

    private int index(Stage stage) {
        if(null == stage) throw new IllegalArgumentException("Null stage provided");
        // for short lists this should be faster on list than on set
        int pos = stageList.indexOf(stage);
        if(-1 == pos) throw new IllegalArgumentException("Unknown stage provided, valid stages are: '" + stageList + "'");
        return pos;
    }


    /**
     * Builder class for {@link TaskStageChain}, not thread-safe
     */
    public static class Builder {
        private final List<Stage> list = new ArrayList<Stage>();
        private final Set<String> stages = new HashSet<String>();

        private Builder(String startStage) {
            this.list.add(new Stage(startStage));
            this.stages.add(startStage);
        }

        /**
         * Adds new enum stage to chain
         *
         * @param intermediate intermediate stage, e.g. 'running', 'loading_data'
         * @param completed completed stage, e.g. 'finished', 'data_loaded'
         * @param processorId id of the processor that will be used for this stage
         * @return builder instance
         */
        public Builder add(Enum<?> intermediate, Enum<?> completed, String processorId) {
            if(null == intermediate) throw new IllegalArgumentException("Null intermediate stage provided");
            if(null == completed) throw new IllegalArgumentException("Null completed stage provided");
            return add(intermediate.name(), completed.name(), processorId);
        }

        /**
         * Adds new stage to chain
         *
         * @param intermediate intermediate stage name, e.g. 'running', 'loading_data'
         * @param completed completed stage name, e.g. 'finished', 'data_loaded'
         * @param processorId id of the processor that will be used for this stage
         * @return builder instance
         */
        public Builder add(String intermediate, String completed, String processorId) {
            if(null == intermediate) throw new IllegalArgumentException("Null intermediate stage provided");
            if(null == completed) throw new IllegalArgumentException("Null completed stage provided");
            boolean unique1 = this.stages.add(intermediate);
            if(!unique1) throw new IllegalArgumentException("Duplicate stage provided: '" + intermediate + "'");
            boolean unique2 = this.stages.add(completed);
            if(!unique2) throw new IllegalArgumentException("Duplicate stage provided: '" + completed + "'");
            this.list.add(new Stage(intermediate, completed, processorId));
            return this;
        }

        /**
         * @return stage chain instance
         */
        public TaskStageChain build() {
            return new TaskStageChain(list);
        }
    }

    /**
     * Inner implementation of stage
     */
    protected static class Stage implements Serializable {
        private static final long serialVersionUID = 6127466720110180244L;

        private final String intermediate;
        private final String completed;
        private final String processorId;
        private final boolean start;

        Stage(String completed) {
            this.completed = completed;
            this.start = true;
            this.intermediate = null;
            this.processorId = null;
        }

        Stage(String intermediate, String completed, String processorId) {
            this.intermediate = intermediate;
            this.completed = completed;
            this.processorId = processorId;
            this.start = false;
        }

        String getIntermediate() {
            if(start) throw new IllegalArgumentException("Start stage: '" + intermediate + "' has no intermediate stage");
            return intermediate;
        }

        String getCompleted() {
            return completed;
        }

        String getProcessorId() {
            if(start)  throw new IllegalArgumentException("Start stage: '" + completed + "' has no processorId");
            return processorId;
        }

        boolean isStart() {
            return start;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Stage stage = (Stage) o;
            return completed.equals(stage.completed);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return completed.hashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return completed;
        }
    }
}
