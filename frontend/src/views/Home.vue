<template>
  <div class="home">
    <!-- 微横幅：数据驱动 -->
    <section class="micro-banner">
      <div class="banner-bg"></div>
      <div class="banner-dots"></div>
      <div class="banner-inner">
        <div class="banner-left">
          <p class="banner-greeting">{{ greeting }}</p>
          <h1>发现校园好物</h1>
          <p class="banner-sub">让闲置在校园里流动起来</p>
        </div>
        <div class="banner-stats">
          <div class="stat-chip">
            <span class="stat-num">{{ products.length }}</span>
            <span class="stat-label">件在售</span>
          </div>
          <div class="stat-chip">
            <span class="stat-num">{{ categories.length }}</span>
            <span class="stat-label">个分类</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 分类卡片网格 -->
    <section class="category-section">
      <h2 class="section-label">
        <span class="label-line"></span>
        <span>逛逛分类</span>
        <span class="label-line"></span>
      </h2>
      <div class="category-grid">
        <div
          v-for="(cat, i) in categories"
          :key="cat.id"
          class="category-card"
          :class="getCardColor(i)"
          :style="{ animationDelay: `${i * 0.06}s` }"
          @click="router.push({ path: '/products', query: { categoryId: cat.id } })"
        >
          <div class="card-bg-glow"></div>
          <div class="card-emoji">{{ getCatEmoji(cat.name) }}</div>
          <span class="card-name">{{ cat.name }}</span>
          <span class="card-count">{{ cat.children?.length || 0 }} 个子类</span>
        </div>
        <!-- 发布入口卡片 -->
        <div
          class="category-card publish-entry"
          :style="{ animationDelay: `${categories.length * 0.06}s` }"
          @click="router.push('/publish')"
        >
          <div class="publish-ripple"></div>
          <div class="card-emoji publish-emoji">
            <el-icon :size="22"><Plus /></el-icon>
          </div>
          <span class="card-name">发布闲置</span>
          <span class="card-count">快速出手</span>
        </div>
      </div>
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
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getProductList } from '@/api/product'
import { getMainCategories } from '@/api/category'
import ProductCard from '@/components/ProductCard.vue'
import SkeletonCard from '@/components/SkeletonCard.vue'

const router = useRouter()
const products = ref([])
const categories = ref([])
const loading = ref(false)
const total = ref(0)
const feedPage = ref(1)

const catEmoji = {
  '数码电子': '📱', '图书教材': '📚', '生活用品': '🏠',
  '服饰鞋包': '👔', '运动户外': '⚽', '音乐器材': '🎸',
  '其他闲置': '🎁'
}

function getCatEmoji(name) {
  return catEmoji[name] || '📌'
}

const cardColors = ['card-teal','card-amber','card-blue','card-purple','card-rose','card-indigo','card-cyan']
function getCardColor(index) { return cardColors[index % cardColors.length] }

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 9) return '早上好 ☀️'
  if (h < 13) return '上午好 🌿'
  if (h < 18) return '下午好 🌤️'
  return '晚上好 🌙'
})

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
    categories.value = (await getMainCategories()).data
  } finally { /* ignore */ }
  loadProducts()
})
</script>

