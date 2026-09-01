<template>
  <div class="cart-page">
    <!-- 页头 -->
    <div class="cart-head">
      <h1 class="cart-title">购物车</h1>
      <span class="cart-promo">校园好物 · 淘到就是赚到</span>
    </div>

    <template v-if="items.length">
      <!-- 页签 -->
      <div class="cart-tabs">
        <button class="tab" :class="{ active: tab === 'all' }" @click="tab = 'all'">
          <el-icon><Grid /></el-icon>全部商品 ({{ items.length }})
        </button>
        <button class="tab" :class="{ active: tab === 'invalid' }" @click="tab = 'invalid'">
          <el-icon><WarningFilled /></el-icon>失效商品 ({{ invalidCount }})
        </button>
      </div>

      <div class="cart-body" v-loading="loading">
        <div class="cart-main">
          <!-- 工具栏 -->
          <div class="cart-toolbar">
            <div class="toolbar-left">
              <el-checkbox
                :model-value="allSelected"
                :indeterminate="someSelected"
                :disabled="!validFiltered.length"
                @change="toggleAll"
              >全选</el-checkbox>
              <el-button :disabled="!selectedIds.length" @click="batchMoveFavorite">移入收藏</el-button>
              <el-button :disabled="!selectedIds.length" @click="batchDelete">删除</el-button>
            </div>
            <div class="toolbar-right">
              <el-select v-model="filterCategory" placeholder="分类" clearable class="tb-select">
                <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
              <el-select v-model="filterStatus" placeholder="状态" clearable class="tb-select">
                <el-option label="可购买" value="valid" />
                <el-option label="不可购买" value="invalid" />
              </el-select>
              <el-input v-model="keyword" placeholder="搜索购物车内商品" clearable class="tb-search">
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
            </div>
          </div>

          <!-- 按卖家分组列表 -->
          <div v-if="groups.length" class="cart-groups">
            <div v-for="g in groups" :key="g.sellerId ?? 'none'" class="cart-group">
              <div class="group-head">
                <el-checkbox
                  :model-value="groupAll(g)"
                  :indeterminate="groupSome(g)"
                  :disabled="!g.items.some(i => !i.invalid)"
                  @change="v => toggleGroup(g, v)"
                />
                <el-icon class="group-icon"><Shop /></el-icon>
                <span class="group-name">{{ g.sellerName }}</span>
              </div>

              <div v-for="item in g.items" :key="item.id" class="cart-row" :class="{ invalid: item.invalid }">
                <el-checkbox
                  :model-value="selectedIds.includes(item.id)"
                  :disabled="item.invalid"
                  @change="v => toggleItem(item, v)"
                />
                <div class="row-img">
                  <el-image :src="item.images?.[0]" fit="cover">
                    <template #error>
                      <div class="img-fallback"><el-icon :size="22"><Picture /></el-icon></div>
                    </template>
                  </el-image>
                  <span v-if="item.invalid" class="img-mask">不可购买</span>
                </div>
                <div class="row-info">
                  <h4 class="row-title" @click="!item.invalid && router.push(`/product/${item.productId}`)">
                    {{ item.title }}
                  </h4>
                  <p v-if="item.categoryName" class="row-meta">{{ item.categoryName }}</p>
                  <p v-if="item.invalid" class="row-hint">商品已下架或售出，无法购买</p>
                </div>
                <div class="row-price">¥{{ item.price }}</div>
                <div class="row-qty">1</div>
                <div class="row-actions">
                  <button class="link" @click="moveOne(item)">移入收藏</button>
                  <button class="link" @click="removeOne(item)">删除</button>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="filter-empty">没有符合筛选条件的商品</div>
        </div>

        <!-- 右侧结算明细 -->
        <aside class="cart-summary">
          <h3>结算明细</h3>
          <div v-if="!selectedIds.length" class="summary-empty">
            <div class="empty-cart">
              <el-icon :size="44"><ShoppingCart /></el-icon>
              <span class="empty-badge">空</span>
            </div>
            <p class="summary-hint">选择商品查看实际支付价格</p>
          </div>
          <div v-else class="summary-lines">
            <div class="line"><span>已选商品</span><b>{{ selectedIds.length }} 件</b></div>
          </div>
          <div class="summary-total">合计: <em>¥{{ totalAmount }}</em></div>
          <el-button class="btn-settle" :disabled="!selectedIds.length" @click="openCheckout">结算</el-button>
        </aside>
      </div>
    </template>

    <!-- 空购物车 -->
    <div v-else-if="!loading" class="empty-state">
      <div class="empty-icon"><el-icon :size="40"><ShoppingCart /></el-icon></div>
      <p class="empty-title">购物车是空的</p>
      <p class="empty-hint">去挑选心仪的商品，加入购物车后批量结算</p>
      <el-button type="primary" @click="router.push('/products')">去逛逛</el-button>
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
          确认结算（{{ selectedIds.length }} 件）
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Grid, WarningFilled, Shop, Picture, ShoppingCart, Search, Location } from '@element-plus/icons-vue'
import { getCartList, removeFromCart, removeBatchFromCart, moveToFavorite, checkoutCart } from '@/api/cart'
import { getAddressList } from '@/api/address'

