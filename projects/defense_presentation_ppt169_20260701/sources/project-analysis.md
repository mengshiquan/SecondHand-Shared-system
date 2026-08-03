# 校园二手物品共享平台 - 项目分析文档

## 一、项目概述

**项目名称**: 校园二手物品共享平台 (SecondHand-Shared)

**项目定位**: 面向校园学生群体的二手物品交易平台，旨在促进闲置物品的流通，实现资源共享与可持续发展。

**项目状态**: 已完成基础功能开发，包含用户、商品、订单、收藏、评论、分类、管理等完整模块。

---

## 二、技术架构

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        前端层 (Vue 3)                        │
│  Views / Components / API / Router / Store / Utils          │
└────────────────────────────┬────────────────────────────────┘
                             │ HTTP (Axios)
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                       后端层 (Spring Boot)                   │
│  Controller / Service / Mapper / Entity / DTO / VO          │
└────────────────────────────┬────────────────────────────────┘
                             │ JDBC (MyBatis Plus)
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                     数据层 (MySQL)                           │
│  t_user / t_product / t_order / t_favorite / t_comment      │
│  t_category                                                 │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4.21 | 前端框架 |
| Vue Router | 4.3.0 | 路由管理 |
| Pinia | 2.1.7 | 状态管理 |
| Element Plus | 2.6.3 | UI组件库 |
| Axios | 1.6.8 | HTTP请求 |
| Vite | 5.2.8 | 构建工具 |
| ECharts | 6.1.0 | 数据可视化（管理后台） |

### 2.3 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3.5 | 后端框架 |
| MyBatis Plus | 3.5.9 | ORM框架 |
| MySQL | - | 数据库 |
| JWT (JJWT) | 0.12.6 | 认证授权 |
| Hutool | 5.8.25 | 工具库 |
| Lombok | - | 简化代码 |

---

## 三、项目结构

### 3.1 前端结构

```
frontend/
├── src/
│   ├── api/              # 接口请求封装
│   │   ├── user.js       # 用户接口
│   │   ├── product.js    # 商品接口
│   │   ├── order.js      # 订单接口
│   │   ├── favorite.js   # 收藏接口
│   │   ├── comment.js    # 评论接口
│   │   ├── category.js   # 分类接口
│   │   ├── file.js       # 文件接口
│   │   └── admin.js      # 管理接口
│   ├── components/       # 公共组件
│   │   ├── Layout.vue    # 布局组件
│   │   ├── AppHeader.vue # 头部导航
│   │   ├── ProductCard.vue # 商品卡片
│   │   ├── CategoryPicker.vue # 分类选择器
│   │   ├── ImageUpload.vue # 图片上传
│   │   ├── AnimatedCharacters.vue # 动画字符
│   │   ├── EyeBall.vue   # 眼球动画
│   │   ├── Pupil.vue     # 瞳孔动画
│   │   └── SkeletonCard.vue # 骨架屏
│   ├── stores/           # Pinia状态管理
│   │   └── user.js       # 用户状态
│   ├── utils/            # 工具函数
│   │   ├── request.js    # 请求封装（含拦截器）
│   │   ├── auth.js       # 认证工具（Token管理）
│   │   └── index.js      # 通用工具
│   ├── views/            # 页面视图
│   │   ├── Login.vue     # 登录页
│   │   ├── Register.vue  # 注册页
│   │   ├── Home.vue      # 首页
│   │   ├── About.vue     # 关于页
│   │   ├── Help.vue      # 帮助页
│   │   ├── ProductList.vue # 商品列表页
│   │   ├── ProductDetail.vue # 商品详情页
│   │   ├── PublishProduct.vue # 发布商品页
│   │   ├── Profile.vue   # 个人中心页
│   │   ├── MyOrders.vue  # 我的订单页
│   │   └── Admin.vue     # 管理后台页
│   ├── router/           # 路由配置
│   │   └── index.js
│   ├── assets/           # 静态资源
│   │   └── styles/       # 全局样式
│   ├── App.vue           # 根组件
│   └── main.js           # 入口文件
├── dist/                 # 构建产物
├── package.json          # 依赖配置
└── vite.config.js        # Vite配置
```

### 3.2 后端结构

