import request from '@/utils/request'

/** 上传图片等文件，返回可直接访问的文件 URL。 */
export function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
