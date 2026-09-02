import request from '@/utils/request'

/** 切换商品收藏状态：已收藏则取消，未收藏则收藏。 */
export function toggleFavorite(productId) {
  return request.post(`/favorite/${productId}`)
}

/** 分页查询当前用户收藏商品。 */
export function getFavoriteList(params) {
  return request.get('/favorite/list', { params })
}

/** 批量取消收藏。 */
export function removeFavoriteBatch(productIds) {
  return request.delete('/favorite/batch', { data: { productIds } })
}
