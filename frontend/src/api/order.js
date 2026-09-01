import request from '@/utils/request'

export function createOrder(data) {
  return request.post('/order', data)
}

export function getOrderList(params) {
  return request.get('/order/list', { params })
}

export function getOrderStatusCounts() {
  return request.get('/order/status-counts')
}

export function getOrderDetail(id) {
  return request.get(`/order/${id}`)
}

export function updateOrderStatus(id, status) {
  return request.put(`/order/${id}/status`, { status })
}

export function cancelOrder(id) {
  return request.post(`/order/${id}/cancel`)
}

export function applyRefund(id, reason) {
  return request.post(`/order/${id}/refund`, { reason })
}

export function handleRefund(id, agree) {
  return request.put(`/order/${id}/refund/handle`, { agree })
}

export function applyArbitration(id) {
  return request.post(`/order/${id}/arbitration`)
}

export function updateOrderAddress(id, addressId) {
  return request.put(`/order/${id}/address`, { addressId })
}

export function deleteOrder(id) {
  return request.delete(`/order/${id}`)
}
