import request from '@/utils/request'

export function getTagList(params) {
  return request({
    url: '/tag/list',
    method: 'get',
    params
  })
}

export function addTag(data) {
  return request({
    url: '/tag',
    method: 'post',
    data
  })
}

export function updateTag(data) {
  return request({
    url: '/tag',
    method: 'put',
    data
  })
}

export function deleteTag(id) {
  return request({
    url: '/tag/' + id,
    method: 'delete'
  })
}
