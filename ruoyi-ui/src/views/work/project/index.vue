<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px" class="project-search">
      <el-form-item label="项目名称" prop="name">
        <el-input
          v-model="queryParams.name"
          :placeholder="searchNamePlaceholder"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="项目代码" prop="code">
        <el-input
          v-model="queryParams.code"
          :placeholder="searchCodePlaceholder"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="关联客户ID" prop="customerId">
        <el-select-v2
          v-model="queryParams.customerId"
          :options="customerOptionsV2"
          filterable
          clearable
          placeholder="请选择关联客户"
          style="width: 260px"
        />
      </el-form-item>
      <el-form-item label="项目负责人ID" prop="projectManagerId">
        <el-input
          v-model="queryParams.projectManagerId"
          placeholder="请输入项目负责人ID(关联sys_user.user_id)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="联系人" prop="contactPerson">
        <el-input
          v-model="queryParams.contactPerson"
          placeholder="请输入联系人"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>

      <!-- 搜索：是否启用 下拉 -->
      <el-form-item label="是否启用" prop="isActive">
        <el-select v-model="queryParams.isActive" placeholder="是否启用" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>

      <!-- 搜索：是否默认项目 下拉 -->
      <el-form-item label="是否默认项目" prop="isDefault">
        <el-select v-model="queryParams.isDefault" placeholder="是否默认项目" clearable>
          <el-option label="是" :value="1" />
          <el-option label="否" :value="0" />
        </el-select>
      </el-form-item>

      <!-- 搜索：是否可计费 下拉 -->
      <el-form-item label="是否可计费" prop="isBillable">
        <el-select v-model="queryParams.isBillable" placeholder="是否可计费" clearable>
          <el-option label="可计费" :value="1" />
          <el-option label="不可计费" :value="0" />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['work:project:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['work:project:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['work:project:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['work:project:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="projectList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="项目名称" align="center" prop="name" />
      <el-table-column label="项目代码" align="center" prop="code" />
      <!-- <el-table-column label="关联客户ID" align="center" prop="customerId" /> -->
      <el-table-column label="关联客户" align="center" prop="customer.customerName" />
      <!-- <el-table-column label="项目负责人ID" align="center" prop="projectManagerId" /> -->
      <el-table-column label="项目负责人" align="center" prop="user.nickName" />
      <el-table-column label="项目描述" align="center" prop="description" />
      <el-table-column label="联系人" align="center" prop="contactPerson" />

      <!-- 是否默认项目 开关 -->
      <el-table-column label="是否默认项目" align="center" prop="isDefault" width="120">
        <template #default="scope">
          <el-switch
            v-model="scope.row.isDefault"
            :active-value="1"
            :inactive-value="0"
            :loading="scope.row.__switchLoading || false"
            :before-change="() => beforeToggleFlag(scope.row, 'isDefault', '设为默认', '取消默认')"
          />
        </template>
      </el-table-column>

      <!-- 是否可计费 开关 -->
      <el-table-column label="是否可计费" align="center" prop="isBillable" width="120">
        <template #default="scope">
          <el-switch
            v-model="scope.row.isBillable"
            :active-value="1"
            :inactive-value="0"
            :loading="scope.row.__switchLoading || false"
            :before-change="() => beforeToggleFlag(scope.row, 'isBillable', '设为可计费', '设为不可计费')"
          />
        </template>
      </el-table-column>

      <!-- 是否启用 开关 -->
      <el-table-column label="是否启用" align="center" prop="isActive" width="100">
        <template #default="scope">
          <el-switch
            v-model="scope.row.isActive"
            :active-value="1"
            :inactive-value="0"
            :loading="scope.row.__switchLoading || false"
            :before-change="() => beforeToggleActive(scope.row)"
          />
        </template>
      </el-table-column>

      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="large"
            type="text"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['work:project:edit']"
          />
          <el-button
            size="large"
            type="text"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['work:project:remove']"
          />
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

    <!-- 添加或修改项目信息对话框 -->
    <el-dialog :title="title" v-model="open" width="720px" append-to-body class="project-dialog">
      <el-form ref="projectRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="项目名称" prop="name">
              <el-input v-model="form.name" :placeholder="formNamePlaceholder" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目代码" prop="code">
              <el-input v-model="form.code" :placeholder="formCodePlaceholder" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="关联客户ID" prop="customerId">
              <el-select-v2
                v-model="form.customerId"
                :options="customerOptionsV2"
                filterable
                clearable
                placeholder="请选择关联客户"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目负责人ID" prop="projectManagerId">
              <el-input v-model="form.projectManagerId" placeholder="请输入项目负责人ID(关联sys_user.user_id)" />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="项目描述" prop="description">
              <el-input v-model="form.description" type="textarea" :autosize="{minRows:3,maxRows:6}" placeholder="请输入内容" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="联系人" prop="contactPerson">
              <el-input v-model="form.contactPerson" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>

          <!-- 表单：是否默认项目 单选 -->
          <el-col :span="12">
            <el-form-item label="是否默认项目" prop="isDefault">
              <el-radio-group v-model="form.isDefault">
                <el-radio :label="1">是</el-radio>
                <el-radio :label="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>

          <!-- 表单：是否可计费 单选 -->
          <el-col :span="12">
            <el-form-item label="是否可计费" prop="isBillable">
              <el-radio-group v-model="form.isBillable">
                <el-radio :label="1">可计费</el-radio>
                <el-radio :label="0">不可计费</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="是否启用" prop="isActive">
              <el-radio-group v-model="form.isActive">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">停用</el-radio>
              </el-radio-group>
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

<script setup name="Project">
import { listProject, getProject, delProject, addProject, updateProject, customerSelect } from "@/api/work/project"

const { proxy } = getCurrentInstance()

const projectList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const isCreate = ref(false)
const customerOptions = ref(undefined)
const enabledCustomerOptions = ref(undefined)

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: null,
    code: null,
    customerId: null,
    projectManagerId: null,
    description: null,
    contactPerson: null,
    isDefault: null,
    isBillable: null,
    isActive: null
  },
  rules: {
    name: [
      { required: true, message: '项目名称不能为空', trigger: 'blur' }
    ],
    code: [
      { required: true, message: '项目代码不能为空', trigger: 'blur' }
    ],
    customerId: [
      { required: true, message: '关联客户ID不能为空', trigger: 'blur' }
    ],
    isActive: [
      { required: true, message: '是否启用不能为空', trigger: 'change' }
    ]
  }
})

