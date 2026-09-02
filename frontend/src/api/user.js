import request from '@/utils/request'

/**
 * 获取图形验证码（返回 base64 图片与验证 key）
 * @returns {Promise} { key, image }
 */
export function getCaptcha() {
  return request.get('/captcha')
}

/**
 * 用户登录
 * @param {Object} data 包含 username、password、captchaKey、captchaCode
 * @returns {Promise} 登录成功返回 token 与用户信息
 */
export function login(data) {
  return request.post('/user/login', data)
}

/**
 * 用户注册
 * @param {Object} data 包含 username、password、nickname、phone、studentId 等
 * @returns {Promise}
 */
export function register(data) {
  return request.post('/user/register', data)
}

/**
 * 获取当前登录用户信息
 * @returns {Promise} 用户对象
 */
export function getUserInfo() {
  return request.get('/user/info')
}

/**
 * 更新当前用户个人资料
 * @param {Object} data 可包含 nickname、avatar、phone 等
 * @returns {Promise}
 */
export function updateProfile(data) {
  return request.put('/user/profile', data)
}

/**
 * 修改当前用户密码
 * @param {Object} data 包含旧密码与新密码
 * @returns {Promise}
 */
export function updatePassword(data) {
  return request.put('/user/password', data)
}

/**
 * 上传文件/图片（multipart/form-data）
 * @param {FormData} data 文件表单数据
 * @returns {Promise} 文件访问 URL
 */
export function uploadFile(data) {
  return request.post('/file/upload', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 查询当前用户黑名单状态
 * @returns {Promise} 是否被封禁等信息
 */
export function getUserBlacklistStatus() {
  return request.get('/user/blacklist-status')
}

/**
 * 获取卖家主页公开信息（昵称、头像），无需登录
 * @param {number|string} id 卖家用户 ID
 * @returns {Promise} { id, nickname, avatar }
 */
export function getSellerInfo(id) {
  return request.get(`/user/seller/${id}`)
}

/**
 * 注销当前登录账号
 * @param {Object} data 身份校验参数
 * @returns {Promise}
 */
export function deactivateAccount(data) {
  return request.put('/user/deactivate', data)
}
