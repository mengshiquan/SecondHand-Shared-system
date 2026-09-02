<template>
  <div class="detail page-container" v-loading="loading">
    <template v-if="product">
      <el-row :gutter="32">
        <!-- 左侧图片 -->
        <el-col :xs="24" :md="12">
          <div class="gallery-card" v-if="product.images?.length">
            <div class="gallery-main">
              <el-image
                :src="galleryIndex !== null ? product.images[galleryIndex] : product.images[0]"
                fit="contain"
                class="gallery-main-img"
                :preview-src-list="product.images"
                :initial-index="galleryIndex || 0"
              />
              <div class="gallery-nav" v-if="product.images.length > 1">
                <button class="gallery-arrow prev" @click="prevImage" :disabled="galleryIndex <= 0">
                  <el-icon :size="18"><ArrowLeft /></el-icon>
                </button>
                <button class="gallery-arrow next" @click="nextImage" :disabled="galleryIndex >= product.images.length - 1">
                  <el-icon :size="18"><ArrowRight /></el-icon>
                </button>
              </div>
              <span class="gallery-counter" v-if="product.images.length > 1">
                {{ (galleryIndex || 0) + 1 }} / {{ product.images.length }}
              </span>
              <span class="gallery-hangtag" v-if="product.categoryName">{{ product.categoryName }}</span>
            </div>
            <div class="gallery-thumbs" v-if="product.images.length > 1">
              <div
                v-for="(img, i) in product.images"
                :key="i"
                class="thumb-item"
                :class="{ active: (galleryIndex || 0) === i }"
                @click="galleryIndex = i"
              >
                <el-image :src="img" fit="cover" class="thumb-img" />
              </div>
            </div>
          </div>
          <div v-else class="no-image">
            <el-icon :size="48"><Picture /></el-icon>
            <span>暂无图片</span>
          </div>
        </el-col>

        <!-- 右侧信息 -->
        <el-col :xs="24" :md="12">
          <div class="info-section">
            <div class="info-header">
              <h1 class="title">{{ product.title }}</h1>
              <div class="header-actions">
                <div class="tags">
                  <el-tag effect="plain" type="success">{{ product.categoryName }}</el-tag>
                  <el-tag :type="product.status === 'ON_SALE' ? '' : 'info'" effect="plain">{{ statusText }}</el-tag>
                </div>
                <button class="btn-share" @click="handleShare" title="分享商品">
                  <el-icon :size="18"><Share /></el-icon>
                </button>
              </div>
            </div>

            <div class="price-tag">
              <span class="tag-hole"></span>
              <div class="price-main">
                <span class="price-symbol">¥</span>
                <span class="price-value">{{ product.price }}</span>
                <span v-if="product.originalPrice" class="price-original">¥{{ product.originalPrice }}</span>
              </div>
              <div class="price-meta">
                <span class="price-view">
                  <el-icon :size="14"><View /></el-icon>
                  {{ product.viewCount }} 次浏览
                </span>
                <span class="price-time">
                  <el-icon :size="14"><Clock /></el-icon>
                  {{ formatTime(product.createTime) }}
                </span>
              </div>
            </div>

            <div class="desc-card">
              <div class="desc-label">
                <el-icon :size="16"><Document /></el-icon>
                <span>商品描述</span>
              </div>
              <p>{{ product.description }}</p>
            </div>

            <div class="seller-card">
              <div class="seller-main" title="进入 TA 的商品主页" @click="goSellerHome">
                <div class="seller-stamp">
                  <el-avatar :size="52" :src="sellerAvatar || undefined" class="seller-avatar">
                    {{ product.sellerName?.[0] }}
                  </el-avatar>
                </div>
                <div class="seller-info">
                  <span class="seller-name">{{ product.sellerName }}</span>
                  <span class="seller-label">校园认证卖家 · 点击查看主页</span>
                </div>
              </div>
              <div class="seller-actions">
                <el-button
                  v-if="!isOwner"
                  class="btn-contact"
                  round
                  @click="goChat"
                >
                  <el-icon><ChatDotRound /></el-icon>联系卖家
                </el-button>
                <el-button
                  v-if="!isOwner && userStore.isLoggedIn"
                  class="btn-complaint"
                  round
                  @click="openComplaint(product.userId)"
                >
                  <el-icon><WarningFilled /></el-icon>投诉
                </el-button>
              </div>
              <div class="awning-edge"></div>
            </div>

            <div class="actions">
              <el-button
                v-if="product.status === 'ON_SALE' && !isOwner"
                type="primary"
                size="large"
                class="btn-buy"
                @click="handleBuy"
              >立即购买</el-button>
              <el-button
                v-if="product.status === 'ON_SALE' && !isOwner"
                size="large"
                plain
                class="btn-cart"
                @click="handleAddCart"
              >
                <el-icon><ShoppingCart /></el-icon>
                加入购物车
              </el-button>
              <el-button
                v-if="!isOwner && userStore.isLoggedIn"
                :type="product.favorited ? 'warning' : ''"
                size="large"
                class="btn-fav"
                @click="handleFavorite"
              >
                <el-icon><Star /></el-icon>
                {{ product.favorited ? '已收藏' : '收藏' }}
              </el-button>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 移动端底部操作栏：悬浮于底部导航之上，提升拇指可达性 -->
      <div v-if="product.status === 'ON_SALE' && !isOwner" class="mobile-action-bar">
        <el-button class="bar-fav" :type="product.favorited ? 'warning' : ''" @click="handleFavorite">
          <el-icon><Star /></el-icon>
        </el-button>
        <el-button class="bar-cart" plain @click="handleAddCart">
          <el-icon><ShoppingCart /></el-icon>加购
        </el-button>
        <el-button class="bar-buy" type="primary" @click="handleBuy">立即购买</el-button>
      </div>

      <!-- 卖家其他在售 -->
      <section v-if="sellerProducts.length > 0" class="seller-products">
        <div class="section-sign">
          <h2>{{ product.sellerName }} 的其他在售</h2>
          <span class="sign-count">{{ sellerProducts.length }} 件</span>
        </div>
        <div class="seller-products-grid">
          <ProductCard v-for="sp in sellerProducts" :key="sp.id" :product="sp" />
        </div>
      </section>

      <!-- 购买信息弹窗 -->
      <el-dialog v-model="buyDialogVisible" width="440px" :close-on-click-modal="false">
        <template #header>
          <div class="dialog-header">
            <el-icon :size="22" color="#10B981"><Goods /></el-icon>
            <span>填写收货信息</span>
          </div>
        </template>
        <div v-if="addressFilled" class="address-auto-fill">
          <el-icon><Location /></el-icon>
          已根据你的默认收货地址自动填充，可修改
        </div>
        <el-form ref="buyFormRef" :model="buyForm" :rules="buyRules" label-width="80px">
          <el-form-item label="收货人" prop="buyerName">
            <el-input v-model="buyForm.buyerName" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="联系电话" prop="buyerPhone">
            <el-input v-model="buyForm.buyerPhone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="收货地址" prop="buyerAddress">
            <el-input v-model="buyForm.buyerAddress" placeholder="宿舍楼/快递点" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="buyForm.remark" type="textarea" :rows="2" placeholder="给卖家留言（选填）" />
          </el-form-item>
          <div class="buy-notice">
            <el-icon><Clock /></el-icon>
            提交后请在 <strong>30 分钟</strong>内完成付款，超时将自动取消
          </div>
        </el-form>
        <template #footer>
          <el-button @click="buyDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="buying" @click="confirmBuy">确定购买</el-button>
        </template>
      </el-dialog>

      <!-- 投诉弹窗 -->
      <el-dialog v-model="showComplaintDialog" width="440px" :close-on-click-modal="false">
        <template #header>
          <div class="dialog-header">
            <el-icon :size="22" color="#DC2626"><WarningFilled /></el-icon>
            <span>投诉用户</span>
          </div>
        </template>
        <el-form :model="complaintForm" label-width="70px">
          <el-form-item label="投诉原因">
            <el-select v-model="complaintForm.reason" placeholder="请选择" style="width:100%">
              <el-option label="恶意评论" value="恶意评论" />
              <el-option label="虚假描述" value="虚假描述" />
              <el-option label="骚扰行为" value="骚扰行为" />
              <el-option label="其他违规" value="其他违规" />
            </el-select>
          </el-form-item>
          <el-form-item label="详细描述">
            <el-input v-model="complaintForm.description" type="textarea" :rows="4" placeholder="请描述该用户的违规行为..." />
          </el-form-item>
          <div class="contact-hint">
            <el-icon><InfoFilled /></el-icon>
            投诉提交后管理员将尽快核实处理
          </div>
        </el-form>
        <template #footer>
          <el-button @click="showComplaintDialog = false">取消</el-button>
          <el-button type="danger" :loading="complaining" @click="submitComplaintHandler">提交投诉</el-button>
        </template>
      </el-dialog>

      <!-- 评论区 -->
      <section class="comments-section">
        <div class="section-sign">
          <h2>商品评价</h2>
          <span class="sign-count" v-if="comments.length">{{ comments.length }} 条评价</span>
        </div>

        <div v-if="userStore.isLoggedIn" class="comment-form-card">
          <div class="comment-form-row">
            <el-rate v-model="commentForm.rating" />
            <span class="rating-text">{{ ratingText(commentForm.rating) }}</span>
          </div>
          <el-input v-model="commentForm.content" type="textarea" :rows="3" placeholder="分享你对这个商品的看法..." />
          <el-button type="primary" class="comment-submit" @click="submitComment">发表评论</el-button>
        </div>

        <div v-if="comments.length > 0" class="comment-list">
          <div v-for="c in comments" :key="c.id" class="comment-card">
            <el-avatar :size="40" class="comment-avatar">{{ c.nickname?.[0] }}</el-avatar>
            <div class="comment-body">
              <div class="comment-meta">
                <span class="comment-name">{{ c.nickname }}</span>
                <el-rate :model-value="c.rating" disabled size="small" />
                <el-button
                  v-if="userStore.isLoggedIn && c.userId !== userStore.userInfo?.userId"
                  class="btn-comment-complaint"
                  text size="small" type="danger"
                  @click="openComplaint(c.userId)"
                >投诉</el-button>
              </div>
              <p class="comment-text">{{ c.content }}</p>
              <span class="comment-time">{{ c.createTime }}</span>
            </div>
          </div>
        </div>
        <div v-else class="comment-empty">
          <el-icon :size="36"><ChatLineSquare /></el-icon>
          <p>还没有评价，快来发表第一条吧</p>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
