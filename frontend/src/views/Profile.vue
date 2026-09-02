<template>
  <div class="profile page-container">
    <el-row :gutter="20">
      <!-- 左侧菜单 -->
      <el-col :xs="24" :sm="8" :md="7" :lg="5">
        <div class="side-card">
          <div class="side-nav">
            <div
              v-for="item in navItems"
              :key="item.key"
              class="nav-item"
              :class="{ active: activeTab === item.key }"
              @click="activeTab = item.key"
            >
              <el-icon :size="18"><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 右侧内容区 -->
      <el-col :xs="24" :sm="16" :md="17" :lg="19">
        <!-- 个人首页概览 -->
        <template v-if="activeTab === 'home'">
          <div class="banner-card">
            <div class="banner-left">
              <div class="avatar-upload" :class="{ uploading }" @click="triggerUpload">
                <el-avatar :size="84" :src="avatarUrl">{{ userInfo?.nickname?.[0] }}</el-avatar>
                <div class="avatar-mask"><el-icon :size="22"><Camera /></el-icon></div>
              </div>
              <input ref="fileInput" type="file" accept="image/*" hidden @change="handleAvatarChange" />
              <div class="banner-id">
                <h2>{{ userInfo?.nickname }}</h2>
                <p class="banner-sub">
                  {{ userInfo?.username }}
                  <template v-if="userInfo?.studentId"> · 学号 {{ userInfo?.studentId }}</template>
                  <template v-if="userInfo?.schoolName"> · {{ userInfo?.schoolName }}</template>
                </p>
                <div class="banner-tags">
                  <el-tag v-if="userInfo?.verifyStatus === 'APPROVED'" type="success" size="small" effect="plain">校园已认证</el-tag>
                  <el-tag v-else-if="userInfo?.verifyStatus === 'PENDING'" type="warning" size="small" effect="plain">认证审核中</el-tag>
                  <el-tag v-else-if="userInfo?.verifyStatus === 'REJECTED'" type="danger" size="small" effect="plain">认证未通过</el-tag>
                  <el-tag
                    :type="userInfo?.role === 'SUPER_ADMIN' ? 'danger' : userInfo?.role === 'ADMIN' ? 'warning' : ''"
                    size="small" effect="plain"
                  >
                    {{ userInfo?.role === 'SUPER_ADMIN' ? '超级管理员' : userInfo?.role === 'ADMIN' ? '管理员' : '同学' }}
                  </el-tag>
                </div>
              </div>
            </div>
            <div class="banner-stats">
              <div class="bstat"><b>{{ myTotal }}</b><span>发布</span></div>
              <div class="bstat"><b>{{ favTotal }}</b><span>收藏</span></div>
              <div class="bstat"><b>{{ orderTotal }}</b><span>订单</span></div>
            </div>
          </div>

          <!-- 我的订单：整宽横排卡片 -->
          <div class="dash-card dash-orders">
            <div class="dash-head" @click="router.push('/orders')">我的订单<el-icon><ArrowRight /></el-icon></div>
            <div class="order-counts">
              <div v-for="s in orderStats" :key="s.key" class="oc" @click="router.push('/orders')">
                <b>{{ s.count }}</b><span>{{ s.label }}</span>
              </div>
            </div>
            <p class="orders-hint">当前暂无物流信息更新</p>
            <div class="dash-foot" @click="router.push('/orders')">查看全部订单<el-icon><ArrowRight /></el-icon></div>
          </div>

          <!-- 下方三卡：收藏/发布/购物车 -->
          <div class="dash-grid">
            <div class="dash-card">
              <div class="dash-head" @click="activeTab = 'favorites'">我的收藏<el-icon><ArrowRight /></el-icon></div>
              <div v-if="favorites[0]" class="preview" @click="router.push(`/product/${favorites[0].id}`)">
                <el-image :src="favorites[0].images?.[0]" fit="cover" class="preview-img" />
                <p class="preview-title">{{ favorites[0].title }}</p>
                <p class="preview-price">¥{{ favorites[0].price }}</p>
              </div>
              <div v-else class="preview-empty">还没有收藏</div>
            </div>
            <div class="dash-card">
              <div class="dash-head" @click="activeTab = 'products'">我的发布<el-icon><ArrowRight /></el-icon></div>
              <div v-if="myProducts[0]" class="preview" @click="router.push(`/product/${myProducts[0].id}`)">
                <el-image :src="myProducts[0].images?.[0]" fit="cover" class="preview-img" />
                <p class="preview-title">{{ myProducts[0].title }}</p>
                <p class="preview-price">¥{{ myProducts[0].price }}</p>
              </div>
              <div v-else class="preview-empty">还没有发布</div>
            </div>
            <div class="dash-card">
              <div class="dash-head" @click="router.push('/cart')">购物车<el-icon><ArrowRight /></el-icon></div>
              <div v-if="cartPreview" class="preview" @click="router.push('/cart')">
                <el-image :src="cartPreview.images?.[0]" fit="cover" class="preview-img" />
                <p class="preview-title">{{ cartPreview.title }}</p>
                <p class="preview-price">¥{{ cartPreview.price }}</p>
              </div>
              <div v-else class="preview-empty">购物车是空的</div>
            </div>
          </div>
        </template>
        <!-- 个人资料 -->
        <div v-if="activeTab === 'info'" class="content-card">
          <h2 class="content-title">
            <el-icon :size="20"><User /></el-icon>
            编辑资料
          </h2>
          <p class="content-desc">完善你的个人资料，让交易伙伴更了解你</p>
          <el-form :model="profileForm" label-width="70px" class="profile-form">
            <el-form-item label="昵称">
              <el-input v-model="profileForm.nickname" placeholder="请输入昵称" maxlength="20" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveProfile">保存修改</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 我的发布 -->
        <div v-if="activeTab === 'products'" class="content-card">
          <h2 class="content-title">
            <el-icon :size="20"><Goods /></el-icon>
            我的发布
          </h2>
          <div v-if="myProducts.length" class="profile-grid">
            <div v-for="item in myProducts" :key="item.id" class="profile-card">
              <ProductCard :product="item" />
              <div class="product-actions">
                <el-button size="small" :icon="Edit" @click="router.push({ path: '/publish', query: { id: item.id } })">编辑</el-button>
                <el-button size="small" type="warning" plain :icon="Hide" @click="handleOffShelf(item.id)">下架</el-button>
                <el-button size="small" type="danger" plain :icon="Delete" @click="handleDelete(item.id)">删除</el-button>
              </div>
            </div>
          </div>
          <div v-else class="empty-panel">
            <el-icon :size="40"><Goods /></el-icon>
            <p>还没有发布过商品</p>
            <el-button type="primary" @click="router.push('/publish')">去发布</el-button>
          </div>
          <div class="profile-pager" v-if="myTotal > 8">
            <el-pagination v-model:current-page="myPage" :page-size="8" :total="myTotal" layout="prev, pager, next" background small @change="loadMyProducts" />
          </div>
        </div>

        <!-- 我的收藏 -->
        <div v-if="activeTab === 'favorites'" class="content-card">
          <h2 class="content-title">
            <el-icon :size="20"><Star /></el-icon>
            我的收藏
          </h2>
          <div class="fav-toolbar">
            <el-input
              v-model="favoriteKeyword"
              placeholder="搜索收藏的商品"
              clearable
              class="fav-search"
              @keyup.enter="loadFavorites"
              @clear="loadFavorites"
            >
              <template #append>
                <el-button :icon="Search" @click="loadFavorites" />
              </template>
            </el-input>
            <el-button v-if="favSelected.length" type="danger" plain @click="handleBatchUnfavorite">
              批量取消收藏（{{ favSelected.length }}）
            </el-button>
          </div>
          <div v-if="favorites.length" class="profile-grid">
            <div v-for="item in favorites" :key="item.id" class="profile-card">
              <el-checkbox
                class="fav-check"
                :model-value="favSelected.includes(item.id)"
                @change="toggleFavSelect(item.id)"
              />
              <ProductCard :product="item" />
            </div>
          </div>
          <div v-else class="empty-panel">
            <el-icon :size="40"><Star /></el-icon>
            <p>还没有收藏商品</p>
            <el-button type="primary" @click="router.push('/products')">去逛逛</el-button>
          </div>
        </div>

        <!-- 收货地址 -->
        <div v-if="activeTab === 'address'" class="content-card">
          <h2 class="content-title">
            <el-icon :size="20"><Location /></el-icon>
            收货地址
          </h2>
          <p class="content-desc">管理你的收货地址，结算时可快速选择</p>
          <div class="address-toolbar">
            <el-button type="primary" :icon="Plus" @click="openAddressDialog()">新增地址</el-button>
          </div>
          <el-table v-if="addresses.length" :data="addresses" stripe>
            <el-table-column prop="receiverName" label="收货人" width="120" />
            <el-table-column prop="phone" label="手机号" width="140" />
            <el-table-column prop="address" label="收货地址" min-width="200" show-overflow-tooltip />
            <el-table-column label="默认" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.isDefault === 1" type="success" size="small">默认</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="230">
              <template #default="{ row }">
                <el-button size="small" @click="openAddressDialog(row)">编辑</el-button>
                <el-button v-if="row.isDefault !== 1" size="small" type="primary" plain @click="handleSetDefault(row.id)">设默认</el-button>
                <el-button size="small" type="danger" plain @click="handleDeleteAddress(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-else class="empty-panel">
            <el-icon :size="40"><Location /></el-icon>
            <p>还没有收货地址</p>
            <el-button type="primary" @click="openAddressDialog()">新增地址</el-button>
          </div>
        </div>

        <!-- 修改密码 -->
        <div v-if="activeTab === 'password'" class="content-card">
          <h2 class="content-title">
            <el-icon :size="20"><Lock /></el-icon>
            修改密码
          </h2>
          <p class="content-desc">请妥善保管你的密码，不要与他人共享</p>
          <el-form :model="pwdForm" label-width="90px" class="profile-form">
            <el-form-item label="原密码">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码（6-20位）" />
            </el-form-item>
            <el-form-item label="确认新密码">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="changePassword">确认修改</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 危险操作 -->
        <div v-if="activeTab === 'danger'" class="content-card">
          <h2 class="content-title">
            <el-icon :size="20"><Warning /></el-icon>
            注销账号
          </h2>
          <div class="danger-zone">
            <div class="danger-item">
              <div class="danger-info">
                <h4>注销账号</h4>
                <p>注销后你将无法登录，已发布的商品和订单记录将保留在平台。</p>
              </div>
              <el-button type="danger" plain size="small" @click="showDeactivateDialog = true">
                注销账号
              </el-button>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 注销确认对话框 -->
    <el-dialog v-model="showDeactivateDialog" title="确认注销账号" width="420" :close-on-click-modal="false">
      <div class="deactivate-dialog-body">
        <el-alert type="error" :closable="false" show-icon style="margin-bottom: 16px">
          此操作不可自行恢复！注销后你将无法使用此账号登录。
        </el-alert>
        <el-form label-width="70px">
          <el-form-item label="密码确认">
            <el-input v-model="deactivatePassword" type="password" show-password placeholder="请输入当前密码" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="showDeactivateDialog = false; deactivatePassword = ''">取消</el-button>
        <el-button type="danger" :loading="deactivating" @click="handleDeactivate">确认注销</el-button>
      </template>
    </el-dialog>
    <!-- 地址编辑弹窗 -->
    <el-dialog v-model="addressDialogVisible" :title="addressForm.id ? '编辑地址' : '新增地址'" width="440px" :close-on-click-modal="false">
      <el-form ref="addressFormRef" :model="addressForm" :rules="addressRules" label-width="80px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="addressForm.receiverName" placeholder="请输入收货人姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addressForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="详细地址" prop="address">
          <el-input v-model="addressForm.address" type="textarea" :rows="2" placeholder="宿舍楼/快递点" maxlength="200" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="addressForm.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addressDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="addressSaving" @click="saveAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// 个人中心页：维护资料、密码、头像、收藏、地址、我的商品和账号注销。
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Hide, Delete, Plus, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { updateProfile, updatePassword, deactivateAccount } from '@/api/user'
import { getMyProducts, deleteProduct, offShelfProduct } from '@/api/product'
import { getFavoriteList, removeFavoriteBatch } from '@/api/favorite'
import { getAddressList, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '@/api/address'
import { getOrderStatusCounts } from '@/api/order'
import { getCartList } from '@/api/cart'
import { uploadFile } from '@/api/file'
import ProductCard from '@/components/ProductCard.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
// 支持顶栏图标入口通过 /profile?tab=xxx 直接定位到对应页签
const TAB_KEYS = ['home', 'info', 'products', 'favorites', 'address']
const activeTab = ref(TAB_KEYS.includes(route.query.tab) ? route.query.tab : 'home')
watch(() => route.query.tab, t => {
  if (TAB_KEYS.includes(t)) activeTab.value = t
})
const userInfo = ref(null)
const myProducts = ref([])
const favorites = ref([])
const myTotal = ref(0)
const favTotal = ref(0)
const myPage = ref(1)
const uploading = ref(false)
const fileInput = ref(null)

// 首页概览：订单状态计数与购物车预览
const orderCounts = ref({})
const cartPreview = ref(null)
const orderTotal = computed(() => orderCounts.value.TOTAL || 0)
const orderStats = computed(() => [
  { key: 'PENDING', label: '待付款', count: orderCounts.value.PENDING || 0 },
  { key: 'PAID', label: '待发货', count: orderCounts.value.PAID || 0 },
  { key: 'SHIPPED', label: '待收货', count: orderCounts.value.SHIPPED || 0 },
  { key: 'COMPLETED', label: '已完成', count: orderCounts.value.COMPLETED || 0 },
  { key: 'REFUND', label: '退款/售后', count: orderCounts.value.REFUND || 0 }
])

/** 加载个人中心统计数据。 */
async function loadOverview() {
  try {
    const [oc, cc] = await Promise.all([getOrderStatusCounts(), getCartList()])
    orderCounts.value = oc.data || {}
    cartPreview.value = (cc.data || []).filter(i => !i.invalid)[0] || null
  } catch { /* 概览加载失败不影响主流程 */ }
}

const navItems = [
  { key: 'home', label: '个人首页', icon: 'HomeFilled' },
  { key: 'info', label: '个人资料', icon: 'User' },
  { key: 'products', label: '我的发布', icon: 'Goods' },
  { key: 'favorites', label: '我的收藏', icon: 'Star' },
  { key: 'address', label: '收货地址', icon: 'Location' },
  { key: 'password', label: '修改密码', icon: 'Lock' },
  { key: 'danger', label: '注销账号', icon: 'Warning' }
]

const avatarUrl = computed(() => userInfo.value?.avatar || '')

/** 打开系统文件选择器。 */
function triggerUpload() { fileInput.value?.click() }

/** 上传并更新头像。 */
async function handleAvatarChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) { ElMessage.warning('头像不能超过 5MB'); return }
  uploading.value = true
  try {
    const res = await uploadFile(file)
    const url = res.data
    await updateProfile({ avatar: url })
    ElMessage.success('头像已更新')
    loadData()
  } finally { uploading.value = false; e.target.value = '' }
}

