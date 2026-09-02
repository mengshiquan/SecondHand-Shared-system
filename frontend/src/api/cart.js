import request from '@/utils/request'

/** 查询当前用户购物车及商品展示信息。 */
export function getCartList() {
  return request.get('/cart/list')
}

/** 将商品加入当前用户购物车。 */
export function addToCart(productId) {
  return request.post(`/cart/${productId}`)
}

/** 删除单个购物车项。 */
export function removeFromCart(id) {
  return request.delete(`/cart/${id}`)
}

/** 批量删除购物车项。 */
export function removeBatchFromCart(ids) {
  return request.delete('/cart/batch', { data: { ids } })
}

/** 将选中的购物车商品移入收藏。 */
export function moveToFavorite(ids) {
  return request.post('/cart/move-to-favorite', { ids })
}

/** 清空当前用户购物车。 */
export function clearCart() {
  return request.delete('/cart/clear')
}

/** 按选中购物车项批量下单。 */
export function checkoutCart(data) {
  return request.post('/cart/checkout', data)
}
