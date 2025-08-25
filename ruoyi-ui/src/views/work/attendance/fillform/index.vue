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
        <el-switch v-model="showWeekend" active-text="含周末" inactive-text="工作日" size="large" />
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
            <div class="week-total-summary">
              <span>周总计 (含周末):</span>
              <strong>{{ grandTotalFullWeek.toFixed(1) }}</strong>
            </div>
          </div>
          <div class="right-actions">
            <!-- 新增: 确认后保存 / 重置 -->
            <el-button size="small" plain :disabled="savingAll" @click="confirmResetUnsaved">重置未保存</el-button>
            <el-button size="small" type="primary" :loading="savingAll" @click="confirmSaveAll">保存全部</el-button>
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
                :class="{ weekend: isWeekend(d), 'has-note': hasComment(row.projectId,d), 'disabled-cell': !isAllowed(row.projectId,d) }"
              >
                <template v-if="isAllowed(row.projectId,d)">
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
                 <el-tooltip
                   content="删除该日数据"
                   placement="bottom"
                   :show-after="300"
                 >
                   <el-button
                     link
                     size="small"
                     class="delete-btn"
                     type="danger"
                     @click.stop="askDeleteCell(row.projectId,d)"
                   >
                     <el-icon><Delete /></el-icon>
                   </el-button>
                 </el-tooltip>
                </template>
                <template v-else>
                  <span class="range-blocked">不可填报</span>
                </template>
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

      <!-- 保存确认弹窗 -->
      <el-dialog v-model="confirmDialog.open" width="360px" append-to-body :close-on-click-modal="false" :show-close="false">
        <template #header>
          <span>确认保存</span>
        </template>
        <div>是否确认保存当前所有修改的工时与备注？</div>
        <template #footer>
          <div class="dialog-footer">
            <el-button size="small" @click="confirmDialog.open=false" :disabled="savingAll">取 消</el-button>
            <el-button size="small" type="primary" :loading="savingAll" @click="runSaveAll">确 认</el-button>
          </div>
        </template>
      </el-dialog>
      <!-- 重置确认弹窗 -->
      <el-dialog v-model="resetDialog.open" width="360px" append-to-body :close-on-click-modal="false" :show-close="false">
        <template #header>
          <span>确认重置</span>
        </template>
        <div>确定要放弃本次未保存的修改并还原到上次加载/保存状态吗？</div>
        <template #footer>
          <div class="dialog-footer">
            <el-button size="small" @click="resetDialog.open=false" :disabled="savingAll">取 消</el-button>
            <el-button size="small" type="warning" @click="runResetUnsaved" :disabled="savingAll">确 认</el-button>
          </div>
        </template>
      </el-dialog>

      <!-- 删除确认弹窗 -->
      <el-dialog v-model="deleteDialog.open" width="360px" append-to-body :close-on-click-modal="false" :show-close="false">
        <template #header>
          <span>确认删除</span>
        </template>
        <div>
          确定要删除
          <strong>{{ deletingCellLabel }}</strong>
          的工时与备注吗？<br/>
          已保存的记录将立即从服务器删除；未保存的修改将被直接清除。
        </div>
        <template #footer>
          <div class="dialog-footer">
            <el-button size="small" @click="deleteDialog.open=false" :disabled="deleteDialog.loading">取 消</el-button>
            <el-button size="small" type="danger" @click="runDeleteCell" :loading="deleteDialog.loading">删 除</el-button>
          </div>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup name="SimpleWorkHours">
import { ref, reactive, computed, onMounted } from 'vue'
import WeekCalendar from './WeekCalendar.vue'
import { ArrowLeft, ArrowRight, ChatDotRound } from '@element-plus/icons-vue'
import { Delete } from '@element-plus/icons-vue'
import { getUserProfile } from '@/api/system/user'
import { getAllAssignments } from '@/services/assignmentService'
import { addAttendance, updateAttendance, listAttendance, delAttendance } from '@/api/work/attendance'
import { ElMessage } from 'element-plus'
import { getCurrentInstance } from 'vue'

const { proxy } = getCurrentInstance() || {}

/* ===== 动态数据（服务获取：客户 -> 项目） ===== */
const customers = ref([]) // [{ customerId, customerName, projects:[{projectId, projectName}] }]
const projectRanges = reactive({}) // { projectId: { start: Date|null, end: Date|null } }
const assignmentsRaw = ref([])

/* ===== 状态 ===== */
const selectedDate = ref(new Date())
const showWeekend = ref(false)
const comment = ref('')
const hours = reactive({})        // { projectId: { '2025-08-22': 8 } }
const comments = reactive({})     // { projectId: { '2025-08-22': '说明' } }
const attendanceIds = reactive({}) // { projectId: { '2025-08-22': attendanceId } }
// 保存中状态
const savingAll = ref(false)
let currentUserId = null

