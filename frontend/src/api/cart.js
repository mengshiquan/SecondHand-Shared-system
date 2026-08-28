import request from '@/utils/request'

export function getCartList() {
  return request.get('/cart/list')
}

export function addToCart(productId) {
  return request.post(`/cart/${productId}`)
}

export function removeFromCart(id) {
  return request.delete(`/cart/${id}`)
}

export function removeBatchFromCart(ids) {
  return request.delete('/cart/batch', { data: { ids } })
}

export function clearCart() {
  return request.delete('/cart/clear')
}

export function checkoutCart(data) {
  return request.post('/cart/checkout', data)
}
