import request from '@/utils/request'

export function listAiChatSessions() {
  return request({
    url: '/ai-chat/sessions',
    method: 'get'
  })
}

export function createAiChatSession(data = {}) {
  return request({
    url: '/ai-chat/sessions',
    method: 'post',
    data
  })
}

export function getAiChatSessionDetail(sessionId) {
  return request({
    url: `/ai-chat/sessions/${sessionId}`,
    method: 'get'
  })
}

export function updateAiChatSession(sessionId, data) {
  return request({
    url: `/ai-chat/sessions/${sessionId}`,
    method: 'put',
    data
  })
}

export function deleteAiChatSession(sessionId) {
  return request({
    url: `/ai-chat/sessions/${sessionId}`,
    method: 'delete'
  })
}

export function sendAiChatMessage(data) {
  return request({
    url: '/ai-chat/messages',
    method: 'post',
    data,
    timeout: 60000  // AI 聊天需要更长的超时时间（60秒）
  })
}