// 商品详情页：加载商品信息，处理收藏、加购、立即购买、咨询和评价。
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Clock, ChatLineSquare, Goods, Picture, View, ArrowLeft, ArrowRight, Document, Share, ChatDotRound, InfoFilled, WarningFilled, ShoppingCart, Location } from '@element-plus/icons-vue'
import { getProductDetail, getProductList } from '@/api/product'
import { createOrder } from '@/api/order'
import { addToCart } from '@/api/cart'
import { getAddressList } from '@/api/address'
import { toggleFavorite } from '@/api/favorite'
import { getCommentList, addComment } from '@/api/comment'
import { submitComplaint } from '@/api/complaint'
import { getSellerInfo } from '@/api/user'
import { useUserStore } from '@/stores/user'
import ProductCard from '@/components/ProductCard.vue'

/**
 * 商品详情页：展示商品信息、图片画廊、卖家卡片、购买/收藏/购物车、评论与投诉
 */
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const product = ref(null)              // 商品详情对象
const comments = ref([])               // 商品评价列表
const loading = ref(false)             // 详情加载状态
const buying = ref(false)              // 下单提交中
const buyDialogVisible = ref(false)    // 购买信息弹窗显隐
const buyFormRef = ref(null)           // 购买表单引用
const galleryIndex = ref(0)            // 当前展示图片索引
const sellerProducts = ref([])         // 卖家其他在售商品
const sellerAvatar = ref('')           // 卖家真实头像
const showComplaintDialog = ref(false) // 投诉弹窗显隐
const complaining = ref(false)         // 投诉提交中
const complaintForm = reactive({ targetUserId: null, reason: '', description: '' })

