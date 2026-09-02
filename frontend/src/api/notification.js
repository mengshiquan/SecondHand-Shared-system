import request from '@/utils/request'

/** 分页查询当前用户通知。 */
export function getNotificationList(params) {
  return request.get('/notification/list', { params })
}

/** 查询当前用户未读通知数量。 */
export function getUnreadCount() {
  return request.get('/notification/unread-count')
}

/** 标记单条通知为已读。 */
export function markRead(id) {
  return request.put(`/notification/${id}/read`)
}

/** 标记全部通知为已读。 */
export function markAllRead() {
  return request.put('/notification/read-all')
}

/** 删除单条通知。 */
export function deleteNotification(id) {
  return request.delete(`/notification/${id}`)
}

/** 清空所有已读通知。 */
export function clearRead() {
  return request.delete('/notification/clear')
}