```
backend/
├── src/main/java/com/campus/secondhand/
│   ├── controller/       # 控制器层
│   │   ├── UserController.java       # 用户接口
│   │   ├── ProductController.java    # 商品接口
│   │   ├── OrderController.java      # 订单接口
│   │   ├── FavoriteController.java   # 收藏接口
│   │   ├── CommentController.java    # 评论接口
│   │   ├── CategoryController.java   # 分类接口
│   │   ├── FileController.java       # 文件上传接口
│   │   ├── FileServerController.java # 文件服务接口
│   │   └── AdminController.java      # 管理接口
│   ├── service/          # 服务层
│   │   ├── impl/         # 服务实现
│   │   │   ├── UserServiceImpl.java
│   │   │   ├── ProductServiceImpl.java
│   │   │   ├── OrderServiceImpl.java
│   │   │   ├── FavoriteServiceImpl.java
│   │   │   ├── CommentServiceImpl.java
│   │   │   ├── CategoryServiceImpl.java
│   │   │   └── AdminServiceImpl.java
│   │   ├── UserService.java
│   │   ├── ProductService.java
│   │   ├── OrderService.java
│   │   ├── FavoriteService.java
│   │   ├── CommentService.java
│   │   ├── CategoryService.java
│   │   └── AdminService.java
│   ├── mapper/           # 数据访问层
│   │   ├── UserMapper.java
│   │   ├── ProductMapper.java
│   │   ├── OrderMapper.java
│   │   ├── FavoriteMapper.java
│   │   ├── CommentMapper.java
│   │   └── CategoryMapper.java
│   ├── entity/           # 数据库实体
│   │   ├── User.java
│   │   ├── Product.java
│   │   ├── Order.java
│   │   ├── Favorite.java
│   │   ├── Comment.java
│   │   └── Category.java
│   ├── dto/              # 数据传输对象（请求）
│   │   ├── LoginDTO.java
│   │   ├── RegisterDTO.java
│   │   ├── ProductDTO.java
│   │   ├── ProductQueryDTO.java
│   │   ├── OrderDTO.java
│   │   └── CommentDTO.java
│   ├── vo/               # 视图对象（响应）
│   │   ├── LoginVO.java
│   │   ├── ProductVO.java
│   │   ├── OrderVO.java
│   │   ├── CommentVO.java
│   │   └── DashboardVO.java
│   ├── config/           # 配置类
│   │   ├── CorsConfig.java           # 跨域配置
│   │   ├── WebMvcConfig.java         # Web配置（拦截器注册）
│   │   ├── MybatisPlusConfig.java    # MyBatis Plus配置
│   │   ├── MetaObjectHandlerConfig.java # 自动填充配置
│   │   ├── interceptor/
│   │   │   └── JwtInterceptor.java   # JWT拦截器
│   │   └── handler/
│   │       └── JsonListTypeHandler.java # JSON类型处理器
│   ├── common/           # 公共类
│   │   ├── Result.java               # 统一响应封装
│   │   ├── ResultCode.java           # 响应码枚举
│   │   ├── BusinessException.java    # 业务异常
│   │   └── GlobalExceptionHandler.java # 全局异常处理
│   ├── util/             # 工具类
│   │   ├── JwtUtil.java              # JWT工具
│   │   ├── UserContext.java          # 用户上下文
│   │   └── FileUploadUtil.java       # 文件上传工具
│   └── SecondHandApplication.java    # 启动类
├── src/main/resources/
│   ├── mapper/           # MyBatis XML映射文件
│   └── application.yml   # 应用配置
├── uploads/              # 文件上传目录
├── pom.xml               # Maven配置
└── target/               # 构建产物
```

---

## 四、数据库设计

### 4.1 实体关系图 (ERD)

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│   t_user    │       │ t_product   │       │   t_order   │
├─────────────┤       ├─────────────┤       ├─────────────┤
│ id (PK)     │◄──────│ userId(FK)  │       │ id (PK)     │
│ username    │       │ categoryId  │       │ orderNo     │
│ password    │       │ title       │       │ productId   │
│ nickname    │       │ description │       │ buyerId     │
│ avatar      │       │ price       │       │ sellerId    │
│ phone       │       │ images(JSON)│       │ price       │
│ email       │       │ status      │       │ status      │
│ role        │       │ viewCount   │       │ expireTime  │
│ status      │       └─────────────┘       └─────────────┘
└─────────────┘             │                   ▲
                            │                   │
                   ┌────────┴────────┐   ┌──────┴──────┐
                   │ t_comment       │   │ t_favorite  │
                   ├─────────────────┤   ├─────────────┤
                   │ id (PK)         │   │ id (PK)     │
                   │ productId (FK)  │   │ productId   │
                   │ userId (FK)     │   │ userId      │
                   │ content         │   └─────────────┘
                   │ rating          │
                   └─────────────────┘

                   ┌─────────────┐
                   │ t_category  │
                   ├─────────────┤
                   │ id (PK)     │
                   │ name        │
                   │ parentId    │
                   │ sortOrder   │
                   └─────────────┘
