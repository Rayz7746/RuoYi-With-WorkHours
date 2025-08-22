<template>
  <div class="week-calendar">
    <div class="cal-header">
      <el-button text size="small" @click="shiftMonth(-1)" :icon="ArrowLeft" />
      <span class="month-label">{{ displayMonth }}</span>
      <el-button text size="small" @click="shiftMonth(1)" :icon="ArrowRight" />
    </div>
    <div class="grid head">
      <div v-for="w in weekLabels" :key="w" class="cell head-cell">{{ w }}</div>
    </div>
    <div class="weeks-wrapper">
      <div
        v-for="(week, wi) in matrix"
        :key="wi"
        class="grid week-row"
        :class="{ active: isSameWeek(week[0], modelValue) }"
        @click="pickWeek(week)"
      >
        <div
          v-for="(day, di) in week"
          :key="di"
          class="cell day-cell"
          :class="{
            outside: day.getMonth() !== baseMonth.getMonth(),
            today: isToday(day),
            weekend: isWeekend(day),
            sel: isSameDay(day, modelValue)
          }"
          @click.stop="selectDay(day)"
        >
          <span>{{ day.getDate() }}</span>
        </div>
      </div>
    </div>
    <div class="footer-tools">
      <el-button size="small" text @click="goToday">今天</el-button>
      <el-button size="small" text type="primary" @click="emitWeekStart">选中周</el-button>
    </div>
  </div>
</template>

<script setup name="WeekCalendar">
import { ref, computed, watch } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: Date, default: () => new Date() }, // 任意落在一周中的日期
  mondayFirst: { type: Boolean, default: true }
})
const emit = defineEmits(['update:modelValue', 'pick-week'])

const baseMonth = ref(new Date(props.modelValue.getFullYear(), props.modelValue.getMonth(), 1))
watch(() => props.modelValue, v => {
  baseMonth.value = new Date(v.getFullYear(), v.getMonth(), 1)
})

const weekLabels = props.mondayFirst
  ? ['一','二','三','四','五','六','日']
  : ['日','一','二','三','四','五','六']

function startOfCalendar(first) {
  const fd = new Date(first)
  const w = fd.getDay() || 7
  if (props.mondayFirst) fd.setDate(1 - (w - 1))
  else fd.setDate(1 - w)
  fd.setHours(0,0,0,0)
  return fd
}
function addDays(d,n){ const nd=new Date(d); nd.setDate(nd.getDate()+n); return nd }
function isSameDay(a,b){return a&&b&&a.getFullYear()===b.getFullYear()&&a.getMonth()===b.getMonth()&&a.getDate()===b.getDate()}
function isWeekend(d){const g=d.getDay(); return props.mondayFirst? (g===0||g===6) : (g===0||g===6)}
function isToday(d){return isSameDay(d,new Date())}
function startOfWeek(d){
  const dd=new Date(d)
  let g=dd.getDay(); if(g===0) g=7
  if(props.mondayFirst) dd.setDate(dd.getDate()-(g-1))
  else dd.setDate(dd.getDate()-g)
  dd.setHours(0,0,0,0)
  return dd
}
function isSameWeek(a,b){
  if(!a||!b) return false
  return startOfWeek(a).getTime()===startOfWeek(b).getTime()
}

const matrix = computed(()=>{
  const first = new Date(baseMonth.value)
  const start = startOfCalendar(first)
  const weeks = []
  for(let w=0; w<6; w++){
    const row=[]
    for(let d=0; d<7; d++) row.push(addDays(start, w*7+d))
    weeks.push(row)
  }
  return weeks
})

const displayMonth = computed(()=> `${baseMonth.value.getFullYear()}年${baseMonth.value.getMonth()+1}月`)

function selectDay(day){ emit('update:modelValue', new Date(day)) }
function pickWeek(week){
  // 找到周一 (或周起始) 作为代表
  let rep = week.find(d=> d.getDay()===1) || week[0]
  if(!props.mondayFirst){ rep = week[0] }
  emit('update:modelValue', new Date(rep))
  emit('pick-week', startOfWeek(rep))
}
function emitWeekStart(){
  emit('pick-week', startOfWeek(props.modelValue))
}
function goToday(){ emit('update:modelValue', new Date()) }
function shiftMonth(n){
  baseMonth.value = new Date(baseMonth.value.getFullYear(), baseMonth.value.getMonth()+n,1)
}
</script>

<style scoped>
.week-calendar{border:1px solid var(--el-border-color);border-radius:6px;padding:6px 6px 4px;background:#fff;display:flex;flex-direction:column;width:240px;}
.cal-header{display:flex;align-items:center;justify-content:space-between;font-weight:600;margin-bottom:4px;font-size:13px;}
.month-label{user-select:none;}
.grid{display:grid;grid-template-columns:repeat(7,1fr);} 
.head-cell{font-size:12px;font-weight:600;text-align:center;padding:4px 0;color:#606266;}
.week-row{border-radius:4px;margin-bottom:2px;cursor:pointer;transition:background .15s;}
.week-row.active{background:#ecf5ff;box-shadow:0 0 0 1px #409eff inset;}
.cell{height:30px;display:flex;align-items:center;justify-content:center;font-size:12px;position:relative;}
.day-cell.today{color:#409eff;font-weight:600;}
.day-cell.sel{background:#409eff;color:#fff;border-radius:4px;}
.day-cell.weekend{color:#f56c6c;}
.day-cell.outside{color:#b9c0c8;}
.day-cell:hover{background:rgba(64,158,255,.12);}
.footer-tools{display:flex;justify-content:space-between;margin-top:4px;padding-top:4px;border-top:1px dashed var(--el-border-color-lighter);} 
</style>
