<template>
  <div class="seller-home page-container" v-loading="loading">
    <!-- 摊位招牌：卖家身份与经营数据 -->
    <section class="storefront">
      <div class="storefront-inner">
        <div class="stamp">
          <el-avatar :size="72" :src="sellerAvatar || undefined" class="stamp-avatar">{{ sellerName?.[0] || '?' }}</el-avatar>
        </div>
        <div class="store-id">
          <h1 class="store-name">{{ sellerName || '未知卖家' }}</h1>
          <span class="store-tag">
            <el-icon :size="13"><Medal /></el-icon>
            校园认证卖家 · 摊位号 {{ stallNo }}
          </span>
        </div>
        <div class="store-stats">
          <div class="stat">
            <span class="stat-num">{{ onSaleTotal }}</span>
            <span class="stat-label">在售</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat">
            <span class="stat-num stat-sold">{{ soldTotal }}</span>
            <span class="stat-label">已售</span>
          </div>
          <el-button class="btn-contact" round @click="goChat">
            <el-icon><ChatDotRound /></el-icon>联系卖家
          </el-button>
        </div>
      </div>
      <!-- 遮阳篷垂边：摊位招牌的记忆点 -->
      <div class="awning-edge"></div>
    </section>

    <!-- 货架切换：在售 / 已售 -->
    <div class="shelf-bar">
      <button class="shelf-tab" :class="{ active: tab === 'ON_SALE' }" @click="switchTab('ON_SALE')">
        在售<span class="tab-count">{{ onSaleTotal }}</span>
      </button>
      <button class="shelf-tab" :class="{ active: tab === 'SOLD' }" @click="switchTab('SOLD')">
        已售<span class="tab-count">{{ soldTotal }}</span>
      </button>
    </div>

    <!-- 商品货架 -->
    <div v-if="products.length" class="product-grid">
      <ProductCard v-for="p in products" :key="p.id" :product="p" />
    </div>
    <el-empty v-else-if="!loading" :description="tab === 'ON_SALE' ? '该摊位暂无在售商品' : '暂无已售记录'" />

    <div class="pager" v-if="total > pageSize">
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        @current-change="handlePage"
      />
    </div>
  </div>
</template>

<script setup>
// 卖家商品主页：摊位招牌 + 在售/已售货架 + 联系卖家入口。
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Medal } from '@element-plus/icons-vue'
import { getProductList } from '@/api/product'
import { getSellerInfo } from '@/api/user'
import { useUserStore } from '@/stores/user'
import ProductCard from '@/components/ProductCard.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const products = ref([])      // 当前货架商品列表
const sellerName = ref('')    // 卖家昵称
const sellerAvatar = ref('')  // 卖家真实头像
const tab = ref('ON_SALE')    // 当前货架：ON_SALE-在售 SOLD-已售
const onSaleTotal = ref(0)    // 在售总数
const soldTotal = ref(0)      // 已售总数
const total = ref(0)          // 当前货架总数
const pageNum = ref(1)        // 当前页码
const pageSize = 12           // 每页数量
const loading = ref(false)    // 加载状态

// 摊位号：由卖家 ID 补零生成，作为招牌上的身份编号
const stallNo = computed(() => String(route.params.id || 0).padStart(4, '0'))

/**
 * 拉取经营统计（在售/已售总数）与卖家公开信息（昵称/头像）
 */
async function loadStats() {
  // 卖家公开信息独立请求，失败不阻塞货架展示
  try {
    const info = await getSellerInfo(route.params.id)
    sellerName.value = info.data.nickname || ''
    sellerAvatar.value = info.data.avatar || ''
  } catch { sellerName.value = ''; sellerAvatar.value = '' }
  try {
    const [onSale, sold] = await Promise.all([
      getProductList({ sellerId: route.params.id, status: 'ON_SALE', pageNum: 1, pageSize: 1 }),
      getProductList({ sellerId: route.params.id, status: 'SOLD', pageNum: 1, pageSize: 1 })
    ])
    onSaleTotal.value = onSale.data.total || 0
    soldTotal.value = sold.data.total || 0
  } catch { /* 统计失败不阻塞货架展示 */ }
}

/**
 * 加载当前货架的商品列表
 */
