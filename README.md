Engine for asynchronous multistage suspendable tasks
====================================================

Library imlements background tasks engine with support for tasks suspending.

Library only depends on [commons-logging](http://commons.apache.org/logging/).

Library is available in [Maven cental](http://repo1.maven.org/maven2/com/alexkasko/tasks/).

Javadocs for the latest release are available [here](http://alexkasko.github.com/task-engine/javadocs).

Multistage suspendable tasks
----------------------------

Library was designed for tasks, that have separate "stage" and "status" fields.

####stage
For most tasks, processing can be splitted to separate stages. Task stage-chain example:

 * `CREATED` (static)
 * `LOADING_DATA` (dynamic)
 * `DATA_LOADED` (static)
 * `BUILDING_REPORT` (dynamic)
 * `FINISHED` (static)

All stages (except first) come in pairs:

 * "dynamic" - processing something in memory (`LOADING_DATA`)
 * "static" - persistent state, result of previous stage (`DATA_LOADED`)


####task suspending and transactions

Task may be supended during execution of dynamic stage, it will rolled back to last "static" stage, from which it may be resumed later.
But in transactional environment rollback to previos "static" stage cannot be achieved simply by:

`start transaction` -> `update task stage field in DB` -> `start "dynamic" stage processing` -> `rollback transaction on usr suspension call`

because task stage field in DB must be visible to users (to other transactions in READ_COMMITTED mode) during long
"dynamic" stage proccessing. So task stage field must be updated to "running" value in separate transaction just before starting "dynamic stage"
and then updated again to previous "static" value on suspension.

####status
During it's lifetime task may reside in different statuses ("status" is orthogonal to "stage" here):

 `NORMAL`, `IN_PROCESSING`, `SUSPENDED`, `RESUMED`, `ERROR`

####task lifetime
Task lifetime looks like this in (stage, status) pairs:

`CREATED, NORMAL` -(start first stage)-> `LOADING_DATA, IN_PROCESSING` -(first stage finished)-> `DATA_LOADED, IN_PROCESSING`
-(start second stage)-> `BUILDING_REPORT, IN_PROCESSING` -(suspended by user)-> `DATA_LOADED, SUSPENDED`
-(later resumed by user)-> `DATA_LOADED, RESUMED` -(starting second stage)-> `BUILDING_REPORT, IN_PROCESSING`
-(finished successfully)-> `FINISHED. NORMAL`

On all processing stages task may be switched to `ERROR` and

Library usage
-------------

Maven dependency (available in central repository):

    <dependency>
        <groupId>com.alexkasko.tasks</groupId>
        <artifactId>task-engine</artifactId>
        <version>1.0</version>
    </dependency>

`TaskEngine` implements tasks running, suspending and resuming with proper stage and status updating. `TaskEngine`
also implements `Runnable` interface to be run by some kind of scheduler.

To use `TaskEngine` application must implement these interfaces:

 * `Task` - task intances
 * `TaskManager` - DAO for tasks
 * `TaskStageProcessor` - business logic implementation for each "dynamic" task stage
 * `TaskProcessorProvider` - must return processors by string identifiers (got from tasks instances)

Usage example:

    // engine creation
    TaskEngine taskEngine = TaskEngine(executor, taskManager, processorProvider);
    // on init engine calls taskManager for suspended tasks,
    // so it must be called after application init
    taskEngine.init();
    //loads new tasks and starts execution
    // also will be called by "run()" method, may be called
    // by applications events or linked to scheduler
    taskEngine.fire();
    // task suspension
    taskEngine.suspend(long taskId);

_Note: you may find example implementations of these interfaces for spring-jdbc and hibernate in 
[project tests](https://github.com/alexkasko/task-engine/tree/master/src/test/java/com/alexkasko/tasks/impl)._

####stage chains creation

`TaskEngine` determine next processor based on `TaskChain` got from `Task` instance. So one engine
may run different tasks with different chains simultaneously. `stageChain` method may be implemented
on some upper level of tasks hierarchy returning preconfigured chain for all sublcasses:

    // somewhere in AbstractTaskType1
    @Override
    public TaskStageChain stageChain() {
        return TaskStageChain.builder(CREATED)
               .add(LOADING_DATA, DATA_LOADED, "dataLoadService")
               .add(BUILDING_REPORT, FINISHED, "reportService")
               .build();
    }

####tasks suspending and resuming

Task suspension call must cause next aftermath:

 * task status update to "suspended"
 * task stage update to previous "static" stage
 * current task stage processing, running in executor, must be rolled back

For first two points `TaskEngine` makes direct calls to `TaskManager`. To implement actual task intteruption
engine holds set of suspended task ids and provides `checkSuspended` method that should be called periodically
by `TaskStageProcessor` implementation. If task is suspended, `TaskEngine` removes it from set and throws `TaskSuspendedException`
that should rollback current stage execution.

`TaskEngine` has no separate API for task resuming. It run all tasks provided by `TaskManager` choosing next
stage (not first for resumed tasks) based on current "static" stage (`getStageName` method in tasks).

####loading tasks for execution

All `TaskManager` methods are quite straightforward for implementation except `markProcessingAndLoad` that must
load requests for execution. This method must return all requests that can be fired just now (newly created or resumed),
return each task no more then once (to prevent double firing, except resumed tasks) and do it implying, that it may be
called concurently (if two different threads call `TaskEngine.fire()` simultaniously).

Example of proper implementation assuming READ_COMMITTED transaction isolation level
(uses additional `SELECTED` status to prevent double firing and spring's `JdbcTemplate`):

    // lock requests for fire
    int updated = jt.update("update tasks set status='SELECTED' where (status='NORMAL' and stage='CREATED') or status='RESUMED'");
    if(0 == updated) return ImmutableList.of();
    // load selected requests
    List<TaskImpl> tasks = jt.query("select * from tasks where status='SELECTED'", MAPPER);
    // extract list of task ids
    List<Long> taskIds = Lists.transform(tasks, TaskImpl.ID_FUNCTION);
    // mark loaded requests as "processing"
    npjt.update("update tasks set status='PROCESSING' where id in (:taskIds)", ImmutableMap.of("taskIds", taskIds));
    // "status" returned is not synchronized with db ('SELECTED' returns, where 'PROCESSING' in db),
    // but that doesn't matter for TaskEngine
    return tasks;

####task stage listeners

If task stage processor implements `TaskStageListenableProcessor` interface, it can have `TaskStageListener`s
attached, that will be fired by `TaskEngine` before or after task stage execution.

Changelog
---------

**1.0** (2012-11-11)

 * initial version