<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="90px" class="attendance-search">
      <!-- 客户（驱动项目联动） -->
      <el-form-item label="客户" prop="customerId">
        <el-select-v2
          v-model="queryParams.customerId"
          :options="customerOptionsV2"
          filterable
          clearable
          placeholder="请选择客户"
          style="width: 260px"
        />
      </el-form-item>

      <!-- 项目（由客户筛选） -->
      <el-form-item label="项目" prop="projectId">
        <el-select-v2
          v-model="queryParams.projectId"
          :options="searchProjectOptionsV2"
          filterable
          clearable
          placeholder="请先选择客户，再选择项目"
          style="width: 260px"
        />
      </el-form-item>

      <!-- 项目人员 -->
      <el-form-item label="项目人员" prop="userId">
        <el-select-v2
          v-model="queryParams.userId"
          :options="memberOptionsV2"
          filterable
          clearable
          placeholder="请选择项目人员"
          style="width: 260px"
        />
      </el-form-item>

      <!-- 考勤日期 -->
      <el-form-item label="考勤日期" prop="attendanceDate">
        <el-date-picker
          clearable
          v-model="queryParams.attendanceDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择考勤日期"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="attendanceList">
      <!-- 1. 项目客户 -->
      <el-table-column label="项目客户" align="center" prop="project.customer.customerName" />
      <!-- 2. 项目名称 -->
      <el-table-column label="项目名称" align="center" prop="project.name" />
      <!-- 3. 项目人员 -->
      <el-table-column label="项目人员" align="center" prop="user.nickName" />
      <!-- 4. 考勤日期 -->
      <el-table-column label="考勤日期" align="center" prop="attendanceDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.attendanceDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <!-- 5. 工作时长 -->
      <el-table-column label="工作时长" align="center" prop="workingHours" />
      <!-- 6. 考勤备注 -->
      <el-table-column label="考勤备注" align="center" prop="comment" />
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script setup name="Attendance">
import { ref, reactive, toRefs, computed, watch, getCurrentInstance, onMounted } from "vue"
import { listAttendance } from "@/api/work/attendance"
import { userSelect, projectNameSelect, customerSelect } from "@/api/work/assignment"

const { proxy } = getCurrentInstance()

const attendanceList = ref([])
const loading = ref(true)
const total = ref(0)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    customerId: null,
    projectId: null,
    userId: null,
    attendanceDate: null
  }
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listAttendance(queryParams.value).then(response => {
    const rows = response.rows || []
    attendanceList.value = rows
    total.value = response.total
  }).finally(() => {
    loading.value = false
  })
}
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

// 下拉：项目人员
const memberOptions = ref(undefined)
const enabledMemberOptions = ref(undefined)
const memberOptionsV2 = computed(() => {
  const list = enabledMemberOptions.value || []
  return list.map(u => ({
    value: u.userId ?? u.value,
    label: u.nickName ?? u.label,
    disabled: u.disabled === true || u.status === 1 || u.status === '1'
  })).filter(o => o.value != null && o.label != null)
})
function getMemberSelectOption() {
  userSelect().then(res => {
    memberOptions.value = res.data || []
    enabledMemberOptions.value = (memberOptions.value || []).filter(u => !(u?.disabled === true || u?.status === 1 || u?.status === '1'))
  })
}

// 下拉：客户
const customerOptions = ref(undefined)
const enabledCustomerOptions = ref(undefined)
const customerOptionsV2 = computed(() => {
  const list = enabledCustomerOptions.value || []
  return list.map(c => ({
    value: c.customerId ?? c.value,
    label: c.customerName ?? c.label,
    disabled: c.isActiveCustomer === 0 || c.disabled === true
  })).filter(o => o.value != null && o.label != null)
})
function getCustomerSelectOption() {
  customerSelect().then(res => {
    const list = res.data || []
    customerOptions.value = list
    enabledCustomerOptions.value = (list || []).filter(c => !(c?.isActiveCustomer === 0 || c?.disabled === true))
  })
}

// 项目
const allProjects = ref([])
function preloadAllProjects() {
  projectNameSelect().then(res => {
    allProjects.value = Array.isArray(res.data) ? res.data : []
  })
}
function getProjectCustomerId(p) {
  return p?.customerId ?? p?.customer?.customerId ?? p?.customer?.id ?? null
}
const enabledSearchProjectOptions = ref([])
const searchProjectOptionsV2 = computed(() => {
  const list = enabledSearchProjectOptions.value || []
  return list.map(p => ({
    value: p.projectId ?? p.value,
    label: p.name ?? p.label,
    disabled: p.isActive === 0
  })).filter(o => o.value != null && o.label != null)
})
function loadSearchProjectsByCustomer(customerId) {
  if (!customerId) {
    enabledSearchProjectOptions.value = []
    return
  }
  const list = (allProjects.value || []).filter(p => String(getProjectCustomerId(p)) === String(customerId))
  enabledSearchProjectOptions.value = list.filter(p => !(p?.isActive === 0 || p?.disabled === true || p?.status === 0 || p?.status === '0'))
}
watch(() => queryParams.value.customerId, (cid) => {
  queryParams.value.projectId = null
  loadSearchProjectsByCustomer(cid)
})

onMounted(() => {
  getCustomerSelectOption()
  getMemberSelectOption()
  preloadAllProjects()
  if (queryParams.value.customerId) {
    loadSearchProjectsByCustomer(queryParams.value.customerId)
  }
})
getList()
</script>

<style scoped>
.attendance-search { --el-font-size-base: 14px; }
.attendance-search .el-form-item { margin-right: 16px; margin-bottom: 10px; }
.attendance-search :deep(.el-form-item__label) { font-weight: 500; }
.attendance-search .el-input, .attendance-search .el-select { width: 260px; }
</style>
