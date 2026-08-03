import request from '@/utils/request'

export function getNotificationList(params) {
  return request.get('/notification/list', { params })
}

export function getUnreadCount() {
  return request.get('/notification/unread-count')
}

export function markRead(id) {
  return request.put(`/notification/${id}/read`)
}

export function markAllRead() {
  return request.put('/notification/read-all')
}

export function deleteNotification(id) {
  return request.delete(`/notification/${id}`)
}

export function clearRead() {
  return request.delete('/notification/clear')
}
