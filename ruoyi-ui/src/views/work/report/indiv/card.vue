<template>
  <div class="app-container individual-report">
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
      <!-- 项目（多选，受客户联动） -->
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
      <!-- 日期范围 -->
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
      <el-table-column label="客户名称" align="center" prop="project.customer.customerName" />
      <el-table-column label="项目名称" align="center" prop="project.name" />
      <el-table-column label="考勤日期" align="center" prop="attendanceDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.attendanceDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="工作时长" align="center" prop="workingHours" />
      <el-table-column label="备注" align="center" prop="comment" />
    </el-table>
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      :page-sizes="[5,10,20,30,50]"
      @pagination="getList"
    />
    <!-- 新增：图表卡片 -->
    <div class="charts-wrapper">
      <!-- 日期工时分布：超过 35 天或未选日期时隐藏 -->
      <div class="chart-card" v-if="showDateChart">
        <div class="chart-card-header">日期工时分布</div>
        <div ref="dateChartRef" class="chart-body" />
      </div>
      <div class="chart-card" v-else>
        <div class="chart-card-header">日期工时分布</div>
        <div class="chart-body empty-tip">
          {{ chartHideReason === 'over-range' ? `所选日期跨度超过 ${MAX_RANGE_DAYS} 天，未显示。` : '未选择日期范围，显示全部记录（日期分布关闭）' }}
        </div>
      </div>
      <div class="chart-card">
        <div class="chart-card-header">项目投入分布</div>
        <div ref="projectPieRef" class="chart-body" />
      </div>
    </div>
  </div>
</template>

<script setup name="IndividualReport">
import { ref, reactive, toRefs, watch, computed, onMounted, getCurrentInstance, nextTick } from 'vue'
import { getUserProfile } from '@/api/system/user'
import { listAttendanceArray } from '@/api/work/attendance'
import { projectNameSelect, customerSelect } from '@/api/work/assignment'
import VChart from '@visactor/vchart'

const loading = ref(false)
const error = ref(null)
const attendanceList = ref([])
const total = ref(0)
const userId = ref(null)
const dateRange = ref([])
const { proxy } = getCurrentInstance()

// 查询参数（人员固定为当前登录用户）
const state = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 5, // 改为每页显示5条
    customerIds: [],
    projectIds: [],
    startTime: '',
    endTime: ''
  }
})
const { queryParams } = toRefs(state)

// 仅保留原文件中的 getCurrentUserId
async function getCurrentUserId() {
  try {
    const response = await getUserProfile();
    userId.value = response?.data?.userId;
    if (!userId.value) throw new Error('未能从用户信息中获取用户ID');
  } catch (e) {
    error.value = e.message || '获取用户信息失败';
  }
}

// 选项：客户 / 项目
const customerOptions = ref([])
const enabledCustomerOptions = ref([])
const customerOptionsV2 = computed(() => (enabledCustomerOptions.value || []).map(c => ({
  value: c.customerId ?? c.value,
  label: c.customerName ?? c.label,
  disabled: c.isActiveCustomer === 0 || c.disabled === true
})).filter(o => o.value != null && o.label != null))
function getCustomerSelectOption() {
  customerSelect().then(res => {
    const list = res.data || []
    customerOptions.value = list
    enabledCustomerOptions.value = list.filter(c => !(c?.isActiveCustomer === 0 || c?.disabled === true))
  })
}

const allProjects = ref([])
function preloadAllProjects() { projectNameSelect().then(res => { allProjects.value = Array.isArray(res.data) ? res.data : [] }) }
function getProjectCustomerId(p) { return p?.customerId ?? p?.customer?.customerId ?? p?.customer?.id ?? null }
const enabledSearchProjectOptions = ref([])
const searchProjectOptionsV2 = computed(() => (enabledSearchProjectOptions.value || []).map(p => ({
  value: p.projectId ?? p.value,
  label: p.name ?? p.label,
  disabled: p.isActive === 0
})).filter(o => o.value != null && o.label != null))
function loadSearchProjectsByCustomers(customerIds) {
  if (!Array.isArray(customerIds) || customerIds.length === 0) { enabledSearchProjectOptions.value = []; return }
  const idSet = new Set(customerIds.map(String))
  const list = (allProjects.value || []).filter(p => idSet.has(String(getProjectCustomerId(p))))
  enabledSearchProjectOptions.value = list.filter(p => !(p?.isActive === 0 || p?.disabled === true || p?.status === 0 || p?.status === '0'))
}
watch(() => queryParams.value.customerIds, (cids) => { queryParams.value.projectIds = []; loadSearchProjectsByCustomers(cids) }, { deep: true })