function parseDate(val){
  if(!val) return null
  // 仅取前10位，兼容 '2025-08-22 00:00:00'
  const s = String(val).slice(0,10)
  const d = new Date(s.replace(/-/g,'/'))
  return isNaN(d.getTime()) ? null : d
}

function inRange(range,date){
  if(!range) return true
  const { start, end } = range
  if(start && date < start) return false
  if(end && date > end) return false
  return true
}

function isAllowed(pid, date){
  const r = projectRanges[pid]
  return inRange(r, date)
}

function buildCustomersFromAssignments(list) {
  const filtered = (list || []).filter(a =>
    (a.isActiveAssignment === 1 || a.isActiveAssignment === '1') &&
    (a.isActiveCustomer === 1 || a.isActiveCustomer === '1') &&
    (a.isActive === 1 || a.isActive === '1')
  )
  // 清空旧范围
  Object.keys(projectRanges).forEach(k=> delete projectRanges[k])
  const map = new Map()
  filtered.forEach(a => {
    // 记录时间范围
    projectRanges[a.projectId] = {
      start: parseDate(a.dateStart),
      end: parseDate(a.dateEnd)
    }
    const cid = a.customerId || 'UNKNOWN'
    if (!map.has(cid)) {
      map.set(cid, { customerId: cid, customerName: a.customerName || cid, projects: [] })
    }
    const entry = map.get(cid)
    if (!entry.projects.find(p => p.projectId === a.projectId)) {
      entry.projects.push({ projectId: a.projectId, projectName: a.projectName })
    }
  })
  return Array.from(map.values())
}

function initStructures() {
  // 重建 hours / comments / attendanceIds
  Object.keys(hours).forEach(k => delete hours[k])
  Object.keys(comments).forEach(k => delete comments[k])
  Object.keys(attendanceIds).forEach(k => delete attendanceIds[k])
  customers.value.forEach(c => c.projects.forEach(p => {
    if (!hours[p.projectId]) hours[p.projectId] = {}
    if (!comments[p.projectId]) comments[p.projectId] = {}
    if (!attendanceIds[p.projectId]) attendanceIds[p.projectId] = {}
  }))
}

/* ===== 编辑备注对话框 ===== */
const editing = ref({ open:false, projectId:'', date:null, text:'' })
const editingTitle = computed(()=>{
  if(!editing.value.date) return '备注'
  return `备注 - ${projectNameById(editing.value.projectId)} ${formatMD(editing.value.date)}`
})

/* ===== 工具函数 ===== */
function startOfWeek(d) { const tmp = new Date(d); let g = tmp.getDay() || 7; tmp.setDate(tmp.getDate() - (g - 1)); tmp.setHours(0,0,0,0); return tmp }
function addDays(d,n){ const nd=new Date(d); nd.setDate(nd.getDate()+n); return nd }
function dateKey(d){ // 覆盖: 避免时区偏移, 使用本地日期
  const y = d.getFullYear(); const m = (d.getMonth()+1).toString().padStart(2,'0'); const dd = d.getDate().toString().padStart(2,'0');
  return `${y}-${m}-${dd}`
}
function formatMD(d){ return `${d.getMonth()+1}/${d.getDate()}` }
function weekday(d){ return ['周一','周二','周三','周四','周五','周六','周日'][ (d.getDay()||7)-1 ] }
function isWeekend(d){ const g=d.getDay(); return g===0||g===6 }
function projectNameById(pid){
  for(const c of customers.value){ const f = c.projects.find(p=>p.projectId===pid); if(f) return f.projectName }
  return pid
}

/* ===== Week 计算 ===== */
const weekDates = computed(()=>{ const start = startOfWeek(selectedDate.value); const len = showWeekend.value?7:5; return Array.from({length:len},(_,i)=>addDays(start,i)) })
const weekRangeLabel = computed(()=>`${formatMD(weekDates.value[0])} ~ ${formatMD(weekDates.value[weekDates.value.length-1])}`)

/* ===== 表格数据 ===== */
const tableRows = computed(()=>{ const rows=[]; customers.value.forEach(c=> c.projects.forEach(p=> rows.push({ customerId:c.customerId, customerName:c.customerName, projectId:p.projectId, projectName:p.projectName })) ); return rows })
function spanMethod({ column, rowIndex, row }) { if(column.property==='customerName'){ const id=row.customerId; let first=-1,count=0; tableRows.value.forEach((r,i)=>{ if(r.customerId===id){ if(first<0) first=i; count++ } }); if(rowIndex===first) return {rowspan:count,colspan:1}; return {rowspan:0,colspan:0} } }

