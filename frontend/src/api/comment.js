import request from '@/utils/request'

/** 分页查询指定商品的评论。 */
export function getCommentList(productId, params) {
  return request.get(`/comment/list/${productId}`, { params })
}

/** 对已完成交易的商品发表评论。 */
export function addComment(data) {
  return request.post('/comment', data)
}

/** 删除本人或管理员指定的评论。 */
export function deleteComment(id) {
  return request.delete(`/comment/${id}`)
}