```

### 4.2 数据表详情

#### t_user（用户表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| username | VARCHAR(50) | 用户名 |
| password | VARCHAR(100) | 密码（MD5加密） |
| nickname | VARCHAR(50) | 昵称 |
| avatar | VARCHAR(255) | 头像URL |
| phone | VARCHAR(20) | 手机号 |
| email | VARCHAR(100) | 邮箱 |
| role | VARCHAR(20) | 角色：USER / ADMIN |
| status | INT | 状态：0-禁用 1-正常 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| deleted | INT | 逻辑删除 |

#### t_product（商品表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| title | VARCHAR(200) | 商品标题 |
| description | TEXT | 商品描述 |
| price | DECIMAL(10,2) | 售价 |
| original_price | DECIMAL(10,2) | 原价 |
| images | TEXT | 图片列表（JSON数组） |
| category_id | BIGINT | 分类ID |
| user_id | BIGINT | 发布者ID |
| status | VARCHAR(20) | 状态：ON_SALE / SOLD / OFF_SHELF |
| view_count | INT | 浏览量 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| deleted | INT | 逻辑删除 |

#### t_order（订单表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| order_no | VARCHAR(50) | 订单编号 |
| product_id | BIGINT | 商品ID |
| buyer_id | BIGINT | 买家ID |
| seller_id | BIGINT | 卖家ID |
| price | DECIMAL(10,2) | 成交价格 |
| buyer_name | VARCHAR(50) | 收货人姓名 |
| buyer_phone | VARCHAR(20) | 联系电话 |
| buyer_address | VARCHAR(255) | 收货地址 |
| expire_time | DATETIME | 预留过期时间（30分钟） |
| status | VARCHAR(20) | 状态：PENDING / PAID / SHIPPED / COMPLETED / CANCELLED |
| remark | TEXT | 备注 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| deleted | INT | 逻辑删除 |

#### t_comment（评论表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| product_id | BIGINT | 商品ID |
| user_id | BIGINT | 用户ID |
| content | TEXT | 评论内容 |
| rating | INT | 评分（1-5） |
| create_time | DATETIME | 创建时间 |
| deleted | INT | 逻辑删除 |

#### t_favorite（收藏表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| product_id | BIGINT | 商品ID |
| user_id | BIGINT | 用户ID |
| create_time | DATETIME | 创建时间 |

#### t_category（分类表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| name | VARCHAR(50) | 分类名称 |
| parent_id | BIGINT | 父分类ID（0为顶级） |
| sort_order | INT | 排序号 |
| create_time | DATETIME | 创建时间 |

---

## 五、核心功能模块

### 5.1 用户模块

**功能清单**:
- 用户注册：验证用户名唯一性，密码MD5加密存储
- 用户登录：验证用户名密码，生成JWT Token
- 获取当前用户信息：从Token解析用户ID，查询用户数据（屏蔽密码）
- 更新个人资料：支持昵称、头像、手机号、邮箱修改
- 修改密码：验证原密码，更新新密码

**认证流程**:
```
用户登录 → 验证用户名密码 → 生成JWT Token → 返回给前端
前端请求 → 请求拦截器携带Token → JWT拦截器验证 → 提取用户信息 → 业务处理
```

**关键文件**:
- [UserController.java](file:///d:/code/SecondHand-Shared-system/backend/src/main/java/com/campus/secondhand/controller/UserController.java)
- [UserServiceImpl.java](file:///d:/code/SecondHand-Shared-system/backend/src/main/java/com/campus/secondhand/service/impl/UserServiceImpl.java)
- [JwtInterceptor.java](file:///d:/code/SecondHand-Shared-system/backend/src/main/java/com/campus/secondhand/config/interceptor/JwtInterceptor.java)
- [JwtUtil.java](file:///d:/code/SecondHand-Shared-system/backend/src/main/java/com/campus/secondhand/util/JwtUtil.java)

### 5.2 商品模块

**功能清单**:
- 发布商品：上传图片，设置标题、描述、价格、分类
- 编辑商品：验证所有权，更新商品信息
- 删除商品：验证所有权或管理员权限，逻辑删除
- 下架商品：验证所有权，状态变更为OFF_SHELF
- 商品列表：支持分页、搜索、分类筛选
- 我的发布：查询当前用户发布的所有商品
- 商品详情：查询商品详情，浏览量+1，关联卖家信息

**商品状态流转**:
```
ON_SALE（在售） → SOLD（已售）
ON_SALE（在售） → OFF_SHELF（下架）
```

**关键文件**:
- [ProductController.java](file:///d:/code/SecondHand-Shared-system/backend/src/main/java/com/campus/secondhand/controller/ProductController.java)
- [ProductServiceImpl.java](file:///d:/code/SecondHand-Shared-system/backend/src/main/java/com/campus/secondhand/service/impl/ProductServiceImpl.java)
- [ProductVO.java](file:///d:/code/SecondHand-Shared-system/backend/src/main/java/com/campus/secondhand/vo/ProductVO.java)

### 5.3 订单模块

**功能清单**:
- 创建订单：关联商品、买家、卖家，生成订单编号，设置30分钟过期时间
- 订单列表：按买家或卖家身份查询订单
- 订单详情：查询订单完整信息
- 订单状态更新：付款、发货、完成、取消

**订单状态流转**:
```
PENDING（待付款） → PAID（已付款） → SHIPPED（已发货） → COMPLETED（已完成）
PENDING（待付款） → CANCELLED（已取消，超时自动取消）
```

**关键文件**:
- [OrderController.java](file:///d:/code/SecondHand-Shared-system/backend/src/main/java/com/campus/secondhand/controller/OrderController.java)
- [OrderServiceImpl.java](file:///d:/code/SecondHand-Shared-system/backend/src/main/java/com/campus/secondhand/service/impl/OrderServiceImpl.java)
- [Order.java](file:///d:/code/SecondHand-Shared-system/backend/src/main/java/com/campus/secondhand/entity/Order.java)

### 5.4 收藏模块

**功能清单**:
- 添加收藏：用户收藏商品
- 取消收藏：用户取消收藏
- 我的收藏：查询当前用户收藏的商品列表

**关键文件**:
- [FavoriteController.java](file:///d:/code/SecondHand-Shared-system/backend/src/main/java/com/campus/secondhand/controller/FavoriteController.java)
- [FavoriteServiceImpl.java](file:///d:/code/SecondHand-Shared-system/backend/src/main/java/com/campus/secondhand/service/impl/FavoriteServiceImpl.java)

### 5.5 评论模块

**功能清单**:
- 添加评论：用户对已购买商品发表评论和评分
- 评论列表：查询商品的所有评论
- 评论统计：统计商品评论数量和平均评分

**关键文件**:
- [CommentController.java](file:///d:/code/SecondHand-Shared-system/backend/src/main/java/com/campus/secondhand/controller/CommentController.java)
- [CommentServiceImpl.java](file:///d:/code/SecondHand-Shared-system/backend/src/main/java/com/campus/secondhand/service/impl/CommentServiceImpl.java)

### 5.6 分类模块

**功能清单**:
- 获取分类列表：支持多级分类查询
- 添加分类：管理员新增分类
- 修改分类：管理员修改分类信息
- 删除分类：管理员删除分类

**关键文件**:
- [CategoryController.java](file:///d:/code/SecondHand-Shared-system/backend/src/main/java/com/campus/secondhand/controller/CategoryController.java)
- [CategoryServiceImpl.java](file:///d:/code/SecondHand-Shared-system/backend/src/main/java/com/campus/secondhand/service/impl/CategoryServiceImpl.java)

### 5.7 文件模块

**功能清单**:
- 文件上传：支持图片上传，生成唯一文件名
- 文件下载：根据文件名下载文件
- 文件服务：静态文件访问

**关键文件**:
- [FileController.java](file:///d:/code/SecondHand-Shared-system/backend/src/main/java/com/campus/secondhand/controller/FileController.java)
- [FileServerController.java](file:///d:/code/SecondHand-Shared-system/backend/src/main/java/com/campus/secondhand/controller/FileServerController.java)
- [FileUploadUtil.java](file:///d:/code/SecondHand-Shared-system/backend/src/main/java/com/campus/secondhand/util/FileUploadUtil.java)

### 5.8 管理模块

**功能清单**:
- 仪表盘：统计用户数、商品数、订单数、分类统计
- 用户管理：用户列表、启用/禁用用户
- 商品管理：商品列表、搜索、删除商品
- 订单管理：订单列表、状态筛选
- 分类管理：分类列表、新增/编辑/删除分类

**关键文件**:
- [AdminController.java](file:///d:/code/SecondHand-Shared-system/backend/src/main/java/com/campus/secondhand/controller/AdminController.java)
- [AdminServiceImpl.java](file:///d:/code/SecondHand-Shared-system/backend/src/main/java/com/campus/secondhand/service/impl/AdminServiceImpl.java)
- [DashboardVO.java](file:///d:/code/SecondHand-Shared-system/backend/src/main/java/com/campus/secondhand/vo/DashboardVO.java)

---

## 六、前端页面路由

### 6.1 路由配置

| 路径 | 名称 | 组件 | 权限要求 |
|------|------|------|----------|
| /login | Login | Login.vue | 游客可访问 |
| /register | Register | Register.vue | 游客可访问 |
| / | Home | Home.vue | 公开 |
| /about | About | About.vue | 公开 |
| /help | Help | Help.vue | 公开 |
| /products | ProductList | ProductList.vue | 公开 |
| /product/:id | ProductDetail | ProductDetail.vue | 公开 |
| /publish | PublishProduct | PublishProduct.vue | 需登录 |
| /profile | Profile | Profile.vue | 需登录 |
| /orders | MyOrders | MyOrders.vue | 需登录 |
| /admin | Admin | Admin.vue | 需管理员 |

### 6.2 路由守卫

```
1. requiresAuth：需要登录才能访问，未登录重定向到/login
2. requiresAdmin：需要管理员权限，非管理员重定向到首页
3. guest：游客页面，已登录用户重定向到首页
```

**关键文件**:
- [router/index.js](file:///d:/code/SecondHand-Shared-system/frontend/src/router/index.js)

---

## 七、核心业务流程

### 7.1 用户注册/登录流程

```
┌──────────┐     ┌──────────────┐     ┌──────────────┐
│  前端页面  │     │   Controller │     │   Service    │
└────┬─────┘     └──────┬───────┘     └──────┬───────┘
     │                  │                     │
     │ POST /user/register                  │
     │─────────────────►│                     │
     │                  │                     │
     │                  │──验证用户名唯一性───►│
     │                  │                     │
     │                  │──密码MD5加密───────►│
     │                  │                     │
     │                  │──保存用户──────────►│
     │                  │                     │
     │◄─────────────────│                     │
     │   注册成功       │                     │
     │                  │                     │
     │ POST /user/login                    │
     │─────────────────►│                     │
     │                  │                     │
     │                  │──查询用户──────────►│
     │                  │                     │
     │                  │──验证密码──────────►│
     │                  │                     │
     │                  │──生成JWT Token─────►│
     │◄─────────────────│                     │
     │   返回Token      │                     │
     │                  │                     │