/* ===== 汇总 ===== */
function rowTotal(pid){ return weekDates.value.reduce((s,d)=>{ const v=parseFloat(hours[pid]?.[dateKey(d)]); return s+(isNaN(v)?0:v)},0) }
function dayTotal(d){ const k=dateKey(d); return Object.keys(hours).reduce((s,pid)=>{ const v=parseFloat(hours[pid]?.[k]); return s+(isNaN(v)?0:v)},0) }
const grandTotal = computed(()=> weekDates.value.reduce((s,d)=> s+dayTotal(d),0))
const grandTotalFullWeek = computed(() => {
  const start = startOfWeek(selectedDate.value);
  const fullWeekDates = Array.from({length: 7}, (_, i) => addDays(start, i));
  return fullWeekDates.reduce((s, d) => s + dayTotal(d), 0);
});
function summaryMethod(){ const sums = []; sums[0]='日合计'; sums[1]=''; weekDates.value.forEach((d,i)=>{ sums[2+i]=dayTotal(d).toFixed(1) }); sums[2+weekDates.value.length]=grandTotal.value.toFixed(1); return sums }

/* ===== Actions ===== */
function shiftWeek(n){ selectedDate.value = addDays(selectedDate.value, n*7) }
function jumpToday(){ selectedDate.value = new Date() }
function onPickWeek(start){ selectedDate.value = new Date(start) }
function shortWeekday(d){ return ['一','二','三','四','五','六','日'][ (d.getDay()||7)-1 ] }
function weekdayHeader(d){ return `${weekday(d)}\n${formatMD(d)}` }
function rowCls(){ return 'data-row' }

function openComment(pid,d){ editing.value.projectId = pid; editing.value.date = new Date(d); editing.value.text = comments[pid]?.[dateKey(d)] || ''; editing.value.open = true }
function saveEditing(){ const key = dateKey(editing.value.date); if(!comments[editing.value.projectId]) comments[editing.value.projectId] = {}; comments[editing.value.projectId][key] = editing.value.text.trim(); if(!comments[editing.value.projectId][key]) delete comments[editing.value.projectId][key]; editing.value.open = false }
function clearEditing(){ editing.value.text=''; saveEditing() }
function hasComment(pid,d){ return !!comments[pid]?.[dateKey(d)] }

/* ===== 考勤（加载 / 保存） ===== */
async function loadAttendanceAll() {
  if(!currentUserId) return
  try {
    // 拉取当前用户全部(设较大页大小)，后端若有分页限制可循环，这里先简单处理
    const resp = await listAttendance({ pageNum:1, pageSize:1000, userId: currentUserId })
    const rows = resp?.rows || []
    rows.forEach(r => {
      const pid = r.projectId || r.project?.projectId || r.project?.id
      if(!pid) return
      if(!hours[pid]) hours[pid] = {}
      if(!comments[pid]) comments[pid] = {}
      if(!attendanceIds[pid]) attendanceIds[pid] = {}
      const dkey = r.attendanceDate?.slice(0,10)
      if(!dkey) return
      hours[pid][dkey] = parseFloat(r.workingHours)
      if(r.comment) comments[pid][dkey] = r.comment
      attendanceIds[pid][dkey] = r.attendanceId
    })
    updateBaseline()
  } catch(e){
    console.error('加载考勤失败', e)
  }
}

const baselineHours = reactive({})
const baselineComments = reactive({})
const baselineAttendanceIds = reactive({})

function cloneToReactive(src, dest){
  // 清空目标
  Object.keys(dest).forEach(k=> delete dest[k])
  Object.keys(src||{}).forEach(k=> {
    // 深拷贝简单对象
    dest[k] = JSON.parse(JSON.stringify(src[k]))
  })
}
function updateBaseline(){
  // 将当前数据快照到 baseline
  cloneToReactive(hours, baselineHours)
  cloneToReactive(comments, baselineComments)
  cloneToReactive(attendanceIds, baselineAttendanceIds)
}
function restoreFromBaseline(){
  cloneToReactive(baselineHours, hours)
  cloneToReactive(baselineComments, comments)
  cloneToReactive(baselineAttendanceIds, attendanceIds)
}
function resetUnsaved(){
  if(savingAll.value) return
  if(Object.keys(baselineHours).length===0){
    ElMessage.info('暂无可重置数据')
    return
  }
  restoreFromBaseline()
  ElMessage.success('已还原到上次加载/保存状态')
}

