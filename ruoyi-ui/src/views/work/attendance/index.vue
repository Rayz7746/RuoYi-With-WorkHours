<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px" class="attendance-search">
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

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['work:attendance:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['work:attendance:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['work:attendance:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['work:attendance:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="attendanceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
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
      <!-- 7. 操作 -->
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['work:attendance:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['work:attendance:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改考勤记录对话框 -->
    <el-dialog :title="title" v-model="open" width="720px" append-to-body class="attendance-dialog">
      <el-form ref="attendanceRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="20">
          <!-- 客户（表单联动） -->
          <el-col :span="12">
            <el-form-item label="客户">
              <el-select-v2
                v-model="form.customerId"
                :options="customerOptionsV2"
                filterable
                clearable
                placeholder="请选择客户"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <!-- 项目（受客户选择联动约束） -->
          <el-col :span="12">
            <el-form-item label="项目" prop="projectId">
              <el-select-v2
                v-model="form.projectId"
                :options="formProjectOptionsV2"
                filterable
                clearable
                placeholder="请先选择客户，再选择项目"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <!-- 项目人员 -->
          <el-col :span="12">
            <el-form-item label="项目人员" prop="userId">
              <el-select-v2
                v-model="form.userId"
                :options="memberOptionsV2"
                filterable
                clearable
                placeholder="请选择项目人员"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <!-- 考勤日期 -->
          <el-col :span="12">
            <el-form-item label="考勤日期" prop="attendanceDate">
              <el-date-picker
                clearable
                v-model="form.attendanceDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择考勤日期"
              />
            </el-form-item>
          </el-col>

          <!-- 工作时长 -->
          <el-col :span="12">
            <el-form-item label="工作时长" prop="workingHours">
              <el-input v-model="form.workingHours" placeholder="请输入工作时长" />
            </el-form-item>
          </el-col>

          <!-- 考勤备注 -->
          <el-col :span="24">
            <el-form-item label="考勤备注" prop="comment">
              <el-input v-model="form.comment" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Attendance">
import { ref, reactive, toRefs, computed, watch, getCurrentInstance, onMounted, nextTick } from "vue"
import { listAttendance, getAttendance, delAttendance, addAttendance, updateAttendance } from "@/api/work/attendance"
// 复用 assignment 的下拉接口
import { userSelect, projectNameSelect, customerSelect } from "@/api/work/assignment"

const { proxy } = getCurrentInstance()

const attendanceList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

// 编辑回填时的静默标志，避免 watch 清空 projectId
const silentlySyncingFormCustomer = ref(false)

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    customerId: null,
    projectId: null,
    userId: null,
    attendanceDate: null
  },
  rules: {
    projectId: [{ required: true, message: "项目不能为空", trigger: "blur" }],
    userId: [{ required: true, message: "项目人员不能为空", trigger: "blur" }],
    attendanceDate: [{ required: true, message: "考勤日期不能为空", trigger: "change" }],
    workingHours: [{ required: true, message: "工作时长不能为空", trigger: "blur" }],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询考勤记录列表 */
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

// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    attendanceId: null,
    customerId: null,   // UI 辅助字段，用于项目联动
    projectId: null,
    userId: null,
    attendanceDate: null,
    workingHours: null,
    comment: null
  }
  proxy.resetForm("attendanceRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.attendanceId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加考勤记录"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _attendanceId = row?.attendanceId || ids.value
  getAttendance(_attendanceId).then(response => {
    const d = response.data || {}
    const currentProject = d.project || {}
    const derivedCustomerId = currentProject.customerId
      ?? currentProject?.customer?.customerId
      ?? null
    const derivedProjectId = d.projectId
      ?? currentProject.projectId
      ?? currentProject.id
      ?? null

    silentlySyncingFormCustomer.value = true
    form.value = {
      attendanceId: d.attendanceId,
      customerId: derivedCustomerId,
      projectId: null, // 待项目选项加载后回填
      userId: d.userId ?? d.user?.userId ?? null,
      attendanceDate: d.attendanceDate ?? null,
      workingHours: d.workingHours ?? null,
      comment: d.comment ?? null
    }

    ensureAllProjects()
      .then(() => {
        loadFormProjectsByCustomer(derivedCustomerId)
      })
      .then(() => nextTick(() => {
        const match = (allProjects.value || []).find(p =>
          String(getProjectCustomerId(p)) === String(derivedCustomerId) &&
          String(p.projectId) === String(derivedProjectId)
        )
        form.value.projectId = match ? match.projectId : derivedProjectId
        silentlySyncingFormCustomer.value = false
      }))
      .finally(() => {
        open.value = true
        title.value = "修改考勤记录"
      })
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["attendanceRef"].validate(valid => {
    if (valid) {
      const payload = { ...form.value }
      // 清理仅前端使用的字段
      delete payload.customerId

      if (payload.attendanceId != null) {
        updateAttendance(payload).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addAttendance(payload).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _attendanceIds = row?.attendanceId || ids.value
  proxy.$modal.confirm('是否确认删除考勤记录编号为"' + _attendanceIds + '"的数据项？').then(function() {
    return delAttendance(_attendanceIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  const params = { ...queryParams.value }
  if (params.customerId != null) {
    params['project.customerId'] = params.customerId
    delete params.customerId
  }
  proxy.download('work/attendance/export', params, `attendance_${new Date().getTime()}.xlsx`)
}

/* ===== 下拉选项（用户 / 客户 / 项目，参照 Assignment） ===== */

// 项目人员下拉
const memberOptions = ref(undefined)
const enabledMemberOptions = ref(undefined)
const memberOptionsV2 = computed(() => {
  const list = enabledMemberOptions.value || []
  return list
    .map(u => ({
      value: u.userId ?? u.value,
      label: u.nickName ?? u.label,
      disabled: u.disabled === true || u.status === 1 || u.status === '1'
    }))
    .filter(o => o.value != null && o.label != null)
})
function getMemberSelectOption() {
  userSelect().then(res => {
    memberOptions.value = res.data || []
    enabledMemberOptions.value = filterDisabledMember(JSON.parse(JSON.stringify(memberOptions.value)))
  })
}
function filterDisabledMember(list) {
  return (list || []).filter(u => !(u?.disabled === true || u?.status === 1 || u?.status === '1'))
}

// 客户下拉
const customerOptions = ref(undefined)
const enabledCustomerOptions = ref(undefined)
const customerOptionsV2 = computed(() => {
  const list = enabledCustomerOptions.value || []
  return list
    .map(c => ({
      value: c.customerId ?? c.value,
      label: c.customerName ?? c.label,
      disabled: c.isActiveCustomer === 0 || c.disabled === true
    }))
    .filter(o => o.value != null && o.label != null)
})
function getCustomerSelectOption() {
  customerSelect().then(res => {
    const list = res.data || []
    customerOptions.value = list
    enabledCustomerOptions.value = (list || []).filter(c => !(c?.isActiveCustomer === 0 || c?.disabled === true))
  })
}

// 预加载所有项目（全量，本地按客户过滤）
const allProjects = ref([])
function preloadAllProjects() {
  projectNameSelect().then(res => {
    allProjects.value = Array.isArray(res.data) ? res.data : []
  })
}
function ensureAllProjects() {
  if (allProjects.value && allProjects.value.length > 0) return Promise.resolve()
  return projectNameSelect().then(res => {
    allProjects.value = Array.isArray(res.data) ? res.data : []
  })
}
function getProjectCustomerId(p) {
  return p?.customerId ?? p?.customer?.customerId ?? p?.customer?.id ?? null
}

// 搜索区项目选项（由 queryParams.customerId 驱动）
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

// 表单项目选项（由 form.customerId 驱动）
const enabledFormProjectOptions = ref([])
const formProjectOptionsV2 = computed(() => {
  const list = enabledFormProjectOptions.value || []
  return list.map(p => ({
    value: p.projectId ?? p.value,
    label: p.name ?? p.label,
    disabled: p.isActive === 0
  })).filter(o => o.value != null && o.label != null)
})
function loadFormProjectsByCustomer(customerId) {
  if (!customerId) {
    enabledFormProjectOptions.value = []
    form.value.projectId = null
    return
  }
  if (!silentlySyncingFormCustomer.value) {
    form.value.projectId = null
  }
  const list = (allProjects.value || []).filter(p => String(getProjectCustomerId(p)) === String(customerId))
  enabledFormProjectOptions.value = list.filter(p => !(p?.isActive === 0 || p?.disabled === true || p?.status === 0 || p?.status === '0'))
}

// 监听：搜索客户 -> 清空项目并加载客户项目
watch(() => queryParams.value.customerId, (cid) => {
  queryParams.value.projectId = null
  loadSearchProjectsByCustomer(cid)
})

// 监听：表单客户 -> 清空项目并加载客户项目（编辑回填时不清空）
watch(() => form.value.customerId, (cid) => {
  if (!cid) {
    enabledFormProjectOptions.value = []
    form.value.projectId = null
    return
  }
  if (!silentlySyncingFormCustomer.value) {
    form.value.projectId = null
  }
  loadFormProjectsByCustomer(cid)
})

// 初始化
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
/* 搜索区域样式 */
.attendance-search {
  --el-font-size-base: 14px;
}
.attendance-search .el-form-item {
  margin-right: 16px;
  margin-bottom: 10px;
}
.attendance-search :deep(.el-form-item__label) {
  font-weight: 500;
}
.attendance-search .el-input,
.attendance-search .el-select {
  width: 260px;
}

/* 弹窗样式 */
.attendance-dialog {
  --el-font-size-base: 14px;
}
.attendance-dialog :deep(.el-dialog__body) {
  padding-top: 6px;
}
.attendance-dialog .el-form {
  padding-top: 4px;
}
.attendance-dialog .el-input,
.attendance-dialog .el-select {
  width: 100%;
}
</style>
