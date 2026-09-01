import request from '@/utils/request'

export function getProductList(params) {
  return request.get('/product/list', { params })
}

export function getMyProducts(params) {
  return request.get('/product/my', { params })
}

export function getProductDetail(id) {
  return request.get(`/product/detail/${id}`)
}

export function publishProduct(data) {
  return request.post('/product', data)
}

export function updateProduct(data) {
  return request.put('/product', data)
}

export function deleteProduct(id) {
  return request.delete(`/product/${id}`)
}

export function offShelfProduct(id) {
  return request.put(`/product/${id}/off-shelf`)
}

export function contactSeller(id, data) {
  return request.post(`/product/${id}/contact`, data)
}
