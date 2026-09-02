import request from '@/utils/request'

/** 提交用户投诉。 */
export function submitComplaint(data) {
  return request.post('/complaint', data)
}
