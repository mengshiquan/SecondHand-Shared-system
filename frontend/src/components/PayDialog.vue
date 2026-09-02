<template>
  <el-dialog
    :model-value="visible"
    width="400px"
    :close-on-click-modal="false"
    @update:model-value="emit('update:visible', $event)"
    @close="reset"
  >
    <template #header>
      <div class="pay-header" :class="channel === 'WECHAT' ? 'wechat' : 'alipay'">
        <span class="pay-header-dot"></span>
        <span>{{ headerText }}</span>
      </div>
    </template>

    <!-- 第一步：选择支付渠道 -->
    <div v-if="step === 'select'" class="pay-select">
      <p class="pay-amount">¥ {{ order?.price }}</p>
      <p class="pay-orderno">订单号 {{ order?.orderNo }}</p>
      <div class="pay-method" @click="choose('ALIPAY')">
        <span class="method-icon alipay">支</span>
        <div class="method-text">
          <b>支付宝</b>
          <small>沙箱环境 · 沙箱版支付宝 App 扫码支付</small>
        </div>
      </div>
      <div class="pay-method" @click="choose('WECHAT')">
        <span class="method-icon wechat">微</span>
        <div class="method-text">
          <b>微信支付</b>
          <small>模拟环境 · 模拟扫码付款流程</small>
        </div>
      </div>
    </div>

    <!-- 第二步：扫码支付 -->
    <div v-else-if="step === 'qr'" class="pay-qr">
      <p class="pay-amount">¥ {{ order?.price }}</p>
      <img :src="qrUrl" class="qr-img" alt="支付二维码" />
      <p v-if="channel === 'ALIPAY'" class="pay-hint">请用沙箱版支付宝 App 扫描上方二维码完成支付（登录沙箱买家账号）</p>
      <template v-else>
        <p class="pay-hint">模拟环境：点击下方按钮即视为已用微信扫码完成付款</p>
        <el-button type="success" :loading="simulating" @click="simulateScan">模拟扫码完成，确认付款</el-button>
      </template>
      <p class="pay-polling">正在等待支付结果…</p>
      <el-button link type="primary" @click="backToSelect">更换支付方式</el-button>
    </div>

    <!-- 第三步：支付成功 -->
    <div v-else class="pay-success">
      <el-icon :size="56" color="#10B981"><CircleCheck /></el-icon>
      <p class="success-title">支付成功</p>
      <p class="pay-amount">¥ {{ order?.price }}</p>
      <el-button type="primary" @click="finish">完成</el-button>
    </div>
  </el-dialog>
</template>

<script setup>
// 支付弹窗：选择渠道、展示二维码、轮询支付状态并处理模拟扫码。
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck } from '@element-plus/icons-vue'
import QRCode from 'qrcode'
import { createAlipayPay, createWechatPay, wechatMockNotify, getPayStatus } from '@/api/pay'

const props = defineProps({
  visible: Boolean,
  order: Object
})
const emit = defineEmits(['update:visible', 'paid'])

const step = ref('select') // select | qr | success
const channel = ref('')
const qrUrl = ref('')
const creating = ref(false)
const simulating = ref(false)
let pollTimer = null

const headerText = computed(() => {
  if (step.value === 'select') return '选择支付方式'
  if (step.value === 'success') return '支付结果'
  return channel.value === 'ALIPAY' ? '支付宝（沙箱环境）' : '微信支付（模拟环境）'
})

watch(() => props.visible, (v) => {
  if (v) reset()
})

/** 选择支付渠道并创建支付单。 */
async function choose(ch) {
  if (creating.value) return
  creating.value = true
  try {
    const res = ch === 'ALIPAY' ? await createAlipayPay(props.order.id) : await createWechatPay(props.order.id)
    channel.value = ch
    qrUrl.value = await QRCode.toDataURL(res.data.qrCode, { width: 240, margin: 2 })
    step.value = 'qr'
    startPoll()
  } catch (e) {
    // 拦截器已提示错误（如支付宝沙箱未配置），停留在选择页
  } finally {
    creating.value = false
  }
}

/** 启动支付状态轮询。 */
function startPoll() {
  stopPoll()
  pollTimer = setInterval(async () => {
    try {
      const res = await getPayStatus(props.order.id)
      if (res.data.status === 'PAID') {
        stopPoll()
        step.value = 'success'
        emit('paid')
      }
    } catch (e) { /* 轮询失败忽略，下轮重试 */ }
  }, 3000)
}

/** 停止支付状态轮询。 */
function stopPoll() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

/** 演示环境下模拟用户扫码完成微信支付。 */
async function simulateScan() {
  simulating.value = true
  try {
    await wechatMockNotify(props.order.orderNo)
    ElMessage.success('扫码成功，等待支付结果回调')
  } catch (e) {
    // 拦截器已提示
  } finally {
    simulating.value = false
  }
}

/** 返回支付渠道选择。 */
function backToSelect() {
  stopPoll()
  step.value = 'select'
}

/** 关闭时清理二维码、轮询和内部状态。 */
function reset() {
  stopPoll()
  step.value = 'select'
  channel.value = ''
  qrUrl.value = ''
}

/** 标记支付成功并通知父页面刷新订单。 */
function finish() {
  emit('update:visible', false)
  reset()
}

onBeforeUnmount(stopPoll)
</script>

<style scoped>
.pay-header { display: flex; align-items: center; gap: 8px; font-weight: 600; }
.pay-header-dot { width: 10px; height: 10px; border-radius: 50%; background: #1677FF; }
.pay-header.wechat .pay-header-dot { background: #07C160; }

.pay-amount { font-size: 26px; font-weight: 700; color: #F59E0B; text-align: center; margin: 4px 0; }
.pay-orderno { font-size: 12px; color: #9CA3AF; text-align: center; margin-bottom: 14px; }

.pay-method {
  display: flex; align-items: center; gap: 12px;
  border: 1px solid #E5E7EB; border-radius: 10px;
  padding: 12px 14px; margin-bottom: 10px; cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}
.pay-method:hover { border-color: #10B981; background: #F0F9F4; }
.method-icon {
  width: 36px; height: 36px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-weight: 700; font-size: 18px; flex-shrink: 0;
}
.method-icon.alipay { background: #1677FF; }
.method-icon.wechat { background: #07C160; }
.method-text b { display: block; font-size: 14px; color: #111827; }
.method-text small { color: #9CA3AF; font-size: 12px; }

.pay-qr { text-align: center; }
.qr-img { width: 240px; height: 240px; border: 1px solid #E5E7EB; border-radius: 8px; padding: 6px; background: #fff; }
.pay-hint { font-size: 12px; color: #6B7280; margin: 10px 0; }
.pay-polling { font-size: 12px; color: #10B981; margin: 8px 0 4px; }

.pay-success { text-align: center; padding: 10px 0 4px; }
.success-title { font-size: 18px; font-weight: 600; color: #10B981; margin: 10px 0 4px; }
.pay-success .el-button { margin-top: 14px; }
</style>