```

### 7.2 商品发布流程

```
┌──────────┐     ┌──────────────┐     ┌──────────────┐
│  前端页面  │     │   Controller │     │   Service    │
└────┬─────┘     └──────┬───────┘     └──────┬───────┘
     │                  │                     │
     │ POST /product   │                     │
     │─────────────────►│                     │
     │                  │                     │
     │                  │──获取当前用户ID─────►│
     │                  │                     │
     │                  │──图片列表转JSON─────►│
     │                  │                     │
     │                  │──设置状态ON_SALE────►│
     │                  │                     │
     │                  │──保存商品──────────►│
     │                  │                     │
     │◄─────────────────│                     │
     │   发布成功       │                     │
```

### 7.3 订单创建流程

```
┌──────────┐     ┌──────────────┐     ┌──────────────┐
│  前端页面  │     │   Controller │     │   Service    │
└────┬─────┘     └──────┬───────┘     └──────┬───────┘
     │                  │                     │
     │ POST /order     │                     │
     │─────────────────►│                     │
     │                  │                     │
     │                  │──查询商品信息───────►│
     │                  │                     │
     │                  │──验证商品在售───────►│
     │                  │                     │
     │                  │──生成订单编号───────►│
     │                  │                     │
     │                  │──设置30分钟过期─────►│
     │                  │                     │
     │                  │──保存订单──────────►│
     │                  │                     │
     │◄─────────────────│                     │
     │   下单成功       │                     │
