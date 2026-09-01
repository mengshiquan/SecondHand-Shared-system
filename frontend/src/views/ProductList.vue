<template>
  <div class="plist page-container">
    <!-- 分类横条（横向滚动胶囊） -->
    <div class="cat-strip">
      <button class="cat-chip" :class="{ active: !selectedMain }" @click="selectMain(null)">
        <span class="chip-emoji">🛍️</span>全部
      </button>
      <button
        v-for="cat in mainCategories"
        :key="cat.id"
        class="cat-chip"
        :class="{ active: selectedMain === cat.id }"
        @click="selectMain(cat.id)"
      >
        <span class="chip-emoji">{{ catEmoji[cat.name] || '📌' }}</span>{{ cat.name }}
      </button>
    </div>

    <!-- 子分类胶囊条 -->
    <div v-if="subCategories.length" class="sub-strip">
      <button class="sub-pill" :class="{ active: !query.categoryId }" @click="selectSub(null)">
        全部{{ currentMainName }}
      </button>
      <button
        v-for="s in subCategories"
        :key="s.id"
        class="sub-pill"
        :class="{ active: query.categoryId === s.id }"
        @click="selectSub(s.id)"
      >{{ s.name }}</button>
    </div>

    <!-- 排序工具栏 -->
    <div class="sort-bar">
      <div class="sort-left">
        <button class="sort-btn" :class="{ active: sort === 'default' }" @click="setSort('default')">综合</button>
        <button class="sort-btn" :class="{ active: sort === 'view_desc' }" @click="setSort('view_desc')">人气</button>
        <button class="sort-btn" :class="{ active: sort === 'price_asc' || sort === 'price_desc' }" @click="togglePrice">
          价格
          <span class="price-arrows" :class="{ asc: sort === 'price_asc', desc: sort === 'price_desc' }">⇅</span>
        </button>
      </div>
      <div class="sort-right">
        <el-input
          v-model="query.keyword"
          placeholder="搜索商品"
          clearable
          class="sort-search"
          @clear="loadData"
          @keyup.enter="loadData"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <span v-if="total > 0" class="sort-total">共 <strong>{{ total }}</strong> 件</span>
      </div>
    </div>

    <!-- 商品网格 -->
    <div v-if="!loading" class="goods-grid">
      <div
        v-for="(item, i) in products"
        :key="item.id"
        class="goods-card"
        :style="{ animationDelay: `${i * 0.04}s` }"
        @click="router.push(`/product/${item.id}`)"
      >
        <div class="goods-img">
          <el-image :src="firstImage(item)" fit="cover">
            <template #error>
              <div class="img-ph"><el-icon :size="30"><Picture /></el-icon></div>
            </template>
          </el-image>
          <el-tag v-if="item.status !== 'ON_SALE'" class="g-status" size="small" type="info">
            {{ statusText(item) }}
          </el-tag>
          <button
            v-if="item.status === 'ON_SALE'"
            class="g-cart"
            title="加入购物车"
            @click.stop="handleAddCart(item)"
          >
            <el-icon :size="15"><ShoppingCart /></el-icon>
          </button>
        </div>
        <div class="goods-info">
          <h4 class="g-title"><span class="t-tag">校园</span>{{ item.title }}</h4>
          <div class="g-price">
            <em>¥</em><b>{{ item.price }}</b>
            <s v-if="item.originalPrice">¥{{ item.originalPrice }}</s>
          </div>
          <div class="g-meta">{{ item.viewCount || 0 }}人看过 · {{ item.sellerName || '匿名' }}</div>
          <div class="g-foot"><span class="g-cat">{{ item.categoryName || '其他闲置' }}</span></div>
        </div>
      </div>
    </div>
    <div v-else class="goods-grid">
      <SkeletonCard v-for="n in 10" :key="n" />
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && products.length === 0" class="empty-state">
      <div class="empty-icon"><el-icon :size="48"><Search /></el-icon></div>
      <p class="empty-title">暂无相关商品</p>
      <p class="empty-hint">试试调整筛选条件或换个关键词吧</p>
    </div>

    <!-- 分页 -->
    <div v-if="total > 0" class="pagination">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next"
        background
        @change="loadData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Picture, ShoppingCart } from '@element-plus/icons-vue'
