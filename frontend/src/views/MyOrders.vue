<template>
  <div class="orders page-container">
    <div class="page-header">
      <h1 class="page-title">我的订单</h1>
      <p class="page-sub">管理你的购买与出售记录</p>
    </div>

    <el-tabs v-model="activeTab" @tab-change="loadData" class="order-tabs">
      <el-tab-pane v-for="tab in tabs" :key="tab.key" :name="tab.key">
        <template #label>
          <span class="tab-label">{{ tab.label }}
            <span v-if="tab.count" class="tab-badge">{{ tab.count }}</span>
          </span>
        </template>
      </el-tab-pane>
    </el-tabs>

    <div v-for="(order, i) in orders" :key="order.id" class="order-item" :class="'order-' + order.status" :style="{ animationDelay: `${i * 0.06}s` }">
      <div class="order-status-bar"></div>
      <div class="order-content">
        <div class="order-header">
          <span class="order-no">订单号：{{ order.orderNo }}</span>
          <div class="order-header-right">
            <span v-if="order.status === 'PENDING' && countdown(order.expireTime)" class="countdown">
              {{ countdown(order.expireTime) }}
            </span>
            <el-tag :type="statusType(order.status)" size="small" effect="plain">{{ statusText(order.status) }}</el-tag>
          </div>
        </div>

        <div class="order-body">
          <div class="order-image">
            <el-image :src="order.productImage" style="width: 80px; height: 80px;" fit="cover">
              <template #error>
                <div class="img-fallback"><el-icon :size="24"><Picture /></el-icon></div>
              </template>
            </el-image>
          </div>
          <div class="order-info">
            <h4>{{ order.productTitle }}</h4>
            <p class="order-price">¥{{ order.price }}</p>
            <div class="parties" v-if="order.status !== 'PENDING'">
              <el-icon :size="14"><User /></el-icon>
              <span>{{ order.buyerName }}</span>
              <template v-if="order.buyerPhone"> · {{ order.buyerPhone }}</template>
              <template v-if="order.buyerAddress"> · {{ order.buyerAddress }}</template>
            </div>
            <div class="parties">
              买家：{{ order.buyerNickname || order.buyerId }}
               · 卖家：{{ order.sellerName }}
            </div>
          </div>

          <div class="order-actions">
            <template v-if="order.status === 'PENDING'">
              <el-button v-if="isBuyer(order)" type="primary" size="small" @click="handlePay(order.id)">
                确认付款
              </el-button>
              <el-button size="small" @click="handleCancel(order.id)">取消订单</el-button>
            </template>
            <el-button v-if="order.status === 'PAID' && isSeller(order)" type="primary" size="small" @click="updateStatus(order.id, 'SHIPPED')">
              确认发货
            </el-button>
            <el-button v-if="order.status === 'SHIPPED' && isBuyer(order)" type="success" size="small" @click="updateStatus(order.id, 'COMPLETED')">
              确认收货
            </el-button>
          </div>
        </div>
        <div class="order-time">{{ order.createTime }}</div>
      </div>
    </div>

    <div v-if="orders.length === 0" class="empty-state">
      <div class="empty-icon">
        <el-icon :size="40"><Document /></el-icon>
      </div>
      <p class="empty-title">暂无相关订单</p>
      <p class="empty-hint">切换上方标签页查看其他状态的订单</p>
    </div>

    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        background
        @change="loadData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Picture, User } from '@element-plus/icons-vue'
import { getOrderList, payOrder, updateOrderStatus } from '@/api/order'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const orders = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const activeTab = ref('PENDING')
let ticker = null

const tabs = reactive([
  { key: 'PENDING', label: '待付款', count: 0 },
  { key: 'PAID', label: '已付款', count: 0 },
  { key: 'SHIPPED', label: '已发货', count: 0 },
  { key: 'COMPLETED', label: '已完成', count: 0 },
  { key: 'CANCELLED', label: '已取消', count: 0 }
])

