<template>
  <nav v-if="showBottomNav" class="bottom-nav">
    <div class="bottom-nav-inner">
      <router-link to="/" class="nav-item" :class="{ active: route.path === '/' }">
        <el-icon :size="22"><HomeFilled /></el-icon>
        <span class="nav-label">首页</span>
      </router-link>
      <router-link to="/products" class="nav-item" :class="{ active: route.path.startsWith('/products') }">
        <el-icon :size="22"><Goods /></el-icon>
        <span class="nav-label">商品</span>
      </router-link>

      <!-- FAB 占位 — 4 个 Tab 均分，FAB 居中浮于上方 -->
      <div class="nav-item fab-spacer"></div>

      <router-link to="/notifications" class="nav-item" :class="{ active: route.path.startsWith('/notifications') }">
        <span class="bell-wrap">
          <el-icon :size="22"><Bell /></el-icon>
          <span v-if="unreadCount > 0" class="bell-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </span>
        <span class="nav-label">消息</span>
      </router-link>

      <router-link to="/profile" class="nav-item" :class="{ active: route.path.startsWith('/profile') }">
        <el-icon :size="22"><User /></el-icon>
        <span class="nav-label">我的</span>
      </router-link>
    </div>

    <!-- FAB -->
    <div class="fab-btn" @click="router.push('/publish')">
      <el-icon :size="28"><Plus /></el-icon>
    </div>
  </nav>
</template>

<script setup>
// 移动端底部导航：按登录状态和当前路由渲染常用入口。
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { HomeFilled, Goods, User, Plus, Bell } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getUnreadCount } from '@/api/notification'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const unreadCount = ref(0)

// 登录/注册页不显示底部 Tab
const showBottomNav = computed(() => {
  return !['/login', '/register'].includes(route.path)
})

// 登录用户拉取未读消息数
async function fetchUnread() {
  if (!userStore.isLoggedIn) { unreadCount.value = 0; return }
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data.count
  } catch { unreadCount.value = 0 }
}

// 路由变化时刷新未读数
watch(() => route.path, () => {
  if (userStore.isLoggedIn) fetchUnread()
}, { immediate: true })
</script>

<style scoped>
/* 平板及以上隐藏 */
@media (min-width: 769px) {
  .bottom-nav { display: none !important; }
}

.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 200;
  background: #fff;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);
  padding-bottom: env(safe-area-inset-bottom, 8px);
  height: calc(64px + env(safe-area-inset-bottom, 8px));
}

.bottom-nav-inner {
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 64px;
  max-width: 500px;
  margin: 0 auto;
  position: relative;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  flex: 1;
  text-decoration: none;
  color: #9CA3AF;
  cursor: pointer;
  transition: color 0.2s ease, transform 0.2s ease;
  -webkit-tap-highlight-color: transparent;
  user-select: none;
}

.nav-item.active {
  color: #10B981;
  font-weight: 600;
}

.nav-item.active :deep(.el-icon) {
  transform: scale(1.15);
  transition: transform 0.2s cubic-bezier(.34, 1.56, .64, 1);
}

.nav-label {
  font-size: 11px;
  font-weight: inherit;
}

/* 消息铃铛红点 */
.bell-wrap {
  position: relative;
  display: inline-flex;
}
.bell-badge {
  position: absolute;
  top: -4px;
  right: -8px;
  min-width: 16px;
  height: 16px;
  line-height: 16px;
  border-radius: 8px;
  background: #EF4444;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  text-align: center;
  padding: 0 4px;
}

/* FAB 占位 */
.fab-spacer {
  flex: 1;
}

/* FAB 发布按钮 — 浮高于底部栏 */
.fab-btn {
  position: absolute;
  top: -24px;
  left: 50%;
  transform: translateX(-50%);
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, #10B981, #059669);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.4);
  transition: transform 0.3s cubic-bezier(.34, 1.56, .64, 1),
              box-shadow 0.3s ease;
  z-index: 1;
}
.fab-btn:active {
  transform: translateX(-50%) scale(0.9);
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}
</style>
