package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.execution.ExecutionLogger;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.jobs.JobLifeCycleEvent;

import java.util.Map;

public interface JobExecutionEvent extends JobLifeCycleEvent {

    /**
     *
     * @return StepExecutionContext of the event.
     */
    StepExecutionContext getExecutionContext();

    /**
     *
     * @return Map<String, String> options of the job.
     */
    Map<String, String> getOptions();

    /**
     *
     * @return ExecutionLogger logger of the job.
     */
    ExecutionLogger getExecutionLogger();

    /**
     *
     * @return String user name triggering the job.
     */
    String getUserName();

    /**
     *
     * @return String job execution id.
     */
    String getExecutionId();


}
