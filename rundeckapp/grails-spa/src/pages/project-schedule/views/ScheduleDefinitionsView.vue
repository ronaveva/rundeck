<template>
  <div v-if="project">
    <div class="col-xs-12">
      <div class="card">
        <div class="input-group">
          <input type="search" name="name" placeholder="Project Schedule search: type name" class="form-control input-sm" v-model="searchFilters.name"/> <!-- i18n here -->
          <span class="input-group-addon"><i class="glyphicon glyphicon-search "></i></span>
        </div>
        <div>
          <ul>
            <div v-if="loading" class="project_list_item">
              <b class="fas fa-spinner fa-spin loading-spinner text-muted fa-2x"></b>
            </div>
            <div class="project_list_item" data-bind="attr: { 'data-project': project }, " v-for="projectSchedule in scheduledDefinitions">
              <div class="row row-hover row-border-top">
                <div class="col-sm-6 col-md-8">
                  <a href="${g.createLink(action:'',controller:'menu',params:[project:'<$>'])}" class="text-h3  link-hover  text-inverse project_list_item_link">
                    <span>{{projectSchedule.name}}</span>
                    <span class="text-secondary text-base"><em>{{projectSchedule.description}}</em></span>
                  </a>
                </div>
                <div class="col-sm-6 col-md-2 text-center">
                  <span>{{getCronExpression(projectSchedule)}}</span>
                </div>
                <div class="col-sm-12 col-md-2" >
                  <div class="pull-right">
                    <div class="btn-group dropdown-toggle-hover"> <!-- TODO: Permission checks to display appropriate options-->
                      <a href="#" class="as-block link-hover link-block-padded text-inverse dropdown-toggle" data-toggle="dropdown">
                        <span>{{$t("button.actions")}}</span>
                        <span class="caret"></span>
                      </a>
                      <ul class="dropdown-menu pull-right" role="menu">
                        <li>
                          <a href="#"
                             @click="openSchedulePersistModal(projectSchedule)">
                            <span>{{$t("button.editSchedule")}}</span>
                          </a>
                        </li>
                        <li class="divider"></li>
                        <li>
                          <a href="#" @click="openScheduleAssign(projectSchedule)">
                            <i class="glyphicon glyphicon-plus"></i>
                            <span>{{$t("button.assignToJobs")}}</span>
                          </a>
                        </li>
                        <li class="divider"></li>
                        <li>
                          <a href="#" @click="deleteSchedule(projectSchedule)">
                            <i class="glyphicon glyphicon-minus"></i>
                            <span>{{$t("button.deleteSchedule")}}</span>
                          </a>
                        </li>
                      </ul>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <offset-pagination
              :pagination="pagination"
              @change="changePageOffset($event)"
              :disabled="loading"
              :showPrefix="false"
            ></offset-pagination>
          </ul>
        </div>
      </div>
      <br>
      <div class="row" >
        <div class=" pull-right">
          <button href="#" class="btn btn-default" @click="openUploadDefinitionModal()">
            <i class="glyphicon glyphicon-upload"></i>
            <span>{{$t("button.uploadScheduleDefinitions")}}</span>
          </button>
        </div>
        <div class=" pull-right">
          <button href="#" class="btn btn-default" @click="openSchedulePersistModal(null)"> <!-- TODO: add permission checks -->
            <i class="glyphicon glyphicon-upload"></i>
            <span>{{$t("button.createDefinition")}}</span>
          </button>
        </div>
      </div>
    </div>
    <schedule-assign v-if="showScheduleAssign" v-bind:schedule="activeSchedule" v-bind:event-bus="eventBus"/>

    <schedule-persist
      v-if="showEditSchedule"
      v-bind:event-bus="eventBus"
      v-bind:schedule="activeSchedule">
    </schedule-persist>
    <schedule-upload
      v-if="this.showUploadDefinitionModal"
      v-bind:event-bus="eventBus">
    </schedule-upload>
  </div>
</template>