async function load() {
  loading.value = true
  try {
    const res = await getProductList({
      sellerId: route.params.id,
      status: tab.value,
      pageNum: pageNum.value,
      pageSize
    })
    products.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

/** 切换在售/已售货架 */
function switchTab(next) {
  if (tab.value === next) return
  tab.value = next
  pageNum.value = 1
  load()
}

/** 翻页 */
function handlePage(page) {
  pageNum.value = page
  load()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

/** 联系卖家：未登录提示并跳转登录，已登录进入聊天 */
function goChat() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再联系卖家')
    router.push('/login')
    return
  }
  router.push({ path: `/chat/${route.params.id}` })
}

onMounted(() => {
  loadStats()
  load()
})

// 卖家切换时重置货架与统计
watch(() => route.params.id, (id, oldId) => {
  if (id && id !== oldId) {
    pageNum.value = 1
    tab.value = 'ON_SALE'
    products.value = []
    sellerName.value = ''
    sellerAvatar.value = ''
    onSaleTotal.value = 0
    soldTotal.value = 0
    total.value = 0
    loadStats()
    load()
  }
})
</script>

<style scoped>
/* ====== 摊位招牌 ====== */
.storefront {
  position: relative;
  margin-top: 20px;
  padding: 28px 32px 34px;
  background: #059669;
  border-radius: 20px 20px 0 0;
  color: #fff;
  animation: stall-in 0.45s cubic-bezier(0.22, 1, 0.36, 1);
}
/* 招牌布纹与顶部高光，底部渐隐以保证垂边颜色一致 */
.storefront::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background:
    radial-gradient(120% 90% at 85% 0%, rgba(255, 255, 255, 0.18), transparent 60%),
    repeating-linear-gradient(-45deg, rgba(255, 255, 255, 0.07) 0 14px, transparent 14px 28px);
  -webkit-mask-image: linear-gradient(#000 55%, transparent);
  mask-image: linear-gradient(#000 55%, transparent);
  pointer-events: none;
}
/* 遮阳篷垂边使用全局 .awning-edge 工具类 */

@keyframes stall-in {
  from { opacity: 0; transform: translateY(-8px); }
}
@media (prefers-reduced-motion: reduce) {
  .storefront { animation: none; }
}

.storefront-inner {
  position: relative;
  display: flex;
  align-items: center;
  gap: 20px;
}

/* 摊位印章头像 */
.stamp {
  flex-shrink: 0;
  padding: 5px;
  border: 2px dashed rgba(255, 255, 255, 0.65);
  border-radius: 50%;
}
.stamp-avatar {
  background: #fff;
  color: #059669;
  font-size: 28px;
  font-weight: 800;
}

.store-name {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 0.5px;
  line-height: 1.2;
}
.store-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  color: #D1FAE5;
  font-size: 12px;
  letter-spacing: 1px;
}

/* 经营数据与联系入口 */
.store-stats {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 18px;
}
.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.stat-num {
  font-size: 24px;
  font-weight: 800;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}
.stat-sold { color: #FCD34D; }
.stat-label {
  margin-top: 2px;
  font-size: 12px;
  letter-spacing: 2px;
  color: rgba(255, 255, 255, 0.75);
}
.stat-divider {
  width: 1px;
  height: 28px;
  background: rgba(255, 255, 255, 0.25);
}
.btn-contact {
  margin-left: 6px;
  background: #fff;
  color: #059669;
  font-weight: 700;
  border: none;
}
.btn-contact:hover { background: #ECFDF5; }

/* ====== 货架切换 ====== */
.shelf-bar {
  display: flex;
  gap: 8px;
  margin: 30px 0 18px;
}
.shelf-tab {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  border: none;
  border-radius: 999px;
  background: #fff;
  color: #6B7280;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: background 0.2s ease, color 0.2s ease;
}
.shelf-tab:hover { color: #059669; }
.shelf-tab.active { background: #059669; color: #fff; }
.tab-count {
  padding: 1px 8px;
  border-radius: 999px;
  background: rgba(5, 150, 105, 0.1);
  color: #059669;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}
.shelf-tab.active .tab-count {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

/* ====== 商品货架 ====== */
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.pager {
  display: flex;
  justify-content: center;
  margin: 32px 0 16px;
}

/* ====== 响应式 ====== */
@media (max-width: 768px) {
  .storefront { padding: 22px 20px 28px; }
  .store-name { font-size: 22px; }
}
@media (max-width: 480px) {
  .storefront { padding: 18px 16px 26px; }
  .storefront-inner { flex-wrap: wrap; gap: 14px; }
  .stamp-avatar { font-size: 22px; }
  .store-name { font-size: 20px; margin-bottom: 6px; }
  .store-stats {
    margin-left: 0;
    width: 100%;
    justify-content: space-between;
    gap: 12px;
  }
  .btn-contact { margin-left: 0; }
  .product-grid { grid-template-columns: repeat(2, 1fr); gap: 10px; }
}
</style>
