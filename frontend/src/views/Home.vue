<template>
  <div class="home">
    <!-- 待办提醒条：订单状态与未读消息标红提醒，处理后自动消失 -->
    <section v-if="showReminders" class="reminder-bar">
      <span class="reminder-title">
        <el-icon><Bell /></el-icon>待办提醒
      </span>
      <div class="reminder-chips">
        <div v-if="orderCounts.BUYER_PENDING" class="reminder-chip" @click="router.push({ path: '/orders', query: { tab: 'PENDING' } })">
          待付款 <span class="chip-badge">{{ orderCounts.BUYER_PENDING }}</span>
        </div>
        <div v-if="orderCounts.SELLER_PAID" class="reminder-chip" @click="router.push({ path: '/orders', query: { tab: 'PAID' } })">
          待发货 <span class="chip-badge">{{ orderCounts.SELLER_PAID }}</span>
        </div>
        <div v-if="orderCounts.BUYER_SHIPPED" class="reminder-chip" @click="router.push({ path: '/orders', query: { tab: 'SHIPPED' } })">
          待收货 <span class="chip-badge">{{ orderCounts.BUYER_SHIPPED }}</span>
        </div>
        <div v-if="orderCounts.SELLER_REFUND" class="reminder-chip" @click="router.push({ path: '/orders', query: { tab: 'REFUND' } })">
          退款待处理 <span class="chip-badge">{{ orderCounts.SELLER_REFUND }}</span>
        </div>
        <div v-if="unreadCount" class="reminder-chip" @click="router.push('/notifications')">
          未读消息 <span class="chip-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </div>
      </div>
    </section>

    <!-- 问候条 -->
    <div class="hero-topbar">
      <span class="greeting">{{ greeting }}</span>
      <span class="topbar-stats">{{ total }} 件在售 · {{ categories.length }} 个分类</span>
    </div>

    <!-- 三栏主视觉：左分类侧栏 + 中轮播图 + 右快捷入口（借鉴淘宝首页布局） -->
    <section class="home-hero">
      <!-- 左：分类侧栏，悬停弹出子类 -->
      <aside class="cat-sidebar">
        <div class="cat-side-head">分类</div>
        <div class="cat-side-list">
          <el-popover
            v-for="cat in categories"
            :key="cat.id"
            trigger="hover"
            placement="right-start"
            :width="300"
            :offset="6"
            :disabled="!(cat.children?.length)"
            popper-class="cat-sub-popover"
          >
            <template #reference>
              <div
                class="cat-side-item"
                @click="router.push({ path: '/products', query: { parentCategoryId: cat.id } })"
              >
                <span class="side-emoji">{{ getCatEmoji(cat.name) }}</span>
                <span class="side-name">{{ cat.name }}</span>
                <el-icon :size="12" class="side-arrow"><ArrowRight /></el-icon>
              </div>
            </template>
            <div class="sub-head">{{ cat.name }} · 子类</div>
            <div class="sub-items">
              <span
                v-for="sub in cat.children"
                :key="sub.id"
                class="sub-item"
                @click="router.push({ path: '/products', query: { categoryId: sub.id } })"
              >{{ sub.name }}</span>
            </div>
          </el-popover>
        </div>
      </aside>

      <!-- 中：轮播图 -->
      <div class="hero-carousel">
        <el-carousel height="380px" :interval="4500" arrow="hover">
          <el-carousel-item v-for="(s, i) in slides" :key="i">
            <div
              class="slide" :class="s.theme"
              :style="{ backgroundImage: `linear-gradient(90deg, rgba(15,23,42,0.42), rgba(15,23,42,0.16) 52%, rgba(15,23,42,0) 78%), url(${s.bg})` }"
              @click="goSlide(s)"
            >
              <span class="slide-tag">{{ s.tag }}</span>
              <h2 class="slide-title">{{ s.title }}</h2>
              <p class="slide-sub">{{ s.sub }}</p>
              <span class="slide-btn">{{ s.btn }}</span>
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>

      <!-- 右：快捷入口卡片 -->
      <aside class="hero-quick">
        <div class="quick-card quick-green" @click="router.push('/publish')">
          <span class="quick-title">发布闲置</span>
          <span class="quick-sub">一键快速出手</span>
        </div>
        <div class="quick-card quick-blue" @click="router.push('/orders')">
          <span class="quick-title">我的订单</span>
          <span class="quick-sub">{{ todoTotal ? `${todoTotal} 笔待处理` : '查看交易进度' }}</span>
          <span v-if="todoTotal" class="quick-badge">{{ todoTotal > 99 ? '99+' : todoTotal }}</span>
        </div>
        <div class="quick-card quick-amber" @click="router.push('/cart')">
          <span class="quick-title">购物车</span>
          <span class="quick-sub">{{ cartCount ? `${cartCount} 件待结算` : '去挑点好物' }}</span>
          <span v-if="cartCount" class="quick-badge">{{ cartCount > 99 ? '99+' : cartCount }}</span>
        </div>
        <div class="quick-card quick-rose" @click="router.push('/notifications')">
          <span class="quick-title">消息通知</span>
          <span class="quick-sub">{{ unreadCount ? `${unreadCount} 条未读` : '暂无新消息' }}</span>
          <span v-if="unreadCount" class="quick-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </div>
      </aside>
    </section>

    <!-- 最新上架商品 -->
    <section class="feed-section">
      <div class="feed-header">
        <h2 class="section-label">
          <span class="label-line"></span>
          <span>最新上架</span>
          <span class="label-line"></span>
        </h2>
        <el-button text @click="router.push('/products')" class="view-all">
          全部 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
      <div class="feed-grid" v-if="!loading">
        <div v-for="(item, i) in products" :key="item.id" class="feed-card" :style="{ animationDelay: `${i * 0.06}s` }">
          <ProductCard :product="item" />
        </div>
      </div>
      <div class="feed-grid" v-else>
        <SkeletonCard v-for="n in 10" :key="n" />
      </div>
      <el-empty v-if="!loading && products.length === 0" description="还没有商品，去发布第一个吧" />

      <div class="feed-pager" v-if="total > 10">
        <el-pagination
          v-model:current-page="feedPage"
          :page-size="10"
          :total="total"
          layout="prev, pager, next"
          background
          small
          @change="loadProducts"
        />
      </div>
    </section>

    <!-- 浮动发布按钮（移动端可见） -->
    <div class="fab-publish" @click="router.push('/publish')">
      <el-icon :size="24"><Plus /></el-icon>
    </div>
  </div>
