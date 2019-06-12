package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.IRundeckProject;
import com.dtolabs.rundeck.core.execution.ExecutionLogger;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;

import java.util.Map;

/**
 * Describes the job life cycle event.
 * Created by rnavarro
 * Date: 5/07/19
 */

public interface JobLifeCycleEvent {

    /**
     *
     * @return String project where the event occurs.
     */
    String getProjectName();

    /**
     *
     * @return the framework.
     */
    Framework getFramework();

    /**
     *
     * @return all of the rundeck project information.
     */
    IRundeckProject getRundeckProject();
}