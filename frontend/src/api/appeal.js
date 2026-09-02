import request from '@/utils/request'

/** 提交账号小黑屋申诉。 */
export function submitAppeal(data) {
  return request.post('/appeal', data)
}