const { queryParams, form, rules } = toRefs(data)

// 占位文案
const searchNamePlaceholder = computed(() => '请输入项目名称，如“AI - 人工智能”')
const searchCodePlaceholder = computed(() => '请输入项目代码，如“AI”')
const formNamePlaceholder = computed(() => isCreate.value ? '请输入项目名称，如“AI - 人工智能”' : '请输入项目名称')
const formCodePlaceholder = computed(() => isCreate.value ? '请输入项目代码，如“AI”' : '请输入项目代码')

// 将后端数据映射为 el-select-v2 需要的 { value, label, disabled } 结构
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

/* 自定义客户下拉菜单 */
function getCustomerSelectOption() {
  customerSelect().then(response => {
    customerOptions.value = response.data
    enabledCustomerOptions.value = filterDisabledCustomer(JSON.parse(JSON.stringify(response.data)))
  })
}

/** 过滤禁用的客户 */
function filterDisabledCustomer(customerList) {
  return customerList.filter(customer => {
    if (customer.isActiveCustomer) {
      return true
    }
    else {
      return false
    }
  })
}


/** 查询项目信息列表 */
function getList() {
  loading.value = true
  listProject(queryParams.value).then(response => {
    projectList.value = (response.rows || []).map(r => ({
      ...r,
      isActive: r.isActive === 1 || r.isActive === '1' ? 1 : 0,
      isDefault: r.isDefault === 1 || r.isDefault === '1' ? 1 : 0,
      isBillable: r.isBillable === 1 || r.isBillable === '1' ? 1 : 0
    }))
    total.value = response.total
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
    projectId: null,
    name: null,
    code: null,
    customerId: null,
    projectManagerId: null,
    description: null,
    contactPerson: null,
    isDefault: 0,  // 默认：否
    isBillable: 1, // 可计费：是
    isActive: 1    // 启用：是
  }
  proxy.resetForm('projectRef')
  isCreate.value = false
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.projectId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  isCreate.value = true
  open.value = true
  title.value = '添加项目信息'
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _projectId = row?.projectId || ids.value
  getProject(_projectId).then(response => {
    const d = response.data || {}
    form.value = {
      ...d,
      isActive: d.isActive === 1 || d.isActive === '1' ? 1 : 0,
      isDefault: d.isDefault === 1 || d.isDefault === '1' ? 1 : 0,
      isBillable: d.isBillable === 1 || d.isBillable === '1' ? 1 : 0
    }
    isCreate.value = false
    open.value = true
    title.value = '修改项目信息'
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs['projectRef'].validate(valid => {
    if (!valid) return
    const payload = { ...form.value }
    payload.isActive = Number(payload.isActive)
    payload.isDefault = Number(payload.isDefault)
    payload.isBillable = Number(payload.isBillable)
    delete payload.createdAt
    delete payload.updatedAt

    if (payload.projectId != null) {
      updateProject(payload).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        open.value = false
        getList()
      })
    } else {
      addProject(payload).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        open.value = false
        getList()
      })
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _projectIds = row?.projectId || ids.value
  proxy.$modal.confirm('是否确认删除项目信息编号为"' + _projectIds + '"的数据项？').then(function () {
    return delProject(_projectIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('work/project/export', {
    ...queryParams.value
  }, `project_${new Date().getTime()}.xlsx`)
}

// 表格开关：是否启用
function beforeToggleActive(row) {
  const next = row.isActive === 1 ? 0 : 1
  const text = next === 1 ? '启用' : '停用'
  return new Promise((resolve) => {
    proxy.$modal.confirm(`确认要"${text}"项目"${row.name || row.projectId}"吗?`)
      .then(() => {
        row.__switchLoading = true
        const payload = {
          projectId: row.projectId,
          name: row.name,
          code: row.code,
          customerId: row.customerId,
          projectManagerId: row.projectManagerId,
          description: row.description,
          contactPerson: row.contactPerson,
          isDefault: row.isDefault,
          isBillable: row.isBillable,
          isActive: next
        }
        return updateProject(payload)
      })
      .then(() => {
        proxy.$modal.msgSuccess(text + '成功')
        getList()
        resolve(true)
      })
      .catch(() => resolve(false))
      .finally(() => { row.__switchLoading = false })
  })
}

// 表格开关：通用布尔字段切换（默认/可计费）
function beforeToggleFlag(row, field, onText, offText) {
  const next = row[field] === 1 ? 0 : 1
  const text = next === 1 ? onText : offText
  return new Promise((resolve) => {
    proxy.$modal.confirm(`确认要"${text}"项目"${row.name || row.projectId}"吗?`)
      .then(() => {
        row.__switchLoading = true
        const payload = {
          projectId: row.projectId,
          name: row.name,
          code: row.code,
          customerId: row.customerId,
          projectManagerId: row.projectManagerId,
          description: row.description,
          contactPerson: row.contactPerson,
          isDefault: row.isDefault,
          isBillable: row.isBillable,
          isActive: row.isActive
        }
        payload[field] = next
        return updateProject(payload)
      })
      .then(() => {
        proxy.$modal.msgSuccess(text + '成功')
        getList()
        resolve(true)
      })
      .catch(() => resolve(false))
      .finally(() => { row.__switchLoading = false })
  })
}

onMounted(() => {
  getList()
  getCustomerSelectOption()
})
</script>

<style scoped>
/* 搜索区域美化 */
.project-search {
  --el-font-size-base: 14px;
}
.project-search .el-form-item {
  margin-right: 16px;
  margin-bottom: 10px;
}
.project-search :deep(.el-form-item__label) {
  font-weight: 500;
}
.project-search .el-input,
.project-search .el-select {
  width: 260px;
}

/* 弹窗美化 */
.project-dialog {
  --el-font-size-base: 14px;
}
.project-dialog :deep(.el-dialog__body) {
  padding-top: 6px;
}
.project_dialog .el-form {
  padding-top: 4px;
}
.project_dialog .el-input,
.project_dialog .el-select {
  width: 100%;
}
.project_dialog :deep(.el-textarea__inner) {
  min-height: 88px;
}
</style>
