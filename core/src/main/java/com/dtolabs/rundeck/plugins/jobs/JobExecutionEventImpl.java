package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.IRundeckProject;
import com.dtolabs.rundeck.core.execution.ExecutionLogger;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.jobs.JobLifeCycleEvent;

import java.util.Map;

public class JobExecutionEventImpl implements JobExecutionEvent {

    private StepExecutionContext executionContext;

    @Override
    public StepExecutionContext getExecutionContext() {
        return this.executionContext;
    }

    public void setExecutionContext(StepExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public JobExecutionEventImpl(StepExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public JobExecutionEventImpl() {
    }

    @Override
    public Map<String, String> getOptions() {
        if (this.executionContext != null && this.executionContext.getDataContext() != null
            && !this.executionContext.getDataContext().isEmpty()
            && this.executionContext.getDataContext().containsKey("option")) {
            return this.executionContext.getDataContext().get("option");
        }
        return null;
    }

    @Override
    public ExecutionLogger getExecutionLogger() {
        if (this.executionContext != null){
            return this.executionContext.getExecutionLogger();
        }
        return null;
    }

    @Override
    public String getUserName() {
        if (this.executionContext != null && this.executionContext.getDataContext() != null
            && !this.executionContext.getDataContext().isEmpty()
            && this.executionContext.getDataContext().containsKey("job")){
            return this.executionContext.getDataContext().get("job").get("user.name");
        }
        return null;
    }

    @Override
    public String getExecutionId() {
        if (this.executionContext != null && this.executionContext.getDataContext() != null
                && !this.executionContext.getDataContext().isEmpty()
                && this.executionContext.getDataContext().containsKey("job")){
            return this.executionContext.getDataContext().get("job").get("execid");
        }
        return null;
    }

    @Override
    public String getProjectName() {
        if (this.executionContext != null){
            return this.executionContext.getFrameworkProject();
        }
        return null;
    }

    @Override
    public Framework getFramework() {
        if (this.executionContext != null){
            return this.executionContext.getFramework();
        }
        return null;
    }

    @Override
    public IRundeckProject getRundeckProject() {
        if(this.executionContext != null && this.getFramework() != null
            && this.getFramework().getProjectManager() != null && this.getProjectName() != null){
            return this.getFramework().getProjectManager().getFrameworkProject(this.getProjectName());
        }
        return null;
    }
}
