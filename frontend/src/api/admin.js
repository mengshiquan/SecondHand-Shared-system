import request from '@/utils/request'

export function getDashboard() {
  return request.get('/admin/dashboard')
}

export function getAdminUsers(params) {
  return request.get('/admin/users', { params })
}

export function updateUserStatus(id, status) {
  return request.put(`/admin/users/${id}/status`, { status })
}

export function getAdminProducts(params) {
  return request.get('/admin/products', { params })
}

export function getAdminOrders(params) {
  return request.get('/admin/orders', { params })
}

export function saveCategory(data) {
  return request.post('/admin/category', data)
}

export function deleteCategory(id) {
  return request.delete(`/admin/category/${id}`)
}

export function getCategoryStats() {
  return request.get('/admin/category-stats')
}

// === 小黑屋 ===
export function getBlacklist(params) {
  return request.get('/admin/blacklist', { params })
}

export function manualBlacklist(id, data) {
  return request.put(`/admin/users/${id}/blacklist`, data)
}

export function unblacklistUser(id) {
  return request.put(`/admin/users/${id}/unblacklist`)
}

export function triggerBlacklistScan() {
  return request.post('/admin/blacklist/scan')
}

// === 投诉 ===
export function getAdminComplaints(params) {
  return request.get('/admin/complaints', { params })
}

export function handleComplaint(id, data) {
  return request.put(`/admin/complaints/${id}/handle`, data)
}

// === 申诉 ===
export function getAdminAppeals(params) {
  return request.get('/admin/appeals', { params })
}

export function handleAppeal(id, data) {
  return request.put(`/admin/appeals/${id}/handle`, data)
}

export function getNotifications() {
  return request.get('/admin/notifications')
}
