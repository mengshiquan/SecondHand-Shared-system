<template>
  <header class="header" :class="{ 'header-blacklisted': blacklisted || disabled }">
    <div v-if="blacklisted || disabled" class="blacklist-bar">
      <div class="blacklist-bar-inner page-container">
        <span v-if="disabled">
          <el-icon><WarningFilled /></el-icon>
          你的账号已被禁用，无法发布、购买、评论和投诉，如有异议可提交申诉
        </span>
        <span v-else>
          <el-icon><WarningFilled /></el-icon>
          {{ blacklistInfo.status === 'AUTO' ? '系统检测到违规行为，' : '管理员已限制' }}你的账号已被限制使用
          <template v-if="blacklistInfo.until">，解封时间：{{ formatUntil(blacklistInfo.until) }}</template>
        </span>
        <el-button size="small" type="warning" plain @click="showAppealDialog = true">
          <el-icon><EditPen /></el-icon>申诉
        </el-button>
      </div>
    </div>
    <div class="header-inner page-container">
      <div class="logo" @click="router.push('/')">
        <img src="/logo-horizontal.png" alt="校园物品" class="logo-img">
      </div>

      <div class="search-box">
        <el-input
          v-model="keyword"
          placeholder="搜索闲置好物..."
          clearable
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" />
          </template>
        </el-input>
      </div>

      <nav class="nav-links">
        <router-link to="/" class="hide-tablet">首页</router-link>
        <router-link to="/products" class="hide-tablet">商品</router-link>
        <el-dropdown trigger="hover" class="hide-tablet">
          <span class="nav-more">更多 <el-icon :size="14"><ArrowDown /></el-icon></span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/about')">
                <el-icon><InfoFilled /></el-icon>关于我们
              </el-dropdown-item>
              <el-dropdown-item @click="showContact = true">
                <el-icon><ChatDotRound /></el-icon>联系我们
              </el-dropdown-item>
              <el-dropdown-item @click="router.push('/help')">
                <el-icon><QuestionFilled /></el-icon>帮助中心
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <!-- 聊天入口（手机端同样展示，保证移动端有聊天入口） -->
        <span v-if="userStore.isLoggedIn" class="bell-wrap" title="消息" @click="router.push('/chat')">
          <el-icon :size="22"><ChatLineSquare /></el-icon>
          <span v-if="chatStore.unread > 0" class="bell-badge">{{ chatStore.unread > 99 ? '99+' : chatStore.unread }}</span>
        </span>

        <!-- 通知铃铛 -->
        <el-popover
          v-if="userStore.isLoggedIn"
          trigger="click"
          :width="280"
          placement="bottom-end"
          @show="refreshNotifications"
          @hide="userStore.isAdmin ? dismissNotifications() : null"
        >
          <template #reference>
            <span class="bell-wrap bell-notify">
              <el-icon :size="22"><Bell /></el-icon>
              <span v-if="badgeCount > 0" class="bell-badge">{{ badgeCount > 99 ? '99+' : badgeCount }}</span>
            </span>
          </template>
          <!-- 管理员内容 -->
          <div v-if="userStore.isAdmin" class="notify-list">
            <div class="notify-title">待处理通知</div>
            <div class="notify-item" @click="dismissNotifications(); router.push('/admin'); goAdminTab('reports')">
              <span>新投诉</span>
              <el-tag size="small" :type="notify.pendingComplaints > 0 ? 'danger' : 'info'">{{ notify.pendingComplaints }}</el-tag>
            </div>
            <div class="notify-item" @click="dismissNotifications(); router.push('/admin'); goAdminTab('reports')">
              <span>新申诉</span>
              <el-tag size="small" :type="notify.pendingAppeals > 0 ? 'danger' : 'info'">{{ notify.pendingAppeals }}</el-tag>
            </div>
            <div class="notify-item" @click="dismissNotifications(); router.push('/admin'); goAdminTab('blacklist')">
              <span>小黑屋</span>
              <el-tag size="small" :type="notify.blacklistCount > 0 ? 'warning' : 'info'">{{ notify.blacklistCount }}</el-tag>
            </div>
            <div class="notify-item" @click="dismissNotifications(); router.push('/admin'); goAdminTab('ordersArb')">
              <span>新仲裁</span>
              <el-tag size="small" :type="notify.pendingArbitrations > 0 ? 'danger' : 'info'">{{ notify.pendingArbitrations }}</el-tag>
            </div>
          </div>
          <!-- 个人消息（普通用户与管理员均可见，管理员同时是卖家/买家） -->
          <div v-if="userStore.isLoggedIn" class="notify-list" :class="{ 'notify-list-second': userStore.isAdmin }">
            <div class="notify-title">消息通知</div>
            <div v-if="userNotifications.length === 0" style="text-align:center;color:#9CA3AF;padding:16px 0">暂无通知</div>
            <div
              v-for="n in userNotifications.slice(0, 5)"
              :key="n.id"
              class="notify-item"
              :class="{ unread: !n.isRead }"
              @click="goNotifications()"
            >
              <div class="notify-item-text">
                <span class="notify-item-title">{{ n.title }}</span>
                <span class="notify-item-time">{{ n.createTime?.replace('T',' ').substring(0,16) }}</span>
              </div>
            </div>
            <div v-if="userNotifications.length > 0" class="notify-footer" @click="goNotifications()">
              查看全部{{ userUnreadCount > 0 ? ` (${userUnreadCount} 条未读)` : '' }}
            </div>
          </div>
        </el-popover>

        <!-- 购物车入口（登录后可见，带数量角标） -->
        <span v-if="userStore.isLoggedIn" class="bell-wrap" @click="router.push('/cart')">
          <el-icon :size="22"><ShoppingCart /></el-icon>
          <span v-if="cartCount > 0" class="bell-badge">{{ cartCount > 99 ? '99+' : cartCount }}</span>
        </span>

        <!-- 我的收藏入口（仅图标） -->
        <span v-if="userStore.isLoggedIn" class="bell-wrap hide-mobile" title="我的收藏" @click="router.push('/profile?tab=favorites')">
          <el-icon :size="22"><Star /></el-icon>
        </span>

        <!-- 收货地址入口（仅图标） -->
        <span v-if="userStore.isLoggedIn" class="bell-wrap hide-mobile" title="收货地址" @click="router.push('/profile?tab=address')">
          <el-icon :size="22"><Location /></el-icon>
        </span>

        <el-button v-if="userStore.isLoggedIn" type="primary" class="hide-tablet" @click="router.push('/publish')">
          <el-icon><Plus /></el-icon>发布
        </el-button>
        <template v-if="userStore.isLoggedIn">
          <el-dropdown>
            <span class="user-dropdown">
              <el-avatar :size="32" :src="userStore.userInfo?.avatar">
                {{ userStore.userInfo?.nickname?.[0] }}
              </el-avatar>
              <span class="nickname">{{ userStore.userInfo?.nickname }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item @click="router.push('/orders')">
                  我的订单
                  <span v-if="pendingOrderCount > 0" class="menu-badge">{{ pendingOrderCount > 99 ? '99+' : pendingOrderCount }}</span>
                </el-dropdown-item>
                <el-dropdown-item @click="router.push('/notifications')">
                  消息通知
                  <span v-if="userUnreadCount > 0" class="menu-badge">{{ userUnreadCount }}</span>
                </el-dropdown-item>
                <el-dropdown-item v-if="userStore.isAdmin" @click="router.push('/admin')">
                  后台管理
                  <span v-if="notifyTotal > 0" class="menu-badge">{{ notifyTotal }}</span>
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button class="hide-tablet" @click="router.push('/login')">登录</el-button>
          <el-button class="hide-tablet" type="primary" @click="router.push('/register')">注册</el-button>
          <el-button class="show-mobile" @click="router.push('/login')">登录</el-button>
        </template>

        <!-- 手机端汉堡菜单按钮 -->
        <span class="show-mobile hamburger-btn" @click="mobileMenuOpen = true">
          <el-icon :size="24"><Menu /></el-icon>
        </span>
      </nav>
    </div>

    <!-- 联系我们弹窗 -->
    <el-dialog v-model="showContact" width="420px" title="联系我们">
      <div class="contact-dialog">
        <div class="contact-item">
          <el-icon :size="20" color="#10B981"><ChatDotRound /></el-icon>
          <div>
            <span class="contact-label">在线反馈</span>
            <span class="contact-desc">通过校内论坛或 QQ 群与我们沟通</span>
          </div>
        </div>
        <div class="contact-item">
          <el-icon :size="20" color="#10B981"><Message /></el-icon>
          <div>
            <span class="contact-label">邮箱联系</span>
            <span class="contact-desc">3433967503@qq.com</span>
          </div>
        </div>
        <div class="contact-item">
          <el-icon :size="20" color="#10B981"><Clock /></el-icon>
          <div>
            <span class="contact-label">工作日：周一 至 周五</span>
            <span class="contact-desc">工作时间 9:00 - 18:00</span>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 申诉弹窗 -->
    <el-dialog v-model="showAppealDialog" width="420px" :close-on-click-modal="false">
      <template #header>
        <div class="dialog-header">
          <el-icon :size="22" color="#10B981"><EditPen /></el-icon>
          <span>账号申诉</span>
        </div>
      </template>
      <el-input v-model="appealReason" type="textarea" :rows="5" placeholder="请说明你被误判的原因，管理员将尽快审核..." />
      <template #footer>
        <el-button @click="showAppealDialog = false">取消</el-button>
        <el-button type="primary" :loading="appealing" @click="submitAppealHandler">提交申诉</el-button>
      </template>
    </el-dialog>

    <!-- 手机端汉堡菜单遮罩 -->
    <teleport to="body">
      <transition name="menu-fade">
        <div v-if="mobileMenuOpen" class="menu-overlay" @click.self="mobileMenuOpen = false">
          <transition name="menu-slide">
            <div v-if="mobileMenuOpen" class="menu-panel">
              <div class="menu-head">
                <span>菜单</span>
                <el-icon :size="22" class="menu-close" @click="mobileMenuOpen = false"><Close /></el-icon>
              </div>

              <!-- 导航区 -->
              <div class="menu-section">
                <div class="menu-section-title">导航</div>
                <div class="menu-item" @click="mobileMenuOpen = false; router.push('/')">
                  <el-icon :size="18"><HomeFilled /></el-icon>首页
                </div>
                <div class="menu-item" @click="mobileMenuOpen = false; router.push('/products')">
                  <el-icon :size="18"><Goods /></el-icon>商品
                </div>
                <div v-if="userStore.isLoggedIn" class="menu-item" @click="mobileMenuOpen = false; router.push('/publish')">
                  <el-icon :size="18"><Plus /></el-icon>发布闲置
                </div>
              </div>

              <!-- 更多区 -->
              <div class="menu-section">
                <div class="menu-section-title">更多</div>
                <div class="menu-item" @click="mobileMenuOpen = false; router.push('/about')">
                  <el-icon :size="18"><InfoFilled /></el-icon>关于我们
                </div>
                <div class="menu-item" @click="mobileMenuOpen = false; showContact = true">
                  <el-icon :size="18"><ChatDotRound /></el-icon>联系我们
                </div>
                <div class="menu-item" @click="mobileMenuOpen = false; router.push('/help')">
                  <el-icon :size="18"><QuestionFilled /></el-icon>帮助中心
                </div>
              </div>

              <!-- 用户区 -->
              <div class="menu-section">
                <div class="menu-section-title">用户</div>
                <template v-if="userStore.isLoggedIn">
                  <div class="menu-item" @click="mobileMenuOpen = false; router.push('/profile')">
                    <el-icon :size="18"><User /></el-icon>个人中心
                  </div>
                  <div class="menu-item" @click="mobileMenuOpen = false; router.push('/orders')">
                    <el-icon :size="18"><Document /></el-icon>我的订单
                    <span v-if="pendingOrderCount > 0" class="menu-badge">{{ pendingOrderCount > 99 ? '99+' : pendingOrderCount }}</span>
                  </div>
                  <div class="menu-item" @click="mobileMenuOpen = false; router.push('/cart')">
                    <el-icon :size="18"><ShoppingCart /></el-icon>购物车
                  </div>
                  <div class="menu-item" @click="mobileMenuOpen = false; router.push('/profile?tab=favorites')">
                    <el-icon :size="18"><Star /></el-icon>我的收藏
                  </div>
                  <div class="menu-item" @click="mobileMenuOpen = false; router.push('/profile?tab=address')">
                    <el-icon :size="18"><Location /></el-icon>收货地址
                  </div>
                  <div class="menu-item" @click="mobileMenuOpen = false; router.push('/notifications')">
                    <el-icon :size="18"><Bell /></el-icon>消息通知
                    <span v-if="userUnreadCount > 0" class="menu-badge">{{ userUnreadCount }}</span>
                  </div>
                  <div class="menu-item" @click="mobileMenuOpen = false; router.push('/chat')">
                    <el-icon :size="18"><ChatLineSquare /></el-icon>聊天
                    <span v-if="chatStore.unread > 0" class="menu-badge">{{ chatStore.unread }}</span>
                  </div>
                  <div v-if="userStore.isAdmin" class="menu-item" @click="mobileMenuOpen = false; router.push('/admin')">
                    <el-icon :size="18"><Setting /></el-icon>后台管理
                  </div>
                  <div class="menu-item menu-item-danger" @click="mobileMenuOpen = false; handleLogout()">
                    <el-icon :size="18"><SwitchButton /></el-icon>退出登录
                  </div>
                </template>
                <template v-else>
                  <div class="menu-item" @click="mobileMenuOpen = false; router.push('/login')">
                    <el-icon :size="18"><User /></el-icon>登录
                  </div>
                  <div class="menu-item" @click="mobileMenuOpen = false; router.push('/register')">
                    <el-icon :size="18"><Plus /></el-icon>注册
                  </div>
                </template>
              </div>
            </div>
          </transition>
        </div>
      </transition>
    </teleport>
  </header>
</template>

<script setup>
// 全局顶部导航：维护搜索、用户信息、后台入口、通知中心和账号封禁提示。
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { Search, ArrowDown, InfoFilled, ChatDotRound, ChatLineSquare, QuestionFilled, Message, Clock, WarningFilled, EditPen, Bell, Menu, Close, HomeFilled, Goods, User, Document, Setting, SwitchButton, ShoppingCart, Star, Location } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import { ElMessage } from 'element-plus'
import { getUserBlacklistStatus } from '@/api/user'
import { submitAppeal } from '@/api/appeal'
import { getNotifications } from '@/api/admin'
import { getNotificationList, getUnreadCount } from '@/api/notification'
import { getOrderStatusCounts } from '@/api/order'
import { getCartList } from '@/api/cart'

const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()
watch(() => userStore.isLoggedIn, (v) => v ? chatStore.connect() : chatStore.disconnect())
const keyword = ref('')
const showContact = ref(false)

// 手机端汉堡菜单
const mobileMenuOpen = ref(false)

// 小黑屋状态
const blacklisted = ref(false)
const blacklistInfo = ref({})
// 账号禁用状态（管理员禁用后残留登录态的提醒）
const disabled = ref(false)
const showAppealDialog = ref(false)
const appealReason = ref('')
const appealing = ref(false)

// 管理员通知
const notify = reactive({ pendingComplaints: 0, pendingAppeals: 0, blacklistCount: 0, pendingArbitrations: 0, total: 0 })
const notifyTotal = ref(0)
let notifyTimer = null

// 用户通知
const userNotifications = ref([])
const userUnreadCount = ref(0)
// 铃铛角标 = 管理员待处理 + 个人未读消息
const badgeCount = computed(() => notifyTotal.value + userUnreadCount.value)
// 订单待办与购物车数量角标：随 60 秒轮询自动更新，订单完成后自动消失
const pendingOrderCount = ref(0)
const cartCount = ref(0)

/** 拉取当前用户的订单角标、购物车数量和未读聊天消息。 */
async function loadUserNotifications() {
  try {
    const [listRes, countRes, orderRes, cartRes] = await Promise.all([
      getNotificationList({ pageNum: 1, pageSize: 5 }),
      getUnreadCount(),
      getOrderStatusCounts(),
      getCartList()
    ])
    userNotifications.value = listRes.data.records || []
    userUnreadCount.value = countRes.data.count
    pendingOrderCount.value = (orderRes.data?.BUYER_PENDING || 0) + (orderRes.data?.BUYER_SHIPPED || 0) + (orderRes.data?.SELLER_PAID || 0) + (orderRes.data?.SELLER_REFUND || 0)
    cartCount.value = (cartRes.data || []).length
  } catch {}
}

/** 拉取管理员待处理事项和个人通知数量。 */
async function loadNotifications() {
  if (!userStore.isAdmin) return
  try {
    const res = await getNotifications()
    Object.assign(notify, res.data)
    notifyTotal.value = notify.total
  } catch {}
}

// 管理员同时拉取后台待处理与个人消息，普通用户只拉个人消息
function refreshNotifications() {
  if (userStore.isAdmin) loadNotifications()
  if (userStore.isLoggedIn) loadUserNotifications()
}

/** 跳转到后台指定页签。 */
function goAdminTab(tab) {
  sessionStorage.setItem('adminTab', tab)
}

/** 跳转到通知中心。 */
function goNotifications() {
  document.body.click() // 关闭 popover
  router.push('/notifications')
}

/** 手动收起通知下拉面板。 */
function dismissNotifications() {
  notifyTotal.value = 0
  Object.assign(notify, { pendingComplaints: 0, pendingAppeals: 0, blacklistCount: 0, pendingArbitrations: 0, total: 0 })
}

/** 格式化小黑屋解封时间。 */
function formatUntil(d) {
  if (!d) return ''
  return d.replace('T', ' ').substring(0, 16)
}

/** 登录后检查账号是否处于小黑屋或被禁用。 */
async function checkBlacklist() {
  if (!userStore.isLoggedIn) return
  try {
    const res = await getUserBlacklistStatus()
    blacklisted.value = res.data.blacklisted
    disabled.value = !!res.data.disabled
    blacklistInfo.value = res.data
  } catch { blacklisted.value = false; disabled.value = false }
}

/** 提交账号封禁申诉。 */
async function submitAppealHandler() {
  if (!appealReason.value.trim()) {
    ElMessage.warning('请输入申诉理由')
    return
  }
  appealing.value = true
  try {
    await submitAppeal({ reason: appealReason.value })
    ElMessage.success('申诉已提交，管理员将尽快审核')
    showAppealDialog.value = false
    appealReason.value = ''
  } finally { appealing.value = false }
}

onMounted(() => {
  checkBlacklist()
  refreshNotifications()
  notifyTimer = setInterval(refreshNotifications, 60000)
  if (userStore.isLoggedIn) chatStore.connect()
})

onBeforeUnmount(() => clearInterval(notifyTimer))

/** 将关键字写入商品列表查询参数并跳转。 */
function handleSearch() {
  router.push({ path: '/products', query: { keyword: keyword.value } })
}

/** 退出登录并回到登录页。 */
function handleLogout() {
  chatStore.disconnect()
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/')
}
</script>

<style scoped>
.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  display: flex;
  align-items: center;
  gap: 24px;
  padding-top: 12px;
  padding-bottom: 12px;
}

