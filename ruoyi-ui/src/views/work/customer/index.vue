<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px" class="customer-search">
      <el-form-item label="客户名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入客户名称，如“专网”" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="客户代码" prop="code">
        <el-input v-model="queryParams.code" placeholder="请输入客户代码，如“ZW”" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="是否启用" prop="isActive">
        <el-select v-model="queryParams.isActive" placeholder="是否启用" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
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
          v-hasPermi="['work:customer:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['work:customer:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['work:customer:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['work:customer:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="customerList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="客户名称" align="center" prop="name" />
      <el-table-column label="客户代码" align="center" prop="code" />
      <el-table-column label="客户描述" align="center" prop="description" />
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
            v-hasPermi="['work:customer:edit']"
          ></el-button>
          <el-button
            size="large"
            type="text"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['work:customer:remove']"
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

    <!-- 添加或修改客户信息对话框 -->
    <el-dialog :title="title" v-model="open" width="720px" append-to-body class="customer-dialog">
      <el-form ref="customerRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="客户名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入客户名称，如“专网”" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户代码" prop="code">
              <el-input v-model="form.code" placeholder="请输入客户代码，如“ZW”" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="客户描述" prop="description">
              <el-input v-model="form.description" type="textarea" :autosize="{minRows:3,maxRows:6}" placeholder="请输入内容" />
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

<script setup name="Customer">
import { listCustomer, getCustomer, delCustomer, addCustomer, updateCustomer } from "@/api/work/customer"

const { proxy } = getCurrentInstance()

const customerList = ref([])
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
    name: null,
    code: null,
    description: null,
    isActive: null
  },
  rules: {
    name: [
      { required: true, message: '客户名称，如“专网”不能为空', trigger: 'blur' }
    ],
    code: [
      { required: true, message: '客户代码，如“ZW”不能为空', trigger: 'blur' }
    ],
    isActive: [
      { required: true, message: '是否启用不能为空', trigger: 'change' }
    ]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询客户信息列表 */
function getList() {
  loading.value = true
  listCustomer(queryParams.value).then(response => {
    customerList.value = (response.rows || []).map(r => ({
      ...r,
      isActive: r.isActive === 1 || r.isActive === '1' ? 1 : 0
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
    customerId: null,
    name: null,
    code: null,
    description: null,
    isActive: 1
  }
  proxy.resetForm("customerRef")
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
  ids.value = selection.map(item => item.customerId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加客户信息"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _customerId = row?.customerId || ids.value
  getCustomer(_customerId).then(response => {
    const d = response.data || {}
    form.value = {
      ...d,
      isActive: d.isActive === 1 || d.isActive === '1' ? 1 : 0
    }
    open.value = true
    title.value = "修改客户信息"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["customerRef"].validate(valid => {
    if (valid) {
      const payload = { ...form.value }
      payload.isActive = Number(payload.isActive)
      delete payload.createdAt
      delete payload.updatedAt
      if (payload.customerId != null) {
        updateCustomer(payload).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addCustomer(payload).then(() => {
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
  const _customerIds = row?.customerId || ids.value
  proxy.$modal.confirm('是否确认删除客户信息编号为"' + _customerIds + '"的数据项？').then(function() {
    return delCustomer(_customerIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('work/customer/export', {
    ...queryParams.value
  }, `customer_${new Date().getTime()}.xlsx`)
}

// 仅在用户点击时触发，返回 boolean/Promise<boolean>
function beforeToggleActive(row) {
  const next = row.isActive === 1 ? 0 : 1
  const text = next === 1 ? '启用' : '停用'
  return new Promise((resolve) => {
    proxy.$modal.confirm(`确认要"${text}"客户"${row.name || row.customerId}"吗?`)
      .then(() => {
        row.__switchLoading = true
        const payload = {
          customerId: row.customerId,
          name: row.name,
          code: row.code,
          description: row.description,
          isActive: next
        }
        return updateCustomer(payload)
      })
      .then(() => {
        proxy.$modal.msgSuccess(text + "成功")
        getList()
        resolve(true)
      })
      .catch(() => resolve(false))
      .finally(() => { row.__switchLoading = false })
  })
}

getList()
</script>

<style scoped>
/* 搜索区域美化 */
.customer-search {
  --el-font-size-base: 14px;
}
.customer-search .el-form-item {
  margin-right: 16px;
  margin-bottom: 10px;
}
.customer-search :deep(.el-form-item__label) {
  font-weight: 500;
}
.customer-search .el-input,
.customer-search .el-select {
  width: 260px;
}

/* 弹窗美化 */
.customer-dialog {
  --el-font-size-base: 14px;
}
.customer-dialog :deep(.el-dialog__body) {
  padding-top: 6px;
}
.customer-dialog .el-form {
  padding-top: 4px;
}
.customer-dialog .el-input,
.customer-dialog .el-select {
  width: 100%;
}
.customer-dialog :deep(.el-textarea__inner) {
  min-height: 88px;
}
</style>
