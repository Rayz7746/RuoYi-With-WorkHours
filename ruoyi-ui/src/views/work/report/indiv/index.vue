<template>
  <div class="app-container individual-report">
    <div class="report-header unified-header">
      <h1>个人工时月报</h1>
      <div class="header-right">
        <!-- 只有部门负责人显示人员下拉 -->
        <el-select v-if="isDeptLeader" v-model="selectedUserId" placeholder="选择人员" size="default" style="width: 180px; margin-right: 16px;" @change="handleUserChange" filterable>
          <el-option v-for="u in staffList" :key="u.userId" :label="u.nickName || u.userName" :value="u.userId" />
        </el-select>
        <div class="month-navigator">
          <el-button icon="ArrowLeft" circle @click="prevMonth" />
          <span class="month-display">{{ year }} 年 {{ month + 1 }} 月</span>
          <el-button icon="ArrowRight" circle @click="nextMonth" />
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading-container">正在加载数据...</div>
    <div v-if="error" class="error-container">错误：{{ error }}</div>

    <div v-if="!loading && !error" class="content-layout">
      <!-- 汇总 -->
      <div class="summary-section">
        <div class="summary-title-bar">
          <h2 class="summary-title">月度工时汇总</h2>
        </div>
        <el-table class="summary-table" :data="monthlySummary" style="width: 100%" show-summary :summary-method="getSummaries" :span-method="spanCustomerCell" size="small" stripe :cell-class-name="summaryCellClass">
          <el-table-column prop="customerName" label="客户名称" />
          <el-table-column prop="projectName" label="项目名称" />
          <el-table-column prop="totalHours" label="工作时长 (小时)" align="right">
            <template #default="scope">{{ scope.row.totalHours.toFixed(2) }}</template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 日历 -->
      <div class="calendar-section">
        <div class="section-title-bar">
          <h2 class="section-title">当月日历</h2>
        </div>
        <div class="calendar-grid">
          <div class="calendar-header" v-for="day in weekDays" :key="day">{{ day }}</div>
          <div v-for="day in calendarDays" :key="day.date.toISOString()" :class="['calendar-day', { 'not-current-month': !day.isCurrentMonth, 'is-today': day.isToday }]"><div class="day-number">{{ day.date.getDate() }}</div><div class="day-entries" v-if="day.entries.length > 0"><div v-for="entry in day.entries" :key="entry.attendanceId" class="entry"><span class="entry-project" :title="entry.project.name">{{ entry.project.name }}</span><span class="entry-hours">{{ entry.workingHours }}h</span></div></div></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup name="IndividualReport">
import { ref, onMounted, computed } from 'vue'
import { getUserProfile, getAllStaff } from '@/api/system/user'
import { listAttendance } from '@/api/work/attendance'
import { ElButton, ElTable, ElTableColumn, ElSelect, ElOption } from 'element-plus'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

const loading = ref(true)
const error = ref(null)
const attendanceList = ref([])
const userId = ref(null)
const selectedUserId = ref(null)
const staffList = ref([])
const currentDate = ref(new Date())
const isDeptLeader = ref(false)

const year = computed(() => currentDate.value.getFullYear())
const month = computed(() => currentDate.value.getMonth())

const weekDays = ['星期一', '星期二', '星期三', '星期四', '星期五', '星期六', '星期日']

const monthlySummary = computed(() => {
  if (!attendanceList.value || attendanceList.value.length === 0) return []
  const curYear = year.value
  const curMonth = month.value + 1
  const summaryMap = new Map()
  attendanceList.value.forEach(entry => {
    if (!entry) return
    const dateStr = entry.attendanceDate?.split(' ')[0]
    if (!dateStr) return
    const [ey, em] = dateStr.split('-').map(n => parseInt(n, 10))
    if (ey !== curYear || em !== curMonth) return
    const project = entry.project
    if (!project) return
    const projectId = project.projectId || project.id
    const projectName = project.name
    const customerName = project.customer?.customerName || '未知客户'
    const workingHours = parseFloat(entry.workingHours) || 0
    if (summaryMap.has(projectId)) {
      summaryMap.get(projectId).totalHours += workingHours
    } else {
      summaryMap.set(projectId, { projectId, projectName, customerName, totalHours: workingHours })
    }
  })
  return Array.from(summaryMap.values()).sort((a, b) => {
    if (a.customerName === b.customerName) return a.projectName.localeCompare(b.projectName, 'zh')
    return a.customerName.localeCompare(b.customerName, 'zh')
  })
})

