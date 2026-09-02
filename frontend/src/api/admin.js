import request from '@/utils/request'

/** 查询后台仪表盘统计。 */
export function getDashboard() {
  return request.get('/admin/dashboard')
}

/** 分页查询普通用户。 */
export function getAdminUsers(params) {
  return request.get('/admin/users', { params })
}

/** 启用或禁用用户。 */
export function updateUserStatus(id, status) {
  return request.put(`/admin/users/${id}/status`, { status })
}

/** 分页查询后台商品。 */
export function getAdminProducts(params) {
  return request.get('/admin/products', { params })
}

/** 分页查询后台订单。 */
export function getAdminOrders(params) {
  return request.get('/admin/orders', { params })
}

/** 新增或编辑分类。 */
export function saveCategory(data) {
  return request.post('/admin/category', data)
}

/** 删除空分类。 */
export function deleteCategory(id) {
  return request.delete(`/admin/category/${id}`)
}

/** 查询分类商品数量统计。 */
export function getCategoryStats() {
  return request.get('/admin/category-stats')
}

// === 小黑屋 ===
/** 分页查询小黑屋用户。 */
export function getBlacklist(params) {
  return request.get('/admin/blacklist', { params })
}

/** 手动拉黑用户。 */
export function manualBlacklist(id, data) {
  return request.put(`/admin/users/${id}/blacklist`, data)
}

/** 手动解除用户拉黑。 */
export function unblacklistUser(id) {
  return request.put(`/admin/users/${id}/unblacklist`)
}

/** 立即触发一次违规自动扫描。 */
export function triggerBlacklistScan() {
  return request.post('/admin/blacklist/scan')
}

// === 投诉 ===
/** 分页查询用户投诉。 */
export function getAdminComplaints(params) {
  return request.get('/admin/complaints', { params })
}

/** 处理投诉，approve=true 表示投诉成立。 */
export function handleComplaint(id, data) {
  return request.put(`/admin/complaints/${id}/handle`, data)
}

// === 申诉 ===
/** 分页查询用户申诉。 */
export function getAdminAppeals(params) {
  return request.get('/admin/appeals', { params })
}

/** 处理申诉，approve=true 表示解除限制。 */
export function handleAppeal(id, data) {
  return request.put(`/admin/appeals/${id}/handle`, data)
}

/** 查询后台待处理事项汇总。 */
export function getNotifications() {
  return request.get('/admin/notifications')
}

// === 管理员管理（仅超级管理员） ===
/** 分页查询管理员列表。 */
export function getAdminList(params) {
  return request.get('/admin/admins', { params })
}

/** 创建管理员并返回初始密码。 */
export function createAdmin(data) {
  return request.post('/admin/admins', data)
}

/** 更新管理员昵称。 */
export function updateAdmin(id, data) {
  return request.put(`/admin/admins/${id}`, data)
}

/** 删除管理员。 */
export function deleteAdmin(id) {
  return request.delete(`/admin/admins/${id}`)
}

/** 启用或禁用管理员。 */
export function updateAdminStatus(id, status) {
  return request.put(`/admin/admins/${id}/status`, { status })
}

// === 用户管理补全 ===
/** 后台创建普通用户或管理员。 */
export function createUser(data) {
  return request.post('/admin/users', data)
}

/** 后台更新用户基础信息。 */
export function updateUser(id, data) {
  return request.put(`/admin/users/${id}`, data)
}

/** 级联清理并删除用户。 */
export function deleteUser(id) {
  return request.delete(`/admin/users/${id}`)
}

/** 重置用户密码。 */
export function resetUserPassword(id, newPassword) {
  return request.put(`/admin/users/${id}/reset-password`, { newPassword })
}

/** 调整用户角色，仅超级管理员可用。 */
export function updateUserRole(id, role) {
  return request.put(`/admin/users/${id}/role`, { role })
}

/** 批量审核校园身份认证。 */
export function verifyUsers(userIds, action) {
  return request.put('/admin/users/verify', { userIds, action })
}

// === 表格导出（Excel） ===
/** 导出用户 Excel，响应为文件流。 */
export function exportUsers() {
  return request.get('/admin/export/users', { responseType: 'blob' })
}
/** 导出商品 Excel，响应为文件流。 */
export function exportProducts() {
  return request.get('/admin/export/products', { responseType: 'blob' })
}
/** 导出订单 Excel，响应为文件流。 */
export function exportOrders() {
  return request.get('/admin/export/orders', { responseType: 'blob' })
}

// === 商品管理 ===
/** 修改商品状态。 */
export function updateProductStatus(id, status) {
  return request.put(`/admin/products/${id}/status`, { status })
}

/** 删除后台商品。 */
export function deleteAdminProduct(id) {
  return request.delete(`/admin/products/${id}`)
}

// === 订单管理 ===
/** 修改后台订单状态。 */
export function updateAdminOrderStatus(id, status) {
  return request.put(`/admin/orders/${id}/status`, { status })
}

/** 删除已完成或已取消订单。 */
export function deleteAdminOrder(id) {
  return request.delete(`/admin/orders/${id}`)
}

/** 仲裁退款纠纷，refund=true 表示退款。 */
export function arbitrateOrder(id, refund) {
  return request.put(`/admin/order/${id}/arbitration`, { refund })
}
