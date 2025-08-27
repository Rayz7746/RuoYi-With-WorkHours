<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="90px" class="attendance-search">
      <!-- 客户（多选） -->
      <el-form-item label="客户" prop="customerIds">
        <el-select-v2
          v-model="queryParams.customerIds"
          :options="customerOptionsV2"
          multiple
          collapse-tags
          collapse-tags-tooltip
          filterable
            clearable
          placeholder="请选择客户(可多选)"
          style="width: 260px"
        />
      </el-form-item>

      <!-- 项目（受客户多选联动，多选） -->
      <el-form-item label="项目" prop="projectIds">
        <el-select-v2
          v-model="queryParams.projectIds"
          :options="searchProjectOptionsV2"
          multiple
          collapse-tags
          collapse-tags-tooltip
          filterable
          clearable
          placeholder="请选择项目(可多选)"
          style="width: 260px"
        />
      </el-form-item>

      <!-- 项目人员（多选） -->
      <el-form-item label="项目人员" prop="userIds">
        <el-select-v2
          v-model="queryParams.userIds"
          :options="memberOptionsV2"
          multiple
          collapse-tags
          collapse-tags-tooltip
          filterable
          clearable
          placeholder="请选择项目人员(可多选)"
          style="width: 260px"
        />
      </el-form-item>

      <!-- 考勤日期范围 -->
      <el-form-item label="日期范围">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          unlink-panels
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          clearable
          style="width: 260px"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="attendanceList" class="compact-table">
      <el-table-column label="项目客户" align="center" prop="project.customer.customerName" />
      <el-table-column label="项目名称" align="center" prop="project.name" />
      <el-table-column label="项目人员" align="center" prop="user.nickName" />
      <el-table-column label="考勤日期" align="center" prop="attendanceDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.attendanceDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="工作时长" align="center" prop="workingHours" />
      <el-table-column label="考勤备注" align="center" prop="comment" />
    </el-table>
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
    <!-- 图表移动到分页下方 -->
    <div ref="customerHoursChart" class="customer-hours-chart" />
  </div>
</template>

<script setup name="Attendance">
import { ref, reactive, toRefs, computed, watch, getCurrentInstance, onMounted } from "vue"
// 修改：使用 listAttendanceArray
import { listAttendanceArray } from "@/api/work/attendance"
import { userSelect, projectNameSelect, customerSelect } from "@/api/work/assignment"
import VChart from '@visactor/vchart'

const { proxy } = getCurrentInstance()

const attendanceList = ref([])
// 新增：堆叠图需要的响应式集合
const stackedData = ref([])
const uniqueDates = ref([])
const uniqueCustomers = ref([])
const loading = ref(true)
const total = ref(0)
const dateRange = ref([]) // [begin,end]
const fullRangeDates = ref([]) // 选择范围内所有日期（含没有数据的）

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    customerIds: [],
    projectIds: [],
    userIds: [],
    startTime: '',
    endTime: ''
  }
})
const { queryParams } = toRefs(data)

function buildQueryParams() {
  const qp = { ...queryParams.value }
  // 移除空数组
  ;['customerIds','projectIds','userIds'].forEach(k => { if (!Array.isArray(qp[k]) || qp[k].length === 0) delete qp[k] })
  // 处理时间，空字符串不传
  if (!qp.startTime) delete qp.startTime
  if (!qp.endTime) delete qp.endTime
  return qp
}

// 同步日期范围到 startTime / endTime
watch(dateRange, (val) => {
  if (Array.isArray(val) && val.length === 2) {
    queryParams.value.startTime = val[0]
    queryParams.value.endTime = val[1]
  } else {
    queryParams.value.startTime = ''
    queryParams.value.endTime = ''
  }
})

function getList() {
  loading.value = true
  const builtParams = buildQueryParams()
  listAttendanceArray(builtParams).then(response => {
    const rows = response.rows || []
    attendanceList.value = rows
    total.value = response.total
    loadAllRowsForChart(builtParams) // 改为加载全量给图表
  }).finally(() => { loading.value = false })
}