// 日期范围同步
watch(dateRange, (val) => {
  if (Array.isArray(val) && val.length === 2) {
    queryParams.value.startTime = val[0]
    queryParams.value.endTime = val[1]
  } else {
    queryParams.value.startTime = ''
    queryParams.value.endTime = ''
  }
})

function setCurrentMonthRange() {
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth()
  const first = `${y}-${String(m+1).padStart(2,'0')}-01`
  const lastDay = new Date(y, m + 1, 0).getDate()
  const last = `${y}-${String(m+1).padStart(2,'0')}-${String(lastDay).padStart(2,'0')}`
  dateRange.value = [first, last]
  queryParams.value.startTime = first
  queryParams.value.endTime = last
}

function buildQueryParams() {
  const qp = { ...queryParams.value }
  if (!Array.isArray(qp.customerIds) || qp.customerIds.length === 0) delete qp.customerIds
  if (!Array.isArray(qp.projectIds) || qp.projectIds.length === 0) delete qp.projectIds
  if (!qp.startTime) delete qp.startTime
  if (!qp.endTime) delete qp.endTime
  // 固定用户
  qp.userIds = [userId.value]
  return qp
}

const dateChartRef = ref(null)
const projectPieRef = ref(null)
let dateChartInstance = null
let projectPieInstance = null
const stackedData = ref([]) // 日期-客户堆叠
const uniqueDates = ref([])
const uniqueCustomers = ref([])
const customerTotals = ref([]) // 嵌套饼 inner
const projectTotals = ref([])  // 嵌套饼 outer
const showDateChart = ref(true)
const MAX_RANGE_DAYS = 35
let lastShowDateChart = true
const overRangeDisabled = ref(false) // 记录是否因超范围关闭日期图
const chartHideReason = ref('') // '' | 'over-range' | 'all'

// 加载全部数据用于图表（忽略分页）
async function loadAllRowsForCharts(builtParams){
  try {
    const pageSize = 500
    let pageNum = 1
    const all = []
    while(true){
      const resp = await listAttendanceArray({ ...builtParams, pageNum, pageSize })
      const rows = resp?.rows || []
      all.push(...rows)
      const totalCount = resp?.total || 0
      if (pageNum * pageSize >= totalCount || rows.length === 0) break
      pageNum++
    }
    if(showDateChart.value && queryParams.value.startTime && queryParams.value.endTime){
      if(!dateChartRef.value) await nextTick()
      buildDailyStacked(all)
      renderDateChart()
    }
    buildNestedPie(all)
    renderProjectPie()
  } catch(e){ /* ignore */ }
}

function generateDateRange(start,end){
  if(!start||!end) return []
  const res=[]; let cur=new Date(start); const e=new Date(end)
  while(cur<=e){ res.push(formatDate(cur)); cur.setDate(cur.getDate()+1) }
  return res
}
function formatDate(d){ const y=d.getFullYear(); const m=String(d.getMonth()+1).padStart(2,'0'); const day=String(d.getDate()).padStart(2,'0'); return `${y}-${m}-${day}` }

function buildDailyStacked(rows){
  const start=queryParams.value.startTime; const end=queryParams.value.endTime
  const dateList=generateDateRange(start,end)
  const hoursMap=new Map(); const customerSet=new Set()
  for(const r of rows){
    const date=(r.attendanceDate||'').substring(0,10)
    if(!date||date<start||date>end) continue
    const customer=r?.project?.customer?.customerName || r?.project?.customerName || '未分配'
    const hrs=Number(r.workingHours)||0
    const key=date+'||'+customer
    hoursMap.set(key,(hoursMap.get(key)||0)+hrs)
    customerSet.add(customer)
  }
  if(customerSet.size===0) customerSet.add('无数据')
  uniqueDates.value=dateList
  uniqueCustomers.value=Array.from(customerSet)
  const data=[]
  for(const date of dateList){
    for(const c of uniqueCustomers.value){
      const key=date+'||'+c
      const h=hoursMap.get(key)||0
      // 保留 0 值数据以保证横轴完整
      data.push({ date, customer:c, hours:h })
    }
  }
  stackedData.value=data
}

