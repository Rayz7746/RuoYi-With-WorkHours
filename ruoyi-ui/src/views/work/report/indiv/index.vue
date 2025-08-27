<template>
  <div class="app-container individual-report">
    <div class="report-header unified-header">
      <h1>个人工时月报</h1>
      <div class="month-navigator">
        <el-button icon="ArrowLeft" circle @click="prevMonth"></el-button>
        <span class="month-display">{{ year }} 年 {{ month + 1 }} 月</span>
        <el-button icon="ArrowRight" circle @click="nextMonth"></el-button>
      </div>
    </div>

    <div v-if="loading" class="loading-container">正在加载数据...</div>
    <div v-if="error" class="error-container">错误：{{ error }}</div>

    <div v-if="!loading && !error" class="content-layout">
      <!-- Aggregated Hours Summary -->
      <div class="summary-section"> <!-- 统一风格 -->
        <div class="summary-title-bar">
          <h2 class="summary-title">月度工时汇总</h2>
        </div>
        <el-table class="summary-table" :data="monthlySummary" style="width: 100%" show-summary :summary-method="getSummaries" :span-method="spanCustomerCell" size="small" stripe :cell-class-name="summaryCellClass">
          <el-table-column prop="customerName" label="客户名称" />
          <el-table-column prop="projectName" label="项目名称" />
          <el-table-column prop="totalHours" label="工作时长 (小时)" align="right">
            <template #default="scope">
              {{ scope.row.totalHours.toFixed(2) }}
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- Calendar Grid -->
      <div class="calendar-section">
        <div class="section-title-bar">
          <h2 class="section-title">当月日历</h2>
        </div>
        <div class="calendar-grid">
          <div class="calendar-header" v-for="day in weekDays" :key="day">{{ day }}</div>
          <div
            v-for="day in calendarDays"
            :key="day.date.toISOString()"
            :class="['calendar-day', { 'not-current-month': !day.isCurrentMonth, 'is-today': day.isToday }]"
          >
            <div class="day-number">{{ day.date.getDate() }}</div>
            <div class="day-entries" v-if="day.entries.length > 0">
              <div v-for="entry in day.entries" :key="entry.attendanceId" class="entry">
                <span class="entry-project" :title="entry.project.name">{{ entry.project.name }}</span>
                <span class="entry-hours">{{ entry.workingHours }}h</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup name="IndividualReport">
import { ref, onMounted, computed } from 'vue';
import { getUserProfile } from '@/api/system/user';
import { listAttendance } from '@/api/work/attendance';
import { ElButton, ElTable, ElTableColumn } from 'element-plus';
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue';

const loading = ref(true);
const error = ref(null);
const attendanceList = ref([]);
const userId = ref(null);
const currentDate = ref(new Date());

const year = computed(() => currentDate.value.getFullYear());
const month = computed(() => currentDate.value.getMonth());

const weekDays = ['星期一', '星期二', '星期三', '星期四', '星期五', '星期六', '星期日'];

const monthlySummary = computed(() => {
  if (!attendanceList.value || attendanceList.value.length === 0) {
    return [];
  }
  const curYear = year.value;
  const curMonth = month.value + 1; // 1-based
  const summaryMap = new Map();
  attendanceList.value.forEach(entry => {
    if (!entry) return;
    const dateStr = entry.attendanceDate?.split(' ')[0];
    if (!dateStr) return;
    const [ey, em] = dateStr.split('-').map(n => parseInt(n, 10));
    // 仅统计当前界面所选月份的数据，避免跨月/旧数据残留
    if (ey !== curYear || em !== curMonth) return;
    const project = entry.project;
    if (!project) return;
    const projectId = project.projectId || project.id;
    const projectName = project.name;
    const customerName = project.customer?.customerName || '未知客户';
    const workingHours = parseFloat(entry.workingHours) || 0;
    if (summaryMap.has(projectId)) {
      summaryMap.get(projectId).totalHours += workingHours;
    } else {
      summaryMap.set(projectId, { projectId, projectName, customerName, totalHours: workingHours });
    }
  });
  return Array.from(summaryMap.values()).sort((a,b)=>{
    if (a.customerName === b.customerName) return a.projectName.localeCompare(b.projectName,'zh');
    return a.customerName.localeCompare(b.customerName,'zh');
  });
});

// 计算客户列需要的行合并跨度
const customerSpanArr = computed(()=>{
  const list = monthlySummary.value;
  const spans = [];
  let i=0;
  while(i<list.length){
    let j=i+1;
    while(j<list.length && list[j].customerName === list[i].customerName) j++;
    spans[i] = j - i; // 起始行的跨度
    for(let k=i+1;k<j;k++) spans[k] = 0; // 其他行隐藏
    i = j;
  }
  return spans;
});

function spanCustomerCell({ column, rowIndex }) {
  if (column.property === 'customerName') {
    const span = customerSpanArr.value[rowIndex];
    return { rowspan: span, colspan: 1 };
  }
}

