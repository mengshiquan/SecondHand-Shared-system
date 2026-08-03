import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getToken, setToken, getUser, setUser, clearAuth } from '@/utils/auth'
import { login as loginApi, getUserInfo } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken() || '')
  const userInfo = ref(getUser())

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')

  async function login(form) {
    const res = await loginApi(form)
    token.value = res.data.token
    userInfo.value = {
      userId: res.data.userId,
      username: res.data.username,
      nickname: res.data.nickname,
      avatar: res.data.avatar,
      role: res.data.role
    }
    setToken(res.data.token)
    setUser(userInfo.value)
    return res.data
  }

  async function fetchUserInfo() {
    const res = await getUserInfo()
    userInfo.value = { ...userInfo.value, ...res.data }
    setUser(userInfo.value)
    return res.data
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    clearAuth()
  }

  return { token, userInfo, isLoggedIn, isAdmin, login, fetchUserInfo, logout }
})
