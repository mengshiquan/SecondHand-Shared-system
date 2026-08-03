<template>
  <div class="product-list page-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">全部商品</h1>
      <p class="page-sub">找到你需要的校园好物</p>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-card">
      <div class="filter-row">
        <el-input
          v-model="query.keyword"
          placeholder="搜索商品..."
          clearable
          class="filter-search"
          @clear="loadData"
          @keyup.enter="loadData"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button :icon="Search" @click="loadData" />
          </template>
        </el-input>

        <div class="filter-divider"></div>

        <el-select v-model="selectedMain" placeholder="全部分类" clearable class="filter-select" @change="onMainChange">
          <el-option v-for="cat in mainCategories" :key="cat.id" :label="cat.name" :value="cat.id" />
        </el-select>

        <el-select v-if="selectedMain" v-model="query.categoryId" placeholder="子分类" clearable class="filter-select" @change="loadData">
          <el-option v-for="cat in subCategories" :key="cat.id" :label="cat.name" :value="cat.id" />
        </el-select>

        <span class="filter-total" v-if="total > 0">共 <strong>{{ total }}</strong> 件</span>
      </div>
    </div>

    <!-- 商品网格 -->
    <div class="feed-grid" v-if="!loading">
      <div v-for="(item, i) in products" :key="item.id" class="feed-card" :style="{ animationDelay: `${i * 0.05}s` }">
        <ProductCard :product="item" />
      </div>
    </div>
    <div class="feed-grid" v-else>
      <SkeletonCard v-for="n in 12" :key="n" />
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && products.length === 0" class="empty-state">
      <div class="empty-icon">
        <el-icon :size="48"><Search /></el-icon>
      </div>
      <p class="empty-title">暂无相关商品</p>
      <p class="empty-hint">试试调整筛选条件或换个关键词吧</p>
    </div>

    <!-- 分页 -->
    <div class="pagination" v-if="total > 0">
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
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { getProductList } from '@/api/product'
import { getMainCategories, getSubCategories } from '@/api/category'
import ProductCard from '@/components/ProductCard.vue'
import SkeletonCard from '@/components/SkeletonCard.vue'

const route = useRoute()
const products = ref([])
const mainCategories = ref([])
const subCategories = ref([])
const selectedMain = ref(null)
const total = ref(0)
const loading = ref(false)

const query = reactive({
  keyword: '',
  categoryId: null,
  pageNum: 1,
  pageSize: 12
})

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

async function onMainChange(mainId) {
  query.categoryId = null
  if (mainId) {
    const res = await getSubCategories(mainId)
    subCategories.value = res.data
  } else {
    subCategories.value = []
  }
  loadData()
}

onMounted(async () => {
  if (route.query.keyword) query.keyword = route.query.keyword
  if (route.query.categoryId) query.categoryId = Number(route.query.categoryId)
  const res = await getMainCategories()
  mainCategories.value = res.data
  loadData()
})

watch(() => route.query.keyword, (val) => {
  query.keyword = val || ''
  loadData()
})
</script>

<style scoped>
/* ====== 页面标题 ====== */
.page-header {
  margin-bottom: 24px;
}
.page-title {
  font-size: 24px; font-weight: 800; color: #1F2937; margin: 0 0 4px;
}
.page-sub {
  font-size: 14px; color: #9CA3AF; margin: 0;
}

/* ====== 筛选卡片 ====== */
.filter-card {
  background: #fff;
  border-radius: 14px;
  padding: 16px 20px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  border: 1px solid #F3F4F6;
}
.filter-row {
  display: flex; align-items: center; gap: 14px; flex-wrap: wrap;
}
.filter-search { width: 260px; }
.filter-select { width: 150px; }
.filter-divider {
  width: 1px; height: 24px; background: #E5E7EB; flex-shrink: 0;
}
.filter-total {
  margin-left: auto; font-size: 14px; color: #6B7280; white-space: nowrap;
}
.filter-total strong { color: #10B981; font-weight: 700; }

/* ====== 商品网格 ====== */
.feed-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  align-items: stretch;
}
.feed-card {
  min-width: 0;
  animation: fadeInUp 0.5s ease both;
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(24px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ====== 空状态 ====== */
.empty-state {
  text-align: center; padding: 64px 20px;
}
.empty-icon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 80px; height: 80px; border-radius: 20px;
  background: linear-gradient(135deg, #ECFDF5, #D1FAE5);
  color: #10B981; margin-bottom: 16px;
}
.empty-title { font-size: 16px; font-weight: 600; color: #374151; margin: 0 0 6px; }
.empty-hint { font-size: 13px; color: #9CA3AF; margin: 0; }

/* ====== 分页 ====== */
.pagination {
  display: flex; justify-content: center;
  margin-top: 32px;
  padding: 20px 0 12px;
}

/* ====== 响应式 ====== */
@media (max-width: 1024px) { .feed-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 768px) {
  .feed-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .filter-search { width: 100%; }
  .filter-select { width: calc(50% - 14px); }
  .filter-divider { display: none; }
  .filter-total { margin-left: 0; width: 100%; text-align: right; }
}
@media (max-width: 480px) {
  .page-title { font-size: 20px; }
  .filter-card { padding: 12px 14px; border-radius: 10px; }
}
</style>
