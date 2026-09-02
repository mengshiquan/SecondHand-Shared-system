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
            <span v-if="tab.badge && tab.count" class="tab-badge">{{ tab.count > 99 ? '99+' : tab.count }}</span>
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
            <el-tag v-if="order.payChannel" :type="order.payChannel === 'ALIPAY' ? 'primary' : 'success'" size="small" effect="plain">
              {{ order.payChannel === 'ALIPAY' ? '支付宝' : '微信支付' }}
            </el-tag>
            <el-tag v-if="refundText(order.refundStatus)" :type="refundType(order.refundStatus)" size="small" effect="plain">
              {{ refundText(order.refundStatus) }}
            </el-tag>
            <!-- 退款进行中时退款标签优先，不再同时展示订单状态标签，避免状态冲突观感 -->
            <el-tag v-if="!refundActive(order)" :type="statusType(order.status)" size="small" effect="plain">{{ statusText(order.status) }}</el-tag>
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
              <el-button v-if="isBuyer(order)" type="primary" size="small" @click="handlePay(order)">
                支付
              </el-button>
              <el-button v-if="isBuyer(order)" size="small" @click="handleCancel(order.id)">取消订单</el-button>
            </template>
            <el-button v-if="order.status === 'PAID' && isSeller(order) && !refundActive(order)" type="primary" size="small" @click="updateStatus(order.id, 'SHIPPED')">
              确认发货
            </el-button>
            <el-button v-if="order.status === 'SHIPPED' && isBuyer(order) && !refundActive(order)" type="success" size="small" @click="updateStatus(order.id, 'COMPLETED')">
              确认收货
            </el-button>
            <el-button
              v-if="isBuyer(order) && (order.status === 'PAID' || order.status === 'SHIPPED') && (!order.refundStatus || order.refundStatus === 'NONE')"
              type="warning" plain size="small"
              @click="handleApplyRefund(order.id)"
            >申请退款</el-button>
            <template v-if="isSeller(order) && order.refundStatus === 'REQUESTED'">
              <el-button type="success" plain size="small" @click="handleRefundAction(order.id, true)">同意退款</el-button>
              <el-button type="danger" plain size="small" @click="handleRefundAction(order.id, false)">拒绝退款</el-button>
            </template>
            <el-button
              v-if="isBuyer(order) && order.refundStatus === 'SELLER_REJECTED'"
              type="danger" plain size="small"
              @click="handleArbitration(order.id)"
            >申请仲裁</el-button>
            <el-button
              v-if="order.status === 'COMPLETED' || order.status === 'CANCELLED'"
              type="info" plain size="small"
              @click="handleDeleteOrder(order.id)"
            >删除订单</el-button>
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

    <!-- 申请退款弹窗：原因分类选择 -->
    <el-dialog v-model="refundDialogVisible" width="440px" :close-on-click-modal="false">
      <template #header>
        <div class="refund-dialog-header">
          <el-icon :size="20" color="#F59E0B"><Warning /></el-icon>
          <span>申请退款</span>
        </div>
      </template>
      <el-form label-width="80px">
        <el-form-item label="退款原因">
          <el-select v-model="refundForm.category" placeholder="请选择退款原因" style="width: 100%">
            <el-option v-for="r in refundReasons" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="refundForm.category === '其他原因'" label="原因说明">
          <el-input
            v-model="refundForm.custom"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请具体说明退款原因，便于卖家与平台了解情况"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="refundSubmitting" @click="submitRefund">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 支付收银台：选渠道 → 扫码付款 → 支付成功 -->
    <PayDialog v-model:visible="payVisible" :order="payingOrder" @paid="onPaid" />
  </div>
</template>