const commentForm = reactive({ content: '', rating: 5 })                                  // 评论表单
const buyForm = reactive({ buyerName: '', buyerPhone: '', buyerAddress: '', remark: '', addressId: null }) // 购买表单
const addressFilled = ref(false)                                                         // 是否已自动填充默认地址
const buyRules = {
  buyerName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  buyerPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  buyerAddress: [{ required: true, message: '请输入收货地址', trigger: 'blur' }]
}

// 当前登录用户是否为卖家（控制购买/联系等按钮显隐）
const isOwner = computed(() => product.value?.userId === userStore.userInfo?.userId)

// 商品状态中文映射
const statusText = computed(() => {
  const map = { ON_SALE: '在售', SOLD: '已售', OFF_SHELF: '已下架' }
  return map[product.value?.status] || ''
})

/**
 * 将后端 ISO 时间格式化为 yyyy-MM-dd HH:mm
 * @param {string} t ISO 时间字符串
 * @returns {string}
 */
function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

/**
 * 加载商品详情、评价列表及卖家其他商品
 */
async function loadDetail() {
  loading.value = true
  try {
    const res = await getProductDetail(route.params.id)
    product.value = res.data
    const commentRes = await getCommentList(route.params.id, { pageNum: 1, pageSize: 20 })
    comments.value = commentRes.data.records
    if (product.value.userId) {
      loadSellerProducts(product.value.userId)
      loadSellerInfo(product.value.userId)
    }
  } finally {
    loading.value = false
  }
}

