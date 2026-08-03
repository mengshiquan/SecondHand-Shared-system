import request from '@/utils/request'

export function createOrder(data) {
  return request.post('/order', data)
}

export function getOrderList(params) {
  return request.get('/order/list', { params })
}

export function getOrderDetail(id) {
  return request.get(`/order/${id}`)
}

export function payOrder(id) {
  return request.put(`/order/${id}/pay`)
}

export function updateOrderStatus(id, status) {
  return request.put(`/order/${id}/status`, { status })
}