async function saveAll(){
  if(savingAll.value) return
  savingAll.value = true
  try {
    const tasks = []
    // 遍历所有项目日期
    Object.keys(hours).forEach(pid => {
      const dayMap = hours[pid]
      Object.keys(dayMap).forEach(dkey => {
        const val = dayMap[dkey]
        const note = comments[pid]?.[dkey]
        if(val == null && !note) return // 没有内容不提交
        const attendanceId = attendanceIds[pid]?.[dkey]
        const payload = {
          attendanceId: attendanceId || null,
            projectId: pid,
            userId: currentUserId,
            attendanceDate: dkey,
            workingHours: val ?? 0,
            comment: note || null
        }
        if(attendanceId){
          tasks.push(updateAttendance(payload))
        } else {
          tasks.push(addAttendance(payload).then(res=>{ // 记录新ID
            const newId = res?.data?.attendanceId || res?.data?.id
            if(newId){
              if(!attendanceIds[pid]) attendanceIds[pid] = {}
              attendanceIds[pid][dkey] = newId
            }
          }))
        }
      })
    })
    if(tasks.length===0){
      ElMessage.info('无需要保存的数据')
    } else {
      await Promise.all(tasks)
      ElMessage.success('保存成功')
      updateBaseline() // 保存成功后更新基线
      // 可选择刷新：await loadAttendanceAll()
    }
  } catch(e){
    console.error('保存失败', e)
    ElMessage.error('保存失败')
  } finally {
    savingAll.value = false
  }
}

/* ===== 动态获取当前用户项目（默认 + 分配） ===== */
onMounted(async () => {
  try {
    const prof = await getUserProfile()
    const uid = prof?.data?.userId
    currentUserId = uid
    if(!uid) return
    const list = await getAllAssignments(uid)
    assignmentsRaw.value = list
    console.log('[fillform] assignments merged =>', list)
    customers.value = buildCustomersFromAssignments(list)
    initStructures()
    await loadAttendanceAll()
  } catch ( e ) {
    console.error('加载用户项目失败:', e)
  }
})

const confirmDialog = reactive({ open:false })
const resetDialog = reactive({ open:false })
const deleteDialog = reactive({ open:false, projectId:null, date:null, loading:false })

function confirmSaveAll(){
  if(savingAll.value) return
  confirmDialog.open = true
}
async function runSaveAll(){
  await saveAll()
  confirmDialog.open = false
}
function confirmResetUnsaved(){
  if(savingAll.value) return
  if(Object.keys(baselineHours).length===0){
    ElMessage.info('暂无可重置数据')
    return
  }
  resetDialog.open = true
}
function runResetUnsaved(){
  resetDialog.open = false
  resetUnsaved()
}
function askDeleteCell(pid, d){
  deleteDialog.projectId = pid
  deleteDialog.date = new Date(d)
  deleteDialog.open = true
}
function deleteCellData(pid, date){
   const key = dateKey(date)
   const hasId = attendanceIds[pid]?.[key]
   // 清除备注
   if(comments[pid]) delete comments[pid][key]
   if(hasId){
    // 旧逻辑保留（一般不会走到：直接删除时已单独处理）
    if(hours[pid]) delete hours[pid][key]
    if(attendanceIds[pid]) delete attendanceIds[pid][key]
   } else {
     // 未保存：直接移除
     if(hours[pid]) delete hours[pid][key]
   }
  ElMessage.success('已清除未保存数据')
}
async function runDeleteCell(){
  if(!deleteDialog.projectId || !deleteDialog.date){
    deleteDialog.open = false
    return
  }
  const pid = deleteDialog.projectId
  const key = dateKey(deleteDialog.date)
  const attendanceId = attendanceIds[pid]?.[key]
  // 若存在已保存记录，直接请求后端删除
  if(attendanceId){
    deleteDialog.loading = true
    try {
      await delAttendance(attendanceId)
      // 移除本地数据
      if(hours[pid]) delete hours[pid][key]
      if(comments[pid]) delete comments[pid][key]
      if(attendanceIds[pid]) delete attendanceIds[pid][key]
      ElMessage.success('删除成功')
      updateBaseline()
    } catch(e){
      console.error('删除失败', e)
      ElMessage.error('删除失败')
    } finally {
      deleteDialog.loading = false
      deleteDialog.open = false
    }
  } else {
    // 未保存数据，仅本地清除
    deleteCellData(pid, deleteDialog.date)
    deleteDialog.open = false
  }
}
const deletingCellLabel = computed(()=>{
  if(!deleteDialog.date) return ''
  return `${projectNameById(deleteDialog.projectId)} ${formatMD(deleteDialog.date)}`
})
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
.week-total-summary{
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
  margin-left: 12px;
  padding: 4px 10px;
  background: #f9f9f9;
  border: 1px solid #e9e9eb;
  border-radius: 20px;
}
.week-total-summary strong {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
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
/* 新增删除按钮（位于备注按钮下方） */
.delete-btn{position:absolute;right:4px;top:28px;opacity:.55;transition:opacity .2s, transform .2s;}
.delete-btn:hover{opacity:1;transform:scale(1.05);}
.delete-btn :deep(.el-icon){font-size:16px;}
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
.disabled-cell{background:#f5f5f5 !important;}
.range-blocked{font-size:12px;color:#999;}
.right-actions .el-button+.el-button{margin-left:8px;}
</style>