/**
 * 加载卖家其他在售商品（排除当前商品，最多 6 条）
 * @param {number} sellerId 卖家用户 ID
 */
async function loadSellerProducts(sellerId) {
  try {
    const res = await getProductList({ sellerId, status: 'ON_SALE', pageSize: 7 })
    sellerProducts.value = (res.data.records || []).filter(p => p.id !== Number(route.params.id)).slice(0, 6)
  } catch { sellerProducts.value = [] }
}

/**
 * 加载卖家公开信息（真实头像），失败时回退首字头像
 * @param {number} sellerId 卖家用户 ID
 */
async function loadSellerInfo(sellerId) {
  try {
    const res = await getSellerInfo(sellerId)
    sellerAvatar.value = res.data.avatar || ''
  } catch { sellerAvatar.value = '' }
}

/**
 * 点击“立即购买”：
 * 1. 未登录跳转登录页
 * 2. 清空表单并打开购买弹窗
 * 3. 尝试自动填充默认收货地址
 */
async function handleBuy() {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  Object.assign(buyForm, { buyerName: '', buyerPhone: '', buyerAddress: '', remark: '', addressId: null })
  addressFilled.value = false
  buyDialogVisible.value = true
  // 自动填充默认收货地址，避免每次下单重复填写；失败时仍可手动输入
  try {
    const res = await getAddressList()
    const list = res.data || []
    const def = list.find(a => a.isDefault === 1 || a.isDefault === true) || list[0]
    if (def) {
      Object.assign(buyForm, {
        buyerName: def.receiverName,
        buyerPhone: def.phone,
        buyerAddress: def.address,
        addressId: def.id
      })
      addressFilled.value = true
    }
  } catch { /* 忽略：允许手动填写 */ }
}

