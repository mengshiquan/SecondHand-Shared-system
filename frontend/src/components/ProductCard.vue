<template>
  <el-card class="product-card card-hover" shadow="hover" @click="goDetail">
    <div class="image-wrap">
      <el-image :src="coverImage" fit="cover" class="cover">
        <template #placeholder>
          <div class="image-loading"></div>
        </template>
        <template #error>
          <div class="image-placeholder">
            <el-icon :size="32"><Picture /></el-icon>
            <span>暂无图片</span>
          </div>
        </template>
      </el-image>
      <el-tag v-if="product.status !== 'ON_SALE'" class="status-tag" size="small" type="info">
        {{ statusText }}
      </el-tag>
    </div>
    <div class="info">
      <h3 class="title">{{ product.title }}</h3>
      <div class="price">¥{{ product.price }}</div>
      <div class="meta">
        <span>{{ product.sellerName || '匿名' }}</span>
        <span>{{ product.categoryName }}</span>
      </div>
      <button
        v-if="product.status === 'ON_SALE'"
        class="cart-btn"
        title="加入购物车"
        @click.stop="handleAddCart"
      >
        <el-icon :size="15"><ShoppingCart /></el-icon>
      </button>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture, ShoppingCart } from '@element-plus/icons-vue'
import { addToCart } from '@/api/cart'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  product: { type: Object, required: true }
})

const router = useRouter()
const userStore = useUserStore()

const coverImage = computed(() => {
  const images = props.product.images
  if (Array.isArray(images) && images.length > 0) return images[0]
  return ''
})

const statusText = computed(() => {
  const map = { ON_SALE: '在售', SOLD: '已售', OFF_SHELF: '下架' }
  return map[props.product.status] || props.product.status
})

function goDetail() {
  router.push(`/product/${props.product.id}`)
}

async function handleAddCart() {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  try {
    await addToCart(props.product.id)
    ElMessage.success('已加入购物车')
  } catch { /* request.js 已统一提示错误 */ }
}
</script>

<style scoped>
.product-card {
  cursor: pointer;
  border-radius: 12px;
  overflow: hidden;
}

.product-card :deep(.el-card__body) {
  padding: 0;
}

.image-wrap {
  position: relative;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: #F3F4F6;
}

.cover {
  width: 100%;
  height: 100%;
}
.cover :deep(img) {
  transition: opacity 0.4s ease;
}
.cover :deep(.el-image__placeholder),
.cover :deep(.el-image__error) {
  position: absolute; inset: 0;
}

/* 加载中 */
.image-loading {
  width: 100%; height: 100%;
  background: linear-gradient(90deg, #F3F4F6 25%, #E5E7EB 50%, #F3F4F6 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s ease-in-out infinite;
}
@keyframes shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* 无图片 */
.image-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%; height: 100%;
  background: linear-gradient(135deg, #ECFDF5, #D1FAE5);
  color: #6B7280;
  font-size: 13px;
}

.status-tag {
  position: absolute;
  top: 8px;
  right: 8px;
}

.info {
  position: relative;
  padding: 12px 16px 16px;
}

.title {
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 8px;
}

.price {
  color: #F59E0B;
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 8px;
  font-variant-numeric: tabular-nums;
}

.meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  padding-right: 30px;
}

/* 加购按钮 */
.cart-btn {
  position: absolute;
  right: 12px;
  bottom: 12px;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 50%;
  background: #ECFDF5;
  color: #059669;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.cart-btn:hover {
  background: #10B981;
  color: #fff;
  transform: scale(1.1);
  box-shadow: 0 4px 10px rgba(16,185,129,0.35);
}

/* 响应式：卡片尺寸跟随网格 */
@media (max-width: 768px) {
  .info { padding: 10px 12px 12px; }
  .title { font-size: 13px; }
  .price { font-size: 16px; }
}
@media (max-width: 480px) {
  .image-wrap { aspect-ratio: 3/2; }
  .info { padding: 8px 10px 10px; }
  .title { font-size: 12px; margin-bottom: 4px; }
  .price { font-size: 14px; margin-bottom: 4px; }
  .meta { font-size: 11px; }
}
</style>
