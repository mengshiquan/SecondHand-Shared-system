import request from '@/utils/request'

/** 查询当前登录用户的收货地址列表。 */
export function getAddressList() {
  return request.get('/address/list')
}

/** 新增收货地址，单个用户最多保存 10 条。 */
export function addAddress(data) {
  return request.post('/address', data)
}

/** 更新当前用户指定收货地址。 */
export function updateAddress(id, data) {
  return request.put(`/address/${id}`, data)
}

/** 逻辑删除当前用户指定收货地址。 */
export function deleteAddress(id) {
  return request.delete(`/address/${id}`)
}

/** 将指定地址设为当前用户的唯一默认地址。 */
export function setDefaultAddress(id) {
  return request.put(`/address/${id}/default`)
}
