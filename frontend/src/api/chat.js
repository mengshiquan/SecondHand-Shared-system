import request from '@/utils/request'

/**
 * 获取当前用户的会话列表（按最近消息倒序）
 * @returns {Promise} 会话数组
 */
export function getConversations() {
  return request.get('/chat/conversations')
}

/**
 * 获取与指定用户的聊天记录
 * @param {number} peerId 对方用户 ID
 * @param {number|null} beforeId 游标：当前最早消息 ID，用于向上翻页
 * @returns {Promise} 消息数组
 */
export function getMessages(peerId, beforeId) {
  return request.get(`/chat/messages/${peerId}`, { params: { beforeId } })
}

/**
 * 发送聊天消息
 * @param {Object} data 包含 receiverId、content，可选 productId
 * @returns {Promise} 发送成功的消息对象
 */
export function sendChatMessage(data) {
  return request.post('/chat/send', data)
}

/**
 * 标记与指定用户的会话已读，清空未读数
 * @param {number} peerId 对方用户 ID
 * @returns {Promise}
 */
export function markChatRead(peerId) {
  return request.post(`/chat/read/${peerId}`)
}

/**
 * 获取当前用户未读消息总数，用于顶部角标
 * @returns {Promise} { count: number }
 */
export function getChatUnread() {
  return request.get('/chat/unread')
}

/**
 * 编辑自己发送的单条消息
 * @param {number} id 消息 ID
 * @param {string} content 新内容
 * @returns {Promise} 更新后的消息对象
 */
export function updateChatMessage(id, content) {
  return request.put(`/chat/message/${id}`, { content })
}

/**
 * 删除自己发送的单条消息
 * @param {number} id 消息 ID
 * @returns {Promise}
 */
export function deleteChatMessage(id) {
  return request.delete(`/chat/message/${id}`)
}

/**
 * 删除会话并清空双方聊天记录
 * @param {number} peerId 对方用户 ID
 * @returns {Promise}
 */
export function deleteConversation(peerId) {
  return request.delete(`/chat/conversation/${peerId}`)
}