/**
 * 联系卖家：未登录提示并跳转登录，已登录跳转聊天页并带上商品 ID
 */
function goChat() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再联系卖家')
    router.push('/login')
    return
  }
  router.push({ path: `/chat/${product.value.userId}`, query: { productId: product.value.id } })
}

/**
 * 进入卖家商品主页
 */
function goSellerHome() {
  if (product.value?.userId) {
    router.push(`/seller/${product.value.userId}`)
  }
}

/**
 * 确认购买：校验表单 → 创建订单 → 关闭弹窗 → 提示并跳转订单列表
 */
async function confirmBuy() {
  await buyFormRef.value.validate()
  buying.value = true
  try {
    await createOrder({ ...buyForm, productId: product.value.id })
    buyDialogVisible.value = false
    ElMessage.success('下单成功，请在30分钟内完成付款')
    router.push('/orders')
  } catch (e) {
    console.error(e)
    ElMessage.error(e?.response?.data?.message || '下单失败，请重试')
  } finally { buying.value = false }
}

/**
 * 切换商品收藏状态
 */
async function handleFavorite() {
  await toggleFavorite(product.value.id)
  product.value.favorited = !product.value.favorited
  ElMessage.success(product.value.favorited ? '收藏成功' : '已取消收藏')
}

/**
 * 加入购物车：未登录跳转登录，已登录调用接口
 */
async function handleAddCart() {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  await addToCart(product.value.id)
  ElMessage.success('已加入购物车')
}

/**
 * 提交商品评价并清空表单、刷新详情
 */
