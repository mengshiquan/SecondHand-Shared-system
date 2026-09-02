<template>
  <div class="layout">
    <AppHeader />
    <main class="main-content">
      <router-view />
    </main>
    <footer class="footer">
      <p>校园二手共享平台 · 让闲置流动起来</p>
    </footer>
    <BottomNav />
  </div>
</template>

<script setup>
// 基础布局：组合顶部导航、底部导航和路由页面容器。
import AppHeader from './AppHeader.vue'
import BottomNav from './BottomNav.vue'
</script>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
}
/* 手机端为底部 Tab 让位 */
@media (max-width: 768px) {
  .main-content { padding-bottom: 72px; }
}

.footer {
  text-align: center;
  padding: 28px 20px;
  color: #6B7280;
  font-size: 13px;
  background: linear-gradient(to bottom, #F0FDF4, #D1FAE5);
  border-top: 1px solid #A7F3D0;
}
.footer p { margin: 0; }
/* 手机端缩小页脚 */
@media (max-width: 480px) {
  .footer { padding: 16px 14px; font-size: 12px; }
}
</style>