</template>

<script setup>
// 首页：展示轮播提醒、分类导航、推荐商品和登录后快捷入口。
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getProductList } from '@/api/product'
import { getCategoryTree } from '@/api/category'
import { getOrderStatusCounts } from '@/api/order'
import { getUnreadCount } from '@/api/notification'
import { getCartList } from '@/api/cart'
import { useUserStore } from '@/stores/user'
import ProductCard from '@/components/ProductCard.vue'
import SkeletonCard from '@/components/SkeletonCard.vue'

const router = useRouter()
const userStore = useUserStore()
const products = ref([])
const categories = ref([])
const loading = ref(false)
const total = ref(0)
const feedPage = ref(1)

// 待办提醒：按“谁该行动”拆分的订单计数 + 未读消息 + 购物车数量，处理完成后计数归零自动隐藏
const orderCounts = ref({})
const unreadCount = ref(0)
const cartCount = ref(0)
// 我的待办总数：买家待付款/待收货 + 卖家待发货/退款待处理
const todoTotal = computed(() => {
  const c = orderCounts.value
  return (c.BUYER_PENDING || 0) + (c.BUYER_SHIPPED || 0) + (c.SELLER_PAID || 0) + (c.SELLER_REFUND || 0)
})
const showReminders = computed(() => {
  if (!userStore.isLoggedIn) return false
  return todoTotal.value + unreadCount.value > 0
})

/** 根据登录状态加载待付款、待发货等首页提醒。 */
async function loadReminders() {
  if (!userStore.isLoggedIn) return
  try {
    const [ordersRes, unreadRes, cartRes] = await Promise.all([
      getOrderStatusCounts(),
      getUnreadCount(),
      getCartList()
    ])
    orderCounts.value = ordersRes.data || {}
    unreadCount.value = unreadRes.data.count || 0
    cartCount.value = (cartRes.data || []).length
  } catch {}
}

