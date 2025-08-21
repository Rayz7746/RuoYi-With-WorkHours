import request from '@/utils/request'

// 查询考勤记录列表
export function listAttendance(query) {
  const params = { ...query }
  if (params.customerId != null) {
    params['project.customerId'] = params.customerId
    delete params.customerId
  }
  return request({ url: '/work/attendance/list', method: 'get', params })
}

// 查询考勤记录详细
export function getAttendance(attendanceId) {
  return request({
    url: '/work/attendance/' + attendanceId,
    method: 'get'
  })
}

// 新增考勤记录
export function addAttendance(data) {
  return request({
    url: '/work/attendance',
    method: 'post',
    data: data
  })
}

// 修改考勤记录
export function updateAttendance(data) {
  return request({
    url: '/work/attendance',
    method: 'put',
    data: data
  })
}

// 删除考勤记录
export function delAttendance(attendanceId) {
  return request({
    url: '/work/attendance/' + attendanceId,
    method: 'delete'
  })
}