const profileForm = reactive({ nickname: '', phone: '', email: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

/** 加载个人中心所有基础数据。 */
async function loadData() {
  userInfo.value = await userStore.fetchUserInfo()
  Object.assign(profileForm, { nickname: userInfo.value.nickname, phone: userInfo.value.phone, email: userInfo.value.email })
  await loadFavorites()
  await loadMyProducts()
  loadAddresses()
  loadOverview()
}

// ====== 收藏搜索 + 批量取消 ======
const favoriteKeyword = ref('')
const favSelected = ref([])

/** 分页加载收藏商品。 */
async function loadFavorites() {
  const favRes = await getFavoriteList({ pageNum: 1, pageSize: 20, keyword: favoriteKeyword.value || undefined })
  favorites.value = favRes.data.records
  favTotal.value = favRes.data.total || favRes.data.records.length
  favSelected.value = favSelected.value.filter(id => favorites.value.some(p => p.id === id))
}

/** 切换收藏批量选择状态。 */
function toggleFavSelect(id) {
  const idx = favSelected.value.indexOf(id)
  if (idx >= 0) favSelected.value.splice(idx, 1)
  else favSelected.value.push(id)
}

/** 批量取消选中的收藏。 */
async function handleBatchUnfavorite() {
  await ElMessageBox.confirm(`确认取消收藏选中的 ${favSelected.value.length} 件商品？`, '提示', { type: 'warning' })
  await removeFavoriteBatch(favSelected.value)
  ElMessage.success('已取消收藏')
  favSelected.value = []
  loadFavorites()
}

// ====== 收货地址 ======
const addresses = ref([])
const addressDialogVisible = ref(false)
const addressSaving = ref(false)
const addressFormRef = ref(null)
const addressForm = reactive({ id: null, receiverName: '', phone: '', address: '', isDefault: 0 })
const addressRules = {
  receiverName: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

/** 加载收货地址列表。 */
async function loadAddresses() {
  const res = await getAddressList()
  addresses.value = res.data
}

/** 打开地址新增/编辑弹窗。 */
function openAddressDialog(row) {
  if (row) {
    Object.assign(addressForm, { id: row.id, receiverName: row.receiverName, phone: row.phone, address: row.address, isDefault: row.isDefault })
  } else {
    Object.assign(addressForm, { id: null, receiverName: '', phone: '', address: '', isDefault: 0 })
  }
  addressDialogVisible.value = true
}

/** 保存收货地址。 */
async function saveAddress() {
  await addressFormRef.value.validate()
  addressSaving.value = true
  try {
    if (addressForm.id) {
      await updateAddress(addressForm.id, addressForm)
    } else {
      await addAddress(addressForm)
    }
    ElMessage.success('保存成功')
    addressDialogVisible.value = false
    loadAddresses()
  } finally { addressSaving.value = false }
}

/** 删除收货地址。 */
async function handleDeleteAddress(id) {
  await ElMessageBox.confirm('确认删除该地址？', '提示', { type: 'warning' })
  await deleteAddress(id)
  ElMessage.success('删除成功')
  loadAddresses()
}

/** 设置默认收货地址。 */
async function handleSetDefault(id) {
  await setDefaultAddress(id)
  ElMessage.success('已设为默认地址')
  loadAddresses()
}

/** 分页加载我发布的商品。 */
async function loadMyProducts() {
  const res = await getMyProducts({ pageNum: myPage.value, pageSize: 8 })
  myProducts.value = res.data.records
  myTotal.value = res.data.total
}

/** 保存个人资料。 */
async function saveProfile() {
  await updateProfile(profileForm)
  ElMessage.success('保存成功')
  loadData()
}

/** 校验旧密码并修改登录密码。 */
async function changePassword() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword || !pwdForm.confirmPassword) {
    ElMessage.warning('请填写完整')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }
  if (pwdForm.newPassword.length < 6 || pwdForm.newPassword.length > 20) {
    ElMessage.warning('新密码长度需为6-20位')
    return
  }
  await updatePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
  ElMessage.success('密码修改成功')
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
}

/** 下架我发布的商品。 */
async function handleOffShelf(id) {
  await offShelfProduct(id)
  ElMessage.success('已下架')
  loadMyProducts()
}

/** 删除我发布的商品。 */
async function handleDelete(id) {
  await ElMessageBox.confirm('确认删除该商品？', '提示', { type: 'warning' })
  await deleteProduct(id)
  ElMessage.success('删除成功')
  loadMyProducts()
}

const showDeactivateDialog = ref(false)
const deactivatePassword = ref('')
const deactivating = ref(false)

/** 校验密码并注销当前账号。 */
async function handleDeactivate() {
  if (!deactivatePassword.value) { ElMessage.warning('请输入密码'); return }
  deactivating.value = true
  try {
    await deactivateAccount({ password: deactivatePassword.value })
    ElMessage.success('账号已注销')
    showDeactivateDialog.value = false
    deactivatePassword.value = ''
    userStore.logout()
    router.push('/login')
  } catch (e) {
    // request.js 已处理错误提示
  } finally {
    deactivating.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
/* ====== 左侧栏 ====== */
.side-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  margin-bottom: 16px;
  position: sticky; top: 84px;
}

/* ====== 个人首页横幅 ====== */
.banner-card {
  display: flex; align-items: center; justify-content: space-between; gap: 20px;
  background: linear-gradient(120deg, #ECFDF5, #F0FDF4 55%, #FFF7ED);
  border: 1px solid #E7F6EF;
  border-radius: 16px; padding: 24px 28px; margin-bottom: 16px;
}
.banner-left { display: flex; align-items: center; gap: 18px; min-width: 0; }
.banner-id h2 { margin: 0 0 6px; font-size: 20px; font-weight: 800; color: #1F2937; }
.banner-sub { margin: 0 0 8px; font-size: 13px; color: #6B7280; }
.banner-tags { display: flex; gap: 8px; flex-wrap: wrap; }
.banner-stats { display: flex; gap: 28px; flex-shrink: 0; }
.bstat { display: flex; flex-direction: column; align-items: center; gap: 2px; }
.bstat b { font-size: 22px; font-weight: 800; color: #10B981; font-variant-numeric: tabular-nums; }
.bstat span { font-size: 12px; color: #6B7280; }

/* ====== 仪表盘卡片 ====== */
.dash-orders { margin-bottom: 16px; min-height: 0; }
.dash-orders .order-counts { margin: 18px 0 8px; }
.orders-hint {
  text-align: center; font-size: 14px; font-weight: 700; color: #374151;
  margin: 18px 0 6px;
}
.dash-orders .dash-foot { border-top: none; padding-top: 4px; display: flex; align-items: center; justify-content: center; gap: 2px; }
.dash-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.dash-card {
  background: #fff; border: 1px solid #F3F4F6; border-radius: 14px;
  padding: 16px 18px; min-height: 220px;
  display: flex; flex-direction: column;
}
.dash-head {
  display: flex; align-items: center; gap: 4px;
  font-size: 15px; font-weight: 800; color: #1F2937; cursor: pointer;
  margin-bottom: 14px;
}
.dash-head:hover { color: #10B981; }
.order-counts { display: flex; justify-content: space-around; margin: 6px 0 12px; }
.oc { display: flex; flex-direction: column; align-items: center; gap: 4px; cursor: pointer; }
.oc b { font-size: 20px; font-weight: 800; color: #1F2937; font-variant-numeric: tabular-nums; }
.oc:hover b { color: #10B981; }
.oc span { font-size: 12px; color: #6B7280; }
.dash-foot {
  margin-top: auto; text-align: center; font-size: 13px; color: #9CA3AF;
  cursor: pointer; padding-top: 10px; border-top: 1px solid #F3F4F6;
}
.dash-foot:hover { color: #10B981; }
.preview { display: flex; flex-direction: column; align-items: center; cursor: pointer; min-width: 0; }
.preview-img { width: 96px; height: 96px; border-radius: 10px; }
.preview-title {
  margin: 10px 0 2px; max-width: 100%; font-size: 13px; color: #374151;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.preview:hover .preview-title { color: #10B981; }
.preview-price { margin: 0; font-size: 14px; font-weight: 700; color: #F59E0B; }
.preview-empty {
  flex: 1; display: flex; align-items: center; justify-content: center;
  color: #C4C8CE; font-size: 13px;
}

.avatar-upload {
  position: relative; display: inline-block; cursor: pointer; border-radius: 50%;
  z-index: 1;
}
.avatar-upload.uploading { pointer-events: none; opacity: 0.5; }
.avatar-upload :deep(.el-avatar) {
  border: 3px solid #fff; box-shadow: 0 4px 16px rgba(16,185,129,0.18);
}
.avatar-mask {
  position: absolute; inset: 0; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,0.35); color: #fff; opacity: 0; transition: opacity 0.2s;
}
.avatar-upload:hover .avatar-mask { opacity: 1; }

/* 导航 */
.side-nav { padding: 8px 0; }
.nav-item {
  display: flex; align-items: center; gap: 10px;
  padding: 13px 20px; cursor: pointer;
  font-size: 14px; color: #4B5563;
  transition: all 0.2s ease;
  border-left: 3px solid transparent;
  position: relative;
}
.nav-item:hover { background: #F9FAFB; color: #10B981; }
.nav-item.active {
  background: #ECFDF5; color: #059669; font-weight: 600;
  border-left-color: #10B981;
  padding-left: calc(20px + 0px);
}

/* ====== 右侧内容区 ====== */
.content-card {
  background: #fff; border-radius: 16px; padding: 28px 32px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  min-height: 360px;
}

.content-title {
  font-size: 20px; font-weight: 700; color: #1F2937; margin: 0 0 6px;
  display: flex; align-items: center; gap: 8px;
}
.content-title .el-icon { color: #10B981; }
.content-desc {
  font-size: 13px; color: #9CA3AF; margin: 0 0 24px;
}

.profile-form { max-width: 440px; }

/* 商品网格 */
.profile-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  align-items: start;
}

.product-actions {
  display: flex; flex-wrap: wrap; gap: 6px;
  margin-top: 8px; padding: 8px;
  justify-content: center;
  background: #F9FAFB; border-radius: 0 0 8px 8px;
}

/* 收藏工具栏 */
.fav-toolbar {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 16px;
}
.fav-search { max-width: 280px; }
.profile-card { min-width: 0; position: relative; }
.fav-check {
  position: absolute; top: 8px; right: 8px; z-index: 2;
  background: rgba(255,255,255,0.9);
  border-radius: 6px;
  padding: 2px 6px;
}

/* 地址工具栏 */
.address-toolbar { margin-bottom: 16px; }

/* 空面板 */
.empty-panel {
  text-align: center; padding: 48px 20px;
}
.empty-panel .el-icon { color: #D1D5DB; margin-bottom: 12px; }
.empty-panel p { color: #9CA3AF; font-size: 14px; margin: 0 0 16px; }

/* 分页 */
.profile-pager {
  display: flex; justify-content: center;
  margin-top: 28px;
}

/* ====== 危险操作区 ====== */
.danger-zone {
  border: 1px solid #FCA5A5;
  border-radius: 10px;
  padding: 0;
  overflow: hidden;
}
.danger-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  gap: 16px;
}
.danger-info h4 {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 600;
  color: #991B1B;
}
.danger-info p {
  margin: 0;
  font-size: 13px;
  color: #6B7280;
  line-height: 1.5;
}

/* ====== 响应式 ====== */
@media (max-width: 1024px) {
  .profile-grid { grid-template-columns: repeat(3, 1fr); }
  .order-counts { flex-wrap: wrap; gap: 12px; }
}
@media (max-width: 768px) {
  .profile-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .content-card { padding: 20px 16px; }
  .banner-card { flex-direction: column; align-items: flex-start; gap: 14px; padding: 18px; }
  .banner-stats { width: 100%; justify-content: space-around; }
  .dash-grid { grid-template-columns: 1fr; }
}
/* 手机端：侧栏导航改顶部横向滚动 */
@media (max-width: 480px) {
  .side-card { position: static; margin-bottom: 12px; }
  .side-header { padding: 20px 14px 16px; display: flex; flex-direction: column; align-items: center; }
  .side-nav {
    display: flex;
    overflow-x: auto;
    padding: 4px 8px;
    gap: 4px;
    -webkit-overflow-scrolling: touch;
  }
  .nav-item {
    flex-shrink: 0;
    border-left: none;
    border-bottom: 3px solid transparent;
    border-radius: 8px 8px 0 0;
    padding: 10px 14px;
    font-size: 13px;
  }
  .nav-item.active {
    border-left-color: transparent;
    border-bottom-color: #10B981;
    padding-left: 14px;
  }
  .profile-grid { grid-template-columns: repeat(2, 1fr); gap: 10px; }
  .content-card { padding: 16px 14px; }
  .content-title { font-size: 18px; }
  .profile-form { max-width: 100%; }
}
</style>
