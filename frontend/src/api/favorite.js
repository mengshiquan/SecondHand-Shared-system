import request from '@/utils/request'

export function toggleFavorite(productId) {
  return request.post(`/favorite/${productId}`)
}

export function getFavoriteList(params) {
  return request.get('/favorite/list', { params })
}
