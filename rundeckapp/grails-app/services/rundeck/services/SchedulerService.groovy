package rundeck.services

import grails.gorm.transactions.Transactional
import org.quartz.Calendar
import org.quartz.CronScheduleBuilder
import org.quartz.JobBuilder
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.quartz.TriggerKey
import org.springframework.context.ApplicationContextAware
import rundeck.Project
import rundeck.ScheduleDef
import rundeck.ScheduledExecution
import rundeck.quartzjobs.ExecutionJob

@Transactional
class SchedulerService implements ApplicationContextAware{

    Scheduler quartzScheduler
    FrameworkService frameworkService
    JobSchedulerCalendarService jobSchedulerCalendarService

    /**
     * It retrieves all of the schedules that match with the criteria expressed on its params
     * @param project returned schedules belong to this project
     * @param containsName returned schedules contains this string on its name
     * @param paginationParams
     * @return List<ScheduleDef>
     */
    def retrieveProjectSchedulesDefinitionsWithFilters(String projectName, String containsName, Map<String, Integer> paginationParams) {
        if(!containsName) containsName = "";
        def results = ScheduleDef.createCriteria().list (max: paginationParams.max, offset: paginationParams.offset) {
            and {
                like("name", "%"+containsName+"%")
                eq("project", projectName)
            }
            order("name", "asc")
        }
        [
                totalRecords    :results.getTotalCount(),
                schedules       :results
        ]
    }

    def getScheduleDef(name, project){
        ScheduleDef schedDef = ScheduleDef.findOrCreateWhere(name : name, project : project, type: "CRON")
        schedDef.save()
        return schedDef
    }

    def reassociate(scheduleDefId, jobUuidsToAssociate, jobUuidsToDeassociate) {
        def scheduleDef = ScheduleDef.findById(scheduleDefId);

        jobUuidsToAssociate?.each { jobUuid ->
            def scheduledExecution = ScheduledExecution.findByUuid(jobUuid);

            scheduledExecution.addToScheduleDefinitions(scheduleDef);

            try {
                scheduledExecution.save(failOnError: true)
                this.handleScheduleDefinitions(scheduledExecution, true);
            }catch(Exception ex){
                log.error("Persist ScheduledExecution ${scheduleDef} failed when associating a new scheduleDef:",ex)
            }
        }

        jobUuidsToDeassociate?.each { jobUuid ->
            def scheduledExecution = ScheduledExecution.findByUuid(jobUuid);

            scheduledExecution.removeFromScheduleDefinitions(scheduleDef);

            try {
                scheduledExecution.save(failOnError: true)
                this.handleScheduleDefinitions(scheduledExecution, true);
            }catch(Exception ex){
                log.error("Persist ScheduledExecution ${scheduleDef} failed when deassociating a scheduleDef:",ex)
            }
        }


    }

    def persistScheduleDef(Map scheduleDef){
        def currentSchedule = null
        def newSchedule = ScheduleDef.fromMap(scheduleDef)
        def failed = false
        if(scheduleDef.id){
            currentSchedule = ScheduleDef.findById(scheduleDef.id)
            updateScheduleDef(currentSchedule, newSchedule)
        }else{
            currentSchedule = newSchedule
        }
        if(!currentSchedule.validate()){
            failed = true
        }else{
            currentSchedule.save(true)
            for(ScheduledExecution se : currentSchedule.scheduledExecutions){
                handleScheduleDefinitions(se, true)
            }
        }
        return ["schedule":currentSchedule, "failed": failed]
    }

    def delete(Map scheduleMap){
        ScheduleDef sd = ScheduleDef.findById(scheduleMap.id)
        def scheduledExecutions = sd.scheduledExecutions.findAll()
        scheduledExecutions.each {
            it.removeFromScheduleDefinitions(sd)
            handleScheduleDefinitions(it, true)
        }
        sd.delete()
    }

    def updateScheduleDef(ScheduleDef oldSchedule, newSchedule){
        oldSchedule.crontabString = newSchedule.crontabString
        newSchedule.parseCrontabString(newSchedule.crontabString)
        oldSchedule.seconds = newSchedule.seconds
        oldSchedule.minute = newSchedule.minute
        oldSchedule.hour = newSchedule.hour
        oldSchedule.dayOfMonth = newSchedule.dayOfMonth
        oldSchedule.month = newSchedule.month
        oldSchedule.dayOfWeek = newSchedule.dayOfWeek
        oldSchedule.year = newSchedule.year
        oldSchedule.name = newSchedule.name
        oldSchedule.description = newSchedule.description
        return oldSchedule
    }

    /**
     * It removes from quartz scheduler all the schedules that are no longer associated to the job
     * @param ScheduledExecution
     */
    def cleanRemovedScheduleDef(ScheduledExecution scheduledExecution){
        def toDelete = getTriggerNamesToRemoveFromQuartz(scheduledExecution)
        if(toDelete) {
            toDelete?.each {
                quartzScheduler.unscheduleJob(TriggerKey.triggerKey(it, scheduledExecution.generateJobGroupName()))
            }
        }
    }

