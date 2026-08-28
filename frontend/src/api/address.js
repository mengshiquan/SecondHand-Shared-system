import request from '@/utils/request'

export function getAddressList() {
  return request.get('/address/list')
}

export function addAddress(data) {
  return request.post('/address', data)
}

export function updateAddress(id, data) {
  return request.put(`/address/${id}`, data)
}

export function deleteAddress(id) {
  return request.delete(`/address/${id}`)
}

export function setDefaultAddress(id) {
  return request.put(`/address/${id}/default`)
}
