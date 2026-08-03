import request from '@/utils/request'

export function submitComplaint(data) {
  return request.post('/complaint', data)
}
