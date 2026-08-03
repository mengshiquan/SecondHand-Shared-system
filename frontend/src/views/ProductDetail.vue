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

            <div class="price-box">
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
              <div class="seller-main">
                <el-avatar :size="48" class="seller-avatar">
                  {{ product.sellerName?.[0] }}
                </el-avatar>
                <div class="seller-info">
                  <span class="seller-name">{{ product.sellerName }}</span>
                  <span class="seller-label">信用良好 · 已认证</span>
                </div>
              </div>
              <div class="seller-actions">
                <el-button
                  v-if="!isOwner"
                  class="btn-contact"
                  round
                  @click="showContactDialog = true"
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

      <!-- 卖家其他在售 -->
      <section v-if="sellerProducts.length > 0" class="seller-products">
        <div class="section-head">
          <div class="section-head-left">
            <el-icon :size="20"><Shop /></el-icon>
            <h2>{{ product.sellerName }} 的其他在售</h2>
          </div>
          <span class="section-count">{{ sellerProducts.length }} 件</span>
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

      <!-- 联系卖家弹窗 -->
      <el-dialog v-model="showContactDialog" width="420px" :close-on-click-modal="false">
        <template #header>
          <div class="dialog-header">
            <el-icon :size="22" color="#10B981"><ChatDotRound /></el-icon>
            <span>联系卖家</span>
          </div>
        </template>
        <div class="contact-content">
          <div class="contact-seller-info">
            <el-avatar :size="56">{{ product.sellerName?.[0] }}</el-avatar>
            <div class="contact-seller-text">
              <span class="contact-seller-name">{{ product.sellerName }}</span>
              <span class="contact-seller-label">卖家</span>
            </div>
          </div>
          <el-divider />
          <el-form label-width="80px">
            <el-form-item label="商品">
              <span class="contact-product-name">{{ product.title }}</span>
            </el-form-item>
            <el-form-item label="留言">
              <el-input
                v-model="contactMessage"
                type="textarea"
                :rows="4"
                placeholder="例如：请问这个商品还在吗？能否再便宜一些？"
              />
            </el-form-item>
          </el-form>
          <div class="contact-hint">
            <el-icon><InfoFilled /></el-icon>
            下单后可以在订单详情中查看买卖双方的联系方式
          </div>
        </div>
        <template #footer>
          <el-button @click="showContactDialog = false">关闭</el-button>
          <el-button type="primary" @click="goBuyFromContact">
            <el-icon><Goods /></el-icon>直接购买
          </el-button>
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
        <div class="comments-header">
          <h2>商品评价</h2>
          <span class="comments-count" v-if="comments.length">{{ comments.length }} 条评价</span>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Clock, ChatLineSquare, Goods, Picture, View, ArrowLeft, ArrowRight, Document, Share, ChatDotRound, InfoFilled, Shop, WarningFilled } from '@element-plus/icons-vue'
import { getProductDetail, getProductList } from '@/api/product'
import { createOrder } from '@/api/order'
import { toggleFavorite } from '@/api/favorite'
import { getCommentList, addComment } from '@/api/comment'
import { submitComplaint } from '@/api/complaint'
import { useUserStore } from '@/stores/user'
import ProductCard from '@/components/ProductCard.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const product = ref(null)
const comments = ref([])
const loading = ref(false)
const buying = ref(false)
const buyDialogVisible = ref(false)
const buyFormRef = ref(null)
const galleryIndex = ref(0)
const showContactDialog = ref(false)
const contactMessage = ref('')
const sellerProducts = ref([])
const showComplaintDialog = ref(false)
const complaining = ref(false)
const complaintForm = reactive({ targetUserId: null, reason: '', description: '' })

const commentForm = reactive({ content: '', rating: 5 })
const buyForm = reactive({ buyerName: '', buyerPhone: '', buyerAddress: '', remark: '' })
const buyRules = {
  buyerName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  buyerPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  buyerAddress: [{ required: true, message: '请输入收货地址', trigger: 'blur' }]
}

const isOwner = computed(() => product.value?.userId === userStore.userInfo?.userId)

const statusText = computed(() => {
  const map = { ON_SALE: '在售', SOLD: '已售', OFF_SHELF: '已下架' }
  return map[product.value?.status] || ''
})

function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await getProductDetail(route.params.id)
    product.value = res.data
    const commentRes = await getCommentList(route.params.id, { pageNum: 1, pageSize: 20 })
    comments.value = commentRes.data.records
    if (product.value.userId) {
      loadSellerProducts(product.value.userId)
    }
  } finally {
    loading.value = false
  }
}

async function loadSellerProducts(sellerId) {
  try {
    const res = await getProductList({ sellerId, status: 'ON_SALE', pageSize: 7 })
    sellerProducts.value = (res.data.records || []).filter(p => p.id !== Number(route.params.id)).slice(0, 6)
  } catch { sellerProducts.value = [] }
}

function handleBuy() {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  Object.assign(buyForm, { buyerName: '', buyerPhone: '', buyerAddress: '', remark: '' })
  buyDialogVisible.value = true
}

function goBuyFromContact() {
  showContactDialog.value = false
  handleBuy()
}

