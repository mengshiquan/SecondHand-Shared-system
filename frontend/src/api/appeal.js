import request from '@/utils/request'

export function submitAppeal(data) {
  return request.post('/appeal', data)
}