const catEmoji = {
  '数码电子': '📱', '图书教材': '📚', '生活用品': '🏠',
  '服饰鞋包': '👔', '运动户外': '⚽', '音乐器材': '🎸',
  '其他闲置': '🎁'
}

/** 返回分类对应的展示图标。 */
function getCatEmoji(name) {
  return catEmoji[name] || '📌'
}

// 轮播图数据：首屏主题 + 前三个一级分类专区 + 发布入口
const slideThemes = [
  { theme: 'slide-purple' },
  { theme: 'slide-blue' },
  { theme: 'slide-rose' }
]
/** 分类轮播插画映射：3D 黏土风背景图 */
/** 返回分类卡片的品牌配色。 */
function catBg(name) {
  if (/数码|电子|手机|电脑/.test(name)) return '/banners/digital.png'
  if (/图书|教材|书/.test(name)) return '/banners/books.png'
  return '/banners/life.png'
}
const slides = computed(() => {
  const list = [{
    tag: '校园二手',
    title: '发现校园好物',
    sub: '让闲置在校园里流动起来',
    btn: '去逛逛',
    theme: 'slide-green',
    bg: '/banners/brand.png',
    to: '/products'
  }]
  categories.value.slice(0, 3).forEach((cat, i) => {
    list.push({
      tag: cat.name,
      title: `${cat.name}专区`,
      sub: `${cat.children?.length || 0} 个子类 · 好物低价别错过`,
      btn: '去逛逛',
      theme: slideThemes[i % 3].theme,
      bg: catBg(cat.name),
      to: { path: '/products', query: { parentCategoryId: cat.id } }
    })
  })
  list.push({
    tag: '闲置变现',
    title: '一键发布闲置',
    sub: '快速出手，让好物找到新主人',
    btn: '去发布',
    theme: 'slide-amber',
    bg: '/banners/publish.png',
    to: '/publish'
  })
  return list
})

/** 跳转到轮播提醒对应页面。 */
function goSlide(slide) {
  router.push(slide.to)
}

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 9) return '早上好 ☀️'
  if (h < 13) return '上午好 🌿'
  if (h < 18) return '下午好 🌤️'
  return '晚上好 🌙'
})

