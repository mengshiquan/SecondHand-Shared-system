# 校园二手平台 · 全站响应式适配设计

## 概述

将前端全部页面适配 PC（>1024px）、平板（768-1024px）、手机（≤768px）三端，采用底部 Tab 栏 + 顶部汉堡菜单的组合策略。

---

## 设计策略

### 断点体系

| 断点 | 范围 | 设备代表 |
|------|------|---------|
| PC | >1024px | 1920px / 1440px 桌面 |
| 平板 | 768-1024px | iPad Air 820px / iPad Pro 1024px |
| 手机 | ≤768px | iPhone 14 390px / iPhone SE 375px |

### 导航分布

| 断点 | 顶部导航 | 底部导航 |
|------|---------|---------|
| PC (>1024px) | Logo + 搜索 + 导航链接 + 铃铛 + 发布按钮 + 用户/登录 | 无 |
| 平板 (768-1024px) | 第一行: Logo + 铃铛 + 用户/登录；第二行: 搜索框全宽 | 无 |
| 手机 (≤768px) | Logo + 搜索(缩短) + 铃铛 + 汉堡菜单 | 首页/商品/发布(FAB)/我的 |

### 设计 Token 保持不变

主题绿 `#10B981`、暖橙 `#F59E0B`、背景 `#F0F9F4`、卡片白 `#fff`、圆角 14-16px、阴影 `0 1px 3px rgba(0,0,0,0.04)`。

---

## 一、全局基础

### 1.1 全局 CSS（assets/styles/global.css）

新增：
- CSS 自定义属性：`--breakpoint-sm: 480px; --breakpoint-md: 768px; --breakpoint-lg: 1024px`
- `.page-container` 响应式 padding：PC 20px → 平板 14px → 手机 10px
- 工具类：`.hide-mobile`（≤768px 隐藏）、`.hide-tablet`（≤1024px 隐藏）、`.show-mobile`（≤768px 显示）

### 1.2 BottomNav.vue（新建）

- 固定底部 `position: fixed; bottom: 0; z-index: 200`
- 4 个导航项：首页、商品、我的，加居中 FAB 发布按钮
- FAB 位于"商品"和"我的"之间上方 12px，圆形 52px，渐变色 `#10B981 → #059669`，白色加号图标
- FAB 点击：`scale: 1 → 1.25 → 1` 弹跳动画 0.3s
- 活跃 Tab：图标变主题绿 + 文字加粗 + `scale(1.15)` 过渡 0.2s
- 背景白色 + `box-shadow: 0 -2px 12px rgba(0,0,0,0.06)`
- 底部安全区：`padding-bottom: env(safe-area-inset-bottom, 8px)`
- 可见性：登录/注册页隐藏，≤768px 显示

### 1.3 AppHeader.vue 三态适配

**PC (>1024px)**：保持现有布局。

**平板 (768-1024px)**：
- 第一行：Logo + 铃铛 + 用户/登录按钮
- 第二行：搜索框 `flex: 1`，无 max-width 限制，左右 padding 各 20px

**手机 (≤768px)**：
- 顶部单行：Logo + 搜索框（`max-width: 200px`）+ 铃铛 + 汉堡图标
- 汉堡菜单：`teleport` 到 body，右侧滑入面板 260px 宽，左侧圆角 16px，三区菜单（导航/更多/用户），半透明遮罩层，打开/关闭 0.25s cubic-bezier
- "更多"下拉改为汉堡菜单内的二级项
- 通知 popover 宽度调为 260px
- 导航链接（首页/商品）移入汉堡菜单

### 1.4 Layout.vue

- 引入 `BottomNav` 组件，`v-show` 按断点控制
- 主内容区底部 padding 增加以避开 Tab 栏高度（约 64px）
- 页脚手机端 padding/font-size 缩小

---

## 二、页面适配

### 2.1 ProductDetail.vue

- **768px**：图片轮播高度 280px，卖家信息卡片堆叠，按钮等宽分布
- **480px**：轮播高度 200px，「联系卖家」按钮全宽，评论区头像缩小，卖家其他产品改为横向滚动（`overflow-x: auto; flex-wrap: nowrap`）

### 2.2 ProductList.vue

