<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px" class="assignment-search">
      <!-- 新增：客户 下拉，驱动项目联动（搜索区） -->
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

      <!-- 项目 下拉（仅显示所选客户的项目） -->
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
      <!-- 替换：user_id 文本输入 -> 项目人员 下拉 -->
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
      <el-form-item label="角色" prop="role">
        <el-input
          v-model="queryParams.role"
          placeholder="请输入用户在项目中的角色"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否有效" prop="isActiveAssignment">
        <el-select v-model="queryParams.isActiveAssignment" placeholder="是否有效" clearable>
          <el-option label="有效" :value="1" />
          <el-option label="无效" :value="0" />
        </el-select>
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
          v-hasPermi="['work:assignment:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['work:assignment:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['work:assignment:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['work:assignment:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="assignmentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <!-- <el-table-column label="project_id" align="center" prop="projectId" /> -->
      <el-table-column label="项目客户" align="center" prop="project.customer.customerName" />
      <el-table-column label="项目名称" align="center" prop="project.name" />
            <!-- <el-table-column label="user_id" align="center" prop="userId" /> -->
      <el-table-column label="项目人员" align="center" prop="user.nickName" />
      <el-table-column label="角色" align="center" prop="role" />
      <el-table-column label="是否有效" align="center" prop="isActiveAssignment" width="120">
        <template #default="scope">
          <el-switch
            v-model="scope.row.isActiveAssignment"
            :active-value="1"
            :inactive-value="0"
            :loading="scope.row.__switchLoading || false"
            :before-change="() => beforeToggleActive(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="任务开始日期" align="center" prop="dateStart" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.dateStart, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="任务结束日期" align="center" prop="dateEnd" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.dateEnd, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="large"
            type="text"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['work:assignment:edit']"
          ></el-button>
          <el-button
            size="large"
            type="text"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['work:assignment:remove']"
          ></el-button>
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

    <!-- 添加或修改项目任务分配关系对话框 -->
    <el-dialog :title="title" v-model="open" width="720px" append-to-body class="assignment-dialog">
      <el-form ref="assignmentRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="20">
          <!-- 新增：客户 下拉（表单联动） -->
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

          <!-- 替换：user_id 文本输入 -> 项目人员 下拉 -->
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
          <el-col :span="12">
            <el-form-item label="角色" prop="role">
              <el-input v-model="form.role" placeholder="请输入用户在项目中的角色" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否有效" prop="isActiveAssignment">
              <el-radio-group v-model="form.isActiveAssignment">
                <el-radio :label="1">有效</el-radio>
                <el-radio :label="0">无效</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>

          <!-- 新增：是否启用日期 -->
          <el-col :span="24">
            <el-form-item label="是否启用日期">
              <el-radio-group v-model="form.enableDate" @change="onEnableDateChange">
                <el-radio :label="1">是</el-radio>
                <el-radio :label="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>

          <!-- 仅启用日期时展示日期选择框 -->
          <el-col :span="12" v-if="form.enableDate === 1">
            <el-form-item label="任务开始日期" prop="dateStart">
              <el-date-picker
                clearable
                v-model="form.dateStart"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择任务开始日期"
                @change="() => proxy.$refs['assignmentRef']?.validateField('dateEnd')"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.enableDate === 1">
            <el-form-item label="任务结束日期" prop="dateEnd">
              <el-date-picker
                clearable
                v-model="form.dateEnd"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择任务结束日期"
                @change="() => proxy.$refs['assignmentRef']?.validateField('dateStart')"
              />
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

<script setup name="Assignment">
import { listAssignment, getAssignment, delAssignment, addAssignment, updateAssignment, userSelect, projectNameSelect, customerSelect } from "@/api/work/assignment"
import { ref, reactive, toRefs, computed, watch, getCurrentInstance, onMounted, nextTick } from "vue"  // 修正：显式引入

const { proxy } = getCurrentInstance()