    /**
     * It returns all the quartz triggers that are no longer associated to the job
     * @param ScheduledExecution
     * @return List<String> quartz job triggers
     */
    def getTriggerNamesToRemoveFromQuartz(scheduledExecution){
        def toDelete = []
        def jobScheduledDefinitionCrons = scheduledExecution?.getJobScheduleDefinitionMap()
        List<Trigger> triggers = quartzScheduler.getTriggersOfJob(JobKey.jobKey(scheduledExecution.generateJobScheduledName(), scheduledExecution.generateJobGroupName()))
        if(jobScheduledDefinitionCrons){
            triggers.each{ Trigger trigger ->
                if(!jobScheduledDefinitionCrons.containsKey(trigger.getKey().name)){
                    toDelete << trigger.getKey().name
                }
            }
        }else{
            triggers.each { Trigger trigger ->
                if(trigger.getKey().name != scheduledExecution?.generateJobScheduledName()){
                    toDelete << trigger.getKey().name
                }
            }
        }
        return toDelete
    }

    /**
     * It handles the cleaning of no longer associated schedule definitions and triggers the new ones
     * @param ScheduledExecution
     * @return boolean it returns true if at least one job was scheduled
     */
    def handleScheduleDefinitions(ScheduledExecution scheduledExecution, isUpdate = false){
        if(scheduledExecution){
            def calendarName = handleJobCalendar(scheduledExecution)
            cleanRemovedScheduleDef(scheduledExecution)
            def jobDetail = quartzScheduler.getJobDetail(JobKey.jobKey(scheduledExecution.generateJobScheduledName(), scheduledExecution.generateJobGroupName()))
            if(!jobDetail){
                jobDetail = createJobDetail(scheduledExecution)
            }
            if(scheduledExecution.scheduleDefinitions){
                Set triggerList = []
                scheduledExecution.getJobScheduleDefinitionMap().each { triggerName, cronExpression ->
                    if(!quartzScheduler.checkExists(TriggerKey.triggerKey(triggerName, scheduledExecution.generateJobGroupName()))){
                        triggerList << createTrigger(scheduledExecution, calendarName, cronExpression, triggerName)
                        log.info("scheduling new trigger for job ${scheduledExecution.generateJobScheduledName()} in project ${scheduledExecution.project} ${scheduledExecution.extid}: ${triggerName}")
                    }else if(isUpdate){
                        triggerList << createTrigger(scheduledExecution, calendarName, cronExpression, triggerName)
                        log.info("scheduling updated trigger for job ${scheduledExecution.generateJobScheduledName()} in project ${scheduledExecution.project} ${scheduledExecution.extid}: ${triggerName}")
                    }
                }
                scheduledExecution.scheduled = true
                quartzScheduler.scheduleJob(jobDetail, triggerList, true)
                def nextTime = nextExecutionTime(scheduledExecution)
                return [scheduled   : true,
                        nextTime    : nextTime
                ]
            }else if(scheduledExecution.generateCrontabExression() && scheduledExecution.shouldScheduleExecution()){
                Set triggerList = []
                def trigger = createTrigger(scheduledExecution, calendarName)
                triggerList << trigger

                def nextTime
                try{
                    nextTime = quartzScheduler.scheduleJob(jobDetail, triggerList, isUpdate)
                }catch(Exception e){
                    log.error(e.getMessage())
                }

                log.info("scheduling trigger for job ${scheduledExecution.generateJobScheduledName()} in project ${scheduledExecution.project} ${scheduledExecution.extid}: " +
                        "${scheduledExecution.generateJobScheduledName()}")
                return [scheduled   : true,
                        nextTime    : nextTime
                ]
            }
        }
        return [scheduled   : false]
    }

    Trigger createTrigger(String jobName, String jobGroup, String cronExpression, int priority = 5, calendarName = null) {
        Trigger trigger
        try {
            trigger = TriggerBuilder.newTrigger().withIdentity(jobName, jobGroup)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                    .withPriority(priority)
                    .build()

        } catch (java.text.ParseException ex) {
            throw new RuntimeException("Failed creating trigger. Invalid cron expression: " + cronExpression )
        }
        return trigger
    }