<style scoped>
/* ====== 微横幅 ====== */
.micro-banner {
  position: relative;
  margin-bottom: 32px;
  border-radius: 20px;
  overflow: hidden;
  background: linear-gradient(135deg, #059669 0%, #10B981 40%, #34D399 100%);
  padding: 32px 36px;
  color: #fff;
}
.banner-bg {
  position: absolute; inset: 0;
  background: radial-gradient(circle at 20% 80%, rgba(255,255,255,0.12) 0%, transparent 60%),
              radial-gradient(circle at 80% 20%, rgba(255,255,255,0.08) 0%, transparent 50%);
}
.banner-dots {
  position: absolute; inset: 0;
  background-image: radial-gradient(rgba(255,255,255,0.15) 1px, transparent 1px);
  background-size: 18px 18px;
}
.banner-inner {
  position: relative; z-index: 1;
  display: flex; justify-content: space-between; align-items: center;
  flex-wrap: wrap; gap: 20px;
}
.banner-greeting { font-size: 14px; opacity: 0.85; margin-bottom: 4px; }
.banner-left h1 { font-size: 32px; font-weight: 800; letter-spacing: -0.5px; margin-bottom: 4px; }
.banner-sub { font-size: 14px; opacity: 0.8; }
.banner-stats { display: flex; gap: 16px; }
.stat-chip {
  background: rgba(255,255,255,0.18); backdrop-filter: blur(8px);
  border-radius: 14px; padding: 14px 22px; text-align: center;
  border: 1px solid rgba(255,255,255,0.2);
}
.stat-num { display: block; font-size: 28px; font-weight: 800; line-height: 1.1; }
.stat-label { font-size: 12px; opacity: 0.75; }

/* ====== 分区标签 ====== */
.section-label {
  display: flex; align-items: center; gap: 12px;
  font-size: 18px; font-weight: 700; color: #1F2937; margin-bottom: 20px;
}
.label-line { flex: 1; height: 1px; max-width: 40px; background: #D1FAE5; }

/* ====== 分类卡片网格 ====== */
.category-section { margin-bottom: 40px; }
.category-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.category-card {
  position: relative;
  display: flex; flex-direction: column; align-items: center;
  padding: 14px 10px 12px;
  background: #fff;
  border-radius: 14px;
  cursor: pointer;
  border: 2px solid transparent;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04), 0 4px 8px rgba(0,0,0,0.02);
  overflow: hidden;
  transition: transform 0.3s cubic-bezier(.34,1.56,.64,1),
              border-color 0.3s ease,
              box-shadow 0.3s ease;
  animation: cardPopIn 0.5s ease both;
}
@keyframes cardPopIn {
  from { opacity: 0; transform: translateY(16px) scale(0.94); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

/* 底部光晕：悬停时从中心扩散 */
.card-bg-glow {
  position: absolute; bottom: 0; left: 50%; transform: translateX(-50%);
  width: 0; height: 4px;
  border-radius: 50%;
  opacity: 0;
  transition: width 0.35s ease, opacity 0.35s ease, box-shadow 0.35s ease;
}
.category-card:hover { transform: translateY(-4px); border-color: #D1FAE5; box-shadow: 0 12px 32px rgba(16,185,129,0.1); }

/* ---- 多色卡片主题 ---- */
.card-teal  .card-emoji { background: #ECFDF5; }
.card-teal:hover  .card-bg-glow { width: 80%; opacity: 1; box-shadow: 0 0 16px 4px rgba(16,185,129,0.25); background: rgba(16,185,129,0.6); }
.card-teal:hover  .card-emoji { background: #D1FAE5; }

.card-amber .card-emoji { background: #FFFBEB; }
.card-amber:hover .card-bg-glow { width: 80%; opacity: 1; box-shadow: 0 0 16px 4px rgba(245,158,11,0.25); background: rgba(245,158,11,0.6); }
.card-amber:hover .card-emoji { background: #FEF3C7; }

.card-blue  .card-emoji { background: #EFF6FF; }
.card-blue:hover  .card-bg-glow { width: 80%; opacity: 1; box-shadow: 0 0 16px 4px rgba(59,130,246,0.25); background: rgba(59,130,246,0.6); }
.card-blue:hover  .card-emoji { background: #DBEAFE; }

.card-purple .card-emoji { background: #F5F3FF; }
.card-purple:hover .card-bg-glow { width: 80%; opacity: 1; box-shadow: 0 0 16px 4px rgba(139,92,246,0.25); background: rgba(139,92,246,0.6); }
.card-purple:hover .card-emoji { background: #EDE9FE; }

.card-rose  .card-emoji { background: #FFF1F2; }
.card-rose:hover  .card-bg-glow { width: 80%; opacity: 1; box-shadow: 0 0 16px 4px rgba(244,63,94,0.25); background: rgba(244,63,94,0.6); }
.card-rose:hover  .card-emoji { background: #FECDD3; }

.card-indigo .card-emoji { background: #EEF2FF; }
.card-indigo:hover .card-bg-glow { width: 80%; opacity: 1; box-shadow: 0 0 16px 4px rgba(99,102,241,0.25); background: rgba(99,102,241,0.6); }
.card-indigo:hover .card-emoji { background: #E0E7FF; }

.card-cyan  .card-emoji { background: #ECFEFF; }
.card-cyan:hover  .card-bg-glow { width: 80%; opacity: 1; box-shadow: 0 0 16px 4px rgba(6,182,212,0.25); background: rgba(6,182,212,0.6); }
.card-cyan:hover  .card-emoji { background: #CFFAFE; }

.card-emoji {
  position: relative; z-index: 1;
  font-size: 28px;
  width: 48px; height: 48px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 14px;
  margin-bottom: 8px;
  transition: transform 0.3s cubic-bezier(.34,1.56,.64,1), background 0.25s;
}
.category-card:hover .card-emoji { transform: scale(1.12) rotate(-3deg); }

.card-name {
  position: relative; z-index: 1;
  font-size: 14px; font-weight: 600; color: #374151;
  margin-bottom: 4px;
  transition: color 0.2s;
}
.card-count {
  position: relative; z-index: 1;
  font-size: 11px; color: #9CA3AF;
  letter-spacing: 0.3px;
}

/* ---- 发布卡片（独立动效） ---- */
.publish-entry .card-emoji {
  background: linear-gradient(135deg, #10B981, #059669);
  animation: floatEmoji 3s ease-in-out infinite;
}
@keyframes floatEmoji {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}
.publish-entry:hover .card-emoji { animation: none; transform: scale(1.15); }
.publish-emoji { color: #fff; }

/* 发布卡片的波纹 */
.publish-ripple {
  position: absolute;
  top: 50%; left: 50%; transform: translate(-50%, -50%);
  width: 0; height: 0;
  border-radius: 50%;
  background: rgba(16,185,129,0.06);
  transition: width 0.6s ease-out, height 0.6s ease-out, opacity 0.6s ease-out;
  opacity: 0;
}
.publish-entry:hover .publish-ripple {
  width: 300px; height: 300px; opacity: 1;
}
.publish-entry .card-name { color: #059669; }

/* ---- 响应式 ---- */
@media (max-width: 768px) {
  .category-grid { grid-template-columns: repeat(4, 1fr); gap: 8px; }
  .category-card { padding: 10px 6px 10px; border-radius: 12px; }
  .card-emoji { font-size: 22px; width: 38px; height: 38px; border-radius: 10px; margin-bottom: 6px; }
  .card-name { font-size: 11px; }
  .card-count { font-size: 10px; }
}
@media (max-width: 480px) {
  .category-grid { grid-template-columns: repeat(4, 1fr); }
  .card-emoji { font-size: 18px; width: 34px; height: 34px; border-radius: 8px; }
  .card-name { font-size: 10px; }
}

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
.feed-card {
  min-width: 0; /* 防止内容溢出撑大 */
}
.feed-card {
  animation: fadeInUp 0.5s ease both;
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.feed-pager {
  display: flex; justify-content: center;
  margin-top: 16px;
}

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
  animation: fab-breathe 3s ease-in-out infinite;
}
@keyframes fab-breathe {
  0%, 100% { box-shadow: 0 6px 24px rgba(16,185,129,0.45); }
  50% { box-shadow: 0 6px 36px rgba(16,185,129,0.7); }
}

/* ====== 响应式 ====== */
@media (max-width: 1024px) {
  .category-grid { grid-template-columns: repeat(4, 1fr); }
  .micro-banner { padding: 24px; }
  .banner-left h1 { font-size: 26px; }
}
@media (max-width: 768px) {
  .category-grid { grid-template-columns: repeat(3, 1fr); gap: 10px; }
  .category-card { padding: 14px 8px 12px; border-radius: 12px; }
  .card-emoji { font-size: 28px; width: 48px; height: 48px; border-radius: 12px; }
  .card-name { font-size: 12px; }
  .micro-banner { padding: 20px; border-radius: 14px; margin-bottom: 20px; }
  .banner-left h1 { font-size: 22px; }
  .banner-stats { display: none; }
  /* 手机端 FAB 由 BottomNav 提供，此处隐藏 */
  .fab-publish { display: none; }
}
@media (max-width: 480px) {
  .category-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