const router = useRouter()
const items = ref([])
const loading = ref(false)
const selectedIds = ref([])

const tab = ref('all')
const filterCategory = ref(null)
const filterStatus = ref('')
const keyword = ref('')

const checkoutVisible = ref(false)
const checkingOut = ref(false)
const addressList = ref([])
const checkoutAddressId = ref(null)

const invalidCount = computed(() => items.value.filter(i => i.invalid).length)

const categoryOptions = computed(() => {
  const map = new Map()
  items.value.forEach(i => {
    if (i.categoryId != null && !map.has(i.categoryId)) map.set(i.categoryId, i.categoryName || '未分类')
  })
  return [...map.entries()].map(([id, name]) => ({ id, name }))
})

const filtered = computed(() => {
  let list = tab.value === 'invalid' ? items.value.filter(i => i.invalid) : items.value
  if (filterCategory.value != null) list = list.filter(i => i.categoryId === filterCategory.value)
  if (filterStatus.value) list = list.filter(i => (filterStatus.value === 'invalid') === !!i.invalid)
  const kw = keyword.value.trim()
  if (kw) list = list.filter(i => (i.title || '').toLowerCase().includes(kw.toLowerCase()))
  return list
})

const validFiltered = computed(() => filtered.value.filter(i => !i.invalid))

const groups = computed(() => {
  const map = new Map()
  filtered.value.forEach(i => {
    const key = i.sellerId ?? 'none'
    if (!map.has(key)) {
      map.set(key, { sellerId: i.sellerId, sellerName: i.sellerNickname || '未知卖家', items: [] })
    }
    map.get(key).items.push(i)
  })
  return [...map.values()]
})

const allSelected = computed(() =>
  validFiltered.value.length > 0 && validFiltered.value.every(i => selectedIds.value.includes(i.id))
)
const someSelected = computed(() =>
  !allSelected.value && validFiltered.value.some(i => selectedIds.value.includes(i.id))
)

const totalAmount = computed(() =>
  items.value
    .filter(i => selectedIds.value.includes(i.id) && !i.invalid)
    .reduce((sum, i) => sum + Number(i.price || 0), 0)
    .toFixed(2)
)

function toggleAll(v) {
  const ids = validFiltered.value.map(i => i.id)
  selectedIds.value = v
    ? [...new Set([...selectedIds.value, ...ids])]
    : selectedIds.value.filter(id => !ids.includes(id))
}
function toggleItem(item, v) {
  selectedIds.value = v
    ? [...selectedIds.value, item.id]
    : selectedIds.value.filter(id => id !== item.id)
}
function groupAll(g) {
  const valid = g.items.filter(i => !i.invalid)
  return valid.length > 0 && valid.every(i => selectedIds.value.includes(i.id))
}
function groupSome(g) {
  const valid = g.items.filter(i => !i.invalid)
  return !groupAll(g) && valid.some(i => selectedIds.value.includes(i.id))
}
function toggleGroup(g, v) {
  const ids = g.items.filter(i => !i.invalid).map(i => i.id)
  selectedIds.value = v
    ? [...new Set([...selectedIds.value, ...ids])]
    : selectedIds.value.filter(id => !ids.includes(id))
}

