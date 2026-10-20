import request from '@/utils/request'

export function getPersonalRecommend(params) {
  return request({
    url: '/recommend/personal',
    method: 'get',
    params
  })
}

export function getSimilarRecommend(equipmentId, params) {
  return request({
    url: '/recommend/similar/' + equipmentId,
    method: 'get',
    params
  })
}

export function getHotRecommend(params) {
  return request({
    url: '/recommend/hot',
    method: 'get',
    params
  })
}

export function analyzeUserBehavior() {
  return request({
    url: '/recommend/analyze',
    method: 'get'
  })
}

export function getProfileInsight() {
  return request({
    url: '/recommend/profile-insight',
    method: 'get'
  })
}

export function getRecommendationWorkbench() {
  return request({
    url: '/recommend/workbench',
    method: 'get'
  })
}

export function recordClick(equipmentId) {
  return request({
    url: '/recommend/click/' + equipmentId,
    method: 'post'
  })
}
