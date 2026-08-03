# defense_presentation - Design Spec

## I. Project Information

| Item | Value |
| ---- | ----- |
| **Project Name** | defense_presentation |
| **Canvas Format** | ppt169 (1280x720) |
| **Page Count** | 10 |
| **Design Style** | modern-tech |
| **Target Audience** | 答辩委员会、导师、同学 |
| **Use Case** | 毕业设计答辩 |
| **Delivery Purpose** | presentation |
| **Content Strategy** | balanced default |
| **Created Date** | 20260701 |

---

## II. Canvas Specification

| Property | Value |
| -------- | ----- |
| **Format** | ppt169 |
| **Dimensions** | 1280x720 |
| **viewBox** | `0 0 1280 720` |
| **Margins** | left/right 60px, top/bottom 50px |
| **Content Area** | 1160x620 |

---

## III. Visual Theme

### Theme Style

- **Mode**: briefing
- **Visual style**: dark-tech
- **Theme**: Light theme
- **Tone**: tech, professional, modern, innovative

### Color Scheme

| Role | HEX | Purpose |
| ---- | --- | ------- |
| **Background** | `#FFFFFF` | Page background |
| **Secondary bg** | `#F8FAFC` | Card background, section background |
| **Primary** | `#1565C0` | Title decorations, key sections, icons |
| **Accent** | `#0D47A1` | Data highlights, key information, links |
| **Secondary accent** | `#1976D2` | Secondary emphasis, gradient transitions |
| **Body text** | `#1E293B` | Main body text |
| **Secondary text** | `#64748B` | Captions, annotations |
| **Tertiary text** | `#94A3B8` | Supplementary info, footers |
| **Border/divider** | `#E2E8F0` | Card borders, divider lines |
| **Success** | `#10B981` | Positive indicators |
| **Warning** | `#EF4444` | Issue markers |

---

## IV. Typography System

### Font Plan

**Typography direction**: modern CJK sans

| Role | Chinese | English | Fallback tail |
| ---- | ------- | ------- | ------------- |
| **Title** | `"Microsoft YaHei", "PingFang SC"` | `Arial` | `sans-serif` |
| **Body** | `"Microsoft YaHei", "PingFang SC"` | `Arial` | `sans-serif` |

**Per-role font stacks**:

- Title: `"Microsoft YaHei", "PingFang SC", Arial, sans-serif`
- Body: `"Microsoft YaHei", "PingFang SC", Arial, sans-serif`

### Font Size Hierarchy

**Baseline (unitless px)**: Body font size = 32

| Purpose | Ratio to body | Value @ body=32 | Weight |
| ------- | ------------- | ---------------- | ------ |
| Cover title | 3x | 96 | Bold |
| Chapter opener | 2x | 64 | Bold |
| Page title | 1.75x | 56 | Bold |
| Subtitle | 1.25x | 40 | SemiBold |
| Body content | 1x | 32 | Regular |
| Annotation | 0.8x | 26 | Regular |
| Page number | 0.6x | 19 | Regular |

---

## V. Layout Principles

### Page Structure

- **Header area**: 80px - page title
- **Content area**: 560px - main content
- **Footer area**: 80px - page number, project name

---

## VI. Image Resource List

| Filename | Dimensions | Ratio | Purpose | Type | Layout pattern | Acquire Via | Status |
| -------- | --------- | ----- | ------- | ---- | -------------- | ----------- | ------ |
| cover_bg.png | 1280x720 | 16:9 | Cover backdrop with tech abstract pattern | Background | full-bleed background | ai | Pending |

---

## VII. Content Outline

### Part 1: 封面与目录

#### Slide 01 - Cover

- **Layout**: Single column centered with full-bleed background
- **Title**: 校园二手物品共享平台的设计与实现
- **Subtitle**: SecondHand-Shared System
- **Info**: 答辩人：XXX | 指导老师：XXX | 日期：2026年7月