function buildNestedPie(rows){
  // 构建客户与项目层级汇总，保证外环项目顺序与内环客户顺序对应且连续
  const custMap=new Map();
  const projectGroup=new Map();
  for(const r of rows){
    const customer=r?.project?.customer?.customerName || r?.project?.customerName || '未分配';
    const project=r?.project?.name || r?.projectName || '未命名项目';
    const hrs=Number(r.workingHours)||0;
    custMap.set(customer,(custMap.get(customer)||0)+hrs);
    if(!projectGroup.has(customer)) projectGroup.set(customer,[]);
    const arr=projectGroup.get(customer);
    const exist=arr.find(p=>p.type===project);
    if(exist){ exist.value += hrs; } else { arr.push({ type: project, customer, value: hrs }); }
  }
  const customerEntries = Array.from(custMap.entries()).sort((a,b)=>b[1]-a[1]);
  const orderedProjects=[];
  for(const [customer] of customerEntries){
    const list=projectGroup.get(customer)||[];
    list.sort((a,b)=>b.value-a.value);
    for(const item of list){ orderedProjects.push(item); }
  }
  customerTotals.value=customerEntries.map(([type,value])=>({ type, value: Number(value.toFixed(2)) }));
  projectTotals.value=orderedProjects.map(p=>({ ...p, value: Number(p.value.toFixed(2)) }));
  // 移除调试输出
  /* 校验逻辑保留，如需再开启可解除注释
  const diffReport=[]; for(const c of customerTotals.value){ const children=projectTotals.value.filter(p=>p.customer===c.type); const sumChildren=children.reduce((s,i)=>s+i.value,0); const diff=Number((c.value - sumChildren).toFixed(2)); if(Math.abs(diff) > 0.01){ diffReport.push({ customer:c.type, parent:c.value, childrenSum: sumChildren, diff }); } }
  if(diffReport.length){ console.warn('[NestedPie][Mismatch]', diffReport); }
  */
}

// 计算轴最大值辅助
function calcUpperBound(max){ if(!isFinite(max)||max<=0) return 1; const mag=Math.pow(10,Math.floor(Math.log10(max))); const norm=max/mag; let upperNorm; if(norm<=1) upperNorm=1; else if(norm<=2) upperNorm=2; else if(norm<=2.5) upperNorm=2.5; else if(norm<=5) upperNorm=5; else if(norm<=7.5) upperNorm=7.5; else upperNorm=10; let upper=upperNorm*mag; if(upper<max) upper+=mag*0.2; return upper }