import { getProductList } from '@/api/product'
import { getMainCategories, getSubCategories, getCategoryTree } from '@/api/category'
import { addToCart } from '@/api/cart'
import { useUserStore } from '@/stores/user'
import SkeletonCard from '@/components/SkeletonCard.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const products = ref([])
const mainCategories = ref([])
const subCategories = ref([])
const selectedMain = ref(null)
const total = ref(0)
const loading = ref(false)
const sort = ref('default')

const catEmoji = {
  '数码电子': '📱', '图书教材': '📚', '生活用品': '🏠',
  '服饰鞋包': '👔', '运动户外': '⚽', '音乐器材': '🎸',
  '其他闲置': '🎁'
}

const query = reactive({
  keyword: '',
  categoryId: null,
  parentCategoryId: null,
  sortBy: null,
  pageNum: 1,
  pageSize: 12
})

const currentMainName = computed(
  () => mainCategories.value.find(c => c.id === selectedMain.value)?.name || ''
)

function firstImage(item) {
  return Array.isArray(item.images) && item.images.length ? item.images[0] : ''
}
function statusText(item) {
  const map = { ON_SALE: '在售', SOLD: '已售', OFF_SHELF: '下架' }
  return map[item.status] || item.status
}

async function loadData() {
  loading.value = true
  try {
    const res = await getProductList(query)
    products.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function selectMain(mainId) {
  selectedMain.value = mainId
  query.categoryId = null
  query.parentCategoryId = mainId
  query.pageNum = 1
  if (mainId) {
    const res = await getSubCategories(mainId)
    subCategories.value = res.data || []
  } else {
    subCategories.value = []
  }
  loadData()
}

function selectSub(subId) {
  if (subId) {
    query.categoryId = subId
    query.parentCategoryId = null
  } else {
    query.categoryId = null
    query.parentCategoryId = selectedMain.value
  }
  query.pageNum = 1
  loadData()
}

function setSort(s) {
  sort.value = s
  query.sortBy = s === 'default' ? null : s
  query.pageNum = 1
  loadData()
}

function togglePrice() {
  setSort(sort.value === 'price_asc' ? 'price_desc' : 'price_asc')
}

async function handleAddCart(item) {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  try {
    await addToCart(item.id)
    ElMessage.success('已加入购物车')
  } catch { /* request.js 已统一提示错误 */ }
}

onMounted(async () => {
  if (route.query.keyword) query.keyword = route.query.keyword
  const res = await getMainCategories()
  mainCategories.value = res.data
  if (route.query.parentCategoryId) {
    await selectMain(Number(route.query.parentCategoryId))
    return
  }
  if (route.query.categoryId) {
    // 从首页分类悬停下拉点击子类进入：回显一级/二级分类状态
    const subId = Number(route.query.categoryId)
    query.categoryId = subId
    const treeRes = await getCategoryTree()
    const parent = (treeRes.data || []).find(m => (m.children || []).some(s => s.id === subId))
    if (parent) {
      selectedMain.value = parent.id
      subCategories.value = parent.children || []
    }
  }
  loadData()
})

watch(() => route.query.keyword, (val) => {
  query.keyword = val || ''
  query.pageNum = 1
  loadData()
})
</script>

<style scoped>
/* ====== 分类横条 ====== */
.cat-strip {
  display: flex; gap: 10px; overflow-x: auto;
  padding: 2px 2px 10px; scrollbar-width: none;
}
.cat-strip::-webkit-scrollbar { display: none; }
.cat-chip {
  display: inline-flex; align-items: center; gap: 8px; flex-shrink: 0;
  padding: 9px 16px; border-radius: 12px; cursor: pointer;
  background: #fff; border: 1.5px solid #F3F4F6;
  font-size: 14px; font-weight: 600; color: #374151;
  transition: all 0.2s ease;
}
.cat-chip:hover { border-color: #A7F3D0; color: #059669; }
.cat-chip.active {
  border-color: #10B981; background: #F0FDF4; color: #059669;
  box-shadow: 0 0 0 2px rgba(16,185,129,0.12);
}
.chip-emoji { font-size: 18px; line-height: 1; }

/* ====== 子分类胶囊 ====== */
.sub-strip { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.sub-pill {
  padding: 5px 14px; border-radius: 999px; cursor: pointer;
  border: 1px solid #E5E7EB; background: #fff;
  font-size: 13px; color: #6B7280; transition: all 0.15s ease;
}
.sub-pill:hover { border-color: #A7F3D0; color: #059669; }
.sub-pill.active { border-color: #10B981; background: #10B981; color: #fff; }

/* ====== 排序工具栏 ====== */
.sort-bar {
  display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap;
  background: #fff; border: 1px solid #F3F4F6; border-radius: 12px;
  padding: 10px 16px; margin-bottom: 16px;
}
.sort-left { display: flex; align-items: center; gap: 8px; }
.sort-btn {
  padding: 6px 16px; border-radius: 8px; cursor: pointer;
  border: 1px solid transparent; background: none;
  font-size: 14px; font-weight: 600; color: #6B7280;
  transition: all 0.15s ease;
}
.sort-btn:hover { color: #059669; }
.sort-btn.active { color: #059669; border-color: #10B981; background: #F0FDF4; }
.price-arrows { font-size: 12px; color: #9CA3AF; }
.price-arrows.asc, .price-arrows.desc { color: #10B981; }
.sort-right { display: flex; align-items: center; gap: 12px; }
.sort-search { width: 220px; }
.sort-total { font-size: 13px; color: #6B7280; white-space: nowrap; }
.sort-total strong { color: #10B981; }

/* ====== 商品网格 ====== */
.goods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 14px;
}
.goods-card {
  background: #fff; border-radius: 12px; overflow: hidden;
  border: 1px solid #F3F4F6; cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  animation: fadeInUp 0.45s ease both;
}
.goods-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(16,185,129,0.12);
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(16px); }
  to   { opacity: 1; transform: translateY(0); }
}

.goods-img { position: relative; aspect-ratio: 1 / 1; background: #F3F4F6; }
.goods-img .el-image { width: 100%; height: 100%; display: block; }
.img-ph {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #ECFDF5, #D1FAE5); color: #9CA3AF;
}
.g-status { position: absolute; top: 8px; left: 8px; }
.g-cart {
  position: absolute; right: 10px; bottom: 10px;
  width: 32px; height: 32px; border: none; border-radius: 50%;
  background: rgba(255,255,255,0.92); color: #059669; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  opacity: 0; transform: translateY(4px);
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(0,0,0,0.12);
}
.goods-card:hover .g-cart { opacity: 1; transform: translateY(0); }
.g-cart:hover { background: #10B981; color: #fff; }
@media (hover: none) { .g-cart { opacity: 1; transform: none; } }

.goods-info { padding: 10px 12px 12px; }
.g-title {
  margin: 0 0 6px; font-size: 13px; font-weight: 500; color: #1F2937;
  line-height: 1.45; height: 2.9em;
  display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2;
  overflow: hidden;
}
.t-tag {
  display: inline-block; margin-right: 4px; padding: 0 5px;
  border: 1px solid #10B981; border-radius: 4px;
  color: #10B981; font-size: 11px; font-weight: 700;
  vertical-align: 1px;
}
.g-price { display: flex; align-items: baseline; gap: 6px; margin-bottom: 4px; }
.g-price em { font-style: normal; font-size: 12px; font-weight: 700; color: #F59E0B; }
.g-price b { font-size: 19px; font-weight: 800; color: #F59E0B; font-variant-numeric: tabular-nums; }
.g-price s { font-size: 12px; color: #C4C8CE; }
.g-meta { font-size: 12px; color: #9CA3AF; margin-bottom: 6px; }
.g-foot { display: flex; }
.g-cat {
  padding: 1px 8px; border-radius: 999px;
  background: #F0FDF4; color: #059669; font-size: 11px;
}

/* ====== 空状态 ====== */
.empty-state { text-align: center; padding: 64px 20px; }
.empty-icon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 80px; height: 80px; border-radius: 20px;
  background: linear-gradient(135deg, #ECFDF5, #D1FAE5);
  color: #10B981; margin-bottom: 16px;
}
.empty-title { font-size: 16px; font-weight: 600; color: #374151; margin: 0 0 6px; }
.empty-hint { font-size: 13px; color: #9CA3AF; margin: 0; }

/* ====== 分页 ====== */
.pagination { display: flex; justify-content: center; margin-top: 28px; padding: 16px 0 8px; }

/* ====== 响应式 ====== */
@media (max-width: 768px) {
  .sort-search { width: 150px; }
  .goods-grid { grid-template-columns: repeat(2, 1fr); gap: 10px; }
}
@media (max-width: 480px) {
  .sort-right { width: 100%; }
  .sort-search { flex: 1; width: auto; }
}
</style>
