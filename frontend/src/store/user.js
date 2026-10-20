import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  clearAuthState,
  getStoredUserInfo,
  getToken,
  persistAuthSession,
  updateStoredUserInfo
} from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const userInfo = ref(getStoredUserInfo())

  const setToken = (newToken, rememberMe = false) => {
    token.value = newToken
    persistAuthSession({
      token: newToken,
      userInfo: userInfo.value,
      rememberMe
    })
  }

  const setUserInfo = (info) => {
    userInfo.value = info || {}
    updateStoredUserInfo(userInfo.value)
  }

  const setSession = ({ token: newToken, userInfo: info, rememberMe = false }) => {
    token.value = newToken || ''
    userInfo.value = info || {}
    persistAuthSession({
      token: token.value,
      userInfo: userInfo.value,
      rememberMe
    })
  }

  const logout = () => {
    token.value = ''
    userInfo.value = {}
    clearAuthState()
  }

  return {
    token,
    userInfo,
    setToken,
    setUserInfo,
    setSession,
    logout
  }
})