const customerSpanArr = computed(() => {
  const list = monthlySummary.value
  const spans = []
  let i = 0
  while (i < list.length) {
    let j = i + 1
    while (j < list.length && list[j].customerName === list[i].customerName) j++
    spans[i] = j - i
    for (let k = i + 1; k < j; k++) spans[k] = 0
    i = j
  }
  return spans
})

function spanCustomerCell({ column, rowIndex }) {
  if (column.property === 'customerName') {
    const span = customerSpanArr.value[rowIndex]
    return { rowspan: span, colspan: 1 }
  }
}

const getSummaries = ({ columns, data }) => {
  const sums = []
  columns.forEach((column, index) => {
    if (index === 0) { sums[index] = '总计'; return }
    if (column.property === 'totalHours') {
      if (!data || data.length === 0) { sums[index] = '0.00 小时'; return }
      const values = data.map(item => Number(item[column.property]))
      if (!values.every(v => isNaN(v))) {
        const total = values.reduce((sum, v) => sum + (isNaN(v) ? 0 : v), 0)
        sums[index] = `${total.toFixed(2)} 小时`
      } else {
        sums[index] = '0.00 小时'
      }
    } else sums[index] = ''
  })
  return sums
}

const attendanceByDate = computed(() => {
  const map = new Map()
  attendanceList.value.forEach(item => {
    const dateKey = item.attendanceDate.split(' ')[0]
    if (!map.has(dateKey)) map.set(dateKey, [])
    map.get(dateKey).push(item)
  })
  return map
})

function formatLocalDate(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const calendarDays = computed(() => {
  const days = []
  const firstDayOfMonth = new Date(year.value, month.value, 1)
  const lastDayOfMonth = new Date(year.value, month.value + 1, 0)
  const startDayOfWeek = (firstDayOfMonth.getDay() + 6) % 7
  const endDayOfWeek = (lastDayOfMonth.getDay() + 6) % 7
  for (let i = startDayOfWeek; i > 0; i--) {
    const date = new Date(firstDayOfMonth)
    date.setDate(date.getDate() - i)
    days.push({ date, isCurrentMonth: false, isToday: false, entries: [] })
  }
  for (let i = 1; i <= lastDayOfMonth.getDate(); i++) {
    const date = new Date(year.value, month.value, i)
    const dateKey = formatLocalDate(date)
    const entries = attendanceByDate.value.get(dateKey) || []
    const isToday = new Date().toDateString() === date.toDateString()
    days.push({ date, isCurrentMonth: true, isToday, entries })
  }
  for (let i = 1; i < 7 - endDayOfWeek; i++) {
    const date = new Date(lastDayOfMonth)
    date.setDate(date.getDate() + i)
    days.push({ date, isCurrentMonth: false, isToday: false, entries: [] })
  }
  return days
})

function prevMonth() { currentDate.value = new Date(year.value, month.value - 1, 1); fetchAttendanceData() }
function nextMonth() { currentDate.value = new Date(year.value, month.value + 1, 1); fetchAttendanceData() }

async function getCurrentUserId() {
  try {
    const res = await getUserProfile()
    const profile = res?.data || {}
    userId.value = profile.userId
    selectedUserId.value = userId.value
    isDeptLeader.value = String(profile.isDepartmentLeader) === '1'
    if (!userId.value) throw new Error('未获取到用户ID')
  } catch (e) { error.value = e.message || '获取用户信息失败' }
}

async function loadStaffList() {
  if (!isDeptLeader.value) return // 非负责人不请求
  try {
    const res = await getAllStaff()
    const arr = Array.isArray(res?.data) ? res.data : []
    staffList.value = arr
    // 确保当前用户在列表中
    if (userId.value && !staffList.value.find(u => u.userId === userId.value)) {
      staffList.value.unshift({ userId: userId.value, nickName: '当前用户', userName: '当前用户' })
    }
  } catch (e) { /* 忽略 */ }
}

function handleUserChange(val) {
  if (val !== userId.value) {
    userId.value = val
    fetchAttendanceData()
  }
}

async function fetchAttendanceData() {
  if (!userId.value) return
  loading.value = true
  attendanceList.value = []
  const firstDay = new Date(year.value, month.value, 1)
  const lastDay = new Date(year.value, month.value + 1, 0)
  try {
    const queryParams = { userId: userId.value, beginAttendanceDate: formatLocalDate(firstDay), endAttendanceDate: formatLocalDate(lastDay), pageNum: 1, pageSize: 999 }
    const response = await listAttendance(queryParams)
    attendanceList.value = response.rows || []
  } catch (e) { error.value = e.message || '查询考勤记录失败' } finally { loading.value = false }
}

onMounted(async () => {
  await getCurrentUserId()
  if (!error.value) {
    await loadStaffList()
    await fetchAttendanceData()
  }
})

function summaryCellClass({ column, rowIndex }) {
  if (column.property === 'customerName') {
    const span = customerSpanArr.value[rowIndex]
    if (span > 0) return 'customer-col'
  }
  return ''
}
</script>

<style scoped>
.individual-report { padding: 20px; }
.report-header { display: flex; justify-content: space-between; align-items: center; }
.header-right { display: flex; align-items: center; }
.month-navigator { display: flex; align-items: center; gap: 12px; }
.month-display { font-size: 18px; font-weight: 500; width: 140px; text-align: center; }
/* 两栏布局（上：汇总 下：日历，移动端自动纵向） */
.content-layout { display: grid; gap: 24px; }
/* 汇总区块统一为卡片风格 */
.summary-section {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}

.summary-title-bar {
  background-color: #f9fafb;
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
}

.summary-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: #374151;
}

