const listeners = new Set()
let socket = null
let reconnectTimer = null
let manualClose = false

function buildWebSocketUrl() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/api/ws/updates`
}

function scheduleReconnect() {
  clearTimeout(reconnectTimer)
  reconnectTimer = window.setTimeout(() => {
    connectRealtime()
  }, 2000)
}

function connectRealtime() {
  if (typeof window === 'undefined') {
    return
  }

  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
    return
  }

  manualClose = false
  socket = new WebSocket(buildWebSocketUrl())

  socket.onmessage = (event) => {
    try {
      const message = JSON.parse(event.data)
      listeners.forEach((listener) => listener(message))
    } catch (error) {
      console.error('[Realtime] Failed to parse message:', error)
    }
  }

  socket.onclose = () => {
    socket = null
    if (!manualClose && listeners.size > 0) {
      scheduleReconnect()
    }
  }

  socket.onerror = () => {
    if (socket) {
      socket.close()
    }
  }
}

export function subscribeRealtimeUpdates(listener) {
  if (typeof listener !== 'function') {
    return () => {}
  }

  listeners.add(listener)
  connectRealtime()

  return () => {
    listeners.delete(listener)
    if (listeners.size === 0) {
      closeRealtimeConnection(false)
    }
  }
}

export function closeRealtimeConnection(permanent = true) {
  manualClose = permanent
  clearTimeout(reconnectTimer)
  reconnectTimer = null

  if (socket) {
    socket.close()
    socket = null
  }
}

export function isRealtimeEvent(message, types = []) {
  if (!message?.type) {
    return false
  }
  if (!Array.isArray(types) || types.length === 0) {
    return true
  }
  return types.includes(message.type)
}
