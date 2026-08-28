<template>
  <div class="profile page-container">
    <el-row :gutter="20">
      <!-- 左侧栏 -->
      <el-col :xs="24" :sm="8" :md="7" :lg="6">
        <div class="side-card">
          <div class="side-header">
            <svg class="header-decor" viewBox="0 0 200 80" preserveAspectRatio="none">
              <circle cx="100" cy="120" r="90" fill="none" stroke="rgba(255,255,255,0.08)" stroke-width="1.5" />
              <circle cx="100" cy="120" r="70" fill="none" stroke="rgba(255,255,255,0.06)" stroke-width="1" />
              <circle cx="100" cy="120" r="50" fill="none" stroke="rgba(255,255,255,0.05)" stroke-width="0.5" />
            </svg>
            <div class="avatar-upload" @click="triggerUpload" :class="{ uploading }">
              <el-avatar :size="76" :src="avatarUrl">{{ userInfo?.nickname?.[0] }}</el-avatar>
              <div class="avatar-mask"><el-icon :size="22"><Camera /></el-icon></div>
            </div>
            <input ref="fileInput" type="file" accept="image/*" hidden @change="handleAvatarChange" />
            <h3>{{ userInfo?.nickname }}</h3>
            <p class="side-role">
              <el-tag :type="userInfo?.role === 'ADMIN' ? 'danger' : ''" size="small" effect="plain">
                {{ userInfo?.role === 'ADMIN' ? '管理员' : '同学' }}
              </el-tag>
            </p>
          </div>

          <div class="side-stats">
            <div class="stat-row">
              <el-icon :size="18"><Goods /></el-icon>
              <span>已发布 <strong>{{ myTotal }}</strong> 件</span>
            </div>
            <div class="stat-row">
              <el-icon :size="18"><Star /></el-icon>
              <span>已收藏 <strong>{{ favorites.length }}</strong> 件</span>
            </div>
          </div>

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
      <el-col :xs="24" :sm="16" :md="17" :lg="18">
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
          <el-form :model="pwdForm" label-width="80px" class="profile-form">
            <el-form-item label="原密码">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码（6-20位）" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Hide, Delete, Plus, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { updateProfile, updatePassword, deactivateAccount } from '@/api/user'
import { getMyProducts, deleteProduct, offShelfProduct } from '@/api/product'
import { getFavoriteList, removeFavoriteBatch } from '@/api/favorite'
import { getAddressList, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '@/api/address'
import { uploadFile } from '@/api/file'
import ProductCard from '@/components/ProductCard.vue'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('info')
const userInfo = ref(null)
const myProducts = ref([])
const favorites = ref([])
const myTotal = ref(0)
const myPage = ref(1)
const uploading = ref(false)
const fileInput = ref(null)

const navItems = [
  { key: 'info', label: '个人资料', icon: 'User' },
  { key: 'products', label: '我的发布', icon: 'Goods' },
  { key: 'favorites', label: '我的收藏', icon: 'Star' },
  { key: 'address', label: '收货地址', icon: 'Location' },
  { key: 'password', label: '修改密码', icon: 'Lock' },
  { key: 'danger', label: '危险操作', icon: 'Warning' }
]

const avatarUrl = computed(() => userInfo.value?.avatar || '')

function triggerUpload() { fileInput.value?.click() }

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
const pwdForm = reactive({ oldPassword: '', newPassword: '' })

async function loadData() {
  userInfo.value = await userStore.fetchUserInfo()
  Object.assign(profileForm, { nickname: userInfo.value.nickname, phone: userInfo.value.phone, email: userInfo.value.email })
  await loadFavorites()
  await loadMyProducts()
  loadAddresses()
}

// ====== 收藏搜索 + 批量取消 ======
const favoriteKeyword = ref('')
const favSelected = ref([])

async function loadFavorites() {
  const favRes = await getFavoriteList({ pageNum: 1, pageSize: 20, keyword: favoriteKeyword.value || undefined })
  favorites.value = favRes.data.records
  favSelected.value = favSelected.value.filter(id => favorites.value.some(p => p.id === id))
}

function toggleFavSelect(id) {
  const idx = favSelected.value.indexOf(id)
  if (idx >= 0) favSelected.value.splice(idx, 1)
  else favSelected.value.push(id)
}

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

async function loadAddresses() {
  const res = await getAddressList()
  addresses.value = res.data
}

function openAddressDialog(row) {
  if (row) {
    Object.assign(addressForm, { id: row.id, receiverName: row.receiverName, phone: row.phone, address: row.address, isDefault: row.isDefault })
  } else {
    Object.assign(addressForm, { id: null, receiverName: '', phone: '', address: '', isDefault: 0 })
  }
  addressDialogVisible.value = true
}

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

async function handleDeleteAddress(id) {
  await ElMessageBox.confirm('确认删除该地址？', '提示', { type: 'warning' })
  await deleteAddress(id)
  ElMessage.success('删除成功')
  loadAddresses()
}

async function handleSetDefault(id) {
  await setDefaultAddress(id)
  ElMessage.success('已设为默认地址')
  loadAddresses()
}

async function loadMyProducts() {
  const res = await getMyProducts({ pageNum: myPage.value, pageSize: 8 })
  myProducts.value = res.data.records
  myTotal.value = res.data.total
}

async function saveProfile() {
  await updateProfile(profileForm)
  ElMessage.success('保存成功')
  loadData()
}

async function changePassword() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) { ElMessage.warning('请填写完整'); return }
  await updatePassword(pwdForm)
  ElMessage.success('密码修改成功')
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
}