async function loadCart() {
  loading.value = true
  try {
    const res = await getCartList()
    items.value = res.data
    // 清理已不存在的选中项
    selectedIds.value = selectedIds.value.filter(id =>
      items.value.some(i => i.id === id && !i.invalid)
    )
  } finally { loading.value = false }
}

async function moveOne(item) {
  await moveToFavorite([item.id])
  ElMessage.success('已移入收藏')
  loadCart()
}
async function removeOne(item) {
  await removeFromCart(item.id)
  ElMessage.success('已删除')
  loadCart()
}
async function batchMoveFavorite() {
  await ElMessageBox.confirm(`将选中的 ${selectedIds.value.length} 件商品移入收藏？`, '提示', { type: 'info' })
  await moveToFavorite(selectedIds.value)
  ElMessage.success('已移入收藏')
  selectedIds.value = []
  loadCart()
}
async function batchDelete() {
  await ElMessageBox.confirm(`删除选中的 ${selectedIds.value.length} 件商品？`, '提示', { type: 'warning' })
  await removeBatchFromCart(selectedIds.value)
  ElMessage.success('已删除')
  selectedIds.value = []
  loadCart()
}

async function openCheckout() {
  if (!selectedIds.value.length) return
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
      cartItemIds: selectedIds.value,
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
/* ====== 页头 ====== */
.cart-head { display: flex; align-items: baseline; justify-content: center; gap: 14px; margin-bottom: 18px; }
.cart-title { font-size: 26px; font-weight: 800; color: #1F2937; margin: 0; }
.cart-promo { font-size: 14px; font-weight: 600; color: #10B981; }

/* ====== 页签 ====== */
.cart-tabs { display: flex; gap: 28px; border-bottom: 1px solid #E5E7EB; margin-bottom: 14px; }
.tab {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 10px 2px; border: none; background: none; cursor: pointer;
  font-size: 15px; font-weight: 700; color: #6B7280;
  border-bottom: 3px solid transparent; margin-bottom: -1px;
  transition: color 0.2s ease;
}
.tab:hover { color: #10B981; }
.tab.active { color: #10B981; border-bottom-color: #10B981; }

/* ====== 主体两栏 ====== */
.cart-body { display: flex; gap: 16px; align-items: flex-start; }
.cart-main { flex: 1; min-width: 0; }

/* ====== 工具栏 ====== */
.cart-toolbar {
  display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap;
  background: #fff; border: 1px solid #F3F4F6; border-radius: 12px;
  padding: 10px 16px; margin-bottom: 12px;
}
.toolbar-left { display: flex; align-items: center; gap: 10px; }
.toolbar-right { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.tb-select { width: 110px; }
.tb-search { width: 200px; }

/* ====== 分组 ====== */
.cart-group {
  background: #fff; border: 1px solid #F3F4F6; border-radius: 12px;
  margin-bottom: 12px; overflow: hidden;
}
.group-head {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 16px; background: #FAFBFC; border-bottom: 1px solid #F3F4F6;
}
.group-icon { color: #10B981; }
.group-name { font-size: 14px; font-weight: 700; color: #374151; }

/* ====== 商品行 ====== */
.cart-row {
  display: grid;
  grid-template-columns: 40px 92px minmax(0, 1fr) 100px 48px 88px;
  align-items: center; gap: 12px;
  padding: 16px;
}
.cart-row + .cart-row { border-top: 1px solid #F3F4F6; }

.row-img { position: relative; width: 92px; height: 92px; }
.row-img .el-image { width: 92px; height: 92px; border-radius: 10px; display: block; }
.img-fallback {
  width: 92px; height: 92px; display: flex; align-items: center; justify-content: center;
  background: #F3F4F6; color: #9CA3AF; border-radius: 10px;
}
.img-mask {
  position: absolute; left: 0; right: 0; bottom: 0;
  padding: 3px 0; text-align: center;
  font-size: 12px; color: #fff;
  background: rgba(31, 41, 55, 0.62);
  border-radius: 0 0 10px 10px;
}

.row-title {
  margin: 0 0 4px; font-size: 14px; font-weight: 600; color: #1F2937;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; cursor: pointer;
}
.row-title:hover { color: #10B981; }
.row-meta { margin: 0; font-size: 12px; color: #9CA3AF; }
.row-hint { margin: 4px 0 0; font-size: 12px; color: #9CA3AF; }

.row-price { color: #F59E0B; font-weight: 700; font-variant-numeric: tabular-nums; }
.row-qty { text-align: center; color: #6B7280; font-variant-numeric: tabular-nums; }

.row-actions { display: flex; flex-direction: column; gap: 6px; align-items: flex-end; }
.row-actions .link {
  border: none; background: none; padding: 0; cursor: pointer;
  font-size: 13px; color: #6B7280;
}
.row-actions .link:hover { color: #10B981; }

.cart-row.invalid .row-title { color: #9CA3AF; cursor: default; }
.cart-row.invalid .row-title:hover { color: #9CA3AF; }
.cart-row.invalid .row-img .el-image,
.cart-row.invalid .img-fallback { opacity: 0.55; }
.cart-row.invalid .row-price { color: #C4C8CE; }

.filter-empty {
  padding: 48px 0; text-align: center; color: #9CA3AF; font-size: 14px;
  background: #fff; border: 1px solid #F3F4F6; border-radius: 12px;
}

/* ====== 右侧结算明细 ====== */
.cart-summary {
  width: 300px; flex-shrink: 0; position: sticky; top: 76px;
  background: #fff; border: 1px solid #F3F4F6; border-radius: 12px;
  padding: 18px;
}
.cart-summary h3 { margin: 0 0 14px; font-size: 16px; font-weight: 800; color: #1F2937; }
.summary-empty { text-align: center; padding: 18px 0 8px; }
.empty-cart { position: relative; display: inline-block; color: #D1D5DB; }
.empty-badge {
  position: absolute; top: -6px; right: -14px;
  width: 26px; height: 26px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  background: #F59E0B; color: #fff; font-size: 12px; font-weight: 700;
}
.summary-hint { margin: 12px 0 0; font-size: 13px; font-weight: 600; color: #F59E0B; }
.summary-lines .line {
  display: flex; justify-content: space-between; font-size: 13px; color: #6B7280;
  padding: 4px 0;
}
.summary-lines .line b { color: #374151; }
.summary-total {
  display: flex; justify-content: space-between; align-items: baseline;
  margin: 12px 0 14px; font-size: 14px; font-weight: 700; color: #1F2937;
}
.summary-total em {
  font-style: normal; color: #F59E0B; font-size: 22px; font-weight: 800;
  font-variant-numeric: tabular-nums;
}
.btn-settle {
  width: 100%; height: 44px; font-size: 16px; font-weight: 800;
  background: #10B981; border-color: #10B981; color: #fff; border-radius: 10px;
}
.btn-settle:hover:not(:disabled) { background: #0EA371; border-color: #0EA371; }

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

/* ====== 结算弹窗地址列表 ====== */
.addr-list { display: flex; flex-direction: column; gap: 10px; max-height: 360px; overflow-y: auto; }
.addr-item {
  padding: 12px 14px; border-radius: 10px; cursor: pointer;
  border: 1.5px solid #E5E7EB; background: #fff; transition: all 0.2s ease;
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

/* ====== 响应式 ====== */
@media (max-width: 1024px) {
  .cart-body { flex-direction: column; }
  .cart-summary { width: 100%; position: static; }
}
@media (max-width: 768px) {
  .cart-row {
    grid-template-columns: 32px 72px minmax(0, 1fr) 80px;
    grid-template-areas:
      "check img info info"
      "check img price actions";
    row-gap: 8px;
  }
  .cart-row > .el-checkbox { grid-area: check; }
  .row-img { grid-area: img; width: 72px; height: 72px; }
  .row-img .el-image, .img-fallback { width: 72px; height: 72px; }
  .row-info { grid-area: info; }
  .row-price { grid-area: price; }
  .row-qty { display: none; }
  .row-actions { grid-area: actions; flex-direction: row; gap: 12px; justify-content: flex-end; }
  .tb-search { width: 100%; }
}
</style>
