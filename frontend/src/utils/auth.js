const TOKEN_KEY = 'secondhand_token'
const USER_KEY = 'secondhand_user'

/**
 * 双层登录态存储：
 * - sessionStorage：当前标签页私有，保证多个标签页可登录不同账号互不干扰
 * - localStorage：勾选"记住我"时的跨标签/重启浏览器兜底
 * 读取时会话级优先。
 */
export function getToken() {
  return sessionStorage.getItem(TOKEN_KEY) || localStorage.getItem(TOKEN_KEY)
}

export function getUser() {
  const raw = sessionStorage.getItem(USER_KEY) || localStorage.getItem(USER_KEY)
  return raw ? JSON.parse(raw) : null
}

/**
 * 写入登录态。remember=true 时同时写入 localStorage（新标签页/重启浏览器可继承），
 * false 时仅当前标签页有效，并清除旧的"记住我"令牌，
 * 避免新标签页继承上一次记住我的旧账号。
 */
export function setAuth(token, user, remember) {
  sessionStorage.setItem(TOKEN_KEY, token)
  sessionStorage.setItem(USER_KEY, JSON.stringify(user))
  if (remember) {
    localStorage.setItem(TOKEN_KEY, token)
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  } else {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }
}

export function clearAuth() {
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(USER_KEY)
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

/** 当前是否处于“记住我”状态（localStorage 中有兜底令牌） */
export function isRemembered() {
  return !!localStorage.getItem(TOKEN_KEY)
}
