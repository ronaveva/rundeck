package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.execution.JobLifeCycleException
import com.dtolabs.rundeck.core.jobs.JobLifeCycleEvent
import com.dtolabs.rundeck.core.jobs.JobLifeCycleStatus
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.jobs.JobLifeCyclePlugin
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.dtolabs.rundeck.server.plugins.services.JobLifeCyclePluginProviderService
import org.rundeck.core.projects.ProjectConfigurable
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Provides capability to execute certain task based on job life cycle
 * Created by rnavarro
 * Date: 5/07/19
 * Time: 10:32 AM
 */

public class JobLifeCyclePluginService implements ApplicationContextAware, ProjectConfigurable {

    ApplicationContext applicationContext
    PluginService pluginService
    JobLifeCyclePluginProviderService jobLifeCyclePluginProviderService
    FrameworkService frameworkService
    public static final String CONF_PROJECT_ENABLE_JOB = 'project.enable.jobLifeCycle.on.'

    LinkedHashMap<String, String> configPropertiesMapping
    LinkedHashMap<String, String> configProperties
    List<Property> projectConfigProperties

    enum EventType{
        BEFORE('beforeJob'), AFTER('afterJob')
        private final String value
        EventType(String value){
            this.value = value
        }
        String getValue(){
            this.value
        }
    }

    def loadProperties(){
        List<Property> projectConfigProperties = new ArrayList<Property>()
        LinkedHashMap<String, String> configPropertiesMapping = []
        LinkedHashMap<String, String> configProperties = []
        listJobLifeCyclePlugins().each { String name, DescribedPlugin describedPlugin ->
            projectConfigProperties.add(
                    PropertyBuilder.builder().with {
                        options 'jobLifeCycle' + name
                        title 'Enable ' + name
                        values(EventType.values()*.value)
                        labels([(EventType.BEFORE.value): "Run before job starts", (EventType.AFTER.value): "Run after job ends"])
                        required(false)
                        defaultValue null
                    }.build()
            )
            configPropertiesMapping.put('jobLifeCycle' + name , CONF_PROJECT_ENABLE_JOB + name)
            configProperties.put('jobLifeCycle' + name, 'jobLifeCycle')
        }
        this.configPropertiesMapping = configPropertiesMapping
        this.configProperties = configProperties
        this.projectConfigProperties = projectConfigProperties
    }

    @Override
    Map<String, String> getCategories() {
        loadProperties()
        configProperties
    }

    @Override
    Map<String, String> getPropertiesMapping() {
        loadProperties()
        configPropertiesMapping
    }

    @Override
    List<Property> getProjectConfigProperties() {
        loadProperties()
        projectConfigProperties
    }

    ValidatedPlugin validatePluginConfig(String project, String name, Map config) {
        return pluginService.validatePlugin(name, jobLifeCyclePluginProviderService,
                frameworkService.getFrameworkPropertyResolver(project, config), PropertyScope.Instance, PropertyScope.Project)
    }
    ValidatedPlugin validatePluginConfig(String name, Map projectProps, Map config) {
        return pluginService.validatePlugin(name, jobLifeCyclePluginProviderService,
                frameworkService.getFrameworkPropertyResolverWithProps(projectProps, config), PropertyScope.Instance, PropertyScope.Project)
    }

    /**
     *
     * @param name
     * @return map containing [instance:(plugin instance), description: (map or Description), ]
     */
    DescribedPlugin getJobLifeCycleDescriptor(String name) {
        return pluginService.getPluginDescriptor(name, jobLifeCyclePluginProviderService)
    }

    Map listJobLifeCyclePlugins(){
        return pluginService.listPlugins(JobLifeCyclePlugin, jobLifeCyclePluginProviderService)
    }

    JobLifeCycleStatus beforeJobStarts(JobLifeCycleEvent event){
        executeLifeCycle(event, EventType.BEFORE)
    }

    JobLifeCycleStatus afterJobEnds(JobLifeCycleEvent event){
        executeLifeCycle(event, EventType.AFTER)
    }

    def executeLifeCycle(def event, def eventType){
        JobLifeCycleStatus result = new JobLifeCycleStatus()
        String projectName = event.getExecutionContext()?.getFrameworkProject()
        IRundeckProject rundeckProject = event.getExecutionContext()?.getFramework()?.getProjectManager()?.getFrameworkProject(projectName)
        def jlcps = listJobLifeCyclePlugins()
        jlcps.each { String name, DescribedPlugin describedPlugin ->
            if (rundeckProject?.hasProperty(CONF_PROJECT_ENABLE_JOB + name) &&
                    Arrays.asList(rundeckProject?.getProperty(CONF_PROJECT_ENABLE_JOB + name).split("\\s*,\\s*")).contains(eventType.getValue())) {
                try {
                    JobLifeCyclePlugin plugin = (JobLifeCyclePlugin) describedPlugin.instance
                    if(eventType.equals(EventType.BEFORE)){
                        result = plugin.beforeJobStarts(event)
                    }else if(eventType.equals(EventType.AFTER)) {
                        result = plugin.afterJobEnds(event)
                    }
                    if (result != null && !result.isSuccessful()) {
                        log.info("Result from plugin is false an exception will be thrown")
                        if (result.getDescription() != null && !result.getDescription().trim().isEmpty()) {
                            throw new JobLifeCycleException(result.getDescription())
                        } else {
                            throw new JobLifeCycleException('Response from ' + name + ' is false, but no description was provided by the plugin')
                        }
                    }
                } catch (JobLifeCycleException e) {
                    throw e
                } catch (Exception e) {
                    log.error(e.getMessage(), e)
                    throw e
                }
            }
        }
        result
    }

}
