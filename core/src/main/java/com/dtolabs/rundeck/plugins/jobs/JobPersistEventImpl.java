package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.IRundeckProject;

import java.util.Map;

public class JobPersistEventImpl implements JobPersistEvent{

    private Framework framework;
    private Map jobParams;

    JobPersistEventImpl(){}

    JobPersistEventImpl(Framework framework){
        this.framework = framework;
    }

    @Override
    public Map getJobParams() {
        return this.jobParams;
    }

    @Override
    public Map getJobOptions() {
        if(this.jobParams != null && this.jobParams.containsKey("_sessionEditOPTSObject")){
            return (Map) this.jobParams.get("_sessionEditOPTSObject");
        }
        return null;
    }

    @Override
    public String getProjectName() {
        return null;
    }

    @Override
    public Framework getFramework() {
        return null;
    }

    @Override
    public IRundeckProject getRundeckProject() {
        return null;
    }
}
