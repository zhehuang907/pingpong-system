const TOKEN_KEY = 'token'
const USER_INFO_KEY = 'userInfo'
const REMEMBER_ME_KEY = 'auth:rememberMe'
const SESSION_MARKER_KEY = 'auth:sessionMarker'
const LAST_ACCOUNT_KEY = 'auth:lastAccount'
const AUTH_SCOPE_KEY = 'auth:scope'

function getAuthScope() {
  return sessionStorage.getItem(AUTH_SCOPE_KEY) || ''
}

function isSessionScope() {
  return getAuthScope() === 'session'
}

function clearSessionAuth() {
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(USER_INFO_KEY)
  sessionStorage.removeItem(REMEMBER_ME_KEY)
  sessionStorage.removeItem(SESSION_MARKER_KEY)
  sessionStorage.removeItem(AUTH_SCOPE_KEY)
}

function clearLocalAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_INFO_KEY)
  localStorage.removeItem(REMEMBER_ME_KEY)
}

function readAuthItem(key) {
  if (isSessionScope()) {
    return sessionStorage.getItem(key) || ''
  }
  return localStorage.getItem(key) || sessionStorage.getItem(key) || ''
}

export function initAuthState() {
  if (isSessionScope()) {
    const sessionToken = sessionStorage.getItem(TOKEN_KEY)
    if (!sessionToken) {
      clearSessionAuth()
      return
    }
    sessionStorage.setItem(SESSION_MARKER_KEY, '1')
    return
  }

  const hasToken = !!localStorage.getItem(TOKEN_KEY)
  const rememberMe = localStorage.getItem(REMEMBER_ME_KEY) === '1'
  const sessionAlive = sessionStorage.getItem(SESSION_MARKER_KEY) === '1'

  if (hasToken && !rememberMe && !sessionAlive) {
    clearLocalAuth()
    return
  }

  if (hasToken) {
    sessionStorage.setItem(SESSION_MARKER_KEY, '1')
    sessionStorage.setItem(AUTH_SCOPE_KEY, 'local')
  }
}

export function getToken() {
  return readAuthItem(TOKEN_KEY)
}

export function getStoredUserInfo() {
  try {
    return JSON.parse(readAuthItem(USER_INFO_KEY) || '{}')
  } catch (error) {
    return {}
  }
}

export function persistAuthSession({ token, userInfo, rememberMe = false }) {
  if (!token) return

  const serializedUserInfo = JSON.stringify(userInfo || {})

  if (rememberMe) {
    clearSessionAuth()
    clearLocalAuth()
    localStorage.setItem(TOKEN_KEY, token)
    localStorage.setItem(USER_INFO_KEY, serializedUserInfo)
    localStorage.setItem(REMEMBER_ME_KEY, '1')
    sessionStorage.setItem(AUTH_SCOPE_KEY, 'local')
  } else {
    clearSessionAuth()
    sessionStorage.setItem(TOKEN_KEY, token)
    sessionStorage.setItem(USER_INFO_KEY, serializedUserInfo)
    sessionStorage.setItem(REMEMBER_ME_KEY, '0')
    sessionStorage.setItem(AUTH_SCOPE_KEY, 'session')
  }

  sessionStorage.setItem(SESSION_MARKER_KEY, '1')
}

export function updateStoredUserInfo(userInfo) {
  const serializedUserInfo = JSON.stringify(userInfo || {})
  if (isSessionScope()) {
    sessionStorage.setItem(USER_INFO_KEY, serializedUserInfo)
    return
  }
  if (localStorage.getItem(TOKEN_KEY)) {
    localStorage.setItem(USER_INFO_KEY, serializedUserInfo)
    return
  }
  sessionStorage.setItem(USER_INFO_KEY, serializedUserInfo)
}

export function clearAuthState() {
  if (isSessionScope()) {
    clearSessionAuth()
    return
  }
  clearLocalAuth()
  clearSessionAuth()
}

export function rememberLastAccount(profile) {
  if (!profile?.username) return

  localStorage.setItem(LAST_ACCOUNT_KEY, JSON.stringify({
    username: profile.username,
    displayName: profile.displayName || '',
    deviceName: profile.deviceName || '',
    lastLoginAt: new Date().toISOString()
  }))
}

export function getLastAccount() {
  try {
    return JSON.parse(localStorage.getItem(LAST_ACCOUNT_KEY) || '{}')
  } catch (error) {
    return {}
  }
}

export function buildDeviceProfile() {
  const platform = navigator.platform || 'Unknown platform'
  const language = navigator.language || 'zh-CN'
  const screenSize = `${window.screen.width}x${window.screen.height}`
  const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Shanghai'
  const seed = [platform, language, screenSize, timezone, navigator.userAgent || ''].join('|')

  return {
    deviceName: `${platform} / ${screenSize}`,
    deviceFingerprint: createHash(seed)
  }
}

function createHash(input) {
  let hash = 0
  for (let i = 0; i < input.length; i += 1) {
    hash = ((hash << 5) - hash) + input.charCodeAt(i)
    hash |= 0
  }
  return `fp_${Math.abs(hash)}`
}
