import request from '@/utils/request'

/** 创建订单。 */
export function createOrder(data) {
  return request.post('/order', data)
}

/** 分页查询当前用户买卖订单。 */
export function getOrderList(params) {
  return request.get('/order/list', { params })
}

/** 查询各待处理订单状态数量。 */
export function getOrderStatusCounts() {
  return request.get('/order/status-counts')
}

/** 查询订单详情。 */
export function getOrderDetail(id) {
  return request.get(`/order/${id}`)
}

/** 推进订单状态，例如发货、确认收货。 */
export function updateOrderStatus(id, status) {
  return request.put(`/order/${id}/status`, { status })
}

/** 买家取消待付款订单。 */
export function cancelOrder(id) {
  return request.post(`/order/${id}/cancel`)
}

/** 买家申请退款。 */
export function applyRefund(id, reason) {
  return request.post(`/order/${id}/refund`, { reason })
}

/** 卖家同意或拒绝退款。 */
export function handleRefund(id, agree) {
  return request.put(`/order/${id}/refund/handle`, { agree })
}

/** 卖家拒绝退款后，买家申请平台仲裁。 */
export function applyArbitration(id) {
  return request.post(`/order/${id}/arbitration`)
}

/** 买家更新待付款订单收货地址。 */
export function updateOrderAddress(id, addressId) {
  return request.put(`/order/${id}/address`, { addressId })
}

/** 删除已完成或已取消订单。 */
export function deleteOrder(id) {
  return request.delete(`/order/${id}`)
}