// 全量加载当前筛选条件所有数据用于图表
async function loadAllRowsForChart(builtParams) {
  try {
    const pageSize = 500
    let pageNum = 1
    const all = []
    while (true) {
      const resp = await listAttendanceArray({ ...builtParams, pageNum, pageSize })
      const rows = resp?.rows || []
      all.push(...rows)
      const totalCount = resp?.total || 0
      if (pageNum * pageSize >= totalCount || rows.length === 0) break
      pageNum++
    }
    buildStackedChartData(all)
  } catch (e) { console.error('加载全量失败', e) }
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() {
  proxy.resetForm("queryRef")
  queryParams.value.customerIds = []
  queryParams.value.projectIds = []
  queryParams.value.userIds = []
  // 重置到当前月（而不是清空）
  setCurrentMonthRange()
  handleQuery()
}

// 项目人员
const memberOptions = ref()
const enabledMemberOptions = ref()
const memberOptionsV2 = computed(() => (enabledMemberOptions.value || []).map(u => ({ value: u.userId ?? u.value, label: u.nickName ?? u.label, disabled: u.disabled === true || u.status === 1 || u.status === '1' })).filter(o => o.value != null && o.label != null))
function getMemberSelectOption() { userSelect().then(res => { memberOptions.value = res.data || []; enabledMemberOptions.value = (memberOptions.value || []).filter(u => !(u?.disabled === true || u?.status === 1 || u?.status === '1')) }) }

// 客户
const customerOptions = ref()
const enabledCustomerOptions = ref()
const customerOptionsV2 = computed(() => (enabledCustomerOptions.value || []).map(c => ({ value: c.customerId ?? c.value, label: c.customerName ?? c.label, disabled: c.isActiveCustomer === 0 || c.disabled === true })).filter(o => o.value != null && o.label != null))
function getCustomerSelectOption() { customerSelect().then(res => { const list = res.data || []; customerOptions.value = list; enabledCustomerOptions.value = (list || []).filter(c => !(c?.isActiveCustomer === 0 || c?.disabled === true)) }) }

// 项目（受多客户联动）
const allProjects = ref([])
function preloadAllProjects() { projectNameSelect().then(res => { allProjects.value = Array.isArray(res.data) ? res.data : [] }) }
function getProjectCustomerId(p) { return p?.customerId ?? p?.customer?.customerId ?? p?.customer?.id ?? null }
const enabledSearchProjectOptions = ref([])
const searchProjectOptionsV2 = computed(() => (enabledSearchProjectOptions.value || []).map(p => ({ value: p.projectId ?? p.value, label: p.name ?? p.label, disabled: p.isActive === 0 })).filter(o => o.value != null && o.label != null))
function loadSearchProjectsByCustomers(customerIds) {
  if (!Array.isArray(customerIds) || customerIds.length === 0) { enabledSearchProjectOptions.value = []; return }
  const idSet = new Set(customerIds.map(String))
  const list = (allProjects.value || []).filter(p => idSet.has(String(getProjectCustomerId(p))))
  enabledSearchProjectOptions.value = list.filter(p => !(p?.isActive === 0 || p?.disabled === true || p?.status === 0 || p?.status === '0'))
}
watch(() => queryParams.value.customerIds, (cids) => {
  queryParams.value.projectIds = []
  loadSearchProjectsByCustomers(cids)
}, { deep: true })

const customerHoursChart = ref(null)
let vchartInstance = null
// 存储全量汇总后的客户工时（[{name,value}...]）
const fullCustomerHours = ref([])
// 新增：客户-项目明细（用于 hover 展示）[{ customer, project, value }]
const fullCustomerProjectHours = ref([])
let fullLoadingFlag = false

async function loadAllCustomerHours(builtParams) {
  if (fullLoadingFlag) return
  fullLoadingFlag = true
  try {
    const pageSize = 500
    let pageNum = 1
    const totalMap = new Map()                  // customer -> total hours
    const cpMap = new Map()                     // key customer||project -> hours
    while (true) {
      const resp = await listAttendanceArray({ ...builtParams, pageNum, pageSize })
      const rows = resp?.rows || []
      for (const row of rows) {
        const customer = row?.project?.customer?.customerName || row?.project?.customerName || '未分配'
        const project = row?.project?.name || row?.projectName || '未命名项目'
        const hours = Number(row?.workingHours) || 0
        totalMap.set(customer, (totalMap.get(customer) || 0) + hours)
        const key = customer + '||' + project
        cpMap.set(key, (cpMap.get(key) || 0) + hours)
      }
      const totalCount = resp?.total || 0
      if (pageNum * pageSize >= totalCount || rows.length === 0) break
      pageNum++
    }
    fullCustomerHours.value = Array.from(totalMap.entries()).map(([name, value]) => ({ name, value })).sort((a,b)=>b.value-a.value)
    const projList = []
    for (const [key, value] of cpMap.entries()) {
      const [customer, project] = key.split('||')
      projList.push({ customer, project, value })
    }
    // 排序：客户按总工时降序，客户内按项目工时降序
    const customerOrder = fullCustomerHours.value.map(d => d.name)
    const cIndex = new Map(customerOrder.map((c,i)=>[c,i]))
    projList.sort((a,b)=>{ const ci = cIndex.get(a.customer) - cIndex.get(b.customer); return ci !== 0 ? ci : b.value - a.value })
    fullCustomerProjectHours.value = projList
  } catch (e) { /* ignore */ } finally { fullLoadingFlag = false }
}

function buildProjectDataFromCurrentPage() {
  const totalMap = new Map()
  const cpMap = new Map()
  for (const row of attendanceList.value) {
    const customer = row?.project?.customer?.customerName || row?.project?.customerName || '未分配'
    const project = row?.project?.name || row?.projectName || '未命名项目'
    const hours = Number(row?.workingHours) || 0
    totalMap.set(customer, (totalMap.get(customer) || 0) + hours)
    const key = customer + '||' + project
    cpMap.set(key, (cpMap.get(key) || 0) + hours)
  }
  const totals = Array.from(totalMap.entries()).map(([name, value]) => ({ name, value }))
  const projList = []
  for (const [key, value] of cpMap.entries()) {
    const [customer, project] = key.split('||')
    projList.push({ customer, project, value })
  }
  const customerOrder = totals.sort((a,b)=>b.value-a.value).map(d=>d.name)
  const cIndex = new Map(customerOrder.map((c,i)=>[c,i]))
  projList.sort((a,b)=>{ const ci = cIndex.get(a.customer) - cIndex.get(b.customer); return ci !== 0 ? ci : b.value - a.value })
  return { totals, projList }
}

function getProjectData() {
  if (fullCustomerProjectHours.value.length) return { totals: fullCustomerHours.value, projList: fullCustomerProjectHours.value }
  return buildProjectDataFromCurrentPage()
}

// 新增：封装当前月份范围设置，便于复用（重置时也回到当月）
function setCurrentMonthRange() {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth()
  const first = `${year}-${String(month + 1).padStart(2, '0')}-01`
  const lastDay = new Date(year, month + 1, 0).getDate()
  const last = `${year}-${String(month + 1).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
  dateRange.value = [first, last]
  queryParams.value.startTime = first
  queryParams.value.endTime = last
}

// ================= 新增：限制日期只能选同一个月且最长 31 天 =================
function enforceMonthRange(val) {
  if (!Array.isArray(val) || val.length !== 2) return
  const [start, end] = val
  if (!start || !end) return
  const s = new Date(start)
  const e = new Date(end)
  // 不同月或超过 31 天则截断到 start 所在月份的月末
  if (s.getMonth() !== e.getMonth() || (e - s) / 86400000 > 31) {
    const month = s.getMonth()
    const year = s.getFullYear()
    const lastDay = new Date(year, month + 1, 0).getDate()
    const fixedEnd = `${year}-${String(month + 1).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
    dateRange.value = [start, fixedEnd]
    proxy?.$modal?.msgWarning?.('最多只允许选择一个月范围，已自动截断到当月月底')
  }
}

watch(dateRange, (val) => {
  if (Array.isArray(val) && val.length === 2) {
    enforceMonthRange(val)
    queryParams.value.startTime = val[0]
    queryParams.value.endTime = val[1]
  } else {
    queryParams.value.startTime = ''
    queryParams.value.endTime = ''
  }
})

// 初始默认本月
onMounted(() => {
  setCurrentMonthRange()
  getCustomerSelectOption()
  getMemberSelectOption()
  preloadAllProjects()
  if (queryParams.value.customerIds.length) loadSearchProjectsByCustomers(queryParams.value.customerIds)
  getList()
})

// ================= 新增：构建堆叠数据 (日期为 X，客户为 series) =================
function buildStackedChartData(rows) {
  const start = queryParams.value.startTime
  const end = queryParams.value.endTime
  const dateList = generateDateRange(start, end)
  fullRangeDates.value = dateList
  const hoursMap = new Map()          // key: date||customer -> total hours
  const projectMap = new Map()        // key: date||customer -> Map(project -> hours)
  const customerSet = new Set()
  for (const r of rows) {
    const date = (r.attendanceDate || '').substring(0,10)
    if (!date) continue
    if (date < start || date > end) continue
    const customer = r?.project?.customer?.customerName || r?.project?.customerName || '未分配'
    const project = r?.project?.name || r?.projectName || '未命名项目'
    const hrs = Number(r.workingHours) || 0
    const key = date + '||' + customer
    hoursMap.set(key, (hoursMap.get(key) || 0) + hrs)
    if (!projectMap.has(key)) projectMap.set(key, new Map())
    const pMap = projectMap.get(key)
    pMap.set(project, (pMap.get(project) || 0) + hrs)
    customerSet.add(customer)
  }
  if (customerSet.size === 0) customerSet.add('无数据')
  uniqueDates.value = dateList
  uniqueCustomers.value = Array.from(customerSet)
  const data = []
  for (const date of dateList) {
    for (const customer of uniqueCustomers.value) {
      const key = date + '||' + customer
      const hours = hoursMap.get(key) || 0
      if (hours === 0 && customer === '无数据') {
        data.push({ date, customer, hours: 0, projects: [] })
      } else if (hours > 0 || customer !== '无数据') {
        const pMap = projectMap.get(key)
        let projects = []
        if (pMap) {
          projects = Array.from(pMap.entries()).map(([project, value]) => ({ project, value })).sort((a,b)=>b.value-a.value)
        }
        data.push({ date, customer, hours, projects })
      }
    }
  }
  stackedData.value = data
  renderCustomerHoursChart()
}

// 生成日期范围（含起止日期）
function generateDateRange(start, end) {
  if (!start || !end) return []
  const res = []
  let cur = new Date(start)
  const endDate = new Date(end)
  while (cur <= endDate) { res.push(formatDate(cur)); cur.setDate(cur.getDate() + 1) }
  return res
}
function formatDate(d) { const y = d.getFullYear(); const m = String(d.getMonth()+1).padStart(2,'0'); const day = String(d.getDate()).padStart(2, '0'); return `${y}-${m}-${day}` }

// 动态柱宽：日期越少柱越宽
const barWidth = computed(() => {
  const n = fullRangeDates.value.length || uniqueDates.value.length
  if (n <= 7) return 60
  if (n <= 14) return 40
  if (n <= 21) return 30
  if (n <= 31) return 22
  return 18
})

// ================= 修改：渲染堆叠图 =================
function renderCustomerHoursChart() {
  if (!customerHoursChart.value) return
  const values = stackedData.value
  const colors = [ '#1f77b4', '#ff7f0e', '#2ca02c', '#d62728', '#9467bd', '#8c564b', '#e377c2', '#7f7f7f', '#bcbd22', '#17becf' ]
  // 新增高亮色（淡蓝）
  const highlightColor = '#5ab0ff'
  const highlightShadow = 'rgba(90,176,255,0.45)'
  const customerCount = uniqueCustomers.value.length || 1
  const legendPerRow = 6
  const legendRows = Math.ceil(customerCount / legendPerRow)
  const extraForDates = fullRangeDates.value.length > 20 ? 40 : 0
  const dynamicHeight = 320 + legendRows * 26 + extraForDates
  if (customerHoursChart.value.style.height !== dynamicHeight + 'px') {
    customerHoursChart.value.style.height = dynamicHeight + 'px'
  }

  const rawMaxSegment = values.reduce((m,d)=>Math.max(m, Number(d.hours)||0), 0)
  // 计算每个日期堆叠后的总和
  const perDateTotalMap = new Map()
  for (const v of values) {
    const h = Number(v.hours) || 0
    perDateTotalMap.set(v.date, (perDateTotalMap.get(v.date) || 0) + h)
  }
  const stackedMax = perDateTotalMap.size ? Math.max(...perDateTotalMap.values()) : 0
  // 取堆叠最大作为主参考，增加 5% 缓冲再取漂亮刻度
  const baseNeed = stackedMax * 1.05
  function calcUpperBound(max) { // 重新放到这里，避免上方移动后缺失
    if (!isFinite(max) || max <= 0) return 1
    const mag = Math.pow(10, Math.floor(Math.log10(max)))
    const norm = max / mag
    let upperNorm
    if (norm <= 1) upperNorm = 1
    else if (norm <= 2) upperNorm = 2
    else if (norm <= 2.5) upperNorm = 2.5
    else if (norm <= 5) upperNorm = 5
    else if (norm <= 7.5) upperNorm = 7.5
    else upperNorm = 10
    let upper = upperNorm * mag
    if (upper < max) upper = upperNorm * mag // 保底
    if (upper <= stackedMax) upper += mag * 0.2 // 再加一点
    return upper
  }
  const axisMax = calcUpperBound(baseNeed || rawMaxSegment)

  // ===== 新增：每日总数 markPoint（含今日高亮）=====
  const todayStr = formatDate(new Date())
  const markPoint = []
  for (const date of uniqueDates.value) {
    const total = perDateTotalMap.get(date) || 0
    if (total <= 0) continue
    const isToday = date === todayStr
    const textStr = Number.isInteger(total) ? String(total) : total.toFixed(2)
    markPoint.push({
      coordinate: { date, hours: total },
      itemContent: {
        type: 'text',
        autoRotate: false,
        offsetY: -6,
        text: {
          dy: 14,
          text: textStr,
          style: {
            fill: isToday ? '#fff' : '#333',
            fontSize: isToday ? 13 : 12,
            fontWeight: isToday ? 'bold' : 'normal'
          },
          ...(isToday ? {
            labelBackground: {
              padding: [2, 6, 2, 6],
              style: {
                fill: highlightColor,
                stroke: highlightColor,
                cornerRadius: 4,
                shadowBlur: 6,
                shadowColor: highlightShadow
              }
            }
          } : {})
        }
      },
      itemLine: { line: { style: { visible: false } }, startSymbol: { visible: false }, endSymbol: { visible: false } }
    })
  }

  const spec = {
    type: 'bar',
    height: dynamicHeight,
    data: [{ id: 'barData', values }],
    xField: 'date',
    yField: 'hours',
    seriesField: 'customer',
    stack: true,
    bar: { width: barWidth.value, maxWidth: barWidth.value, minWidth: barWidth.value, radius: 2 },
    color: { field: 'customer', range: colors },
    axes: [
      { orient: 'bottom', type: 'band', title: { text: '日期' }, label: { autoRotate: true, formatMethod: v => (typeof v === 'string' && v.length >= 5 ? v.slice(5) : v), style: (txt)=> { const isToday = todayStr.endsWith(txt); return { fill: isToday ? highlightColor : '#666', fontWeight: isToday ? 'bold' : 'normal' } } } },
      { orient: 'left', type: 'linear', title: { text: '工时 (Hours)' }, min: 0, max: axisMax, nice: false }
    ],
    tooltip: {
      visible: true,
      shared: false,
      mark: {
        title: d => (d?.datum?.date || d?.date || '') + (d?.datum?.customer ? ' / ' + d.datum.customer : ''),
        content: items => {
            const extractDataItem = (src) => {
              if (!src) return null
              if (Array.isArray(src)) return extractDataItem(src[0])
              if (src.datum) {
                if (Array.isArray(src.datum)) return src.datum[0]
                return src.datum
              }
              return src
            }
            const dataItem = extractDataItem(items)
            const list = dataItem?.projects || []
            if (!Array.isArray(list) || list.length === 0) {
              return [{ key: '无项目数据', value: '' }]
            }
            return list.map(p => ({ key: p.project, value: p.value }))
        }
      }
    },
    legends: {
      visible: true,
      orient: 'bottom',
      item: { padding: 6, space: 8 },
      maxRow: legendRows,
      autoWrap: true,
      selectable: true
    },
    animation: { appear: { animation: 'grow' } },
    padding: { top: 16, right: 24, bottom: 60 + legendRows * 32, left: 60 },
    markPoint
  }
  if (!vchartInstance) { vchartInstance = new VChart(spec, { dom: customerHoursChart.value }); vchartInstance.renderSync() } else { vchartInstance.updateSpec(spec); vchartInstance.renderSync() }
}

watch(attendanceList, () => { buildStackedChartData(attendanceList.value) }, { deep: true })
watch(barWidth, () => { renderCustomerHoursChart() })
</script>

<style scoped>
.attendance-search { --el-font-size-base: 14px; }
.attendance-search .el-form-item { margin-right: 16px; margin-bottom: 10px; }
.attendance-search :deep(.el-form-item__label) { font-weight: 500; }
.attendance-search .el-input, .attendance-search .el-select { width: 260px; }
.customer-hours-chart { width: 100%; min-height: 520px; overflow: visible; }
.compact-table :deep(.el-table__cell) { padding: 4px 6px; font-size: 12px; }
</style>
