import request from '@/utils/request'

export function getHomeLeaderboards(params) {
  return request({
    url: '/leaderboard/home',
    method: 'get',
    params
  })
}

export function getLeaderboardDetail(boardType, params) {
  return request({
    url: `/leaderboard/detail/${boardType}`,
    method: 'get',
    params
  })
}

export function getEquipmentLeaderboardPositions(equipmentId) {
  return request({
    url: `/leaderboard/equipment/${equipmentId}/positions`,
    method: 'get'
  })
}

export function getLeaderboardSubscriptions() {
  return request({
    url: '/leaderboard/subscriptions',
    method: 'get'
  })
}

export function subscribeLeaderboard(boardType) {
  return request({
    url: `/leaderboard/subscriptions/${boardType}`,
    method: 'post'
  })
}

export function unsubscribeLeaderboard(boardType) {
  return request({
    url: `/leaderboard/subscriptions/${boardType}`,
    method: 'delete'
  })
}

export function recordLeaderboardShare(data) {
  return request({
    url: '/leaderboard/share',
    method: 'post',
    data
  })
}

export function getLeaderboardShareStats(params) {
  return request({
    url: '/leaderboard/share/stats',
    method: 'get',
    params
  })
}
