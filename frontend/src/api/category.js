import request from '@/utils/request'

/** 查询全部分类。 */
export function getCategoryList() {
  return request.get('/category/list')
}

/** 查询一级分类。 */
export function getMainCategories() {
  return request.get('/category/main')
}

/** 查询指定一级分类下的二级分类。 */
export function getSubCategories(parentId) {
  return request.get(`/category/sub/${parentId}`)
}

/** 查询一级/二级分类树。 */
export function getCategoryTree() {
  return request.get('/category/tree')
}