/* 日历容器外层与汇总一致 */
.calendar-section {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}

.section-title-bar {
  background-color: #f9fafb;
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: #374151;
}

/* 表格与日历配色统一 */
:deep(.summary-table .el-table__header th) {
  background-color: #f9fafb !important;
  color: #4b5563;
  font-weight: 600;
  border-bottom: 1px solid #e5e7eb;
  font-size: 13px;
}

:deep(.summary-table .el-table__body td) {
  font-size: 13px;
  color: #374151;
}

:deep(.summary-table .el-table__body tr:hover > td) {
  background-color: #f0f9ff;
}

:deep(.summary-table .el-table__footer td) {
  background-color: #f9fafb;
  font-weight: 600;
  color: #1f2937;
}

/* 替换原先 td:first-child 方式，避免行合并时误伤项目列 */
:deep(.summary-table .customer-col) {
  font-weight: 600;
  color: #1f2937;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  border-radius: 0 0 8px 8px;
  overflow: hidden;
  border-top: 1px solid #e5e7eb;
}

.calendar-header {
  background-color: #f9fafb;
  padding: 12px 8px;
  text-align: center;
  font-weight: 600;
  color: #4b5563;
  font-size: 14px;
  border-bottom: 1px solid #e5e7eb;
}

.calendar-day {
  min-height: 120px;
  padding: 8px;
  border-top: 1px solid #e5e7eb;
  transition: background-color 0.15s;
  background: #ffffff;
}

.calendar-day:not(:nth-child(7n+1)) {
  border-left: 1px solid #e5e7eb;
}

.calendar-day.is-today {
  background: #eff6ff;
}

.calendar-day:hover {
  background: #f0f9ff;
}

.not-current-month {
  background-color: #f9fafb;
  color: #9ca3af;
}

.day-number {
  font-weight: 500;
  font-size: 14px;
  margin-bottom: 8px;
  text-align: right;
}

.is-today .day-number {
  background-color: #3b82f6;
  color: white;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  float: right;
}

.day-entries {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.entry {
  background-color: #f0f9ff;
  color: #0c4a6e;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-left: 3px solid #0ea5e9;
}

.entry-project {
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-right: 8px;
  max-width: 120px;
}

.entry-hours {
  font-weight: 600;
  white-space: nowrap;
}

.loading-container, .error-container {
  text-align: center;
  padding: 40px;
  font-size: 16px;
  color: #6b7280;
}

.error-container {
  color: #ef4444;
}

/* 响应式 */
@media (min-width: 1100px) {
  .content-layout {
    grid-template-columns: 380px 1fr; /* 左侧汇总固定宽 */
    align-items: start;
  }
  .summary-section { height: 100%; }
  .calendar-section { height: 100%; }
}

:deep(.summary-table td.customer-col),
:deep(.summary-table td.customer-col .cell) {
  font-weight: 600 !important;
  color: #1f2937 !important;
}
</style>