const assignmentList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    customerId: null,       // 新增：搜索区客户
    projectId: null,
    userId: null,
    role: null,
    isActiveAssignment: null
  },
  rules: {
    projectId: [{ required: true, message: "项目不能为空", trigger: "blur" }],
    userId: [{ required: true, message: "项目人员不能为空", trigger: "blur" }],
    isActiveAssignment: [{ required: true, message: "是否有效不能为空", trigger: "change" }],
    dateStart: [{ validator: validateDateStart, trigger: "change" }],
    dateEnd:   [{ validator: validateDateEnd,   trigger: "change" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

// 新增：启用日期时的综合校验（开始必填并需 ≤ 结束）
function validateDateStart(rule, value, callback) {
  const enabled = data.form?.enableDate === 1
  if (!enabled) return callback()
  const start = value
  const end = data.form?.dateEnd
  if (!start) return callback(new Error("任务开始日期不能为空"))
  if (end && start > end) return callback(new Error("任务开始日期不能晚于结束日期"))
  callback()
}

// 新增：启用日期时的综合校验（结束必填并需 ≥ 开始）
function validateDateEnd(rule, value, callback) {
  const enabled = data.form?.enableDate === 1
  if (!enabled) return callback()
  const end = value
  const start = data.form?.dateStart
  if (!end) return callback(new Error("任务结束日期不能为空"))
  if (start && end < start) return callback(new Error("任务结束日期不能早于开始日期"))
  callback()
}

// 切换是否启用日期：关闭则清空并清除错误
function onEnableDateChange(val) {
  if (val !== 1) {
    form.value.dateStart = null
    form.value.dateEnd = null
    nextTick(() => proxy.$refs['assignmentRef']?.clearValidate(['dateStart', 'dateEnd']))
  }
}

/** 查询项目任务分配关系列表 */
function getList() {
  loading.value = true
  listAssignment(queryParams.value).then(response => {
    const rows = response.rows || []
    assignmentList.value = rows.map(r => ({
      ...r,
      isActiveAssignment: r.isActiveAssignment === 1 || r.isActiveAssignment === '1' ? 1 : 0
    }))
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
    assignmentId: null,
    customerId: null,       // UI 辅助字段，用于项目联动
    projectId: null,
    userId: null,
    role: null,
    isActiveAssignment: 1,
    // 新增：是否启用日期（默认否）
    enableDate: 0,
    dateStart: null,
    dateEnd: null
  }
  proxy.resetForm("assignmentRef")
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
  ids.value = selection.map(item => item.assignmentId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加项目任务分配关系"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _assignmentId = row?.assignmentId || ids.value
  getAssignment(_assignmentId).then(response => {
    const d = response.data || {}
    const currentProject = d.project || {}
    const derivedCustomerId = currentProject.customerId
      ?? currentProject?.customer?.customerId
      ?? null

    form.value = {
      ...d,
      customerId: derivedCustomerId,
      isActiveAssignment: d.isActiveAssignment === 1 || d.isActiveAssignment === '1' ? 1 : 0,
      // 根据后端日期是否存在推断启用状态
      enableDate: (d.dateStart != null || d.dateEnd != null) ? 1 : 0
    }

    // 加载该客户的项目列表，确保项目下拉可见当前项目
    loadFormProjectsByCustomer(derivedCustomerId)
    open.value = true
    title.value = "修改项目任务分配关系"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["assignmentRef"].validate(valid => {
    if (valid) {
      const payload = { ...form.value }
      payload.isActiveAssignment = Number(payload.isActiveAssignment)

      // 若未启用日期，则提交 null
      if (Number(form.value.enableDate) !== 1) {
        payload.dateStart = null
        payload.dateEnd = null
      }

      // 清理后端不需要或只读字段
      delete payload.enableDate
      delete payload.createAt
      delete payload.updateAt
      delete payload.customerId   // 仅联动用

      if (payload.assignmentId != null) {
        updateAssignment(payload).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addAssignment(payload).then(() => {
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
  const _assignmentIds = row?.assignmentId || ids.value
  proxy.$modal.confirm('是否确认删除项目任务分配关系编号为"' + _assignmentIds + '"的数据项？').then(function() {
    return delAssignment(_assignmentIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('work/assignment/export', {
    ...queryParams.value
  }, `assignment_${new Date().getTime()}.xlsx`)
}

// 仅在用户点击时触发，返回 boolean/Promise<boolean>
function beforeToggleActive(row) {
  const next = row.isActiveAssignment === 1 ? 0 : 1
  const text = next === 1 ? '设为有效' : '设为无效'
  return new Promise((resolve) => {
    proxy.$modal.confirm(`确认要"${text}"该分配记录"${row.assignmentId}"吗?`)
      .then(() => {
        row.__switchLoading = true
        const payload = {
          assignmentId: row.assignmentId,
          projectId: row.projectId,
          userId: row.userId,
          role: row.role,
          dateStart: row.dateStart,
          dateEnd: row.dateEnd,
          isActiveAssignment: next
        }
        return updateAssignment(payload)
      })
      .then(() => {
        proxy.$modal.msgSuccess("操作成功")
        getList()
        resolve(true)
      })
      .catch(() => resolve(false))
      .finally(() => { row.__switchLoading = false })
  })
}

// 新增：项目人员下拉数据
const memberOptions = ref(undefined)
const enabledMemberOptions = ref(undefined)

// 新增：将后端用户列表映射为 el-select-v2 需要的结构
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

// 新增：获取项目人员（这里复用项目经理选择接口，后端返回用户 userId/nickName）
function getMemberSelectOption() {
  userSelect().then(res => {
    memberOptions.value = res.data || []
    enabledMemberOptions.value = filterDisabledMember(JSON.parse(JSON.stringify(memberOptions.value)))
  })
}

// 新增：过滤禁用用户
function filterDisabledMember(list) {
  return (list || []).filter(u => !(u?.disabled === true || u?.status === 1 || u?.status === '1'))
}

// 新增：项目下拉数据
const projectOptions = ref(undefined)
const enabledProjectOptions = ref(undefined)

// 映射为 el-select-v2 结构（显示项目名称）
const projectOptionsV2 = computed(() => {
  const list = enabledProjectOptions.value || []
  return list
    .map(p => ({
      value: p.projectId ?? p.value,
      label: p.name ?? p.label,
      disabled: p.isActive === 0 || p.disabled === true
    }))
    .filter(o => o.value != null && o.label != null)
})

// 获取项目下拉（使用 assignment.js 的 projectNameSelect）
function getProjectSelectOption() {
  projectNameSelect().then(res => {
    projectOptions.value = res.data || []
    enabledProjectOptions.value = filterDisabledProject(JSON.parse(JSON.stringify(projectOptions.value)))
  })
}

// 过滤禁用/停用项目
function filterDisabledProject(list) {
  return (list || []).filter(p => !(p?.isActive === 0 || p?.disabled === true))
}

// 搜索与表单都要用到的客户下拉
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

// 搜索区：项目选项（由 queryParams.customerId 驱动）
const searchProjectOptions = ref([])
const enabledSearchProjectOptions = ref([])
const searchProjectOptionsV2 = computed(() => {
  const list = enabledSearchProjectOptions.value || []
  return list.map(p => ({
    value: p.projectId ?? p.value,
    label: p.name ?? p.label,
    disabled: p.isActive === 0 || p.disabled === true || p.status === 0 || p.status === '0'
  })).filter(o => o.value != null && o.label != null)
})

// 表单：项目选项（由 form.customerId 驱动）
const formProjectOptions = ref([])                 // 新增
const enabledFormProjectOptions = ref([])          // 新增
const formProjectOptionsV2 = computed(() => {      // 新增
  const list = enabledFormProjectOptions.value || []
  return list.map(p => ({
    value: p.projectId ?? p.value,
    label: p.name ?? p.label,
    disabled: p.isActive === 0 || p.disabled === true || p.status === 0 || p.status === '0'
  })).filter(o => o.value != null && o.label != null)
})

// 预加载所有项目（不带 query，前端本地按 customer 过滤）
const allProjects = ref([])
function preloadAllProjects() {
  projectNameSelect().then(res => {
    allProjects.value = Array.isArray(res.data) ? res.data : []
  })
}

// 工具：获取项目归属客户ID（兼容 project.customer.customerId）
function getProjectCustomerId(p) {
  return p?.customerId ?? p?.customer?.customerId ?? p?.customer?.id ?? null
}

// 搜索区：根据客户过滤
function loadSearchProjectsByCustomer(customerId) {
  if (!customerId) {
    enabledSearchProjectOptions.value = []
    return
  }
  const list = (allProjects.value || []).filter(p => String(getProjectCustomerId(p)) === String(customerId))
  enabledSearchProjectOptions.value = list.filter(p => !(p?.isActive === 0 || p?.disabled === true || p?.status === 0 || p?.status === '0'))
}

// 表单：根据客户过滤
function loadFormProjectsByCustomer(customerId) {
  if (!customerId) {
    enabledFormProjectOptions.value = []
    return
  }
  const list = (allProjects.value || []).filter(p => String(getProjectCustomerId(p)) === String(customerId))
  enabledFormProjectOptions.value = list.filter(p => !(p?.isActive === 0 || p?.disabled === true || p?.status === 0 || p?.status === '0'))
}

// 监听：搜索客户 -> 清空项目并加载客户项目
watch(() => queryParams.value.customerId, (cid) => {
  queryParams.value.projectId = null
  loadSearchProjectsByCustomer(cid)
})

// 监听：表单客户 -> 清空项目并加载客户项目
watch(() => form.value.customerId, (cid) => {
  form.value.projectId = null
  loadFormProjectsByCustomer(cid)
})

// handleUpdate 内已按 project.customer 兼容，这里保持不变
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
/* 搜索区域美化 */
.assignment-search {
  --el-font-size-base: 14px;
}
.assignment-search .el-form-item {
  margin-right: 16px;
  margin-bottom: 10px;
}
.assignment-search :deep(.el-form-item__label) {
  font-weight: 500;
}
.assignment-search .el-input,
.assignment-search .el-select {
  width: 260px;
}

/* 弹窗美化 */
.assignment-dialog {
  --el-font-size-base: 14px;
}
.assignment-dialog :deep(.el-dialog__body) {
  padding-top: 6px;
}
.assignment-dialog .el-form {
  padding-top: 4px;
}
.assignment-dialog .el-input,
.assignment-dialog .el-select {
  width: 100%;
}
</style>
