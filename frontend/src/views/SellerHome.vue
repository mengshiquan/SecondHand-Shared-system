<template>
  <div class="seller-home page-container" v-loading="loading">
    <!-- 卖家信息头 -->
    <div class="seller-hero">
      <el-avatar :size="64" class="seller-avatar">{{ sellerName?.[0] || '?' }}</el-avatar>
      <div class="seller-meta">
        <h1>{{ sellerName || '未知卖家' }}</h1>
        <span class="seller-sub">
          <el-icon :size="14"><Shop /></el-icon>
          在售 {{ total }} 件 · 校园认证卖家
        </span>
      </div>
    </div>

    <!-- 在售商品列表 -->
    <div v-if="products.length" class="product-grid">
      <ProductCard v-for="p in products" :key="p.id" :product="p" />
    </div>
    <el-empty v-else-if="!loading" description="该卖家暂无在售商品" />

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
// 卖家商品主页：展示卖家信息与 TA 的在售商品列表。
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Shop } from '@element-plus/icons-vue'
import { getProductList } from '@/api/product'
import ProductCard from '@/components/ProductCard.vue'

const route = useRoute()

const products = ref([])     // 在售商品列表
const sellerName = ref('')   // 卖家昵称
const total = ref(0)         // 在售商品总数
const pageNum = ref(1)       // 当前页码
const pageSize = 12          // 每页数量
const loading = ref(false)   // 加载状态

/**
 * 加载卖家在售商品；卖家昵称从商品数据中提取
 */
async function load() {
  loading.value = true
  try {
    const res = await getProductList({
      sellerId: route.params.id,
      status: 'ON_SALE',
      pageNum: pageNum.value,
      pageSize
    })
    products.value = res.data.records || []
    total.value = res.data.total || 0
    if (products.value.length) {
      sellerName.value = products.value[0].sellerName || ''
    } else {
      await loadSellerName()
    }
  } finally {
    loading.value = false
  }
}

/**
 * 无在售商品时，从历史商品中取卖家昵称兜底
 */
async function loadSellerName() {
  try {
    const res = await getProductList({ sellerId: route.params.id, pageNum: 1, pageSize: 1 })
    sellerName.value = res.data.records?.[0]?.sellerName || ''
  } catch { sellerName.value = '' }
}

/** 翻页 */
function handlePage(page) {
  pageNum.value = page
  load()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(load)

// 卖家切换时重新加载
watch(() => route.params.id, (id, oldId) => {
  if (id && id !== oldId) {
    pageNum.value = 1
    products.value = []
    sellerName.value = ''
    total.value = 0
    load()
  }
})
</script>

<style scoped>
.seller-hero {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 24px 28px;
  margin-top: 20px;
  background: linear-gradient(135deg, #ECFDF5, #D1FAE5);
  border: 1px solid #A7F3D0;
  border-radius: 16px;
}
.seller-avatar {
  flex-shrink: 0;
  background: #10B981;
  color: #fff;
  font-size: 24px;
  font-weight: 700;
}
.seller-meta h1 {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 800;
  color: #1F2937;
}
.seller-sub {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #059669;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin-top: 24px;
}

.pager {
  display: flex;
  justify-content: center;
  margin: 32px 0 16px;
}

@media (max-width: 480px) {
  .seller-hero { padding: 16px 18px; gap: 12px; }
  .seller-meta h1 { font-size: 18px; }
  .product-grid { grid-template-columns: repeat(2, 1fr); gap: 10px; }
}
</style>
