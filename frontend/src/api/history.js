import request from '@/utils/request'

export function recordHistory(payload) {
  if (typeof payload === 'number' || typeof payload === 'string') {
    return request({
      url: `/history/record/${payload}`,
      method: 'post'
    })
  }

  return request({
    url: '/history/record',
    method: 'post',
    data: payload
  })
}

export function getUserHistory(params) {
  return request({
    url: '/history/list',
    method: 'get',
    params
  })
}

export function clearUserHistory() {
  return request({
    url: '/history/clear',
    method: 'delete'
  })
}

export function deleteHistoryItem(historyId) {
  return request({
    url: `/history/${historyId}`,
    method: 'delete'
  })
}
