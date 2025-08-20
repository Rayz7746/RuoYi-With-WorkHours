import request from '@/utils/request'

// 查询项目任务分配关系列表
export function listAssignment(query) {
  return request({
    url: '/work/assignment/list',
    method: 'get',
    params: query
  })
}

// 查询项目任务分配关系详细
export function getAssignment(assignmentId) {
  return request({
    url: '/work/assignment/' + assignmentId,
    method: 'get'
  })
}

// 新增项目任务分配关系
export function addAssignment(data) {
  return request({
    url: '/work/assignment',
    method: 'post',
    data: data
  })
}

// 修改项目任务分配关系
export function updateAssignment(data) {
  return request({
    url: '/work/assignment',
    method: 'put',
    data: data
  })
}

// 删除项目任务分配关系
export function delAssignment(assignmentId) {
  return request({
    url: '/work/assignment/' + assignmentId,
    method: 'delete'
  })
}

