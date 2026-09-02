import request from '@/utils/request'

/** 分页查询商品列表。 */
export function getProductList(params) {
  return request.get('/product/list', { params })
}

/** 分页查询当前用户发布的商品。 */
export function getMyProducts(params) {
  return request.get('/product/my', { params })
}

/** 查询商品详情。 */
export function getProductDetail(id) {
  return request.get(`/product/detail/${id}`)
}

/** 发布商品。 */
export function publishProduct(data) {
  return request.post('/product', data)
}

/** 更新本人商品。 */
export function updateProduct(data) {
  return request.put('/product', data)
}

/** 删除本人商品。 */
export function deleteProduct(id) {
  return request.delete(`/product/${id}`)
}

/** 下架本人商品。 */
export function offShelfProduct(id) {
  return request.put(`/product/${id}/off-shelf`)
}

/** 向卖家发送商品咨询通知。 */
export function contactSeller(id, data) {
  return request.post(`/product/${id}/contact`, data)
}
