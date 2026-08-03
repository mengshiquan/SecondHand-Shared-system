import request from '@/utils/request'

export function getCommentList(productId, params) {
  return request.get(`/comment/list/${productId}`, { params })
}

export function addComment(data) {
  return request.post('/comment', data)
}

export function deleteComment(id) {
  return request.delete(`/comment/${id}`)
}