    Trigger createTrigger(ScheduledExecution se, String calendarName = null, cronExpression = null, triggerName = null) {
        Trigger trigger
        if(!cronExpression){
            cronExpression = se.generateCrontabExression()
        }
        try {

            if(se.timeZone){
                trigger = TriggerBuilder.newTrigger().withIdentity(triggerName? triggerName:se.generateJobScheduledName(), se.generateJobGroupName())
                        .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression).inTimeZone(TimeZone.getTimeZone(se.timeZone)))
                        .modifiedByCalendar(calendarName)
                        .build()
            }else {
                trigger = TriggerBuilder.newTrigger().withIdentity(triggerName? triggerName:se.generateJobScheduledName(), se.generateJobGroupName())
                        .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                        .modifiedByCalendar(calendarName)
                        .build()
            }
        } catch (java.text.ParseException ex) {
            log.error("Failed creating trigger", ex)
            throw new RuntimeException("Failed creating trigger. Invalid cron expression: " + cronExpression )
        }
        return trigger
    }

    JobDetail createJobDetail(ScheduledExecution se, String jobname, String jobgroup) {
        def jobDetailBuilder = JobBuilder.newJob(ExecutionJob)
                .withIdentity(jobname, jobgroup)
                .withDescription(se.description)
                .usingJobData(new JobDataMap(createJobDetailMap(se)))
        return jobDetailBuilder.build()
    }

    JobDetail createJobDetail(ScheduledExecution se) {
        return createJobDetail(se, se.generateJobScheduledName(), se.generateJobGroupName())
    }

    Map createJobDetailMap(ScheduledExecution se) {
        Map data = [:]
        data.put("scheduledExecutionId", se.id.toString())
        data.put("rdeck.base", frameworkService.getRundeckBase())

        if(se.scheduled){
            data.put("userRoles", se.userRoleList)
            if(frameworkService.isClusterModeEnabled()){
                data.put("serverUUID", frameworkService.getServerUUID())
            }
        }

        return data
    }

    /**
     * Return the next scheduled or predicted execution time for the scheduled job, and if it is not scheduled
     * return a time in the future.  If the job is not scheduled on the current server (cluster mode), returns
     * the time that the job is expected to run on its configured server.
     * @param se
     * @return
     */
    Date nextExecutionTime(ScheduledExecution se, boolean require=false) {
        if(!se.scheduled){
            return new Date(ScheduledExecutionService.TWO_HUNDRED_YEARS)
        }
        if(!require && (!se.scheduleEnabled || !se.executionEnabled)){
            return null
        }
        def dates = []

        def triggers = quartzScheduler.getTriggersOfJob(JobKey.jobKey(se.generateJobScheduledName(), se.generateJobGroupName()))
        if(triggers){
            triggers.each {
                if(it.getNextFireTime())
                    dates << it.getNextFireTime()
            }
            if(dates){
                Collections.sort(dates)
                return dates.get(0)
            }
        }else if (frameworkService.isClusterModeEnabled() &&
                se.serverNodeUUID != frameworkService.getServerUUID() || require) {
            //guess next trigger time for the job on the assigned cluster node
            def value= tempNextExecutionTime(se)
            return value
        } else {
            return null;
        }
    }

    /**
     * Return the Date for the next execution time for a scheduled job
     * @param se
     * @return
     */
    Date tempNextExecutionTime(ScheduledExecution se){
        def trigger = createTrigger(se)
        return trigger.getFireTimeAfter(new Date())
    }

    /**
     * It parses the input stream into schedule definitions
     * @param input either an inputStream, a File, or a String
     */
    def parseUploadedFile (input, project){
        def scheduleDefs = input.decodeScheduleDefinitionYAML()
        scheduleDefs.each{
            it.project = project
            it.save()
        }
        return [scheduleDefs : scheduleDefs]
    }

    /**
     * It checks whether a calendar should be applied to the job or not
     * @param ScheduledExecution
     */
    def handleJobCalendar(ScheduledExecution se){
        String calendarName = null

        if(jobSchedulerCalendarService.isCalendarEnable()){
            Map calendarsMap = jobSchedulerCalendarService.getCalendar(se.project, se.uuid)
            if(calendarsMap){
                calendarName = calendarsMap.name
                this.registerCalendar(calendarName,calendarsMap.rundeckCalendar , false )
            }
        }
        return calendarName
    }

    /**
     * It add/update a calendar into the quartz scheduler
     * @param calendarName
     * @param calendar calendar instance
     * @param force it forces the calendar to be added/updated on the quartz scheduler
     */
    def registerCalendar(String calendarName, Calendar calendar, boolean force){
        if(force){
            quartzScheduler.addCalendar(calendarName, calendar, true, false)
        }else{
            if(!quartzScheduler.getCalendar(calendarName)){
                quartzScheduler.addCalendar(calendarName, calendar, false, false)
            }
        }
    }



    /**
     * Return calendars from a scheduled job
     * @param se
     * @return
     */
    List hasCalendars(ScheduledExecution se) {
        if(!se.scheduled){
            return null
        }

        def calendars = []

        def triggers = quartzScheduler.getTriggersOfJob(JobKey.jobKey(se.generateJobScheduledName(), se.generateJobGroupName()))
        if(triggers){
            triggers.each {Trigger trigger->
                if(trigger.calendarName!=null){
                    calendars << trigger.calendarName
                }
            }
        }

        if(calendars.size()==0){
            return null
        }

        return calendars
    }

}