<script setup>
// 订单中心页：展示买卖订单，处理支付、发货、收货、退款仲裁和取消。
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Picture, User, Warning } from '@element-plus/icons-vue'
import { getOrderList, getOrderStatusCounts, updateOrderStatus, cancelOrder, applyRefund, handleRefund, applyArbitration, deleteOrder } from '@/api/order'
import PayDialog from '@/components/PayDialog.vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
const orders = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
// 支持从首页提醒条等处带 ?tab= 参数直达对应状态页签
const VALID_TABS = ['PENDING', 'PAID', 'SHIPPED', 'COMPLETED', 'REFUND', 'CANCELLED']
const activeTab = ref(VALID_TABS.includes(route.query.tab) ? route.query.tab : 'PENDING')
let ticker = null

// 退款申请弹窗状态：预设原因分类 + “其他原因”自定义输入
const refundDialogVisible = ref(false)
const refundOrderId = ref(null)
const refundSubmitting = ref(false)
const refundForm = reactive({ category: '', custom: '' })
const refundReasons = ['不想要了', '与卖家协商一致', '地址填错', '商品不符', '行程不方便', '其他原因']

const tabs = reactive([
  { key: 'PENDING', label: '待付款', count: 0, badge: true },
  { key: 'PAID', label: '已付款', count: 0, badge: true },
  { key: 'SHIPPED', label: '已发货', count: 0, badge: true },
  { key: 'COMPLETED', label: '已完成', count: 0, badge: false },
  { key: 'REFUND', label: '退款/售后', count: 0, badge: true },
  { key: 'CANCELLED', label: '已取消', count: 0, badge: false }
])

const statusMap = { PENDING: '待付款', PAID: '已付款', SHIPPED: '已发货', COMPLETED: '已完成', REFUND: '退款/售后', CANCELLED: '已取消' }
const statusTypeMap = { PENDING: 'warning', PAID: 'primary', SHIPPED: 'success', COMPLETED: 'success', REFUND: 'warning', CANCELLED: 'info' }
const refundStatusMap = {
  REQUESTED: '退款待处理',
  SELLER_AGREED: '卖家已同意退款',
  SELLER_REJECTED: '卖家已拒绝退款',
  ARBITRATION: '仲裁中',
  ARBITRATION_REFUND: '仲裁判定退款',
  ARBITRATION_MAINTAIN: '仲裁维持交易'
}
const refundTypeMap = {
  REQUESTED: 'warning',
  SELLER_AGREED: 'success',
  SELLER_REJECTED: 'danger',
  ARBITRATION: 'danger',
  ARBITRATION_REFUND: 'warning',
  ARBITRATION_MAINTAIN: 'info'
}

/** 订单状态转中文标签。 */
function statusText(s) { return statusMap[s] || s }
/** 订单状态转标签颜色类型。 */
function statusType(s) { return statusTypeMap[s] || '' }
/** 退款状态转中文标签。 */
function refundText(s) { return s && s !== 'NONE' ? (refundStatusMap[s] || s) : '' }
/** 退款状态转标签颜色类型。 */
function refundType(s) { return refundTypeMap[s] || 'warning' }
/** 判断当前用户是否是订单买家。 */
function isBuyer(o) { return o.buyerId === userStore.userInfo?.userId }
/** 判断当前用户是否是订单卖家。 */
function isSeller(o) { return o.sellerId === userStore.userInfo?.userId }
// 退款进行中（待处理/仲裁中）：订单正常流转按钮与状态标签让位于退款处理
/** 判断订单退款流程是否仍在进行。 */
function refundActive(o) { return o.refundStatus === 'REQUESTED' || o.refundStatus === 'ARBITRATION' }

/** 计算待付款订单剩余时间。 */
function countdown(expireTime) {
  if (!expireTime) return ''
  const remain = new Date(expireTime).getTime() - Date.now()
  if (remain <= 0) return '已超时'
  const m = Math.floor(remain / 60000)
  const s = Math.floor((remain % 60000) / 1000)
  return `剩余 ${m}:${String(s).padStart(2, '0')}`
}

/** 按页签和筛选条件加载订单。 */
async function loadData() {
  const res = await getOrderList({
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    status: activeTab.value
  })
  orders.value = res.data.records
  total.value = res.data.total
}

