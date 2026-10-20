import request from '@/utils/request'

export function addComment(data) {
  return request({
    url: '/comment',
    method: 'post',
    data
  })
}

export function getEquipmentComments(equipmentId, params) {
  return request({
    url: '/comment/equipment/' + equipmentId,
    method: 'get',
    params
  })
}

export function getReplyComments(commentId) {
  return request({
    url: '/comment/reply/' + commentId,
    method: 'get'
  })
}

export function likeComment(commentId) {
  return request({
    url: '/comment/like/' + commentId,
    method: 'post'
  })
}

export function deleteComment(commentId) {
  return request({
    url: '/comment/' + commentId,
    method: 'delete'
  })
}
