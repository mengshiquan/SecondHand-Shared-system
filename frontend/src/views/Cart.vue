<template>
  <div class="cart page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">购物车</h1>
        <p class="page-sub">勾选商品批量结算</p>
      </div>
      <el-button v-if="cartItems.length" plain type="danger" @click="handleClear">
        <el-icon><Delete /></el-icon>清空购物车
      </el-button>
    </div>

    <div v-if="cartItems.length" class="cart-card" v-loading="loading">
      <el-table
        ref="cartTableRef"
        :data="cartItems"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" :selectable="row => !row.invalid" />
        <el-table-column label="商品信息" min-width="320">
          <template #default="{ row }">
            <div class="cart-item" :class="{ invalid: row.invalid }">
              <el-image :src="row.images?.[0]" class="cart-item-img" fit="cover">
                <template #error>
                  <div class="img-fallback"><el-icon :size="22"><Picture /></el-icon></div>
                </template>
              </el-image>
              <div class="cart-item-info">
                <h4 class="cart-item-title" @click="!row.invalid && router.push(`/product/${row.productId}`)">{{ row.title }}</h4>
                <p class="cart-item-meta">
                  <span v-if="row.categoryName">{{ row.categoryName }}</span>
                  <span v-if="row.sellerNickname"> · 卖家：{{ row.sellerNickname }}</span>
                </p>
                <el-tag v-if="row.invalid" type="danger" size="small" effect="plain">已失效</el-tag>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="120">
          <template #default="{ row }">
            <span class="cart-price">¥{{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.invalid" type="danger" effect="plain" size="small">已失效</el-tag>
            <el-tag v-else type="success" effect="plain" size="small">可购买</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button size="small" type="danger" text @click="handleRemove(row.id)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="!cartItems.length && !loading" class="empty-state">
      <div class="empty-icon">
        <el-icon :size="40"><ShoppingCart /></el-icon>
      </div>
      <p class="empty-title">购物车是空的</p>
      <p class="empty-hint">去挑选心仪的商品，加入购物车后批量结算</p>
      <el-button type="primary" @click="router.push('/products')">去逛逛</el-button>
    </div>

    <!-- 底部结算栏 -->
    <div v-if="cartItems.length" class="checkout-bar">
      <div class="checkout-info">
        <span>已选 <strong>{{ selectedItems.length }}</strong> 件</span>
        <span class="checkout-total">
          合计：<em>¥{{ totalAmount }}</em>
        </span>
      </div>
      <el-button type="primary" size="large" class="btn-checkout" :disabled="!selectedItems.length" @click="openCheckout">
        去结算{{ selectedItems.length ? `（${selectedItems.length}）` : '' }}
      </el-button>
    </div>

    <!-- 结算弹窗：选择收货地址 -->
    <el-dialog v-model="checkoutVisible" title="确认结算" width="480px" :close-on-click-modal="false">
      <div v-if="addressList.length" class="addr-list">
        <div
          v-for="a in addressList"
          :key="a.id"
          class="addr-item"
          :class="{ active: checkoutAddressId === a.id }"
          @click="checkoutAddressId = a.id"
        >
          <div class="addr-main">
            <span class="addr-name">{{ a.receiverName }}</span>
            <span class="addr-phone">{{ a.phone }}</span>
            <el-tag v-if="a.isDefault === 1" type="success" size="small">默认</el-tag>
          </div>
          <div class="addr-detail">{{ a.address }}</div>
        </div>
      </div>
      <div v-else class="addr-empty">
        <el-icon :size="32"><Location /></el-icon>
        <p>你还没有收货地址，请先到个人中心添加</p>
        <el-button type="primary" @click="checkoutVisible = false; router.push('/profile')">去添加地址</el-button>
      </div>
      <template v-if="addressList.length" #footer>
        <el-button @click="checkoutVisible = false">取消</el-button>
        <el-button type="primary" :loading="checkingOut" :disabled="!checkoutAddressId" @click="confirmCheckout">
          确认结算（{{ selectedItems.length }} 件）
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Picture, ShoppingCart, Location } from '@element-plus/icons-vue'
import { getCartList, removeFromCart, clearCart, checkoutCart } from '@/api/cart'
import { getAddressList } from '@/api/address'

const router = useRouter()
const cartItems = ref([])
const loading = ref(false)
const selectedItems = ref([])
const cartTableRef = ref(null)

const checkoutVisible = ref(false)
const checkingOut = ref(false)
const addressList = ref([])
const checkoutAddressId = ref(null)

const totalAmount = computed(() =>
  selectedItems.value.reduce((sum, item) => sum + Number(item.price || 0), 0).toFixed(2)
)

async function loadCart() {
  loading.value = true
  try {
    const res = await getCartList()
    cartItems.value = res.data
    selectedItems.value = []
  } finally { loading.value = false }
}

function handleSelectionChange(rows) {
  selectedItems.value = rows.filter(r => !r.invalid)
}

async function handleRemove(id) {
  await removeFromCart(id)
  ElMessage.success('已移除')
  loadCart()
}

async function handleClear() {
  await ElMessageBox.confirm('确认清空购物车？', '提示', { type: 'warning' })
  await clearCart()
  ElMessage.success('购物车已清空')
  loadCart()
}

async function openCheckout() {
  if (!selectedItems.value.length) return
  const res = await getAddressList()
  addressList.value = res.data
  const def = addressList.value.find(a => a.isDefault === 1)
  checkoutAddressId.value = def?.id ?? addressList.value[0]?.id ?? null
  checkoutVisible.value = true
}

async function confirmCheckout() {
  checkingOut.value = true
  try {
    const res = await checkoutCart({
      cartItemIds: selectedItems.value.map(i => i.id),
      addressId: checkoutAddressId.value
    })
    checkoutVisible.value = false
    ElMessage.success(`已创建 ${res.data.length} 笔订单`)
    router.push('/orders')
  } finally { checkingOut.value = false }
}

onMounted(loadCart)
</script>

<style scoped>
/* ====== 页面标题 ====== */
.page-header {
  display: flex; align-items: flex-end; justify-content: space-between;
  margin-bottom: 24px;
}
.page-title { font-size: 24px; font-weight: 800; color: #1F2937; margin: 0 0 4px; }
.page-sub { font-size: 14px; color: #9CA3AF; margin: 0; }

/* ====== 表格卡片 ====== */
.cart-card {
  background: #fff; border-radius: 14px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  border: 1px solid #F3F4F6;
  overflow: hidden;
}

.cart-item { display: flex; align-items: center; gap: 12px; }
.cart-item-img {
  width: 64px; height: 64px; flex-shrink: 0;
  border-radius: 10px; cursor: default;
}
.cart-item-info { min-width: 0; }
.cart-item-title {
  margin: 0 0 4px; font-size: 14px; font-weight: 600; color: #1F2937;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  cursor: pointer;
}
.cart-item-title:hover { color: #10B981; }
.cart-item-meta { margin: 0 0 4px; font-size: 12px; color: #9CA3AF; }
.cart-item.invalid .cart-item-title { color: #9CA3AF; cursor: default; }
.cart-item.invalid .cart-item-img { opacity: 0.5; }
.cart-price { color: #F59E0B; font-weight: 700; font-variant-numeric: tabular-nums; }

.img-fallback {
  width: 64px; height: 64px;
  display: flex; align-items: center; justify-content: center;
  background: #F3F4F6; color: #9CA3AF; border-radius: 10px;
}

/* ====== 结算栏 ====== */
.checkout-bar {
  position: sticky; bottom: 16px;
  margin-top: 16px;
  display: flex; align-items: center; justify-content: flex-end; gap: 24px;
  padding: 14px 24px;
  background: #fff; border-radius: 14px;
  border: 1px solid #F3F4F6;
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
}
.checkout-info { display: flex; align-items: center; gap: 20px; font-size: 14px; color: #6B7280; }
.checkout-info strong { color: #10B981; }
.checkout-total em {
  font-style: normal; color: #F59E0B; font-size: 20px; font-weight: 800;
  font-variant-numeric: tabular-nums;
}
.btn-checkout { min-width: 140px; font-weight: 700; }

/* ====== 结算弹窗地址列表 ====== */
.addr-list { display: flex; flex-direction: column; gap: 10px; max-height: 360px; overflow-y: auto; }
.addr-item {
  padding: 12px 14px; border-radius: 10px; cursor: pointer;
  border: 1.5px solid #E5E7EB; background: #fff;
  transition: all 0.2s ease;
}
.addr-item:hover { border-color: #A7F3D0; }
.addr-item.active {
  border-color: #10B981; background: #F0FDF4;
  box-shadow: 0 0 0 2px rgba(16,185,129,0.12);
}
.addr-main { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; }
.addr-name { font-size: 14px; font-weight: 600; color: #1F2937; }
.addr-phone { font-size: 13px; color: #6B7280; }
.addr-detail { font-size: 13px; color: #6B7280; }
.addr-empty { text-align: center; padding: 24px 0; color: #9CA3AF; }
.addr-empty .el-icon { color: #D1D5DB; margin-bottom: 8px; }
.addr-empty p { margin: 0 0 12px; font-size: 14px; }

/* ====== 空状态 ====== */
.empty-state { text-align: center; padding: 64px 20px; }
.empty-icon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 72px; height: 72px; border-radius: 16px;
  background: linear-gradient(135deg, #ECFDF5, #D1FAE5);
  color: #10B981; margin-bottom: 16px;
}
.empty-title { font-size: 16px; font-weight: 600; color: #374151; margin: 0 0 6px; }
.empty-hint { font-size: 13px; color: #9CA3AF; margin: 0 0 16px; }

/* ====== 响应式 ====== */
@media (max-width: 768px) {
  .checkout-bar { flex-wrap: wrap; gap: 12px; padding: 12px 16px; }
  .btn-checkout { width: 100%; }
}
</style>
