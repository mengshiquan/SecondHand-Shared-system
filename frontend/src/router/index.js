import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/stores/user'

/**
 * 路由表：
 * - guest：已登录用户也可访问，用于切换账号；
 * - requiresAuth：必须携带有效登录态；
 * - requiresAdmin：在 requiresAuth 基础上再校验管理员角色。
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { guest: true }
  },
  
  {
    path: '/',
    component: () => import('@/components/Layout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/views/Home.vue') },
      { path: 'about', name: 'About', component: () => import('@/views/About.vue') },
      { path: 'help', name: 'Help', component: () => import('@/views/Help.vue') },
      { path: 'products', name: 'ProductList', component: () => import('@/views/ProductList.vue') },
      { path: 'product/:id', name: 'ProductDetail', component: () => import('@/views/ProductDetail.vue') },
      { path: 'seller/:id', name: 'SellerHome', component: () => import('@/views/SellerHome.vue') },
      { path: 'publish', name: 'PublishProduct', component: () => import('@/views/PublishProduct.vue'), meta: { requiresAuth: true } },
      { path: 'notifications', name: 'Notifications', component: () => import('@/views/Notifications.vue'), meta: { requiresAuth: true } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/Profile.vue'), meta: { requiresAuth: true } },
      { path: 'cart', name: 'Cart', component: () => import('@/views/Cart.vue'), meta: { requiresAuth: true } },
      { path: 'orders', name: 'MyOrders', component: () => import('@/views/MyOrders.vue'), meta: { requiresAuth: true } },
      { path: 'chat/:peerId?', name: 'Chat', component: () => import('@/views/Chat.vue'), meta: { requiresAuth: true } },
      { path: 'admin', name: 'Admin', component: () => import('@/views/Admin.vue'), meta: { requiresAuth: true, requiresAdmin: true } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = getToken()
  const userStore = useUserStore()

  // 未登录访问受保护页面时，先记录目标地址，登录成功后回跳。
  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 普通用户访问后台路由时直接回到首页，避免暴露后台入口数据。
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next('/')
    return
  }

  // 已登录也允许访问登录页：多标签页场景下用于切换账号
  next()
})

export default router