已有三断点覆盖，仅微调：
- 筛选栏 gap 与全局 token 对齐
- 空状态图 480px 缩小尺寸

### 2.3 Home.vue

已有三断点覆盖，仅确保 `page-container` 全局 padding 不冲突。

### 2.4 Login.vue / Register.vue

已有 1024px 断点，不修改布局和样式（CLAUDE.md 要求）。全局 padding 不影响内部。

### 2.5 Profile.vue

- **768px**（已有）：grid 调整
- **480px**：头像居中，统计数字横向一行三列，侧边导航改为顶部横向滚动 Tab（`overflow-x: auto`），发布卡单列

### 2.6 MyOrders.vue

- **768px**（已有）：订单内容 padding、body 换行
- **480px**：标签页压缩，提醒文字缩小，按钮全宽

### 2.7 Admin.vue

- **768px**（已有）：统计卡 padding、图表高度 250px
- **480px**：统计卡每行 2 个，图表高度 220px，表格 `overflow-x: auto`，小黑屋/投诉表格列缩减（隐藏操作备注列）

### 2.8 PublishProduct.vue

- **768px**（已有）：表单卡 padding
- **480px**：图片上传全宽，提交按钮全宽，section 标题缩小

### 2.9 About.vue

- **768px**（已有）：特色网格堆叠
- **480px**：卡片 padding 缩小，图标缩小到 40px

### 2.10 Help.vue（新增适配）

- **768px**：`max-width` 改为 100%，折叠面板标题字体 15px
- **480px**：标题字体 14px，padding 缩小

### 2.11 Notifications.vue（新增适配）

- **768px**：通知行按钮始终可见（取消 hover 显隐），操作按钮全宽
- **480px**：通知内容字体缩小

---

## 三、公共组件微调

### 3.1 ProductCard.vue

- 图片高度：200px (PC) → 160px (≤768px) → 140px (≤480px)
- 标题：15px → 14px → 13px
- 价格添加 `font-variant-numeric: tabular-nums`

### 3.2 CategoryPicker.vue

- `flex-wrap: wrap` 保留，gap 在 480px 从 8px 缩至 6px
- chip font-size 14px → 13px (≤480px)

### 3.3 ImageUpload.vue

- Element Plus 自带响应式，无需改动

### 3.4 SkeletonCard.vue

- 跟随 ProductCard 的尺寸节奏

---

## 四、动效规范

| 元素 | 动效 |
|------|------|
| FAB 发布按钮点击 | `scale(1) → scale(1.25) → scale(1)`，0.3s ease |
| 底部 Tab 切换 | 图标 `scale(1.15)` + 颜色过渡，0.2s ease |
| 汉堡菜单 | 面板 `translateX(100%) → translateX(0)`，遮罩 `opacity: 0 → 1`，0.25s cubic-bezier(0.4, 0, 0.2, 1) |
| 入场动画（已有） | 各页面 staggered fadeInUp 在移动端保留，stagger 间隔可由 0.06s 缩至 0.03s |

---

## 五、实施顺序

1. **全局 CSS** + **BottomNav.vue**（新建）
2. **AppHeader.vue**（工作量最大，三态适配 + 汉堡菜单）
3. **Layout.vue**（引入 BottomNav）
4. **Help.vue** + **Notifications.vue**（从零开始加适配）
5. **PublishProduct.vue** + **About.vue**（已有基础，补 480px）
6. **MyOrders.vue** + **Admin.vue**（已有基础，补 480px）
7. **ProductDetail.vue** + **Profile.vue**（已有基础，补 480px）
8. **ProductCard.vue** + **CategoryPicker.vue**（组件微调）
9. **Home.vue** + **ProductList.vue**（验证即可）

---

## 六、验证清单

每个页面在 Chrome DevTools 中检查：
- [ ] PC 1920px — 布局不溢出，功能完整
- [ ] 平板 820px / 1024px — 底部 Tab 不出现，搜索框换行
- [ ] 手机 390px — 底部 Tab 可见，汉堡菜单正常，FAB 可点击
- [ ] 过渡动画流畅，无布局抖动
- [ ] 登录页/注册页底部 Tab 隐藏
