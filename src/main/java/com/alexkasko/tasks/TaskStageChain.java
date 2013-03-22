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
    protected final Map<String, Stage> stageMap;
    protected final List<Stage> stageList;

    /**
     * Protected constructor for inheritors
     *
     * @param stageList list of stages for this chain
     */
    protected TaskStageChain(List<Stage> stageList) {
        if(null == stageList) throw new TaskEngineException("Null stageList provided");
        this.stageList = stageList;
        this.stageMap = new LinkedHashMap<String, Stage>();
        for(Stage ts : stageList) {
            if(!ts.isStart()) stageMap.put(ts.getIntermediate(), ts);
            stageMap.put(ts.getCompleted(), ts);
        }
    }

    /**
     * Finds stage in chain by name. Made public (not package private) for inheritors.
     *
     * @param stage stage name
     * @return {@link Stage} instance for given name
     */
    public Stage forName(String stage) {
        if(null == stage) throw new TaskEngineException("Null stage provided");
        Stage res = stageMap.get(stage);
        if(null == res) throw new TaskEngineException(
                "Unknown stage provided, valid stages are: [" + stageList + "]");
        return res;
    }

    /**
     * Gets stage from the chain previous to provided stage. Made public (not package private) for inheritors.
     *
     * @param stage input stage
     * @return previous stage
     */
    public Stage previous(Stage stage) {
        int ind = index(stage);
        if(0 == ind) throw new TaskEngineException(
                "Start stage: [" + stage + "], has no previous stage, valid stages are: [" + stageList + "]");
        return stageList.get(ind - 1);
    }

    /**
     * Gets stage from the chain next to provided stage. Made public (not package private) for inheritors.
     *
     * @param stage input stage
     * @return next stage
     */
    public Stage next(Stage stage) {
        int ind = index(stage);
        if(stageList.size() - 1 == ind) throw new TaskEngineException(
                "End stage: [" + stage + "], has no next stage, valid stages are: [" + stageList + "]");
        return stageList.get(ind + 1);
    }

    /**
     * Checks whether provided stage has a next stage. Made public (not package private) for inheritors.
     *
     * @param stage input stage
     * @return whether next stage exists for given stage
     */
    public boolean hasNext(Stage stage) {
        return stageList.size() - 1 > index(stage);
    }

    /**
     * Public method for client-code (so stage names are used instead of stages),
     * return last completed stage for the given stage.
     *
     * @param currentStage current stage
     * @return given stage name, if it's completed name, completed name of previous stage otherwise.
     */
    public String lastCompletedStage(Enum<?> currentStage) {
        if(null == currentStage) throw new TaskEngineException("Null currentStage provided");
        return lastCompletedStage(currentStage.name());
    }

    /**
     * Public method for client-code (so stage names are used instead of stages),
     * return last completed stage for the given stage.
     *
     * @param currentStage name of the current stage
     * @return given stage name, if it's completed name, completed name of previous stage otherwise.
     */
    public String lastCompletedStage(String currentStage) {
        if(null == currentStage) throw new TaskEngineException("Null currentStage provided");
        Stage stage = forName(currentStage);
        if(currentStage.equals(stage.getCompleted())) return currentStage;
        Stage prev = previous(stage);
        return prev.getCompleted();
    }

    /**
     * Builder instance factory method
     *
     * @param startStage start stage
     * @return {@link Builder} builder for chain
     */
    public static Builder builder(Enum<?> startStage) {
        if(null == startStage) throw new TaskEngineException("Null startStage provided");
        return builder(startStage.name());
    }

    /**
     * Builder instance factory method
     *
     * @param startStage start stage name
     * @return {@link Builder} builder for chain
     */
    public static Builder builder(String startStage) {
        if(null == startStage) throw new TaskEngineException("Null startStage provided");
        return new Builder(startStage);
    }

    private int index(Stage stage) {
        if(null == stage) throw new TaskEngineException("Null stage provided");
        // for short lists this should be faster on list than on set
        int pos = stageList.indexOf(stage);
        if(-1 == pos) throw new TaskEngineException("Unknown stage provided, valid stages are: [" + stageList + "]");
        return pos;
    }

    /**
     * Builder class for {@link TaskStageChain}, not thread-safe
     */
    public static class Builder {
        private final List<Stage> list = new ArrayList<Stage>();
        private final Set<String> stages = new HashSet<String>();

        /**
         * Constructor, protected for inheritors
         *
         * @param startStage start stage name
         */
        protected Builder(String startStage) {
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
            if(null == intermediate) throw new TaskEngineException("Null intermediate stage provided");
            if(null == completed) throw new TaskEngineException("Null completed stage provided");
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
            if(null == intermediate) throw new TaskEngineException("Null intermediate stage provided");
            if(null == completed) throw new TaskEngineException("Null completed stage provided");
            boolean unique1 = this.stages.add(intermediate);
            if(!unique1) throw new TaskEngineException("Duplicate stage provided: [" + intermediate + "]");
            boolean unique2 = this.stages.add(completed);
            if(!unique2) throw new TaskEngineException("Duplicate stage provided: [" + completed + "]");
            this.list.add(new Stage(intermediate, completed, processorId));
            return this;
        }

        /**
         * Creates stage chain instance
         *
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

        protected final String intermediate;
        protected final String completed;
        protected final String processorId;
        protected final boolean start;

        /**
         * Constructor for start stage
         *
         * @param startStageName name of the start stage
         */
        protected Stage(String startStageName) {
            if(null == startStageName) throw new TaskEngineException("Null startStageName stage provided");
            this.completed = startStageName;
            this.start = true;
            this.intermediate = null;
            this.processorId = null;
        }

        /**
         * Constructor for stage
         *
         * @param intermediate intermediate stage name
         * @param completed completed stage name
         * @param processorId processorId for this stage
         */
        protected Stage(String intermediate, String completed, String processorId) {
            if(null == intermediate) throw new TaskEngineException("Null intermediate stage provided");
            if(null == completed) throw new TaskEngineException("Null completed stage provided");
            if(null == processorId) throw new TaskEngineException("Null processorId provided");
            this.intermediate = intermediate;
            this.completed = completed;
            this.processorId = processorId;
            this.start = false;
        }

        /**
         * Returns intermediate name of this stage
         *
         * @return intermediate name of this stage
         */
        public String getIntermediate() {
            if(start) throw new TaskEngineException("Start stage: [" + intermediate + "] has no intermediate stage");
            return intermediate;
        }

        /**
         * Returns completed name of this stage
         *
         * @return completed name of this stage
         */
        public String getCompleted() {
            return completed;
        }

        /**
         * Returns processor ID for this stage
         *
         * @return processor ID for this stage
         */
        public String getProcessorId() {
            if(start)  throw new TaskEngineException("Start stage: [" + completed + "] has no processorId");
            return processorId;
        }

        /**
         * Whether this stage is start stage
         *
         * @return whether this stage is start stage
         */
        public boolean isStart() {
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