```

### 7.4 商品购买流程

```
商品详情页 → 点击"立即购买" → 登录验证 → 填写收货信息 → 创建订单 → 跳转订单列表
                                                  │
                                                  ▼
                                      30分钟内完成付款（未付款自动取消）
```

---

## 八、配置说明

### 8.1 后端配置 (application.yml)

**服务端口**: 8080
**上下文路径**: /api

**数据库配置**:
- 数据库：secondhand_db
- 用户名：root
- 密码：12345
- 端口：3306

**JWT配置**:
- 密钥：campus-secondhand-jwt-secret-key-2024-very-long-string
- 过期时间：86400000ms（24小时）

**文件上传配置**:
- 最大文件大小：10MB
- 最大请求大小：20MB
- 上传路径：./uploads/

**关键文件**:
- [application.yml](file:///d:/code/SecondHand-Shared-system/backend/src/main/resources/application.yml)

### 8.2 前端配置 (vite.config.js)

**开发服务器**:
- 端口：5173（默认）
- 代理：/api → http://localhost:8080/api

**关键文件**:
- [vite.config.js](file:///d:/code/SecondHand-Shared-system/frontend/vite.config.js)

---

## 九、运行方式

### 9.1 后端运行

```bash
# 进入后端目录
cd backend

# 编译项目
mvn clean compile

