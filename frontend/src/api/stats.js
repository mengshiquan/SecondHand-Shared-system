import request from '@/utils/request'

/** 查询销售统计汇总和明细。 */
export function getSalesStats(params) {
  return request.get('/admin/stats/sales', { params })
}

// Excel 导出：返回 blob 流，由页面触发下载
export function exportSales(params) {
  return request.get('/admin/stats/sales/export', { params, responseType: 'blob' })
}
