import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getToken, getUser, setAuth, clearAuth, isRemembered } from '@/utils/auth'
import { login as loginApi, getUserInfo } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken() || '')
  const userInfo = ref(getUser())

  // 登录状态只依赖 Token；角色判断供路由守卫和页面展示使用。
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN' || userInfo.value?.role === 'SUPER_ADMIN')

  /**
   * 登录。remember=true 时登录态写入 localStorage（新标签页/重启浏览器继承），
   * false 时仅当前标签页有效——多标签页可各自登录不同账号互不干扰。
   */
  async function login(form, remember = true) {
    const res = await loginApi(form)
    token.value = res.data.token
    userInfo.value = {
      userId: res.data.userId,
      username: res.data.username,
      nickname: res.data.nickname,
      avatar: res.data.avatar,
      role: res.data.role
    }
    setAuth(res.data.token, userInfo.value, remember)
    return res.data
  }

  async function fetchUserInfo() {
    const res = await getUserInfo()
    userInfo.value = { ...userInfo.value, ...res.data }
    // 同步刷新存储；若处于"记住我"状态则保持 localStorage 一致
    setAuth(token.value, userInfo.value, isRemembered())
    return res.data
  }

  /** 退出登录并清空当前标签页及“记住我”兜底登录态。 */
  function logout() {
    token.value = ''
    userInfo.value = null
    clearAuth()
  }

  return { token, userInfo, isLoggedIn, isAdmin, login, fetchUserInfo, logout }
})
