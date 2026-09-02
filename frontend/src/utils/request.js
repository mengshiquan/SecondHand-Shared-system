import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from './auth'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器：自动携带 Token
request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理返回结果
request.interceptors.response.use(
  (response) => {
    // blob 流（如 Excel 导出）直接返回，不走 Result 解析
    if (response.config.responseType === 'blob') {
      return response.data
    }
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        clearAuth()
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    const res = error.response
    if (res && res.status === 401) {
      ElMessage.error(res.data?.message || '登录已过期，请重新登录')
      clearAuth()
      router.push('/login')
    } else if (res && res.data && res.data.message) {
      // 透出后端业务错误消息（如账号禁用/小黑屋限制提醒）
      ElMessage.error(res.data.message)
    } else {
      ElMessage.error(error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request
