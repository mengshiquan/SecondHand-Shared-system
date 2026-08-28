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

// === 管理员管理（仅超级管理员） ===
export function getAdminList(params) {
  return request.get('/admin/admins', { params })
}

export function createAdmin(data) {
  return request.post('/admin/admins', data)
}

export function updateAdmin(id, data) {
  return request.put(`/admin/admins/${id}`, data)
}

export function deleteAdmin(id) {
  return request.delete(`/admin/admins/${id}`)
}

export function updateAdminStatus(id, status) {
  return request.put(`/admin/admins/${id}/status`, { status })
}

// === 用户管理补全 ===
export function createUser(data) {
  return request.post('/admin/users', data)
}

export function updateUser(id, data) {
  return request.put(`/admin/users/${id}`, data)
}

export function deleteUser(id) {
  return request.delete(`/admin/users/${id}`)
}

export function resetUserPassword(id) {
  return request.put(`/admin/users/${id}/reset-password`)
}

export function verifyUsers(userIds, action) {
  return request.put('/admin/users/verify', { userIds, action })
}

// === 商品管理 ===
export function updateProductStatus(id, status) {
  return request.put(`/admin/products/${id}/status`, { status })
}

export function deleteAdminProduct(id) {
  return request.delete(`/admin/products/${id}`)
}

// === 订单管理 ===
export function updateAdminOrderStatus(id, status) {
  return request.put(`/admin/orders/${id}/status`, { status })
}

export function deleteAdminOrder(id) {
  return request.delete(`/admin/orders/${id}`)
}

export function arbitrateOrder(id, refund) {
  return request.put(`/admin/order/${id}/arbitration`, { refund })
}
