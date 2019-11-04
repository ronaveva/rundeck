<template>
  <div>
    <modal id="scheduleUploadModal" v-model="showUploadModal" :title="$t('Persist Schedules')" size="lg" :footer=true @hide="close">
      <div class="form-group">
        <label for="scheduleUploadSelect">Select a Schedule definition file.</label>
        <input v-model="scheduleUploadSelect" type="file" name="scheduleUploadSelect" id="scheduleUploadSelect" ref="scheduleUploadSelect" class="form-control" multiple/>
      </div>
    </modal>
  </div>
</template>

<script>

    import Vue from "vue"
    import {persistUploadedDefinition} from "@/pages/project-schedule/scheduleDefinition";
    import {persistUploadedDefinitions} from "../scheduleDefinition";

    export default Vue.extend({
        name: "ScheduleUpload",
        props: ['eventBus'],
        data : function() {
            return {
                showUploadModal: true,
                scheduleUploadSelect: ''
            }
        },
        methods: {
             close(persist){
                if ('ok' == persist){
                    this.scheduleUploadSelect = this.$refs.scheduleUploadSelect.files
                    let formData = new FormData();
                    for (var i = 0; i < this.scheduleUploadSelect.length; i++) {
                        let file = this.scheduleUploadSelect[i];
                        formData.append("scheduleUploadSelect", file);
                    }
                    persistUploadedDefinitions(formData).then(response =>{
                        console.log(response)
                    })
                }
                this.eventBus.$emit('closeUploadDefinitionModal', {})
            }
        }
    })

</script>

<style scoped>

</style>
