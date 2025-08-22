<template>
  <div class="workhours-wrapper app-container">
    <div class="left-side">
      <week-calendar v-model="selectedDate" @pick-week="onPickWeek" />
      <el-card class="legend-card" shadow="never">
        <template #header>
          <div class="card-header">说明</div>
        </template>
        <ul class="legend-list">
          <li>点击周行或日期选择一周</li>
          <li>右侧输入每天工时 (0~24, 0.5 步长)</li>
          <li>周合计、日合计自动计算</li>
        </ul>
        <el-switch v-model="showWeekend" active-text="含周末" inactive-text="工作日" size="small" />
      </el-card>
    </div>

    <div class="main-side">
      <el-card shadow="never" class="toolbar-card">
        <div class="toolbar">
          <div class="left-actions">
            <el-button class="nav-btn prev" size="small" @click="shiftWeek(-1)">
              <el-icon><ArrowLeft /></el-icon>
              <span>上一周</span>
            </el-button>
            <el-button class="nav-btn today" size="small" @click="jumpToday">今天</el-button>
            <el-button class="nav-btn next" size="small" @click="shiftWeek(1)">
              <span>下一周</span>
              <el-icon><ArrowRight /></el-icon>
            </el-button>
            <span class="week-range">{{ weekRangeLabel }}</span>
          </div>
          <div class="right-actions">
            <el-button size="small" @click="clearWeek" type="warning" plain>清空本周</el-button>
            <el-button size="small" @click="resetAll" type="danger" plain>全部清零</el-button>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="table-card">
        <!-- 移除顶部日合计条，改为表格 summary 行 -->
        <el-table
          :data="tableRows"
          border
          size="small"
          :span-method="spanMethod"
          class="hours-table enhanced"
          header-cell-class-name="hours-head-cell"
          :row-class-name="rowCls"
          show-summary
          :summary-method="summaryMethod"
        >
          <el-table-column prop="customerName" label="客户" width="140" />
          <el-table-column prop="projectName" label="项目" min-width="220" />
          <el-table-column
            v-for="d in weekDates"
            :key="d.toDateString()"
            :min-width="150"
          >
            <template #header>
              <div class="day-col-head" :class="{ weekend: isWeekend(d) }">
                <span class="wk">{{ weekday(d) }}</span>
                <span class="dt">{{ formatMD(d) }}</span>
              </div>
            </template>
            <template #default="{ row }">
              <div
                class="cell-wrapper"
                :class="{ weekend: isWeekend(d), 'has-note': hasComment(row.projectId,d) }"
              >
                <el-input-number
                  v-model="hours[row.projectId][dateKey(d)]"
                  :min="0" :max="24" :step="0.5"
                  size="small"
                  controls-position="right"
                />
                <el-tooltip
                  content="添加备注"
                  placement="top"
                  :show-after="300"
                >
                  <el-button
                    link
                    size="small"
                    class="note-btn"
                    :type="hasComment(row.projectId,d)?'primary':'default'"
                    @click.stop="openComment(row.projectId,d)"
                  >
                    <el-icon><ChatDotRound /></el-icon>
                  </el-button>
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="合计" width="110" fixed="right">
            <template #header>
              <div class="sum-head">合计</div>
            </template>
            <template #default="{ row }">
              <span class="row-total badge">{{ rowTotal(row.projectId).toFixed(1) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 移除底部空白备注卡，只保留弹窗 -->
      <el-dialog
        v-model="editing.open"
        :title="editingTitle"
        width="420px"
        append-to-body
        class="cell-comment-dialog"
      >
        <el-input
          v-model="editing.text"
          type="textarea"
          :rows="5"
          maxlength="200"
          show-word-limit
          placeholder="填写该项目该天的说明 (可留空)"
        />
        <template #footer>
          <div class="dialog-footer">
            <el-button size="small" @click="clearEditing">清空</el-button>
            <el-button size="small" @click="editing.open=false">取消</el-button>
            <el-button size="small" type="primary" @click="saveEditing">保存</el-button>
          </div>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup name="SimpleWorkHours">
import { ref, reactive, computed } from 'vue'
import WeekCalendar from './WeekCalendar.vue'
import { ArrowLeft, ArrowRight, ChatDotRound } from '@element-plus/icons-vue'

/* ===== Mock 数据（客户 -> 项目） ===== */
const customers = [
  { customerId: 'C-ZW', customerName: '专网', projects: [
    { projectId: 'P-ZW-LOGIN', projectName: '综合登录&智能运维' },
    { projectId: 'P-ZW-DEVICE', projectName: '设备资源&线路资源' }
  ]},
  { customerId: 'C-CM', customerName: '中国移动', projects: [ { projectId: 'P-RCMP', projectName: 'RCMP' } ] },
  { customerId: 'C-PROV', customerName: '省项目', projects: [
    { projectId: 'P-SH', projectName: '上海重接' },
    { projectId: 'P-ZJ', projectName: '浙江运控中心' }
  ]},
  { customerId: 'C-INTERNAL', customerName: '内部', projects: [
    { projectId: 'P-LEARN', projectName: '学习时间' },
    { projectId: 'P-HG2', projectName: '设备网管' }
  ]},
  { customerId: 'C-HOL', customerName: '休假', projects: [ { projectId: 'P-HOL', projectName: '休假' } ] }
]

/* ===== 状态 ===== */
const selectedDate = ref(new Date())
const showWeekend = ref(true)
const comment = ref('')
const hours = reactive({})
customers.forEach(c => c.projects.forEach(p => { hours[p.projectId] = {} }))

/* ===== 评论存储 ===== */
const comments = reactive({})
customers.forEach(c=> c.projects.forEach(p=> { comments[p.projectId] = {} }))

const editing = ref({ open:false, projectId:'', date:null, text:'' })
const editingTitle = computed(()=>{
  if(!editing.value.date) return '备注'
  return `备注 - ${projectNameById(editing.value.projectId)} ${formatMD(editing.value.date)}`
})

/* ===== 工具函数 ===== */
function startOfWeek(d) { const tmp = new Date(d); let g = tmp.getDay() || 7; tmp.setDate(tmp.getDate() - (g - 1)); tmp.setHours(0,0,0,0); return tmp }
function addDays(d,n){ const nd=new Date(d); nd.setDate(nd.getDate()+n); return nd }
function dateKey(d){ return d.toISOString().slice(0,10) }
function formatMD(d){ return `${d.getMonth()+1}/${d.getDate()}` }
function weekday(d){ return ['周一','周二','周三','周四','周五','周六','周日'][ (d.getDay()||7)-1 ] }
function isWeekend(d){ const g=d.getDay(); return g===0||g===6 }
function projectNameById(pid){
  for(const c of customers){
    const f = c.projects.find(p=>p.projectId===pid)
    if(f) return f.projectName
  }
  return pid
}

/* ===== Week 计算 ===== */
const weekDates = computed(()=>{ const start = startOfWeek(selectedDate.value); const len = showWeekend.value?7:5; return Array.from({length:len},(_,i)=>addDays(start,i)) })
const weekRangeLabel = computed(()=>`${formatMD(weekDates.value[0])} ~ ${formatMD(weekDates.value[weekDates.value.length-1])}`)

/* ===== 表格数据 ===== */
const tableRows = computed(()=>{ const rows=[]; customers.forEach(c=> c.projects.forEach(p=> rows.push({ customerId:c.customerId, customerName:c.customerName, projectId:p.projectId, projectName:p.projectName })) ); return rows })
function spanMethod({ column, rowIndex, row }) { if(column.property==='customerName'){ const id=row.customerId; let first=-1,count=0; tableRows.value.forEach((r,i)=>{ if(r.customerId===id){ if(first<0) first=i; count++ } }); if(rowIndex===first) return {rowspan:count,colspan:1}; return {rowspan:0,colspan:0} } }

/* ===== 汇总 ===== */
function rowTotal(pid){ return weekDates.value.reduce((s,d)=>{ const v=parseFloat(hours[pid][dateKey(d)]); return s+(isNaN(v)?0:v)},0) }
function dayTotal(d){ const k=dateKey(d); return Object.keys(hours).reduce((s,pid)=>{ const v=parseFloat(hours[pid][k]); return s+(isNaN(v)?0:v)},0) }
const grandTotal = computed(()=> weekDates.value.reduce((s,d)=> s+dayTotal(d),0))
function summaryMethod(){
  const sums = []
  sums[0] = '日合计'
  sums[1] = ''
  weekDates.value.forEach((d,i)=>{ sums[2+i] = dayTotal(d).toFixed(1) })
  sums[2 + weekDates.value.length] = grandTotal.value.toFixed(1)
  return sums
}

/* ===== Actions ===== */
function shiftWeek(n){ selectedDate.value = addDays(selectedDate.value, n*7) }
function jumpToday(){ selectedDate.value = new Date() }
function clearWeek(){ weekDates.value.forEach(d=>{ const k=dateKey(d); Object.values(hours).forEach(h=>{ delete h[k] }) }) }
function resetAll(){ Object.keys(hours).forEach(pid=> hours[pid] = {}) }
function onPickWeek(start){ selectedDate.value = new Date(start) }
function shortWeekday(d){ return ['一','二','三','四','五','六','日'][ (d.getDay()||7)-1 ] }
function weekdayHeader(d){ return `${weekday(d)}\n${formatMD(d)}` }
function rowCls(){ return 'data-row' }

function openComment(pid,d){
  editing.value.projectId = pid
  editing.value.date = new Date(d)
  editing.value.text = comments[pid]?.[dateKey(d)] || ''
  editing.value.open = true
}
function saveEditing(){
  const key = dateKey(editing.value.date)
  if(!comments[editing.value.projectId]) comments[editing.value.projectId] = {}
  comments[editing.value.projectId][key] = editing.value.text.trim()
  if(!comments[editing.value.projectId][key]) delete comments[editing.value.projectId][key]
  editing.value.open = false
}
function clearEditing(){ editing.value.text=''; saveEditing() }
function hasComment(pid,d){ return !!comments[pid]?.[dateKey(d)] }
</script>

<style scoped>
/* Layout */
.workhours-wrapper{display:flex;gap:20px;align-items:flex-start;}
.left-side{width:230px;display:flex;flex-direction:column;gap:12px;position:sticky;top:70px;}
.main-side{flex:1;display:flex;flex-direction:column;gap:16px;min-width:0;}

/* Toolbar */
.toolbar{display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:14px;}
.left-actions{display:flex;align-items:center;gap:10px;flex-wrap:wrap;}
.week-range{font-weight:600;font-size:14px;color:#2c3e50;padding:4px 10px;background:#f2f6fc;border:1px solid #e3e8ef;border-radius:20px;}
.nav-btn{display:flex;align-items:center;gap:4px;background:#fff;border:1px solid #d9e2ec !important;}
.nav-btn.prev .el-icon, .nav-btn.next .el-icon{font-size:14px;}
.nav-btn.today{font-weight:600;}
.nav-btn:hover{background:#f5f9ff;color:#409eff;}

/* Table aesthetics */
.hours-table{--cell-padding:8px 10px;}
.hours-table :deep(.el-table__inner-wrapper){border-radius:8px;overflow:hidden;}
.hours-table :deep(.el-table__header th){background:linear-gradient(180deg,#f8fafc,#eef2f6);font-weight:600;font-size:13px;}
.hours-table :deep(.el-table__row.data-row td){background:#fff;}
.hours-table :deep(.el-table__row.data-row:hover td){background:#f5faff !important;}
.hours-table :deep(.el-table__cell){font-size:14px;}
.day-col-head{display:flex;flex-direction:column;align-items:center;line-height:1.1;padding:2px 0;}
.day-col-head.weekend{color:#e45656;}
.day-col-head .wk{font-weight:600;font-size:13px;}
.day-col-head .dt{font-size:12px;color:#636e7b;}
.cell-wrapper{position:relative;display:flex;align-items:center;justify-content:flex-start;padding:6px 34px 6px 6px;min-height:56px;background:#fff;border:1px solid #f0f3f6;border-radius:6px;transition:box-shadow .15s, background .15s;}
.cell-wrapper.weekend{background:#fff9f9;}
.cell-wrapper:hover{box-shadow:0 0 0 1px #c9dffa inset;background:#f7fbff;}
.cell-wrapper.has-note{box-shadow:0 0 0 1px #9cc4ff inset;}
.note-btn{position:absolute;top:4px;right:4px;opacity:.65;transition:opacity .2s, transform .2s;}
.cell-wrapper.has-note .note-btn{opacity:1;}
.note-btn:hover{opacity:1;transform:scale(1.05);} 
.note-btn :deep(.el-icon){font-size:16px;}
.cell-comment-dialog :deep(.el-dialog__body){padding-top:4px;}
.dialog-footer{display:flex;justify-content:flex-end;gap:8px;}
.row-total.badge{display:inline-block;background:#eef5ff;border:1px solid #c7dfff;padding:4px 10px;border-radius:16px;font-weight:600;color:#1d5fbf;min-width:52px;text-align:center;font-size:14px;}
.sum-head{font-weight:600;}

/* 移除顶部日合计条样式 */
.column-totals-strip{display:none;}
/* Summary 行美化 */
.hours-table :deep(tfoot){font-size:12px;}
.hours-table :deep(tfoot td){background:#fafcff;font-weight:600;border-top:2px solid #e4e9f0;}
.hours-table :deep(tfoot td:last-child){background:#e8f3ff;color:#1d5fbf;}
/* Misc */
.weekend :deep(.el-input__inner){color:#d9534f;}
.legend-card{--el-card-padding:10px;font-size:15px;background:#ffffff;border-radius:10px;box-shadow:0 2px 4px rgba(31,35,41,0.04);min-height:210px;display:flex;flex-direction:column;justify-content:flex-start;}
.legend-list{padding:2px 0 2px 16px;margin:0;line-height:1.3;}
.legend-list li{margin:2px 0;}
</style>