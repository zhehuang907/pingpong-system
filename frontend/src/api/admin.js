import request from '@/utils/request'

export function getAdminDashboardOverview() {
  return request({
    url: '/admin/dashboard/overview',
    method: 'get'
  })
}

export function exportAdminDashboardOverview() {
  return request({
    url: '/admin/dashboard/export',
    method: 'get',
    responseType: 'blob'
  })
}

export function getAdminCommentStatistics() {
  return request({
    url: '/comment/admin/statistics',
    method: 'get'
  })
}

export function getAdminCommentList(params) {
  return request({
    url: '/comment/admin/list',
    method: 'get',
    params
  })
}

export function getAdminCommentDetail(commentId) {
  return request({
    url: `/comment/admin/${commentId}`,
    method: 'get'
  })
}

export function reviewAdminComment(data) {
  return request({
    url: '/comment/admin/review',
    method: 'put',
    data
  })
}

export function deleteAdminComment(commentId) {
  return request({
    url: `/comment/admin/${commentId}`,
    method: 'delete'
  })
}

export function batchDeleteAdminComments(ids) {
  return request({
    url: '/comment/admin/batch-delete',
    method: 'post',
    data: { ids }
  })
}
