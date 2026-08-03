import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/stores/user'

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
      { path: 'publish', name: 'PublishProduct', component: () => import('@/views/PublishProduct.vue'), meta: { requiresAuth: true } },
      { path: 'notifications', name: 'Notifications', component: () => import('@/views/Notifications.vue'), meta: { requiresAuth: true } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/Profile.vue'), meta: { requiresAuth: true } },
      { path: 'orders', name: 'MyOrders', component: () => import('@/views/MyOrders.vue'), meta: { requiresAuth: true } },
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

  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  if (to.meta.requiresAdmin && userStore.userInfo?.role !== 'ADMIN') {
    next('/')
    return
  }

  if (to.meta.guest && token) {
    next('/')
    return
  }

  next()
})

export default router