async function handleOffShelf(id) {
  await offShelfProduct(id)
  ElMessage.success('已下架')
  loadMyProducts()
}

async function handleDelete(id) {
  await ElMessageBox.confirm('确认删除该商品？', '提示', { type: 'warning' })
  await deleteProduct(id)
  ElMessage.success('删除成功')
  loadMyProducts()
}

const showDeactivateDialog = ref(false)
const deactivatePassword = ref('')
const deactivating = ref(false)

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

.side-header {
  background: linear-gradient(165deg, #059669 0%, #10B981 40%, #34D399 100%);
  padding: 32px 16px 24px;
  text-align: center;
  color: #fff;
  position: relative;
  overflow: hidden;
}
.side-header::after {
  content: ''; position: absolute; inset: 0;
  background: radial-gradient(circle at 30% 80%, rgba(255,255,255,0.1) 0%, transparent 50%);
}
.header-decor {
  position: absolute; top: -40px; left: 50%; transform: translateX(-50%);
  width: 200px; height: 80px; z-index: 0; pointer-events: none;
}

.avatar-upload {
  position: relative; display: inline-block; cursor: pointer; border-radius: 50%;
  z-index: 1;
}
.avatar-upload.uploading { pointer-events: none; opacity: 0.5; }
.avatar-upload :deep(.el-avatar) {
  border: 3px solid rgba(255,255,255,0.5); box-shadow: 0 4px 16px rgba(0,0,0,0.15);
}
.avatar-mask {
  position: absolute; inset: 0; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,0.35); color: #fff; opacity: 0; transition: opacity 0.2s;
}
.avatar-upload:hover .avatar-mask { opacity: 1; }

.side-header h3 { position: relative; z-index: 1; margin: 12px 0 6px; font-size: 18px; font-weight: 700; }
.side-role { position: relative; z-index: 1; margin: 0; }
.side-role :deep(.el-tag) {
  font-weight: 500; border-color: rgba(255,255,255,0.4); color: #fff;
  background: rgba(255,255,255,0.15); backdrop-filter: blur(4px);
}
.side-role :deep(.el-tag--danger) {
  background: rgba(239,68,68,0.2); border-color: rgba(239,68,68,0.3);
}

/* 统计行 */
.side-stats {
  padding: 14px 20px;
  display: flex; justify-content: space-around;
  background: #FAFAFA;
  border-top: 1px solid #F3F4F6;
  border-bottom: 1px solid #F3F4F6;
}
.stat-row {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; color: #6B7280;
}
.stat-row strong { color: #1F2937; font-variant-numeric: tabular-nums; }

/* 导航 */
.side-nav { padding: 4px 0; }
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
}
@media (max-width: 768px) {
  .profile-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .content-card { padding: 20px 16px; }
  .side-stats { padding: 12px 10px; }
  .stat-row { font-size: 12px; }
  .side-header { padding: 24px 12px 18px; }
  .side-header h3 { font-size: 16px; }
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
  .profile-grid { grid-template-columns: 1fr; }
  .content-card { padding: 16px 14px; }
  .content-title { font-size: 18px; }
  .profile-form { max-width: 100%; }
}
</style>