function renderDateChart(){
  if(!dateChartRef.value) return
  const values=stackedData.value
  // 判断是否全部为 0
  const hasPositive = values.some(v => Number(v.hours) > 0)
  if(!hasPositive){
    // 清理旧实例
    if(dateChartInstance){ try{ dateChartInstance.release && dateChartInstance.release() }catch(_){} dateChartInstance=null }
    dateChartRef.value.innerHTML = '<div class="empty-tip">本期内没有工时数据</div>'
    return
  }
  const perDateTotal=new Map();
  for(const v of values){ perDateTotal.set(v.date,(perDateTotal.get(v.date)||0)+(Number(v.hours)||0)) }
  const maxTotal = Math.max(...Array.from(perDateTotal.values()),0)
  const axisMax=calcUpperBound(maxTotal*1.05)
  const todayStr=formatDate(new Date())
  const highlightColor = '#1677ff'
  const highlightShadow = 'rgba(22,119,255,0.35)'

  // 重新构建 markPoint：使用 detailed.vue 的结构 (itemContent.text / labelBackground)
  const markPoint=[]
  const markArea=[]
  for(const date of uniqueDates.value){
    const total = perDateTotal.get(date) || 0
    if(total>0){
      const isToday = date === todayStr
      const textStr = Number.isInteger(total)? String(total) : total.toFixed(1)
      markPoint.push({
        coordinate:{ date, hours: total },
        itemSymbol:{ visible:false },
        itemContent:{
          type:'text',
          autoRotate:false,
          offsetY:-6,
          text:{
            dy:14,
            text: textStr,
            style:{
              fill: isToday? '#fff':'#333',
              fontSize: isToday?13:12,
              fontWeight: isToday? 'bold':'600'
            },
            labelBackground:{
              padding:[2,6,2,6],
              style:{
                fill: isToday? highlightColor : '#eef2f7',
                stroke: isToday? highlightColor : '#eef2f7',
                cornerRadius:4,
                shadowBlur: isToday?6:0,
                shadowColor: highlightShadow
              }
            }
          }
        },
        itemLine:{ line:{ style:{ visible:false } }, startSymbol:{ visible:false }, endSymbol:{ visible:false } }
      })
    }
    if(date === todayStr){
      markArea.push({ coordinate:{ dateStart: date, dateEnd: date }, itemRect:{ style:{ fill:'#f5f9ff' } }, label:{ visible:false } })
    }
  }

  const spec={
    type:'bar',
    data:[{ id:'barData', values }],
    xField:'date',
    yField:'hours',
    seriesField:'customer',
    stack:true,
    bar:{ width: uniqueDates.value.length<=7?60: uniqueDates.value.length<=14?40: uniqueDates.value.length<=21?30:22, radius:2 },
    axes:[
      { orient:'bottom', type:'band', label:{ formatMethod:v=>v.slice(5), style: txt=>({ fill: todayStr.endsWith(txt)?highlightColor:'#666', fontWeight: todayStr.endsWith(txt)?'bold':'normal' }) } },
      { orient:'left', type:'linear', min:0, max: axisMax, nice:false, title:{ text:'工时(H)' } }
    ],
    tooltip:{ visible:true },
    legends:{ visible:true, orient:'bottom' },
    animation:{ appear:{ animation:'grow' } },
    padding:{ top:16, right:24, bottom:60, left:60 },
    height: 360,
    markPoint,
    markArea
  }
  if(!dateChartInstance){ dateChartInstance=new VChart(spec,{ dom: dateChartRef.value }); dateChartInstance.renderSync() } else { dateChartInstance.updateSpec(spec); dateChartInstance.renderSync() }
}

function renderProjectPie(){
  if(!projectPieRef.value) return
  const inner=customerTotals.value
  const outer=projectTotals.value
  if(inner.length===0 && outer.length===0){ projectPieRef.value.innerHTML='<div class="empty-tip">暂无数据</div>'; return }
  const parentMap = Object.fromEntries(inner.map(i=>[i.type, i.value]));
  const spec={
    type:'common',
    data:[ { id:'customers', values: inner }, { id:'projects', values: outer } ],
    series:[
      {
        type:'pie',
        dataIndex:0,
        outerRadius:0.55,
        innerRadius:0,
        valueField:'value',
        categoryField:'type',
        label:{ visible:true, position:'inside', style:{ fill:'#fff', fontSize:12 } },
        pie:{ style:{ stroke:'#fff', lineWidth:2 } },
        color:{ field:'type' },
        tooltip:{
          mark:{
            content:(items)=>{
              const it=items?.[0];
              const datum=Array.isArray(it?.datum)? it.datum[0] : (it?.datum||{});
              const val=Number(datum.value)||0;
              const hours=val.toFixed(2)+' h';
              return [
                { key:'客户', value: datum.type },
                { key:'工时', value: hours }
              ]
            }
          }
        }
      },
      {
        type:'pie',
        dataIndex:1,
        outerRadius:0.8,
        innerRadius:0.58,
        valueField:'value',
        categoryField:'type',
        // 使用项目名称区分颜色，避免同客户合并视觉
        color:{ field:'type' },
        label:{ visible:true },
        pie:{ style:{ stroke:'#fff', lineWidth:2 } },
        tooltip:{
          mark:{
            content:(items)=>{
              const it = items?.[0];
              // 兼容 it.datum 为单对象或数组包装两种形态
              const rawDatum = Array.isArray(it?.datum) ? it.datum[0] : (it?.datum || {});
              const projectName = rawDatum.type;
              const customerName = rawDatum.customer;
              const val = Number(rawDatum.value)||0;
              const hours = val.toFixed(2) + ' h';
              return [
                { key:'项目', value: projectName },
                { key:'所属客户', value: customerName },
                { key:'工时', value: hours }
              ]
            }
          }
        }
      }
    ],
    legends:{
      visible:true,
      orient:'right',
      filter:data=>data?.id==='customers'
    },
    title:{ visible:false },
    tooltip:{ visible:true },
    padding:{ top:12,right:12,bottom:12,left:12 },
    height:360
  }
  if(!projectPieInstance){ projectPieInstance=new VChart(spec,{ dom: projectPieRef.value }); projectPieInstance.renderSync() } else { projectPieInstance.updateSpec(spec); projectPieInstance.renderSync() }
}

