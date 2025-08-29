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

      <!-- 项目（多选） -->
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

      <!-- 新增：部门（多选树） -->
      <el-form-item label="部门" prop="deptIds">
        <el-tree-select
          v-model="queryParams.deptIds"
          :data="enabledDeptOptions"
          node-key="id"
          :props="{ label: 'label', children: 'children', disabled: 'disabled' }"
          multiple
          show-checkbox
          check-strictly
          collapse-tags
          collapse-tags-tooltip
          filterable
          clearable
          placeholder="请选择部门(可多选)"
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
import { deptTreeSelect } from "@/api/system/user"
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
    deptIds: [], // 新增
    startTime: '',
    endTime: ''
  }
})
const { queryParams } = toRefs(data)

function buildQueryParams() {
  const qp = { ...queryParams.value }
  ;['customerIds','projectIds','userIds','deptIds'].forEach(k => { if (!Array.isArray(qp[k]) || qp[k].length === 0) delete qp[k] })
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
    // 异步加载全量聚合(项目)
    loadAllProjectHours(builtParams)
  }).finally(() => { loading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() {
  proxy.resetForm("queryRef")
  queryParams.value.customerIds = []
  queryParams.value.projectIds = []
  queryParams.value.userIds = []
  queryParams.value.deptIds = [] // 新增
  // 重置为当前月
  setCurrentMonthRange()
  handleQuery()
  applyDeptFilter() // 还原人员
}

// 项目人员
// ============= 新增：部门树 & 人员按部门过滤 =============
const deptOptions = ref([])
const enabledDeptOptions = ref([])
function filterDisabledDept(list = []) { return list.filter(d => { if (d.disabled) return false; if (Array.isArray(d.children) && d.children.length) d.children = filterDisabledDept(d.children); return true }) }
function getDeptTree() { deptTreeSelect().then(res => { deptOptions.value = res.data || []; enabledDeptOptions.value = filterDisabledDept(JSON.parse(JSON.stringify(deptOptions.value))) }) }

function getUserDeptId(u) { return u?.deptId ?? u?.dept_id ?? u?.dept?.deptId ?? u?.dept?.dept_id ?? null }
function baseMemberEnabled(u) { return !(u?.disabled === true || u?.status === 1 || u?.status === '1') }
function applyDeptFilter() {
  const selected = queryParams.value.deptIds
  if (!Array.isArray(memberOptions.value)) { enabledMemberOptions.value = []; return }
  if (!Array.isArray(selected) || selected.length === 0) {
    enabledMemberOptions.value = (memberOptions.value || []).filter(baseMemberEnabled)
  } else {
    const set = new Set(selected.map(String))
    enabledMemberOptions.value = (memberOptions.value || []).filter(u => baseMemberEnabled(u) && set.has(String(getUserDeptId(u))))
  }
}
watch(() => queryParams.value.deptIds, () => { queryParams.value.userIds = []; applyDeptFilter() }, { deep: true })
// 修改成员获取：获取后做部门过滤
function getMemberSelectOption() { userSelect().then(res => { memberOptions.value = res.data || []; applyDeptFilter() }) }
// 调整 memberOptions/ enabledMemberOptions 初始化为数组
const memberOptions = ref([])
const enabledMemberOptions = ref([])
const memberOptionsV2 = computed(() => (enabledMemberOptions.value || []).map(u => ({ value: u.userId ?? u.value, label: u.nickName ?? u.label, disabled: !baseMemberEnabled(u) })).filter(o => o.value != null && o.label != null))
// ============= 新增结束 =============

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
// 全量项目工时汇总 [{ project, value, customerSet: Set/Array }]
const fullProjectHours = ref([])
// 若需要项目下其他维度明细，可在此扩展
let fullLoadingFlag = false

async function loadAllProjectHours(builtParams) {
  if (fullLoadingFlag) return
  fullLoadingFlag = true
  try {
    const pageSize = 500
    let pageNum = 1
    const projectMap = new Map() // key: projectName -> { value: hours, customers: Set }
    while (true) {
      const resp = await listAttendanceArray({ ...builtParams, pageNum, pageSize })
      const rows = resp?.rows || []
      for (const row of rows) {
        const projectName = row?.project?.name || row?.projectName || '未命名项目'
        const customerName = row?.project?.customer?.customerName || row?.project?.customerName || '未分配'
        const hours = Number(row?.workingHours) || 0
        if (!projectMap.has(projectName)) projectMap.set(projectName, { value: 0, customers: new Set() })
        const obj = projectMap.get(projectName)
        obj.value += hours
        obj.customers.add(customerName)
      }
      const totalCount = resp?.total || 0
      if (pageNum * pageSize >= totalCount || rows.length === 0) break
      pageNum++
    }
    fullProjectHours.value = Array.from(projectMap.entries()).map(([project, v]) => ({ project, value: v.value, customers: Array.from(v.customers) })).sort((a,b)=>b.value-a.value)
  } catch (e) { console.warn('[project.vue] loadAllProjectHours error', e) } finally { fullLoadingFlag = false }
}

function buildProjectDataFromCurrentPage() {
  const projectMap = new Map()
  for (const row of attendanceList.value) {
    const projectName = row?.project?.name || row?.projectName || '未命名项目'
    const customerName = row?.project?.customer?.customerName || row?.project?.customerName || '未分配'
    const hours = Number(row?.workingHours) || 0
    if (!projectMap.has(projectName)) projectMap.set(projectName, { value: 0, customers: new Set() })
    const obj = projectMap.get(projectName)
    obj.value += hours
    obj.customers.add(customerName)
  }
  return Array.from(projectMap.entries()).map(([project, v]) => ({ project, value: v.value, customers: Array.from(v.customers) })).sort((a,b)=>b.value-a.value)
}

function getProjectData() {
  return fullProjectHours.value.length ? fullProjectHours.value : buildProjectDataFromCurrentPage()
}

// 新增：封装当前月份范围设置
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

function renderCustomerHoursChart() { // 函数名沿用以减少其它引用改动
  if (!customerHoursChart.value) return
  const values = getProjectData()

  // 计算一个更友好的上限，使最大柱子不贴边
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
    if (upper === max) upper += mag * 0.2 // 再往上加 20%
    return upper
  }
  const rawMax = values.reduce((m,d)=>Math.max(m, Number(d.value)||0), 0)
  const axisMax = calcUpperBound(rawMax)

  // 动态高度：项目数量 * (条宽+间距) + padding
  const BAR_WIDTH = 20
  const BAR_GAP = 20
  const basePadding = 60
  const targetHeight = Math.max(values.length * (BAR_WIDTH + BAR_GAP) + basePadding, 140)
  const h = targetHeight + 'px'
  if (customerHoursChart.value.style.height !== h) customerHoursChart.value.style.height = h

  const spec = {
    type: 'bar',
    data: [{ id: 'barData', values }],
    direction: 'horizontal',
    xField: 'value',
    yField: 'project',
    bar: { radius: 4, width: BAR_WIDTH, maxWidth: BAR_WIDTH, minWidth: BAR_WIDTH, groupPadding: 0.4, innerPadding: 1 },
    axes: [
      { orient: 'left', type: 'band', title: { text: '项目' }, bandPadding: 0.4 },
      { orient: 'bottom', type: 'linear', title: { text: '工时 (Hours)' }, min: 0, max: axisMax, nice: false }
    ],
    label: { visible: true, position: 'right', format: '{value}', style: { fill: '#000', fontWeight: 600, fontSize: 11 } },
    tooltip: {
      visible: true,
      mark: {
        title: { field: 'project' },
        content: [
          { key: '工时', value: datum => datum.value },
        ]
      }
    },
    color: { range: ['#1f77b4'] },
    animation: { appear: { animation: 'grow' } },
    padding: { top: 10, right: 32, bottom: 30, left: 120 },
    legend: { visible: false },
    height: targetHeight
  }
  if (!vchartInstance) {
    vchartInstance = new VChart(spec, { dom: customerHoursChart.value })
    vchartInstance.renderSync()
  } else {
    vchartInstance.updateSpec(spec)
    vchartInstance.renderSync()
  }
}

watch(attendanceList, () => { renderCustomerHoursChart() }, { deep: true })
watch(fullProjectHours, () => { renderCustomerHoursChart() }, { deep: true })

onMounted(() => {
  setCurrentMonthRange()
  getCustomerSelectOption()
  getMemberSelectOption()
  preloadAllProjects()
  getDeptTree() // 新增
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
.customer-hours-chart { height: 360px; margin: 16px 0 8px; }
.compact-table :deep(.el-table__cell) { padding: 4px 6px; font-size: 12px; }
</style>
