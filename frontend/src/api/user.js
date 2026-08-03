import request from '@/utils/request'

export function login(data) {
  return request.post('/user/login', data)
}

export function register(data) {
  return request.post('/user/register', data)
}

export function getUserInfo() {
  return request.get('/user/info')
}

export function updateProfile(data) {
  return request.put('/user/profile', data)
}

export function updatePassword(data) {
  return request.put('/user/password', data)
}

export function uploadFile(data) {
  return request.post('/file/upload', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getUserBlacklistStatus() {
  return request.get('/user/blacklist-status')
}