const getSummaries = (param) => {
  const { columns, data } = param;
  const sums = [];
  columns.forEach((column, index) => {
    if (index === 0) {
      sums[index] = '总计';
      return;
    }
    if (column.property === 'totalHours') {
      if (!data || data.length === 0) {
        sums[index] = '0.00 小时';
        return;
      }
      const values = data.map(item => Number(item[column.property]));
      if (!values.every(value => isNaN(value))) {
        sums[index] = `${values.reduce((prev, curr) => {
          const value = Number(curr);
          if (!isNaN(value)) {
            return prev + curr;
          } else {
            return prev;
          }
        }, 0).toFixed(2)} 小时`;
      } else {
        sums[index] = '0.00 小时';
      }
    } else {
      sums[index] = '';
    }
  });
  return sums;
};

const attendanceByDate = computed(() => {
  const map = new Map();
  attendanceList.value.forEach(item => {
    const dateKey = item.attendanceDate.split(' ')[0];
    if (!map.has(dateKey)) {
      map.set(dateKey, []);
    }
    map.get(dateKey).push(item);
  });
  return map;
});

// 本地日期格式化，避免 toISOString 带来的时区回退（导致日期匹配不到 & 查询范围错位）
function formatLocalDate(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

const calendarDays = computed(() => {
  const days = [];
  const firstDayOfMonth = new Date(year.value, month.value, 1);
  const lastDayOfMonth = new Date(year.value, month.value + 1, 0);
  
  const startDayOfWeek = (firstDayOfMonth.getDay() + 6) % 7; // 0=Monday
  const endDayOfWeek = (lastDayOfMonth.getDay() + 6) % 7;

  // Days from previous month
  for (let i = startDayOfWeek; i > 0; i--) {
    const date = new Date(firstDayOfMonth);
    date.setDate(date.getDate() - i);
    days.push({ date, isCurrentMonth: false, isToday: false, entries: [] });
  }

  // Days of current month
  for (let i = 1; i <= lastDayOfMonth.getDate(); i++) {
    const date = new Date(year.value, month.value, i);
    const dateKey = formatLocalDate(date); // 替换 toISOString 以避免时区问题
    const entries = attendanceByDate.value.get(dateKey) || [];
    const isToday = new Date().toDateString() === date.toDateString();
    days.push({ date, isCurrentMonth: true, isToday, entries });
  }

  // Days from next month
  for (let i = 1; i < 7 - endDayOfWeek; i++) {
    const date = new Date(lastDayOfMonth);
    date.setDate(date.getDate() + i);
    days.push({ date, isCurrentMonth: false, isToday: false, entries: [] });
  }

  return days;
});

function prevMonth() {
  currentDate.value = new Date(year.value, month.value - 1, 1);
  fetchAttendanceData();
}

function nextMonth() {
  currentDate.value = new Date(year.value, month.value + 1, 1);
  fetchAttendanceData();
}

async function getCurrentUserId() {
  try {
    const response = await getUserProfile();
    userId.value = response?.data?.userId;
    if (!userId.value) throw new Error('未能从用户信息中获取用户ID');
  } catch (e) {
    error.value = e.message || '获取用户信息失败';
  }
}

async function fetchAttendanceData() {
  if (!userId.value) return;
  loading.value = true;
  // 清空旧数据，防止在新月份无数据时仍显示旧汇总
  attendanceList.value = [];

  const firstDay = new Date(year.value, month.value, 1);
  const lastDay = new Date(year.value, month.value + 1, 0);

  try {
    const queryParams = {
      userId: userId.value,
      beginAttendanceDate: formatLocalDate(firstDay), // 使用本地格式
      endAttendanceDate: formatLocalDate(lastDay),
      pageNum: 1,
      pageSize: 999,
    };
    const response = await listAttendance(queryParams);
    attendanceList.value = response.rows || [];
  } catch (e) {
    error.value = e.message || '查询考勤记录失败';
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  await getCurrentUserId();
  if (!error.value) {
    await fetchAttendanceData();
  }
});

function summaryCellClass({ column, rowIndex }) {
  if (column.property === 'customerName') {
    // 仅给实际渲染（rowspan>0）的单元格加样式
    const span = customerSpanArr.value[rowIndex];
    if (span > 0) return 'customer-col';
  }
  return '';
}
</script>

<style scoped>
.individual-report {
  padding: 20px;
}

/* 统一头部样式 */
.unified-header {
  background: linear-gradient(90deg,#f9fafb,#ffffff);
  border: 1px solid #e5e7eb;
  padding: 16px 20px;
  border-radius: 8px;
  margin-bottom: 24px;
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

h1 {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.month-navigator {
  display: flex;
  align-items: center;
  gap: 12px;
}

.month-display {
  font-size: 18px;
  font-weight: 500;
  width: 140px;
  text-align: center;
}

/* 两栏布局（上：汇总 下：日历，移动端自动纵向） */
.content-layout {
  display: grid;
  gap: 24px;
}

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
