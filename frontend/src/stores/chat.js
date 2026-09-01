import { defineStore } from 'pinia'
import { getToken } from '@/utils/auth'
import { getChatUnread } from '@/api/chat'

/**
 * 聊天全局状态：WebSocket 连接 + 未读角标
 * 连接建立后服务端新消息实时推送，角标自动累加
 */
export const useChatStore = defineStore('chat', {
  state: () => ({
    unread: 0,              // 全局未读消息数（顶部角标）
    ws: null,               // WebSocket 实例
    listeners: [],          // 消息订阅者回调数组
    reconnectTimer: null    // 断线重连定时器
  }),
  actions: {
    /**
     * 建立 WebSocket 连接并监听消息/关闭/错误事件
     * 登录后调用；已连接或 token 缺失时直接返回
     */
    connect() {
      const token = getToken()
      if (!token || this.ws) return
      const proto = location.protocol === 'https:' ? 'wss:' : 'ws:'
      const ws = new WebSocket(`${proto}//${location.host}/api/ws/chat?token=${encodeURIComponent(token)}`)
      this.ws = ws
      ws.onmessage = (e) => {
        try {
          const data = JSON.parse(e.data)
          if (data.type === 'chat' && data.message) this.unread += 1
          // 新消息/编辑/删除/清空统一分发给订阅者
          this.listeners.forEach(fn => fn(data))
        } catch { /* 忽略非 JSON 帧 */ }
      }
      ws.onclose = () => {
        this.ws = null
        // 仍登录则断线重连
        if (getToken()) this.reconnectTimer = setTimeout(() => this.connect(), 3000)
      }
      ws.onerror = () => { /* onclose 会随后触发重连 */ }
      this.refreshUnread()
    },
    /**
     * 断开 WebSocket 连接并清理监听器
     * 退出登录或切换账号时调用
     */
    disconnect() {
      clearTimeout(this.reconnectTimer)
      if (this.ws) {
        this.ws.onclose = null
        this.ws.close()
        this.ws = null
      }
      this.unread = 0
      this.listeners = []
    },
    /**
     * 订阅 WebSocket 消息事件
     * @param {Function} fn 收到消息时的回调
     * @returns {Function} 取消订阅的函数
     */
    onMessage(fn) {
      this.listeners.push(fn)
      return () => { this.listeners = this.listeners.filter(f => f !== fn) }
    },
    /**
     * 从服务端拉取最新未读数
     * 连接建立或收到新消息时调用
     */
    async refreshUnread() {
      try {
        const res = await getChatUnread()
        this.unread = res.data.count || 0
      } catch { /* 未登录或网络异常时静默 */ }
    }
  }
})
