/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import grails.converters.JSON
import org.rundeck.core.auth.AuthConstants
import org.springframework.context.ApplicationContext
import org.springframework.web.multipart.MultipartFile
import rundeck.services.AuthorizationService

import rundeck.services.FrameworkService

class ProjectSchedulesController extends ControllerBase{

    FrameworkService frameworkService
    def AuthorizationService authorizationService
    def ApplicationContext applicationContext
    def schedulerService
    static allowedMethods = [
            //deleteFilter    : 'POST',
    ]

    def index(){
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, params.project)

        if (unauthorizedResponse(
                frameworkService.authorizeProjectResourceAll(
                        authContext,
                        AuthorizationUtil.resourceType('event'),
                        [AuthConstants.ACTION_READ],
                        params.project
                ),
                AuthConstants.ACTION_ADMIN,
                'schedules',
                params.project
        )) {
            return
        }
    }

    def reassociate() {
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, params.project)
        if (unauthorizedResponse(
                frameworkService.authorizeProjectResourceAll(
                        authContext,
                        AuthorizationUtil.resourceType('event'),
                        [AuthConstants.ACTION_READ],
                        params.project
                ),
                AuthConstants.ACTION_ADMIN,
                'schedules',
                params.project
        )) {
            return
        }
        schedulerService.reassociate( request.JSON.scheduleDefId, request.JSON.jobUuidsToAssociate, request.JSON.jobUuidsToDeassociate );

        render(contentType:'application/json',text:
                ([
                        result: 'ok'
                ] )as JSON
        )
    }

    def filteredProjectSchedules() {
        def offset = 0
        if(params.offset){
            offset = params.offset
        }

        int max = 10

        def result = schedulerService.retrieveProjectSchedulesDefinitionsWithFilters(params.project, params.name, [max: max, offset: offset])
        result?.schedulesMap = result?.schedules?.collect{
            return it.toMap()
        }
        //TODO: implement auth
        render(contentType:'application/json',text:
                ([
                        schedules       : result.schedulesMap,
                        totalRecords    : result.totalRecords,
                        offset          : offset,
                        maxRows         : max,
                        schedulesMap    : result.schedulesMap

                ] )as JSON
        )
    }

    def persistSchedule(){
        withForm{
            AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, params.project)
            if (unauthorizedResponse(
                    frameworkService.authorizeProjectResourceAll(
                            authContext,
                            AuthorizationUtil.resourceType('event'),
                            [AuthConstants.ACTION_READ],
                            params.project
                    ),
                    AuthConstants.ACTION_ADMIN,
                    'schedules',
                    params.project
            )) {
                return
            }
            def result = schedulerService.persistScheduleDef(request.JSON.schedule)
            def errors
            if(result.failed){
                errors = result.schedule.errors.allErrors.collect {g.message(error: it)}.join(", ")
            }
            render(contentType:'application/json',text:
                    ([
                            schedule        : result.schedule,
                            errors          : errors,
                            failed          : result.failed

                    ] )as JSON
            )
        }.invalidToken{
            request.errorCode='request.error.invalidtoken.message'
            renderErrorView([:])
        }
    }

    def deleteSchedule(){
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, params.project)
        if (unauthorizedResponse(
                frameworkService.authorizeProjectResourceAll(
                        authContext,
                        AuthorizationUtil.resourceType('event'),
                        [AuthConstants.ACTION_READ],
                        params.project
                ),
                AuthConstants.ACTION_ADMIN,
                'schedules',
                params.project
        )) {
            return
        }
        schedulerService.delete(request.JSON.schedule)
        render(contentType:'application/json',text:
                ([
                        result: 'ok'
                ] )as JSON
        )
    }

    def uploadFileDefinition (){
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, params.project)
        if (unauthorizedResponse(
                frameworkService.authorizeProjectResourceAll(
                        authContext,
                        AuthorizationUtil.resourceType('event'),
                        [AuthConstants.ACTION_READ],
                        params.project
                ),
                AuthConstants.ACTION_ADMIN,
                'schedules',
                params.project
        )) {
            return
        }
        def failed = false
        def file = request.getFile("scheduleUploadSelect")
        if (!file || file.empty) {
            request.message = "No file was uploaded."
            failed = true
        }

        def result
        if(!failed){
            result = schedulerService.parseUploadedFile(file.getInputStream(), params.project)
        }
        render(contentType:'application/json',text:
                ([
                        result: failed
                ] )as JSON
        )
    }

}

class ScheduleDefYAMLException extends Exception{

    public ScheduleDefYAMLException() {
        super();
    }

    public ScheduleDefYAMLException(String s) {
        super(s);
    }

    public ScheduleDefYAMLException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ScheduleDefYAMLException(Throwable throwable) {
        super(throwable);
    }

}