async function submitComment() {
  if (!commentForm.content.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  await addComment({
    productId: Number(route.params.id),
    content: commentForm.content,
    rating: commentForm.rating
  })
  commentForm.content = ''
  ElMessage.success('评论成功')
  loadDetail()
}

/**
 * 评分星级对应中文文案
 * @param {number} r 1-5 星
 * @returns {string}
 */
function ratingText(r) {
  const map = { 1: '很差', 2: '较差', 3: '一般', 4: '不错', 5: '很棒' }
  return map[r] || ''
}

/**
 * 分享商品：优先使用系统分享，不支持则复制链接
 */
async function handleShare() {
  const url = window.location.href
  if (navigator.share) {
    try {
      await navigator.share({ title: product.value.title, text: product.value.description, url })
    } catch {}
  } else {
    try {
      await navigator.clipboard.writeText(url)
      ElMessage.success('链接已复制到剪贴板')
    } catch {
      ElMessage.info('分享链接：' + url)
    }
  }
}

/**
 * 图片画廊：切换到上一张
 */
function prevImage() {
  if (galleryIndex.value > 0) galleryIndex.value--
}

/**
 * 图片画廊：切换到下一张
 */
function nextImage() {
  if (product.value && galleryIndex.value < product.value.images.length - 1) galleryIndex.value++
}

/**
 * 打开投诉弹窗并设置被投诉用户 ID
 * @param {number} userId 被投诉用户 ID
 */
function openComplaint(userId) {
  complaintForm.targetUserId = userId
  complaintForm.reason = ''
  complaintForm.description = ''
  showComplaintDialog.value = true
}

/**
 * 提交用户投诉
 */
async function submitComplaintHandler() {
  if (!complaintForm.reason) { ElMessage.warning('请选择投诉原因'); return }
  if (!complaintForm.description.trim()) { ElMessage.warning('请填写详细描述'); return }
  complaining.value = true
  try {
    await submitComplaint({
      targetUserId: complaintForm.targetUserId,
      reason: complaintForm.reason,
      description: complaintForm.description
    })
    ElMessage.success('投诉已提交，管理员将尽快处理')
    showComplaintDialog.value = false
  } finally { complaining.value = false }
}

// 页面挂载后加载商品详情
onMounted(loadDetail)

// 同路由切换商品（组件复用）时重置状态并重新加载，避免详情页内容不刷新
watch(() => route.params.id, (id, oldId) => {
  if (id && id !== oldId) {
    product.value = null
    comments.value = []
    sellerProducts.value = []
    sellerAvatar.value = ''
    galleryIndex.value = 0
    loadDetail()
  }
})
</script>

<style scoped>
/* ====== 图片画廊 ====== */
.gallery-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.gallery-main {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #F9FAFB;
  display: flex; align-items: center; justify-content: center;
}
.gallery-main-img {
  width: 100%; height: 100%;
}
.gallery-nav {
  position: absolute; inset: 0;
  display: flex; justify-content: space-between; align-items: center;
  padding: 0 12px; pointer-events: none;
}
.gallery-arrow {
  pointer-events: auto;
  width: 36px; height: 36px;
  border-radius: 50%;
  border: none;
  background: rgba(255,255,255,0.9);
  color: #374151;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transition: all 0.2s;
  opacity: 0; transform: translateX(-4px);
}
.gallery-arrow.next { transform: translateX(4px); }
.gallery-main:hover .gallery-arrow { opacity: 1; transform: translateX(0); }
.gallery-arrow:disabled { opacity: 0; pointer-events: none; }
.gallery-arrow:hover:not(:disabled) { background: #fff; box-shadow: 0 4px 12px rgba(0,0,0,0.15); }

.gallery-counter {
  position: absolute; bottom: 12px; right: 14px;
  background: rgba(0,0,0,0.5); color: #fff;
  font-size: 12px; padding: 2px 10px; border-radius: 10px;
  font-variant-numeric: tabular-nums;
}

/* 分类吊签：集市标签语言（左端带穿绳孔） */
.gallery-hangtag {
  position: absolute; top: 12px; left: 12px;
  padding: 4px 12px 4px 20px;
  background: var(--sh-primary-deep);
  color: #fff;
  font-size: 12px;
  letter-spacing: 1px;
  border-radius: 4px 999px 999px 4px;
}
.gallery-hangtag::before {
  content: '';
  position: absolute; left: 8px; top: 50%;
  transform: translateY(-50%);
  width: 6px; height: 6px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.85);
}

/* 缩略图 */
.gallery-thumbs {
  display: flex; gap: 8px; padding: 12px;
  overflow-x: auto;
}
.thumb-item {
  flex-shrink: 0;
  width: 64px; height: 64px; border-radius: 10px;
  overflow: hidden; cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.thumb-item.active {
  border-color: #10B981;
  box-shadow: 0 0 0 2px rgba(16,185,129,0.2);
}
.thumb-item:hover:not(.active) { border-color: #D1FAE5; }
.thumb-img { width: 100%; height: 100%; }

.no-image {
  width: 100%;
  aspect-ratio: 1 / 1;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 12px;
  background: linear-gradient(135deg, #F9FAFB, #F3F4F6);
  border-radius: 16px;
  color: #9CA3AF; font-size: 14px;
}

/* ====== 右侧信息 ====== */
.info-section {
  display: flex; flex-direction: column; gap: 20px;
}
.info-header .title {
  font-size: 24px; font-weight: 800; color: #1F2937;
  margin: 0 0 12px; line-height: 1.4;
}
.header-actions { display: flex; align-items: center; justify-content: space-between; }
.tags { display: flex; gap: 8px; }

.btn-share {
  width: 36px; height: 36px;
  border: 1px solid #E5E7EB; border-radius: 10px;
  background: #fff; color: #6B7280;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.btn-share:hover { border-color: #10B981; color: #10B981; background: #F0FDF4; }

/* 集市吊牌式价格区：白底吊牌 + 穿绳孔 + 虚线边 */
.price-tag {
  position: relative;
  padding: 16px 20px 14px 42px;
  background: #fff;
  border: 1.5px dashed #FCD34D;
  border-radius: 12px;
  transform: rotate(-0.6deg);
  box-shadow: var(--sh-shadow-sm);
}
.tag-hole {
  position: absolute; left: 14px; top: 50%;
  transform: translateY(-50%);
  width: 14px; height: 14px;
  border: 2px solid #FCD34D;
  border-radius: 50%;
  background: var(--sh-bg);
}
.price-main { display: flex; align-items: baseline; gap: 4px; margin-bottom: 8px; }
.price-symbol { font-size: 20px; font-weight: 700; color: #F59E0B; }
.price-value {
  font-size: 36px; font-weight: 800; color: #F59E0B;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  font-variant-numeric: tabular-nums;
}
.price-original {
  font-size: 14px; color: #9CA3AF;
  text-decoration: line-through; margin-left: 8px;
}
.price-meta {
  display: flex; align-items: center; gap: 18px;
  font-size: 13px; color: #9CA3AF;
}
.price-view, .price-time {
  display: flex; align-items: center; gap: 4px;
}

/* 描述 */
.desc-card {
  background: #F9FAFB; border-radius: 12px;
  padding: 16px 20px; border: 1px solid #F3F4F6;
}
.desc-label {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; font-weight: 600; color: #6B7280;
  margin-bottom: 10px;
}
.desc-card p {
  color: #4B5563; font-size: 14px; line-height: 1.8; margin: 0;
}

/* 卖家：深绿迷你招牌，与卖家主页摊位语言同源 */
.seller-card {
  position: relative;
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px 20px;
  background: var(--sh-primary-deep);
  border-radius: 14px 14px 0 0;
  gap: 16px;
}
.seller-main {
  display: flex; align-items: center; gap: 14px;
  cursor: pointer;
}
.seller-stamp {
  flex-shrink: 0;
  padding: 4px;
  border: 2px dashed rgba(255, 255, 255, 0.6);
  border-radius: 50%;
}
.seller-avatar { background: #fff; color: #059669; font-weight: 700; }
.seller-info { display: flex; flex-direction: column; gap: 3px; }
.seller-name { font-size: 15px; font-weight: 700; color: #fff; }
.seller-label { font-size: 12px; color: #D1FAE5; }
.seller-actions { flex-shrink: 0; }
.btn-contact {
  background: #fff; border-color: #fff; color: #059669;
  font-weight: 600;
}
.btn-contact:hover { background: #ECFDF5; }
.btn-complaint {
  background: transparent; border-color: rgba(255, 255, 255, 0.5); color: #fff;
  font-weight: 500;
}
.btn-complaint:hover { background: rgba(255, 255, 255, 0.12); border-color: #fff; }
.btn-comment-complaint {
  margin-left: auto; opacity: 0;
  transition: opacity 0.2s;
}
.comment-card:hover .btn-comment-complaint { opacity: 1; }

/* 操作 */
.actions { display: flex; gap: 12px; }
.btn-buy { flex: 1; height: 48px; font-size: 16px; font-weight: 700; }
.btn-cart {
  height: 48px; padding: 0 20px;
  border-color: #10B981; color: #10B981; font-weight: 600;
}
.btn-cart:hover { background: #F0FDF4; }
.btn-fav { height: 48px; }

/* 移动端底部操作栏：默认隐藏，≤768px 时替代页内操作按钮 */
.mobile-action-bar { display: none; }

/* ====== 卖家其他商品（标题用全局 .section-sign 招牌样式） ====== */
.seller-products {
  margin-top: 40px;
  padding-top: 32px;
  border-top: 2px solid #F3F4F6;
}
.seller-products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

/* ====== 购买弹窗 ====== */
.dialog-header {
  display: flex; align-items: center; gap: 8px;
  font-size: 16px; font-weight: 700; color: #1F2937;
}
.buy-notice {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  margin-top: 4px; padding: 10px; font-size: 13px;
  color: #F59E0B; background: #FFFBEB; border-radius: 8px;
}
.address-auto-fill {
  display: flex; align-items: center; gap: 6px;
  margin-bottom: 12px; padding: 8px 12px;
  font-size: 12px; color: #059669; background: #F0FDF4;
  border: 1px solid #D1FAE5; border-radius: 8px;
}
.buy-notice strong { color: #D97706; }

/* ====== 评论区（标题用全局 .section-sign 招牌样式） ====== */
.comments-section {
  margin-top: 48px; padding-top: 32px;
  border-top: 2px solid #F3F4F6;
}

/* 评论表单 */
.comment-form-card {
  background: #fff; border-radius: 14px;
  padding: 20px 24px; margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  border: 1px solid #F3F4F6;
  display: flex; flex-direction: column; gap: 12px;
}
.comment-form-row { display: flex; align-items: center; gap: 10px; }
.rating-text { font-size: 13px; color: #F59E0B; font-weight: 500; }
.comment-submit { align-self: flex-end; }

/* 评论列表 */
.comment-list { display: flex; flex-direction: column; gap: 0; }
.comment-card {
  display: flex; gap: 14px;
  padding: 18px 0;
  border-bottom: 1px solid #F3F4F6;
}
.comment-card:last-child { border-bottom: none; }
.comment-avatar { flex-shrink: 0; }
.comment-body { flex: 1; min-width: 0; }
.comment-meta {
  display: flex; align-items: center; gap: 10px;
  margin-bottom: 6px;
}
.comment-name { font-weight: 600; font-size: 14px; color: #1F2937; }
.comment-text {
  color: #4B5563; font-size: 14px; line-height: 1.7; margin: 0 0 6px;
}
.comment-time { font-size: 12px; color: #c0c4cc; }

/* 空评价 */
.comment-empty {
  text-align: center; padding: 40px 20px;
  color: #9CA3AF; font-size: 14px;
}
.comment-empty .el-icon { margin-bottom: 10px; color: #D1D5DB; }
.comment-empty p { margin: 0; }

/* ====== 响应式 ====== */
@media (max-width: 768px) {
  .info-section { gap: 14px; margin-top: 20px; }
  .info-header .title { font-size: 20px; }
  .price-value { font-size: 28px; }
  .gallery-card { border-radius: 12px; }
  .no-image { height: 280px; }
  .seller-card { flex-direction: column; align-items: flex-start; }
  .seller-products-grid { grid-template-columns: repeat(2, 1fr); }
  /* 页内操作按钮让位给底部固定操作栏 */
  .actions { display: none; }
  .detail { padding-bottom: 88px; }
  .mobile-action-bar {
    position: fixed; left: 0; right: 0;
    bottom: calc(64px + env(safe-area-inset-bottom, 8px));
    z-index: 190;
    display: flex; gap: 8px;
    padding: 8px 12px;
    background: #fff;
    box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);
  }
  .bar-fav { width: 44px; padding: 0; margin: 0; }
  .bar-cart { flex: 1; margin: 0; border-color: #10B981; color: #10B981; font-weight: 600; }
  .bar-buy { flex: 1.4; margin: 0; font-weight: 700; }
}
/* 手机端 */
@media (max-width: 480px) {
  .gallery-main { aspect-ratio: 3/2; }
  .no-image { height: 200px; }
  .seller-card { padding: 12px 14px; }
  .seller-actions { width: 100%; display: flex; flex-direction: column; gap: 8px; }
  .seller-actions .el-button { width: 100%; margin: 0; }
  .seller-products-grid {
    grid-template-columns: repeat(2, 1fr);
    overflow-x: auto;
    gap: 10px;
  }
  .comment-card { padding: 12px 0; }
  .comment-avatar { width: 32px; height: 32px; }
  .price-box { padding: 12px 14px; }
  .desc-card { padding: 12px 14px; }
}
</style>
