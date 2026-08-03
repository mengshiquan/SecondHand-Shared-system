import request from '@/utils/request'

export function getCategoryList() {
  return request.get('/category/list')
}

export function getMainCategories() {
  return request.get('/category/main')
}

export function getSubCategories(parentId) {
  return request.get(`/category/sub/${parentId}`)
}

export function getCategoryTree() {
  return request.get('/category/tree')
}
