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
const loading = ref(true)
const total = ref(0)
const dateRange = ref([]) // [begin,end]

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
    // 异步加载全量聚合
    loadAllCustomerHours(builtParams)
  }).finally(() => { loading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() {
  proxy.resetForm("queryRef")
  queryParams.value.customerIds = []
  queryParams.value.projectIds = []
  queryParams.value.userIds = []
  // 重置为当前月
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

// 新增：封装当前月份范围设置（初始 & 重置使用）
function setCurrentMonthRange() {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth()
  const first = `${year}-${String(month + 1).padStart(2,'0')}-01`
  const lastDay = new Date(year, month + 1, 0).getDate()
  const last = `${year}-${String(month + 1).padStart(2,'0')}-${String(lastDay).padStart(2,'0')}`
  dateRange.value = [first, last]
  queryParams.value.startTime = first
  queryParams.value.endTime = last
}

function renderCustomerHoursChart() {
  if (!customerHoursChart.value) return

  // 构建（总工时 + 项目明细）数据
  function buildAggregatedChartData() {
    let totals, projList
    if (fullCustomerHours.value.length) {
      totals = fullCustomerHours.value
      projList = fullCustomerProjectHours.value.length ? fullCustomerProjectHours.value : []
    } else {
      const tmp = buildProjectDataFromCurrentPage()
      totals = tmp.totals
      projList = tmp.projList
    }
    const projGroup = new Map()
    for (const p of projList) {
      if (!projGroup.has(p.customer)) projGroup.set(p.customer, [])
      projGroup.get(p.customer).push({ project: p.project, value: p.value })
    }
    return totals.slice().sort((a,b)=>b.value-a.value).map(t => ({
      customer: t.name,
      value: t.value,
      projects: (projGroup.get(t.name) || []).sort((a,b)=>b.value-a.value)
    }))
  }

  const values = buildAggregatedChartData()

  function calcUpperBound(max) {
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
    if (upper === max) upper += mag * 0.2
    return upper
  }
  const rawMax = values.reduce((m,d)=>Math.max(m, Number(d.value)||0), 0)
  const axisMax = calcUpperBound(rawMax)

  const BAR_WIDTH = 20
  const BAR_GAP = 20
  const basePadding = 60
  // 不再限制高度：按数据量线性增长（不设置最小/最大）
  const dynamicHeight = values.length * (BAR_WIDTH + BAR_GAP) + basePadding
  const spec = {
    type: 'bar',
    data: [{ id: 'barData', values }],
    direction: 'horizontal',
    xField: 'value',
    yField: 'customer',
    bar: { radius: 4, width: BAR_WIDTH, maxWidth: BAR_WIDTH, minWidth: BAR_WIDTH, groupPadding: 0.4, innerPadding: 1 },
    axes: [
      { orient: 'left', type: 'band', title: { text: '客户' }, bandPadding: 0.4 },
      { orient: 'bottom', type: 'linear', title: { text: '工时 (Hours)' }, min: 0, max: axisMax, nice: false }
    ],
    label: { visible: true, position: 'right', format: '{value}', style: { fill: '#000', fontWeight: 600, fontSize: 11 } },
    tooltip: {
      visible: true,
      mark: {
        title: { field: 'customer' },
        content: items => {
          const extractDataItem = (src) => {
            if (!src) return null
            if (Array.isArray(src)) { return extractDataItem(src[0]) }
            if (src.datum) { if (Array.isArray(src.datum)) return src.datum[0]; return src.datum }
            return src
          }
          const dataItem = extractDataItem(items)
          const list = dataItem?.projects || []
          if (!Array.isArray(list) || list.length === 0) { return [{ key: '无项目数据', value: '' }] }
          return list.map(p => ({ key: p.project, value: p.value }))
        }
      }
    },
    color: { range: ['#1f77b4'] },
    animation: { appear: { animation: 'grow' } },
    padding: { top: 10, right: 32, bottom: 30, left: 110 },
    legend: { visible: false },
    height: dynamicHeight
  }

  if (!vchartInstance) { vchartInstance = new VChart(spec, { dom: customerHoursChart.value }); vchartInstance.renderSync() } else { vchartInstance.updateSpec(spec); vchartInstance.renderSync() }
}

watch(attendanceList, () => { renderCustomerHoursChart() }, { deep: true })
watch(fullCustomerHours, () => { renderCustomerHoursChart() }, { deep: true })
watch(fullCustomerProjectHours, () => { renderCustomerHoursChart() }, { deep: true })

onMounted(() => {
  setCurrentMonthRange()
  getCustomerSelectOption()
  getMemberSelectOption()
  preloadAllProjects()
  if (queryParams.value.customerIds.length) loadSearchProjectsByCustomers(queryParams.value.customerIds)
  renderCustomerHoursChart()
  getList()
})
</script>

<style scoped>
.attendance-search { --el-font-size-base: 14px; }
.attendance-search .el-form-item { margin-right: 16px; margin-bottom: 10px; }
.attendance-search :deep(.el-form-item__label) { font-weight: 500; }
.attendance-search .el-input, .attendance-search .el-select { width: 260px; }
/* 去除固定高度，让图表随数据增长 */
.customer-hours-chart { width: 100%; min-height: 140px; margin: 16px 0 8px; }
.compact-table :deep(.el-table__cell) { padding: 4px 6px; font-size: 12px; }
</style>