/** 加载首页推荐商品。 */
async function loadProducts() {
  loading.value = true
  try {
    const res = await getProductList({ pageNum: feedPage.value, pageSize: 10 })
    products.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

onMounted(async () => {
  try {
    // 使用分类树接口，一级分类携带 children，侧栏悬停才能展示子类
    categories.value = (await getCategoryTree()).data
  } finally { /* ignore */ }
  loadProducts()
  loadReminders()
})
</script>

<style scoped>
/* ====== 待办提醒条（标红提醒，处理后自动隐藏） ====== */
.reminder-bar {
  display: flex; align-items: center; gap: 14px;
  margin-bottom: 14px;
  padding: 10px 16px;
  background: #FEF2F2;
  border: 1px solid #FECACA;
  border-radius: 12px;
}
.reminder-title {
  display: flex; align-items: center; gap: 5px;
  font-size: 13px; font-weight: 700; color: #DC2626;
  white-space: nowrap;
}
.reminder-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.reminder-chip {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 12px;
  background: #fff; border: 1px solid #FECACA;
  border-radius: 999px;
  font-size: 13px; color: #374151;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s;
}
.reminder-chip:hover { border-color: #EF4444; color: #DC2626; }
.chip-badge {
  min-width: 18px; height: 18px; line-height: 18px;
  border-radius: 9px;
  background: #EF4444; color: #fff;
  font-size: 11px; font-weight: 700;
  text-align: center; padding: 0 5px;
}

/* ====== 问候条 ====== */
.hero-topbar {
  position: relative;
  display: flex; justify-content: flex-end; align-items: center;
  margin-bottom: 12px;
}
.greeting {
  position: absolute; left: 50%; transform: translateX(-50%);
  font-size: 15px; font-weight: 700; color: #1F2937;
}
.topbar-stats { font-size: 13px; color: #9CA3AF; }

/* ====== 三栏主视觉 ====== */
.home-hero {
  display: grid;
  grid-template-columns: 210px 1fr 220px;
  gap: 14px;
  margin-bottom: 32px;
}

/* ---- 左：分类侧栏 ---- */
.cat-sidebar {
  background: #fff;
  border-radius: 14px;
  border: 1px solid #F3F4F6;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  display: flex; flex-direction: column;
  overflow: hidden;
}
.cat-side-head {
  padding: 12px 16px 8px;
  font-size: 14px; font-weight: 800; color: #1F2937;
  border-bottom: 1px solid #F3F4F6;
}
.cat-side-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px 0;
  scrollbar-width: thin;
}
.cat-side-item {
  display: flex; align-items: center; gap: 8px;
  padding: 9px 16px;
  cursor: pointer;
  transition: background 0.15s;
}
.cat-side-item:hover { background: #F0FDF4; }
.cat-side-item:hover .side-name { color: #059669; }
.cat-side-item:hover .side-arrow { opacity: 1; color: #10B981; }
.side-emoji { font-size: 15px; }
.side-name { flex: 1; font-size: 13px; color: #374151; font-weight: 500; }
.side-arrow { opacity: 0.35; color: #9CA3AF; transition: all 0.15s; }

/* ---- 中：轮播图 ---- */
.hero-carousel { min-width: 0; border-radius: 14px; overflow: hidden; }
.hero-carousel :deep(.el-carousel) { border-radius: 14px; }
.hero-carousel :deep(.el-carousel__indicator .el-carousel__button) {
  background: #fff; opacity: 0.5;
}
.hero-carousel :deep(.el-carousel__indicator.is-active .el-carousel__button) {
  opacity: 1; background: #10B981;
}
.slide {
  position: relative;
  height: 100%;
  padding: 40px 44px;
  color: #fff;
  cursor: pointer;
  overflow: hidden;
  background-size: cover;
  background-position: center right;
}
/* 主题色作为插画加载前的兜底背景色 */
.slide-green  { background-color: #10B981; }
.slide-purple { background-color: #8B5CF6; }
.slide-blue   { background-color: #3B82F6; }
.slide-amber  { background-color: #F59E0B; }
.slide-rose   { background-color: #F43F5E; }
.slide::before {
  content: '';
  position: absolute; inset: 0;
  background: radial-gradient(circle at 85% 20%, rgba(255,255,255,0.14) 0%, transparent 55%);
}
.slide-tag {
  display: inline-block;
  padding: 3px 10px;
  background: rgba(255,255,255,0.2);
  border: 1px solid rgba(255,255,255,0.35);
  border-radius: 999px;
  font-size: 12px; font-weight: 600;
  backdrop-filter: blur(4px);
}
.slide-title {
  margin: 14px 0 6px;
  font-size: 30px; font-weight: 800; letter-spacing: -0.5px;
  text-shadow: 0 2px 12px rgba(15,23,42,0.35);
}
.slide-sub { font-size: 14px; opacity: 0.92; margin: 0 0 20px; text-shadow: 0 1px 8px rgba(15,23,42,0.35); }
.slide-btn {
  display: inline-block;
  padding: 7px 20px;
  background: #fff;
  color: #059669;
  border-radius: 999px;
  font-size: 13px; font-weight: 700;
  transition: transform 0.2s;
}
.slide:hover .slide-btn { transform: scale(1.05); }

/* ---- 右：快捷入口 ---- */
.hero-quick {
  display: flex; flex-direction: column; gap: 12px;
}
.quick-card {
  position: relative;
  flex: 1;
  border-radius: 14px;
  padding: 14px 16px;
  color: #fff;
  cursor: pointer;
  display: flex; flex-direction: column; justify-content: center; gap: 4px;
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}
.quick-card:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(0,0,0,0.12); }
.quick-green { background: linear-gradient(135deg, #059669, #34D399); }
.quick-blue  { background: linear-gradient(135deg, #2563EB, #60A5FA); }
.quick-amber { background: linear-gradient(135deg, #D97706, #FBBF24); }
.quick-rose  { background: linear-gradient(135deg, #E11D48, #FB7185); }
.quick-title { font-size: 15px; font-weight: 800; }
.quick-sub { font-size: 12px; opacity: 0.85; }
.quick-badge {
  position: absolute; top: 10px; right: 12px;
  min-width: 20px; height: 20px; line-height: 20px;
  border-radius: 10px;
  background: #EF4444;
  border: 2px solid #fff;
  color: #fff;
  font-size: 11px; font-weight: 700;
  text-align: center; padding: 0 5px;
}

/* ====== 分区标签 ====== */
.section-label {
  display: flex; align-items: center; gap: 12px;
  font-size: 18px; font-weight: 700; color: #1F2937; margin-bottom: 20px;
}
.label-line { flex: 1; height: 1px; max-width: 40px; background: #D1FAE5; }

/* ====== 商品网格 + 分页 ====== */
.feed-section { margin-bottom: 24px; }
.feed-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20px;
}
.feed-header .section-label { margin-bottom: 0; }
.view-all {
  color: #10B981; font-weight: 600; display: flex; align-items: center; gap: 4px;
  white-space: nowrap;
}
.feed-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 14px;
  align-items: stretch;
}
.feed-card { min-width: 0; animation: fadeInUp 0.5s ease both; }
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to   { opacity: 1; transform: translateY(0); }
}
.feed-pager { display: flex; justify-content: center; margin-top: 16px; }

@media (max-width: 1024px) { .feed-grid { grid-template-columns: repeat(4, 1fr); } }
@media (max-width: 768px)  { .feed-grid { grid-template-columns: repeat(3, 1fr); gap: 10px; } }
@media (max-width: 480px)  { .feed-grid { grid-template-columns: repeat(2, 1fr); } }

/* ====== FAB ====== */
.fab-publish {
  display: none;
  position: fixed; bottom: 24px; right: 24px; z-index: 200;
  width: 56px; height: 56px;
  background: #10B981; color: #fff;
  border-radius: 50%;
  align-items: center; justify-content: center;
  cursor: pointer;
  box-shadow: 0 6px 24px rgba(16,185,129,0.45);
}

/* ====== 响应式 ====== */
@media (max-width: 1024px) {
  .home-hero { grid-template-columns: 190px 1fr; }
  .hero-quick { display: none; }
}
@media (max-width: 768px) {
  .home-hero { grid-template-columns: 1fr; }
  /* 手机端分类侧栏改为横向滑动胶囊条，保证分类可见 */
  .cat-sidebar { border-radius: 12px; }
  .cat-side-head { display: none; }
  .cat-side-list {
    display: flex; overflow-x: auto; gap: 8px;
    padding: 10px 12px;
    scrollbar-width: none;
  }
  .cat-side-list::-webkit-scrollbar { display: none; }
  .cat-side-item {
    flex-shrink: 0;
    padding: 6px 14px;
    background: #F0FDF4;
    border-radius: 999px;
  }
  .cat-side-item .side-arrow { display: none; }
  .side-name { font-size: 12px; }
  .hero-carousel :deep(.el-carousel),
  .hero-carousel :deep(.el-carousel__container) { height: 220px !important; }
  .slide { padding: 24px; }
  .slide-title { font-size: 22px; }
  /* 手机端 FAB 由 BottomNav 提供，此处隐藏 */
  .fab-publish { display: none; }
}
</style>

<!-- 分类悬停下拉弹层样式：popover 传送到 body，需非 scoped 全局样式 -->
<style>
.cat-sub-popover { padding: 12px 14px !important; }
.cat-sub-popover .sub-head {
  font-size: 13px; font-weight: 700; color: #1F2937;
  margin-bottom: 8px;
}
.cat-sub-popover .sub-items {
  display: flex; flex-wrap: wrap; gap: 8px;
}
.cat-sub-popover .sub-item {
  padding: 4px 12px;
  background: #F0FDF4;
  border-radius: 999px;
  font-size: 12px; color: #059669;
  cursor: pointer;
  transition: background 0.15s;
}
.cat-sub-popover .sub-item:hover { background: #D1FAE5; }
</style>