<script>

    import axios from 'axios'
    import OffsetPagination from '@rundeck/ui-trellis/src/components/utils/OffsetPagination.vue'
    import SchedulePersist from './SchedulePersist.vue'
    import {
        getRundeckContext,
        RundeckContext
    } from "@rundeck/ui-trellis"
    import ScheduleAssign from "@/pages/project-schedule/views/ScheduleAssign.vue"
    import Vue from "vue"
    import ScheduleUtils from "../utils/ScheduleUtils"
    import { getAllProjectSchedules, ScheduleDefinition, ScheduleSearchResult} from "../scheduleDefinition";
    import ScheduleUpload from "./ScheduleUpload";

    export default Vue.extend({
        name: 'ScheduleDefinitionsView',
        props: [ 'eventBus' ],
        components:{
            ScheduleUpload,
            ScheduleAssign,
            OffsetPagination,
            SchedulePersist
        },
        data : function() {
            return {
                showUploadDefinitionModal: false,
                scheduleSearchResult: null,
                showEditSchedule: false,
                scheduledDefinitions: null,
                loading: false,
                project: "",
                rdBase: "",
                filteredProjectSchedules: "",
                searchFilters: {
                    name: ""
                },
                pagination:{
                    offset:0,
                    max:100,
                    total:-1
                },

                //element control
                showScheduleAssign: false,
                activeSchedule: {}
            }
        },
        async mounted() {
            if (window._rundeck && window._rundeck.rdBase && window._rundeck.projectName) {
                this.project = window._rundeck.projectName
                await this.updateSearchResults(0);
            }
            this.eventBus.$on('SCHEDULE_ASSIGN_CLOSING', (payload) => {
                console.log("RECEIVED CLOSE EVENT")
                this.doCloseScheduleAssign();
            });
            this.eventBus.$on('closeSchedulePersistModal', (payload) =>{
                this.closeSchedulePersistModal(payload.reload)
            });
            this.eventBus.$on('closeUploadDefinitionModal', (payload) =>{
                this.closeUploadDefinitionModal()
            });
        },
        watch: {
            'searchFilters.name': function(val, preVal){
                this.updateSearchResults(0);
            }
        },
        methods: {
            changePageOffset(offset) {
                if (this.loading) {
                    return;
                }
                this.updateSearchResults(offset)
            },
            async updateSearchResults(offset) {
                this.loading = true;
                try{
                    this.scheduleSearchResult = await getAllProjectSchedules(this.pagination.offset, this.searchFilters.name)
                    this.scheduledDefinitions = this.scheduleSearchResult.schedules
                    this.pagination.offset = this.scheduleSearchResult.offset
                    this.pagination.max = this.scheduleSearchResult.maxRows
                    this.pagination.total = this.scheduleSearchResult.totalRecords
                }catch(err){
                    //TODO: handle the error
                }
                this.loading = false
            },
            openScheduleAssign(schedule) {
                this.assignActiveSchedule(schedule)
                this.showScheduleAssign = true;
            },
            assignActiveSchedule(schedule){
                this.activeSchedule = schedule;
            },
            doCloseScheduleAssign() {
                this.showScheduleAssign = false;
                this.updateSearchResults(this.pagination.offset)
            },
            openSchedulePersistModal(schedule){
                this.assignActiveSchedule(schedule)
                this.showEditSchedule = true
            },
            closeSchedulePersistModal(reload){
                this.showEditSchedule = false
                this.updateSearchResults(this.pagination.offset)
            },
            getCronExpression(schedule){
                return ScheduleUtils.getCronExpression(schedule)
            },
            deleteSchedule(schedule){
                axios({
                    method: 'post',
                    headers: {'x-rundeck-ajax': true},
                    url: `/projectSchedules/deleteSchedule`,
                    params: {
                        project: window._rundeck.projectName
                    },
                    data: {
                        schedule: schedule
                    },
                    withCredentials: true
                }).then((response) => {
                    this.updateSearchResults(this.pagination.offset)
                })
            },
            openUploadDefinitionModal(){
                this.showUploadDefinitionModal = true
            },
            closeUploadDefinitionModal(){
                this.showUploadDefinitionModal = false
            }

        }
    });
</script>

<style>
</style>