# 运行项目（开发模式）
mvn spring-boot:run

# 或打包后运行
mvn clean package
java -jar target/secondhand-shared-1.0.0.jar
```

### 9.2 前端运行

```bash
# 进入前端目录
cd frontend

# 安装依赖
npm install

# 开发模式运行
npm run dev

# 构建生产版本
npm run build

# 预览生产版本
npm run preview
```

---

## 十、项目特点与亮点

### 10.1 技术特点

1. **前后端分离**: 前端Vue 3 + 后端Spring Boot，通过RESTful API通信
2. **JWT认证**: 无状态认证，Token存储在前端localStorage
3. **统一响应封装**: 使用Result类统一返回格式，包含code、message、data
4. **全局异常处理**: GlobalExceptionHandler统一处理异常，返回标准错误格式
5. **逻辑删除**: 使用MyBatis Plus的逻辑删除功能，数据安全
6. **自动填充**: 创建时间、更新时间自动填充
7. **分页查询**: 使用MyBatis Plus的分页插件

### 10.2 功能亮点

1. **商品分类**: 支持多级分类，首页展示分类卡片网格
2. **商品收藏**: 用户可收藏感兴趣的商品
3. **商品评论**: 支持评分和文字评论
4. **订单超时**: 创建订单后30分钟未付款自动取消
5. **管理后台**: 完整的管理员功能，包含数据统计仪表盘
6. **图片上传**: 支持多图片上传和预览
7. **响应式设计**: 适配桌面端和移动端

### 10.3 安全性

1. **密码加密**: 使用MD5加密存储密码
2. **JWT认证**: 请求拦截器验证Token
3. **权限控制**: 路由守卫控制页面访问权限
4. **操作权限验证**: 修改/删除操作验证用户所有权
5. **跨域配置**: 配置CORS允许前端访问

---

## 十一、待改进事项

### 11.1 技术改进

1. **密码加密强度**: 当前使用MD5，建议使用BCrypt或Argon2
2. **文件上传安全**: 需要增加文件类型校验、文件大小限制
3. **XSS防护**: 需要对用户输入进行过滤和转义
4. **SQL注入**: 当前使用MyBatis Plus，相对安全，但需要注意自定义SQL
5. **接口限流**: 增加接口访问频率限制，防止恶意请求
6. **单元测试**: 补充单元测试覆盖率

### 11.2 功能改进

1. **商品搜索**: 增加全文搜索功能
2. **商品推荐**: 根据用户浏览历史推荐商品
3. **消息通知**: 订单状态变更、评论回复等通知
4. **交易评价**: 买家卖家互评功能
5. **支付集成**: 集成微信/支付宝支付
6. **物流跟踪**: 订单物流信息跟踪
7. **聊天功能**: 买卖双方在线聊天

---

## 十二、总结

校园二手物品共享平台是一个完整的二手交易系统，包含用户管理、商品管理、订单管理、评论系统、收藏功能和管理后台等核心模块。技术栈采用Vue 3 + Spring Boot的主流组合，架构清晰，代码规范，具有良好的扩展性和可维护性。项目适合作为学习和开发校园二手交易平台的参考案例。