#### Slide 02 - 目录

- **Layout**: Single column centered
- **Title**: 论文汇报目录
- **Content**:
  - 研究背景（选题背景及国内外研究现状）
  - 项目技术路线（开发环境与架构）
  - 项目功能（系统功能模块）

---

### Part 2: 研究背景

#### Slide 03 - 研究背景

- **Layout**: Asymmetric split (3:7)
- **Title**: 研究背景
- **Content**:
  - **选题背景**
    - 高校招生规模扩大，闲置物品交易需求增长
    - 毕业季大量物品被低价处理或丢弃
    - 在校学生对高性价比二手商品存在刚性需求
  - **传统交易方式的问题**
    - 信息碎片化：分散在QQ群、微信群、贴吧
    - 信任机制缺失：身份不透明，交易纠纷频发
    - 交易流程不规范：缺乏统一订单管理
    - 缺乏评价体系：无法判断卖家信誉
  - **研究意义**
    - 经济价值：促进闲置物品循环利用，降低生活成本
    - 社会价值：践行绿色校园理念，培养可持续消费习惯
    - 技术价值：实践前后端分离架构、JWT无状态认证

#### Slide 04 - 国内外研究现状

- **Layout**: Single column centered
- **Title**: 国内外研究现状
- **Content**:
  - **现状分析**
    - 国内外二手交易平台发展迅速（闲鱼、转转、Facebook Marketplace）
    - 专门针对校园场景的二手交易系统研究相对较少
    - 主流平台面向全年龄段用户，校园场景特殊性未得到关注
  - **校园二手交易特点**
    - 用户群体相对封闭（仅限校内师生）
    - 交易商品以学生刚需为主（教材、电子产品、生活用品）
    - 交易金额普遍较低
    - 用户之间信任关系相对容易建立
  - **本课题研究目标**
    - 针对现有痛点，设计与实现一套专门的校园二手物品共享平台

---

### Part 3: 项目技术路线

#### Slide 05 - 技术架构

- **Layout**: Three-tier architecture diagram
- **Title**: 系统技术架构
- **Content**:
  - **整体架构**
    - 前端层：Vue 3 + Element Plus + Vite
    - 后端层：Spring Boot + MyBatis-Plus + JWT
    - 数据层：MySQL 8.0
  - **后端分层架构**
    - Controller：接收HTTP请求，参数校验，调用Service
    - Service：业务逻辑处理，权限校验，数据转换
    - Mapper：数据访问，MyBatis-Plus内置CRUD
    - Entity：数据库表映射，软删除支持
  - **前端分层架构**
    - Views：路由级别页面组件
    - Components：可复用UI单元
    - API Modules：封装后端API调用
    - Utils：工具层（请求拦截器、Token管理）
    - Stores：Pinia状态管理

#### Slide 06 - 开发环境与技术栈

- **Layout**: Three column cards
- **Title**: 开发环境与技术栈
- **Content**:
  - **后端技术栈**
    - Spring Boot 3.3.5：应用框架，IoC容器，MVC
    - MyBatis-Plus 3.5.9：ORM框架，分页与逻辑删除
    - MySQL 8.0：关系型数据库，InnoDB引擎
    - JJWT 0.12.6：JWT令牌生成与验证
    - Hutool 5.8.25：Java工具类库
  - **前端技术栈**
    - Vue 3：前端框架，Composition API
    - Vite 5.x：构建工具，快速热更新
    - Element Plus 2.x：UI组件库
    - Pinia 2.x：状态管理库
    - Vue Router 4.x：前端路由
    - Axios：HTTP客户端
    - ECharts 5.x：数据可视化图表
  - **开发工具**
    - IntelliJ IDEA、VS Code、Navicat

---

### Part 4: 项目功能

#### Slide 07 - 功能模块图

