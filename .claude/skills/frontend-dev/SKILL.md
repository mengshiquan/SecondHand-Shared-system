---
name: frontend-dev
description: Frontend development for SecondHand project. Triggers when adding/modifying Vue pages, components, API modules, router, or styling.
---

# Frontend Development — 校园二手平台

## Tech Stack
- Vue 3 (Composition API) + Vite 5 + Element Plus + Pinia + Axios + ECharts
- Package manager: npm

## Project Structure
```
src/
├── views/          # Page-level components (9 pages)
├── components/     # Reusable components (8 components)
├── api/            # API modules (8 files)
├── router/         # Vue Router config + guards
├── stores/         # Pinia stores (user.js)
├── utils/          # auth.js, request.js
└── assets/styles/  # global.css
```

## Design Tokens
```css
--theme-green:    #10B981;
--theme-amber:    #F59E0B;
--bg-light:       #F0F9F4;
--card-shadow:    0 1px 3px rgba(0,0,0,0.04);
--card-hover:     0 8px 24px rgba(16,185,129,0.08);
--title-color:    #1F2937;
--body-color:     #374151;
--muted-color:    #9CA3AF;
--card-radius:    14-16px;
--anim-duration:  0.25s ease;
```

## Adding a New Page

### 1. Create View (`src/views/Xxx.vue`)
```vue
<template>
  <div class="xxx page-container">
    <div class="page-header">
      <h1 class="page-title">页面标题</h1>
      <p class="page-sub">副标题说明</p>
    </div>
    <!-- content -->
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
// ... logic
</script>

<style scoped>
/* Branded styles using design tokens */
</style>
```

- Always use `<style scoped>` — no global styles
- Page header pattern: `.page-header` > `.page-title` + `.page-sub`
- Use `.page-container` for consistent page wrapper

### 2. Add Route (`src/router/index.js`)
```javascript
// Public route:
{ path: 'xxx', name: 'Xxx', component: () => import('@/views/Xxx.vue') }

// Auth-required route:
{ path: 'xxx', name: 'Xxx', component: () => import('@/views/Xxx.vue'), meta: { requiresAuth: true } }

// Admin-only route:
{ path: 'xxx', name: 'Xxx', component: () => import('@/views/Xxx.vue'), meta: { requiresAuth: true, requiresAdmin: true } }
```

### 3. Add API Module (`src/api/xxx.js`)
```javascript
import request from '@/utils/request'

export function getList(params) {
  return request.get('/xxx/list', { params })
}

export function create(data) {
  return request.post('/xxx', data)
}
```
- `request` adds `/api` prefix, JWT header, and handles `res.code !== 200` errors
- `request` returns `res.data` directly (the `data` field from `Result<T>`)
- API modules export plain functions, one per endpoint

## Component Patterns

### ProductCard / SkeletonCard
- Props-driven: `defineProps({ product: { type: Object, required: true } })`
- `computed()` for derived data (images[0], status text)
- `useRouter()` for navigation
- SkeletonCard mirrors ProductCard dimensions for layout stability

### Image Upload (`ImageUpload.vue`)
- Uses Element Plus `el-upload` with custom `http-request`
- Calls `uploadFile()` from `@/api/file`

### CategoryPicker (`CategoryPicker.vue`)
- Cascading selection: parent category → child category
- Calls `getCategoryList()` from `@/api/category`

## Auth Flow
- Token stored in `localStorage` key: `secondhand_token`
- User info in `localStorage` key: `secondhand_user` (JSON)
- `utils/auth.js`: `getToken()`, `setToken()`, `clearAuth()`
- Router guard checks `requiresAuth` and `requiresAdmin` meta
- `userStore.userInfo?.userId` for current user ID
- `userStore.userInfo?.role` for role check

## Empty States
Use branded empty state pattern (NOT `<el-empty>`):
```html
<div v-if="items.length === 0" class="empty-state">
  <div class="empty-icon">
    <el-icon :size="40"><Document /></el-icon>
  </div>
  <p class="empty-title">暂无数据</p>
  <p class="empty-hint">提示文字</p>
</div>
```

## Quick Reference

| Task | Pattern |
|------|---------|
| New page | View.vue → router → AppHeader nav link |
| New API call | api/xxx.js → import in view |
| Auth check | `v-if="userStore.userInfo"` or `meta.requiresAuth` |
| Loading state | `<SkeletonCard />` or `v-loading` |
| Error display | `ElMessage.error('message')` |
| Success toast | `ElMessage.success('message')` |
| Confirmation | `ElMessageBox.confirm('text', 'title', { type: 'warning' })` |
| Form validation | `el-form` with `:rules` + `formRef.validate()` |
| Staggered animation | `:style="{ animationDelay: \`${i * 0.06}s\` }"` + `@keyframes fadeInUp` |