// 修改 getList，加载分页 + 全量图表数据
function getList() {
  if (!userId.value) return
  loading.value = true
  const built = buildQueryParams()
  listAttendanceArray(built).then(res => {
    attendanceList.value = res.rows || []
    total.value = res.total || 0
    loadAllRowsForCharts(built)
  }).finally(()=>{ loading.value = false })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  const [start,end] = dateRange.value || []
  // 空日期 => 全部时间，不显示日期工时图
  if(!start || !end){
    showDateChart.value = false
    chartHideReason.value = 'all'
    overRangeDisabled.value = false
    queryParams.value.startTime = ''
    queryParams.value.endTime = ''
    // 销毁旧实例
    if(dateChartInstance){ try{ dateChartInstance.release && dateChartInstance.release() }catch(_){} dateChartInstance=null }
    getList()
    return
  }
  const diffDays = Math.floor((new Date(end) - new Date(start)) / 86400000) + 1
  if(diffDays > MAX_RANGE_DAYS){
    proxy?.$modal?.confirm(`日期范围超过 ${MAX_RANGE_DAYS} 天，日期工时分布图将不显示，确认继续?`).then(async ()=>{
      lastShowDateChart = showDateChart.value
      showDateChart.value = false
      chartHideReason.value = 'over-range'
      overRangeDisabled.value = true
      if(dateChartInstance){ try{ dateChartInstance.release && dateChartInstance.release() }catch(_){} dateChartInstance=null }
      await nextTick()
      getList()
    })
    return
  } else {
    showDateChart.value = true
    chartHideReason.value = ''
    overRangeDisabled.value = false
  }
  // 切回显示需要先销毁旧实例（可能之前隐藏时 DOM 被移除）
  if(dateChartInstance){ try{ dateChartInstance.release && dateChartInstance.release() }catch(_){} dateChartInstance=null }
  getList()
}
function resetQuery() {
  proxy.resetForm && proxy.resetForm('queryRef')
  queryParams.value.customerIds = []
  queryParams.value.projectIds = []
  setCurrentMonthRange()
  showDateChart.value = true
  chartHideReason.value = ''
  // 若之前关闭（空或超范围），简单策略：重新加载。不再整页刷新，除非多次失败可再考虑。
  if(dateChartInstance){ try{ dateChartInstance.release && dateChartInstance.release() }catch(_){} dateChartInstance=null }
  nextTick(()=>handleQuery())
}

onMounted(async () => {
  setCurrentMonthRange()
  await getCurrentUserId()
  preloadAllProjects()
  getCustomerSelectOption()
  if (userId.value && !error.value) getList()
})
</script>

<style scoped>
.attendance-search { --el-font-size-base: 14px; }
.attendance-search .el-form-item { margin-right: 16px; margin-bottom: 10px; }
.attendance-search :deep(.el-form-item__label) { font-weight: 500; }
.attendance-search .el-input, .attendance-search .el-select { width: 260px; }
.compact-table :deep(.el-table__cell) { padding: 4px 6px; font-size: 12px; }
.charts-wrapper { display: grid; gap: 20px; margin-top: 24px; grid-template-columns: 1fr 1fr; }
.chart-card { background:#fff; border:1px solid #e5e7eb; border-radius:8px; box-shadow:0 1px 2px rgba(0,0,0,0.04); display:flex; flex-direction:column; }
.chart-card-header { padding:12px 16px; border-bottom:1px solid #e5e7eb; font-weight:600; font-size:14px; color:#374151; }
.chart-body { flex:1; min-height:360px; }
.empty-tip { display:flex; align-items:center; justify-content:center; height:100%; color:#6b7280; font-size:13px; }
@media (max-width: 1100px){ .charts-wrapper { grid-template-columns: 1fr; } }
</style>