.logo {
  display: flex;
  align-items: center;
  cursor: pointer;
  white-space: nowrap;
}

.logo-img {
  height: 56px;
  width: auto;
  display: block;
  mix-blend-mode: multiply;
}

.search-box {
  flex: 1;
  max-width: 480px;
  transition: filter 0.3s ease;
}
.search-box:focus-within {
  filter: drop-shadow(0 0 8px rgba(16,185,129,0.25));
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 16px;
}

.nav-links a {
  color: #606266;
  font-size: 15px;
}

.nav-links a.router-link-active {
  color: #10B981;
  font-weight: 600;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.nickname {
  font-size: 14px;
  color: #303133;
}

.nav-more {
  display: flex; align-items: center; gap: 3px;
  font-size: 15px; color: #606266;
  cursor: pointer; padding: 4px 2px;
  transition: color 0.2s;
}
.nav-more:hover { color: #10B981; }

/* ====== 联系我们弹窗 ====== */
.contact-dialog {
  display: flex; flex-direction: column; gap: 20px;
}
.contact-item {
  display: flex; align-items: flex-start; gap: 14px;
  padding: 14px 16px;
  background: #F9FAFB; border-radius: 12px;
}
.contact-item > div {
  display: flex; flex-direction: column; gap: 3px;
}
.contact-label {
  font-size: 14px; font-weight: 600; color: #1F2937;
}
.contact-desc {
  font-size: 13px; color: #6B7280;
}

/* ====== 小黑屋警告条 ====== */
.header-blacklisted {
  box-shadow: 0 2px 8px rgba(220,38,38,0.1);
}
.blacklist-bar {
  background: linear-gradient(135deg, #FEF2F2, #FEE2E2);
  border-bottom: 1px solid #FECACA;
}
.blacklist-bar-inner {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 0;
  font-size: 13px; color: #DC2626;
}
.blacklist-bar-inner .el-icon { margin-right: 4px; vertical-align: -2px; }

/* 申诉弹窗 */
.dialog-header {
  display: flex; align-items: center; gap: 8px;
  font-size: 16px; font-weight: 700; color: #1F2937;
}

/* ====== 通知铃铛 ====== */
.bell-wrap {
  position: relative;
  cursor: pointer;
  color: #606266;
  display: flex; align-items: center;
  transition: color 0.2s;
}
.bell-wrap:hover { color: #10B981; }
/* 手机端顶栏空间有限，通知入口收进汉堡菜单/底部导航 */
@media (max-width: 768px) {
  .bell-notify { display: none; }
}
.bell-badge {
  position: absolute; top: -6px; right: -10px;
  min-width: 18px; height: 18px; line-height: 18px;
  border-radius: 9px;
  background: #EF4444; color: #fff;
  font-size: 11px; font-weight: 700;
  text-align: center;
  padding: 0 5px;
}

.notify-list { display: flex; flex-direction: column; }
.notify-list-second {
  margin-top: 10px; padding-top: 10px;
  border-top: 1px solid #F3F4F6;
}
.notify-title {
  font-size: 14px; font-weight: 700; color: #1F2937;
  margin-bottom: 12px; padding-bottom: 8px;
  border-bottom: 1px solid #F3F4F6;
}
.notify-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 12px; border-radius: 8px;
  cursor: pointer; transition: background 0.15s;
}
.notify-item:hover { background: #F9FAFB; }
.notify-item.unread { background: #F0FDF4; }
.notify-item span:first-child { font-size: 14px; color: #374151; }
.notify-item-text { display: flex; flex-direction: column; gap: 3px; }
.notify-item-title { font-size: 14px; color: #1F2937; font-weight: 500; }
.notify-item-time { font-size: 12px; color: #9CA3AF; }
.notify-footer {
  text-align: center; padding: 10px 0 4px; cursor: pointer;
  font-size: 13px; color: #10B981; border-top: 1px solid #F3F4F6;
  margin-top: 8px;
}
.notify-footer:hover { color: #059669; }

/* 用户菜单红点 */
.menu-badge {
  display: inline-block;
  min-width: 18px; height: 18px; line-height: 18px;
  border-radius: 9px;
  background: #EF4444; color: #fff;
  font-size: 11px; font-weight: 700;
  text-align: center;
  padding: 0 5px;
  margin-left: 6px;
  vertical-align: middle;
}

/* ====== 汉堡菜单按钮 ====== */
.hamburger-btn {
  cursor: pointer;
  color: #606266;
  display: flex;
  align-items: center;
  transition: color 0.2s;
}
.hamburger-btn:hover { color: #10B981; }

/* ====== 平板端（768-1024px）：搜索框换行 ====== */
@media (max-width: 1024px) and (min-width: 769px) {
  .header-inner {
    flex-wrap: wrap;
    gap: 10px;
  }
  .search-box {
    flex-basis: 100%;
    max-width: none;
    order: 1;
    padding: 0 8px;
  }
  .nav-links { gap: 12px; }
}

/* ====== 手机端（≤768px）：紧凑布局 + 汉堡菜单 ====== */
@media (max-width: 768px) {
  .header-inner {
    gap: 8px;
    justify-content: space-between;
    padding-top: 8px;
    padding-bottom: 8px;
    /* 左右内边距收窄，logo 前移，为搜索框和购物车图标留出空间 */
    padding-left: 10px;
    padding-right: 10px;
  }
  .logo-img { height: 36px; }
  .search-box {
    flex: 1 1 auto;
    min-width: 0;
    max-width: none;
  }
  .nav-links { gap: 8px; }
  .nickname { display: none; }
  .bell-wrap .bell-badge {
    top: -4px; right: -8px;
    min-width: 16px; height: 16px; line-height: 16px;
    font-size: 10px;
  }
}

/* 小屏手机：logo 进一步缩小，保证搜索框可用宽度 */
@media (max-width: 480px) {
  .header-inner {
    gap: 6px;
    padding-left: 8px;
    padding-right: 8px;
  }
  .logo-img { height: 30px; }
}

/* ====== 汉堡菜单遮罩与面板 ====== */
.menu-overlay {
  position: fixed;
  inset: 0;
  z-index: 300;
  background: rgba(0, 0, 0, 0.4);
  /* transition handled by Vue <transition> */
}
.menu-panel {
  position: fixed;
  right: 0; top: 0;
  width: 260px; height: 100%;
  background: #fff;
  border-radius: 16px 0 0 16px;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.1);
}
.menu-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px;
  font-size: 16px; font-weight: 700; color: #1F2937;
  border-bottom: 1px solid #F3F4F6;
}
.menu-close { cursor: pointer; color: #9CA3AF; transition: color 0.2s; }
.menu-close:hover { color: #374151; }

.menu-section {
  padding: 8px 0;
  border-bottom: 1px solid #F3F4F6;
}
.menu-section:last-child { border-bottom: none; }
.menu-section-title {
  font-size: 12px; color: #9CA3AF; font-weight: 600;
  padding: 8px 20px 4px;
  text-transform: uppercase; letter-spacing: 0.5px;
}
.menu-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 20px; cursor: pointer;
  font-size: 15px; color: #374151;
  transition: background 0.15s;
}
.menu-item:hover { background: #F9FAFB; }
.menu-item .el-icon { color: #6B7280; flex-shrink: 0; }
.menu-item-danger { color: #DC2626; }
.menu-item-danger .el-icon { color: #DC2626; }

/* 汉堡菜单过渡动画 */
.menu-fade-enter-active,
.menu-fade-leave-active {
  transition: opacity 0.25s ease;
}
.menu-fade-enter-from,
.menu-fade-leave-to {
  opacity: 0;
}

.menu-slide-enter-active,
.menu-slide-leave-active {
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.menu-slide-enter-from,
.menu-slide-leave-to {
  transform: translateX(100%);
}
</style>