const statusMap = { PENDING: '待付款', PAID: '已付款', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消' }
const statusTypeMap = { PENDING: 'warning', PAID: 'primary', SHIPPED: 'success', COMPLETED: 'success', CANCELLED: 'info' }

function statusText(s) { return statusMap[s] || s }
function statusType(s) { return statusTypeMap[s] || '' }
function isBuyer(o) { return o.buyerId === userStore.userInfo?.userId }
function isSeller(o) { return o.sellerId === userStore.userInfo?.userId }

function countdown(expireTime) {
  if (!expireTime) return ''
  const remain = new Date(expireTime).getTime() - Date.now()
  if (remain <= 0) return '已超时'
  const m = Math.floor(remain / 60000)
  const s = Math.floor((remain % 60000) / 1000)
  return `剩余 ${m}:${String(s).padStart(2, '0')}`
}

async function loadData() {
  const res = await getOrderList({
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    status: activeTab.value
  })
  orders.value = res.data.records
  total.value = res.data.total
}

async function loadAllCounts() {
  for (const tab of tabs) {
    try {
      const res = await getOrderList({ pageNum: 1, pageSize: 1, status: tab.key })
      tab.count = res.data.total || 0
    } catch { tab.count = 0 }
  }
}

async function handlePay(id) {
  await ElMessageBox.confirm('确认已与卖家沟通并同意购买？', '确认付款', { type: 'info' })
  await payOrder(id)
  ElMessage.success('付款成功')
  loadData(); loadAllCounts()
}

async function handleCancel(id) {
  await ElMessageBox.confirm('确认取消订单？商品将重新上架。', '取消订单', { type: 'warning' })
  await updateOrderStatus(id, 'CANCELLED')
  ElMessage.success('已取消')
  loadData(); loadAllCounts()
}

async function updateStatus(id, newStatus) {
  await updateOrderStatus(id, newStatus)
  ElMessage.success('操作成功')
  loadData(); loadAllCounts()
}

onMounted(() => { loadData(); loadAllCounts(); ticker = setInterval(loadData, 30000) })
onBeforeUnmount(() => clearInterval(ticker))
</script>

<style scoped>
/* ====== 页面标题 ====== */
.page-header { margin-bottom: 24px; }
.page-title { font-size: 24px; font-weight: 800; color: #1F2937; margin: 0 0 4px; }
.page-sub { font-size: 14px; color: #9CA3AF; margin: 0; }

/* ====== 标签页 ====== */
.order-tabs { margin-bottom: 20px; }
.tab-label { display: flex; align-items: center; gap: 6px; }
.tab-badge {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 20px; height: 20px; padding: 0 6px;
  border-radius: 10px;
  font-size: 11px; font-weight: 700;
  background: #ECFDF5; color: #059669;
}

/* ====== 订单卡片 ====== */
.order-item {
  display: flex;
  background: #fff;
  border-radius: 14px;
  margin-bottom: 14px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  border: 1px solid #F3F4F6;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  animation: fadeInUp 0.5s ease both;
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(16px); }
  to   { opacity: 1; transform: translateY(0); }
}
.order-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(16,185,129,0.08);
}

/* 左侧状态色条 */
.order-status-bar {
  width: 4px; flex-shrink: 0;
}
.order-PENDING .order-status-bar { background: #F59E0B; }
.order-PAID .order-status-bar { background: #3B82F6; }
.order-SHIPPED .order-status-bar { background: #8B5CF6; }
.order-COMPLETED .order-status-bar { background: #10B981; }
.order-CANCELLED .order-status-bar { background: #9CA3AF; }

.order-content { flex: 1; padding: 16px 20px 12px; min-width: 0; }

/* 头部 */
.order-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 12px; font-size: 13px;
}
.order-no { color: #9CA3AF; }
.order-header-right { display: flex; align-items: center; gap: 12px; }

/* 倒计时 */
.countdown {
  color: #F59E0B; font-weight: 600; font-size: 13px;
  animation: breathe 2s ease-in-out infinite;
}
@keyframes breathe {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* 订单主体 */
.order-body { display: flex; align-items: flex-start; gap: 16px; }
.order-image { flex-shrink: 0; border-radius: 10px; overflow: hidden; }
.order-info { flex: 1; min-width: 0; }
.order-info h4 {
  margin: 0 0 6px; font-size: 15px; font-weight: 600; color: #1F2937;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.order-price {
  color: #F59E0B; font-weight: 700; font-size: 18px;
  font-family: 'Inter','PingFang SC',sans-serif;
  font-variant-numeric: tabular-nums; margin: 0 0 6px;
}
.parties {
  display: flex; align-items: center; gap: 4px;
  color: #9CA3AF; font-size: 13px; line-height: 1.6;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

.order-actions { display: flex; gap: 8px; flex-shrink: 0; align-self: center; }
.order-time { margin-top: 8px; font-size: 12px; color: #D1D5DB; }

/* 无图占位 */
.img-fallback {
  width: 80px; height: 80px;
  display: flex; align-items: center; justify-content: center;
  background: #F3F4F6; color: #9CA3AF; border-radius: 10px;
}

/* 空状态 */
.empty-state { text-align: center; padding: 64px 20px; }
.empty-icon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 72px; height: 72px; border-radius: 16px;
  background: linear-gradient(135deg, #ECFDF5, #D1FAE5);
  color: #10B981; margin-bottom: 16px;
}
.empty-title { font-size: 16px; font-weight: 600; color: #374151; margin: 0 0 6px; }
.empty-hint { font-size: 13px; color: #9CA3AF; margin: 0; }

/* 分页 */
.pagination { display: flex; justify-content: center; margin-top: 32px; }

/* ====== 响应式 ====== */
@media (max-width: 768px) {
  .order-content { padding: 12px 14px 10px; }
  .order-body { flex-wrap: wrap; }
  .order-actions { width: 100%; justify-content: flex-end; margin-top: 8px; }
}
/* 手机端：按钮全宽 */
@media (max-width: 480px) {
  .page-title { font-size: 20px; }
  .order-item { border-radius: 10px; }
  .order-actions { flex-direction: column; }
  .order-actions .el-button { width: 100%; }
  .order-image .el-image { width: 60px; height: 60px; }
  .order-info h4 { font-size: 14px; }
  .order-price { font-size: 16px; }
}
</style>
