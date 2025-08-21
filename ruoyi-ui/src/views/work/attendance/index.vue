<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="用户ID" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入用户ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="项目ID" prop="projectId">
        <el-input
          v-model="queryParams.projectId"
          placeholder="请输入项目ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="考勤日期" prop="attendanceDate">
        <el-date-picker clearable
          v-model="queryParams.attendanceDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择考勤日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="工作时长" prop="workingHours">
        <el-input
          v-model="queryParams.workingHours"
          placeholder="请输入工作时长"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="记录创建时间" prop="createAt">
        <el-date-picker clearable
          v-model="queryParams.createAt"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择记录创建时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="记录最后更新时间" prop="updateAt">
        <el-date-picker clearable
          v-model="queryParams.updateAt"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择记录最后更新时间">
        </el-date-picker>
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
      <el-table-column label="考勤记录唯一标识" align="center" prop="attendanceId" />
      <el-table-column label="用户ID" align="center" prop="userId" />
      <el-table-column label="项目ID" align="center" prop="projectId" />
      <el-table-column label="考勤日期" align="center" prop="attendanceDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.attendanceDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="工作时长" align="center" prop="workingHours" />
      <el-table-column label="考勤备注" align="center" prop="comment" />
      <el-table-column label="记录创建时间" align="center" prop="createAt" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createAt, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="记录最后更新时间" align="center" prop="updateAt" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.updateAt, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
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
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="attendanceRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户ID" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户ID" />
        </el-form-item>
        <el-form-item label="项目ID" prop="projectId">
          <el-input v-model="form.projectId" placeholder="请输入项目ID" />
        </el-form-item>
        <el-form-item label="考勤日期" prop="attendanceDate">
          <el-date-picker clearable
            v-model="form.attendanceDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择考勤日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="工作时长" prop="workingHours">
          <el-input v-model="form.workingHours" placeholder="请输入工作时长" />
        </el-form-item>
        <el-form-item label="考勤备注" prop="comment">
          <el-input v-model="form.comment" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="记录创建时间" prop="createAt">
          <el-date-picker clearable
            v-model="form.createAt"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择记录创建时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="记录最后更新时间" prop="updateAt">
          <el-date-picker clearable
            v-model="form.updateAt"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择记录最后更新时间">
          </el-date-picker>
        </el-form-item>
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
import { listAttendance, getAttendance, delAttendance, addAttendance, updateAttendance } from "@/api/work/attendance"

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

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: null,
    projectId: null,
    attendanceDate: null,
    workingHours: null,
    comment: null,
    createAt: null,
    updateAt: null
  },
  rules: {
    userId: [
      { required: true, message: "用户ID不能为空", trigger: "blur" }
    ],
    projectId: [
      { required: true, message: "项目ID不能为空", trigger: "blur" }
    ],
    attendanceDate: [
      { required: true, message: "考勤日期不能为空", trigger: "blur" }
    ],
    workingHours: [
      { required: true, message: "工作时长不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询考勤记录列表 */
function getList() {
  loading.value = true
  listAttendance(queryParams.value).then(response => {
    attendanceList.value = response.rows
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
    attendanceId: null,
    userId: null,
    projectId: null,
    attendanceDate: null,
    workingHours: null,
    comment: null,
    createAt: null,
    updateAt: null
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
  const _attendanceId = row.attendanceId || ids.value
  getAttendance(_attendanceId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改考勤记录"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["attendanceRef"].validate(valid => {
    if (valid) {
      if (form.value.attendanceId != null) {
        updateAttendance(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addAttendance(form.value).then(response => {
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
  const _attendanceIds = row.attendanceId || ids.value
  proxy.$modal.confirm('是否确认删除考勤记录编号为"' + _attendanceIds + '"的数据项？').then(function() {
    return delAttendance(_attendanceIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('work/attendance/export', {
    ...queryParams.value
  }, `attendance_${new Date().getTime()}.xlsx`)
}

getList()
</script>