/** 加载各订单状态角标数量。 */
async function loadAllCounts() {
  // 单接口取三个状态的计数；角标持续提醒直至订单完成/取消后自然消失
  try {
    const res = await getOrderStatusCounts()
    for (const tab of tabs) {
      tab.count = tab.badge ? (res.data[tab.key] || 0) : 0
    }
  } catch {}
}

// 支付收银台状态
const payVisible = ref(false)
const payingOrder = ref(null)

/** 打开支付弹窗并记录待支付订单。 */
function handlePay(order) {
  payingOrder.value = order
  payVisible.value = true
}

/** 支付成功后刷新订单和角标。 */
function onPaid() {
  loadData(); loadAllCounts()
}

/** 取消待付款订单。 */
async function handleCancel(id) {
  await ElMessageBox.confirm('确认取消订单？商品将重新上架。', '取消订单', { type: 'warning' })
  await cancelOrder(id)
  ElMessage.success('已取消')
  loadData(); loadAllCounts()
}

/** 打开退款申请弹窗。 */
async function handleApplyRefund(id) {
  refundOrderId.value = id
  refundForm.category = ''
  refundForm.custom = ''
  refundDialogVisible.value = true
}

/** 提交退款原因。 */
async function submitRefund() {
  if (!refundForm.category) { ElMessage.warning('请选择退款原因'); return }
  if (refundForm.category === '其他原因' && !refundForm.custom.trim()) {
    ElMessage.warning('请填写具体原因说明')
    return
  }
  const reason = refundForm.category === '其他原因'
    ? `其他原因：${refundForm.custom.trim()}`
    : refundForm.category
  refundSubmitting.value = true
  try {
    await applyRefund(refundOrderId.value, reason)
    ElMessage.success('退款申请已提交，等待卖家处理')
    refundDialogVisible.value = false
    loadData()
  } finally { refundSubmitting.value = false }
}

/** 卖家同意或拒绝退款。 */
async function handleRefundAction(id, agree) {
  await ElMessageBox.confirm(agree ? '确认同意退款？订单将取消，商品恢复在售。' : '确认拒绝买家的退款申请？', '处理退款', { type: 'warning' })
  await handleRefund(id, agree)
  ElMessage.success(agree ? '已同意退款' : '已拒绝退款')
  loadData()
}

/** 买家申请平台仲裁。 */
async function handleArbitration(id) {
  await ElMessageBox.confirm('卖家已拒绝退款，确认申请平台仲裁？管理员将介入处理。', '申请仲裁', { type: 'warning' })
  await applyArbitration(id)
  ElMessage.success('已提交仲裁，请等待管理员处理')
  loadData()
}

/** 删除已完成或已取消订单。 */
async function handleDeleteOrder(id) {
  await ElMessageBox.confirm('确认删除该订单？删除后将不再显示。', '删除订单', { type: 'warning' })
  await deleteOrder(id)
  ElMessage.success('订单已删除')
  loadData(); loadAllCounts()
}

/** 通用更新订单状态入口。 */
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

/* ====== 标签页 ====== */
.order-tabs { margin-bottom: 20px; }
.tab-label { display: flex; align-items: center; gap: 6px; }
.tab-badge {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 20px; height: 20px; padding: 0 6px;
  border-radius: 10px;
  font-size: 11px; font-weight: 700;
  background: #EF4444; color: #fff;
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

/* 退款弹窗 */
.refund-dialog-header {
  display: flex; align-items: center; gap: 8px;
  font-size: 16px; font-weight: 700; color: #1F2937;
}

/* ====== 响应式 ====== */
@media (max-width: 768px) {
  .order-content { padding: 12px 14px 10px; }
  .order-body { flex-wrap: wrap; }
  .order-actions { width: 100%; justify-content: flex-end; margin-top: 8px; }
}
/* 手机端：按钮全宽 */
@media (max-width: 480px) {
  
  .order-item { border-radius: 10px; }
  .order-actions { flex-direction: column; }
  .order-actions .el-button { width: 100%; }
  .order-image .el-image { width: 60px; height: 60px; }
  .order-info h4 { font-size: 14px; }
  .order-price { font-size: 16px; }
}
</style>