async function confirmBuy() {
  await buyFormRef.value.validate()
  buying.value = true
  try {
    const remark = contactMessage.value ? `${buyForm.remark}\n买家留言：${contactMessage.value}` : buyForm.remark
    await createOrder({ ...buyForm, remark, productId: product.value.id })
    buyDialogVisible.value = false
    contactMessage.value = ''
    ElMessage.success('下单成功，请在30分钟内完成付款')
    router.push('/orders')
  } finally { buying.value = false }
}

async function handleFavorite() {
  await toggleFavorite(product.value.id)
  product.value.favorited = !product.value.favorited
  ElMessage.success(product.value.favorited ? '收藏成功' : '已取消收藏')
}

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

function ratingText(r) {
  const map = { 1: '很差', 2: '较差', 3: '一般', 4: '不错', 5: '很棒' }
  return map[r] || ''
}

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

function prevImage() {
  if (galleryIndex.value > 0) galleryIndex.value--
}
function nextImage() {
  if (product.value && galleryIndex.value < product.value.images.length - 1) galleryIndex.value++
}

function openComplaint(userId) {
  complaintForm.targetUserId = userId
  complaintForm.reason = ''
  complaintForm.description = ''
  showComplaintDialog.value = true
}

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

onMounted(loadDetail)
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

/* 价格 */
.price-box {
  padding: 18px 20px;
  background: linear-gradient(135deg, #FFFBEB, #FFF7ED);
  border-radius: 14px;
  border: 1px solid #FEF3C7;
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

/* 卖家 */
.seller-card {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px;
  background: linear-gradient(135deg, #ECFDF5, #D1FAE5);
  border-radius: 14px;
  border: 1px solid #A7F3D0;
  gap: 16px;
}
.seller-main {
  display: flex; align-items: center; gap: 14px;
  cursor: pointer;
}
.seller-avatar { flex-shrink: 0; }
.seller-info { display: flex; flex-direction: column; gap: 3px; }
.seller-name { font-size: 15px; font-weight: 600; color: #1F2937; }
.seller-label { font-size: 12px; color: #059669; }
.seller-actions { flex-shrink: 0; }
.btn-contact {
  border-color: #059669; color: #059669;
  font-weight: 600;
}
.btn-contact:hover { background: #059669; color: #fff; }
.btn-complaint {
  border-color: #FCA5A5; color: #DC2626;
  font-weight: 500;
}
.btn-complaint:hover { background: #FEF2F2; border-color: #EF4444; }
.btn-comment-complaint {
  margin-left: auto; opacity: 0;
  transition: opacity 0.2s;
}
.comment-card:hover .btn-comment-complaint { opacity: 1; }

/* 操作 */
.actions { display: flex; gap: 12px; }
.btn-buy { flex: 1; height: 48px; font-size: 16px; font-weight: 700; }
.btn-fav { height: 48px; }

/* ====== 卖家其他商品 ====== */
.seller-products {
  margin-top: 40px;
  padding-top: 32px;
  border-top: 2px solid #F3F4F6;
}
.section-head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 20px;
}
.section-head-left {
  display: flex; align-items: center; gap: 10px;
}
.section-head-left h2 {
  font-size: 18px; font-weight: 700; color: #1F2937; margin: 0;
}
.section-head-left .el-icon { color: #10B981; }
.section-count {
  font-size: 13px; color: #9CA3AF;
  background: #F3F4F6; padding: 4px 12px; border-radius: 20px;
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
.buy-notice strong { color: #D97706; }

/* ====== 联系卖家弹窗 ====== */
.contact-content {
  display: flex; flex-direction: column; gap: 16px;
}
.contact-seller-info {
  display: flex; align-items: center; gap: 14px;
}
.contact-seller-text {
  display: flex; flex-direction: column; gap: 2px;
}
.contact-seller-name { font-size: 16px; font-weight: 700; color: #1F2937; }
.contact-seller-label { font-size: 13px; color: #9CA3AF; }
.contact-product-name { font-size: 14px; color: #374151; font-weight: 500; }
.contact-hint {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 14px;
  background: #F0FDF4; border-radius: 10px;
  font-size: 13px; color: #059669;
}
.contact-hint .el-icon { flex-shrink: 0; }

/* ====== 评论区 ====== */
.comments-section {
  margin-top: 48px; padding-top: 32px;
  border-top: 2px solid #F3F4F6;
}
.comments-header {
  display: flex; align-items: baseline; gap: 12px;
  margin-bottom: 24px;
}
.comments-header h2 {
  font-size: 20px; font-weight: 700; color: #1F2937; margin: 0;
}
.comments-count { font-size: 14px; color: #9CA3AF; }

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
}
/* 手机端 */
@media (max-width: 480px) {
  .gallery-main { aspect-ratio: 3/2; }
  .no-image { height: 200px; }
  .seller-card { padding: 12px 14px; }
  .seller-actions { width: 100%; display: flex; flex-direction: column; gap: 8px; }
  .seller-actions .el-button { width: 100%; margin: 0; }
  .actions { flex-direction: column; }
  .btn-fav { width: 100%; }
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
