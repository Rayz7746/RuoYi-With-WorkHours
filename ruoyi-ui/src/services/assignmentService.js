import { listAssignmentByUserId } from '@/api/work/assignment'
import { listDefaultProject } from '@/api/work/project'

/**
 * 获取合并后的项目分配列表（默认项目 + 指定用户分配，默认项目优先覆盖）
 * 规则：
 * 1. 先查询 isDefault=1 项目，映射成统一结构
 * 2. 再查询用户分配的 assignment 列表
 * 3. 如果 projectId 冲突，用默认项目的字段覆盖
 * 4. 默认项目的 dateStart/dateEnd 固定为 null
 * @param {number|string} userId
 * @returns {Promise<Array<object>>}
 */
export async function getAllAssignments(userId) {
  if (userId == null || userId === '') throw new Error('userId 不能为空')

  // 1. 获取默认项目
  const defaultResp = await listDefaultProject()
  console.log('[getAllAssignments] default raw =>', defaultResp)
  const defaultRows = Array.isArray(defaultResp?.rows) ? defaultResp.rows : (Array.isArray(defaultResp?.data) ? defaultResp.data : [])
  const defaultMapped = defaultRows.map(p => ({
    source: 'default',
    _isDefault: true,
    projectId: p.projectId,
    projectName: p.name,
    customerId: p.customer?.customerId ?? p.customerId ?? '',
    customerName: p.customer?.customerName ?? '',
    isActiveAssignment: 1,
    isActiveCustomer: 1,
    isActive: 1,
    dateStart: null,
    dateEnd: null
  }))

  // 2. 获取用户分配
  const assignResp = await listAssignmentByUserId(userId)
  console.log('[getAllAssignments] assignment raw =>', assignResp)
  const assignRows = Array.isArray(assignResp?.rows) ? assignResp.rows : (Array.isArray(assignResp?.data) ? assignResp.data : [])
  const assignmentMapped = assignRows.map(a => ({
    source: 'assignment',
    _isDefault: false,
    assignmentId: a.assignmentId,
    projectId: a.projectId,
    projectName: a.project?.name ?? '',
    customerId: a.project?.customer?.customerId ?? a.project?.customerId ?? '',
    customerName: a.project?.customer?.customerName ?? '',
    isActiveAssignment: (a.isActiveAssignment === 1 || a.isActiveAssignment === '1') ? 1 : 0,
    isActiveCustomer: a.project?.customer?.isActiveCustomer ?? '',
    isActive: a.project?.isActive ?? '',
    dateStart: a.dateStart ?? '',
    dateEnd: a.dateEnd ?? ''
  }))

  // 3. 合并（默认优先）
  const map = new Map()
  defaultMapped.forEach(d => map.set(d.projectId, { ...d }))
  assignmentMapped.forEach(a => {
    const existing = map.get(a.projectId)
    if (existing) {
      // 默认覆盖 assignment（保证 _isDefault 保持 true）
      const merged = { ...a, ...existing }
      if (!merged.customerName && a.customerName) merged.customerName = a.customerName
      merged._isDefault = existing._isDefault === true
      map.set(a.projectId, merged)
    } else {
      map.set(a.projectId, { ...a })
    }
  })

  const result = Array.from(map.values()).map(m => ({
    customerId: m.customerId,
    customerName: m.customerName,
    projectId: m.projectId,
    projectName: m.projectName,
    isActiveAssignment: m.isActiveAssignment,
    isActiveCustomer: m.isActiveCustomer,
    isActive: m.isActive,
    dateStart: m._isDefault ? null : (m.dateStart === '' ? null : m.dateStart),
    dateEnd: m._isDefault ? null : (m.dateEnd === '' ? null : m.dateEnd)
  }))

  console.log('[getAllAssignments] merged =>', result)
  return result
}
