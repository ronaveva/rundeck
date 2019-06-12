package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.jobs.JobLifeCycleEvent;

import java.util.Map;

public interface JobPersistEvent extends JobLifeCycleEvent {

    /**
     *
     * @return Map with all the job information.
     */
    Map getJobParams();

    /**
     *
     * @return Map with all of the job options.
     */
    Map getJobOptions();
}
