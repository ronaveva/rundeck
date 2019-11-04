export default {

  getCronExpression : function (scheduleDef){
    return [scheduleDef.schedule.seconds?scheduleDef.schedule.seconds:'0',scheduleDef.schedule.minute,scheduleDef.schedule.hour,scheduleDef.schedule.dayOfMonth.toUpperCase(),scheduleDef.schedule.month.toUpperCase(),scheduleDef.schedule.dayOfMonth=='?'?scheduleDef.schedule.dayOfWeek.toUpperCase():'?',scheduleDef.schedule.year?scheduleDef.schedule.year:'*'].join(" ")
  }
};