- **Layout**: Feature matrix grid
- **Title**: 系统功能模块
- **Content**:
  - **用户模块**
    - 用户注册（用户名+密码+昵称）
    - 用户登录（JWT令牌认证）
    - 个人资料管理（头像、手机号、邮箱）
    - 修改密码
  - **商品模块**
    - 发布商品（标题、描述、价格、分类、图片）
    - 编辑/删除/下架商品
    - 商品列表（分页+搜索+分类筛选）
    - 商品详情（浏览量统计）
  - **订单模块**
    - 创建订单（买家下单）
    - 订单状态流转（待付款→已付款→已发货→已完成）
    - 取消订单（商品自动恢复上架）
    - 订单列表（按状态分类展示）
  - **收藏模块**：收藏/取消收藏、收藏列表
  - **评论模块**：发表评论（1-5星评分）、评论列表
  - **后台管理模块**：数据概览、用户管理、商品管理、订单管理、分类管理

#### Slide 08 - 数据库设计

- **Layout**: Entity relationship diagram
- **Title**: 数据库设计
- **Content**:
  - **核心数据表**
    - t_user（用户表）：id, username, password, nickname, avatar, phone, email, role, status
    - t_category（分类表）：id, name, parent_id, icon, sort
    - t_product（商品表）：id, title, description, price, original_price, images, category_id, user_id, status, view_count
    - t_order（订单表）：id, order_no, product_id, buyer_id, seller_id, price, status
    - t_favorite（收藏表）：id, user_id, product_id
    - t_comment（评论表）：id, product_id, user_id, content, rating
  - **数据库信息**
    - 数据库名称：secondhand_db
    - 存储引擎：InnoDB
    - 字符集：utf8mb4

---

### Part 5: 核心流程与总结

#### Slide 09 - 核心业务流程

- **Layout**: Flowchart
- **Title**: 核心业务流程
- **Content**:
  - **用户认证流程**
    - 用户提交用户名+密码 → 服务端验证密码 → 生成JWT Token → 前端存储Token → 后续请求自动附加Token
  - **订单状态流转**
    - 待付款(PENDING) → 已付款(PAID) → 已发货(SHIPPED) → 已完成(COMPLETED)
    - 待付款(PENDING) → 已取消(CANCELLED)（买家取消或超时）
  - **商品发布流程**
    - 用户上传图片 → 填写商品信息 → 设置分类和价格 → 保存发布（状态设为ON_SALE）

#### Slide 10 - 总结与展望

- **Layout**: Single column centered
- **Title**: 总结与展望
- **Content**:
  - **项目总结**
    - 完成了校园二手物品共享平台的需求分析
    - 设计了前后端分离的系统架构
    - 实现了用户认证、商品管理、订单流转、收藏评论等核心功能
    - 建立了统一的数据返回格式与全局异常处理机制
    - 设计了品牌化的视觉规范，实现了响应式布局
  - **后续改进方向**
    - 引入WebSocket实现实时消息推送
    - 集成Elasticsearch实现全文检索
    - 对接微信支付/支付宝校园接口
    - 基于用户行为实现协同过滤推荐
    - 编写Dockerfile实现容器化部署
  - **致谢**
    - 感谢导师的悉心指导
    - 感谢答辩委员会的评审

---

## VIII. Technical Constraints Reminder

### SVG Generation Must Follow:

1. viewBox: `0 0 1280 720`
2. Background uses `<rect>` elements
3. Text wrapping uses `<tspan>`
4. Transparency uses `fill-opacity` / `stroke-opacity`; `rgba()` FORBIDDEN
5. FORBIDDEN: `mask`, `<style>`, `class`, `foreignObject`, `textPath`, `animate*`, `script`
6. Text characters: write typography & symbols as raw Unicode; HTML named entities FORBIDDEN

### PPT Compatibility Rules:

- `<g opacity="...">` FORBIDDEN
- Image transparency uses overlay mask layer
- Inline styles only