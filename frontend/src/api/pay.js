import request from '@/utils/request'

/**
 * 为订单创建支付宝支付（返回支付链接或表单）
 * @param {number} orderId 订单 ID
 * @returns {Promise} 支付所需数据
 */
export function createAlipayPay(orderId) {
  return request.post(`/pay/alipay/create/${orderId}`)
}

/**
 * 为订单创建微信支付（返回支付二维码或调起参数）
 * @param {number} orderId 订单 ID
 * @returns {Promise} 支付所需数据
 */
export function createWechatPay(orderId) {
  return request.post(`/pay/wechat/create/${orderId}`)
}

// 微信模拟扫码回调（模拟手机扫码完成付款）
export function wechatMockNotify(orderNo) {
  return request.post(`/pay/wechat/notify/${orderNo}`)
}

/**
 * 查询订单支付状态
 * @param {number} orderId 订单 ID
 * @returns {Promise} 支付状态
 */
export function getPayStatus(orderId) {
  return request.get(`/pay/status/${orderId}`)
}
