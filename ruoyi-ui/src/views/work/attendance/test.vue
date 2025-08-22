<template>
  <div class="app-container">
    <h1>我的新页面</h1>
    <p>仅显示当前用户ID</p>

    <div v-if="error" style="color:red">错误：{{ error }}</div>
    <h2 v-else>{{ userId }}</h2>

    <!-- 合并结果（服务返回） -->
    <h3 style="margin-top:24px;">最终合并列表 getAllAssignments()</h3>
    <table v-if="allAssignments.length" style="margin-top:8px;width:100%;border-collapse:collapse;">
      <thead>
        <tr style="background:#f5f7fa;">
          <th style="border:1px solid #ddd;padding:6px;">序号</th>
          <th style="border:1px solid #ddd;padding:6px;">Customer ID</th>
          <th style="border:1px solid #ddd;padding:6px;">Customer Name</th>
          <th style="border:1px solid #ddd;padding:6px;">Project ID</th>
          <th style="border:1px solid #ddd;padding:6px;">Project Name</th>
          <th style="border:1px solid #ddd;padding:6px;">isActiveAssignment</th>
          <th style="border:1px solid #ddd;padding:6px;">isActiveCustomer</th>
          <th style="border:1px solid #ddd;padding:6px;">isActive(Project)</th>
          <th style="border:1px solid #ddd;padding:6px;">dateStart</th>
          <th style="border:1px solid #ddd;padding:6px;">dateEnd</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item,idx) in allAssignments" :key="item.projectId">
          <td style="border:1px solid #ddd;padding:6px;">{{ idx + 1 }}</td>
            <td style="border:1px solid #ddd;padding:6px;">{{ item.customerId }}</td>
          <td style="border:1px solid #ddd;padding:6px;">{{ item.customerName }}</td>
          <td style="border:1px solid #ddd;padding:6px;">{{ item.projectId }}</td>
          <td style="border:1px solid #ddd;padding:6px;">{{ item.projectName }}</td>
          <td style="border:1px solid #ddd;padding:6px;">{{ item.isActiveAssignment }}</td>
          <td style="border:1px solid #ddd;padding:6px;">{{ item.isActiveCustomer }}</td>
          <td style="border:1px solid #ddd;padding:6px;">{{ item.isActive }}</td>
          <td style="border:1px solid #ddd;padding:6px;">{{ item.dateStart == null || item.dateStart === '' ? '-' : item.dateStart }}</td>
          <td style="border:1px solid #ddd;padding:6px;">{{ item.dateEnd == null || item.dateEnd === '' ? '-' : item.dateEnd }}</td>
        </tr>
      </tbody>
    </table>

    <h3 style="margin-top:32px;">JSON 预览</h3>
    <pre v-if="allAssignments.length" style="background:#111;color:#cce;padding:8px;white-space:pre;overflow:auto;">{{ allAssignmentsJson }}</pre>
  </div>
</template>

<script name="TestPage">
import { ref, computed, onMounted } from 'vue'
import { getUserProfile } from '@/api/system/user'
import { getAllAssignments } from '@/services/assignmentService'

export default {
  name: 'TestPage',
  setup() {
    const userId = ref('')
    const error = ref(null)
    const allAssignments = ref([])

    const allAssignmentsJson = computed(() => JSON.stringify(allAssignments.value, null, 2))

    onMounted(async () => {
      try {
        const res = await getUserProfile()
        const id = res?.data?.userId
        if (!id) throw new Error('未获取到用户ID')
        userId.value = id
        allAssignments.value = await getAllAssignments(id)
      } catch (e) {
        error.value = e.message || '获取用户ID失败'
      }
    })

    return { userId, error, allAssignments, allAssignmentsJson }
  }
}
</script>