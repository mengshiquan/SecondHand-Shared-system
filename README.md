# 校园二手物品共享平台

前后端分离的校园二手交易平台，后端基于 Spring Boot 3.3 + Java 17 + MyBatis-Plus + JWT，前端基于 Vue 3 + Vite + Element Plus + Pinia + ECharts。

## 项目结构

```
SecondHand-Shared-system/
├── backend/          # Spring Boot 后端
├── frontend/         # Vue 3 前端
├── sql/              # 数据库脚本
├── picture/          # 截图/图片资源
└── README.md
```

## 技术栈

### 后端
- Spring Boot 3.3.5（最低 JDK 17）
- MyBatis-Plus 3.5.9
- MySQL 8.0
- JJWT 0.12.6（HMAC-SHA384）
- Hutool 5.8.25
- Lombok

### 前端
- Vue 3 + Vite
- Element Plus + Icons
- Pinia 状态管理
- Vue Router 4
- Axios
- ECharts（数据图表）

## 设计规范

| 设计元素 | 色值/规格 |
|---------|----------|
| 主题色 | `#10B981`（翠绿） |
| 强调色 | `#F59E0B`（暖橙） |
| 背景色 | `#F0F9F4`（浅绿底） |
| 字体 | Helvetica Neue / PingFang SC / Microsoft YaHei |
| 卡片圆角 | 12px |
| 按钮圆角 | 6px |

## 功能模块

| 模块 | 功能 |
|------|------|
| 用户模块 | 登录、注册、个人资料、修改密码 |
| 商品模块 | 发布、编辑、删除、下架、分页列表、搜索、分类筛选 |
| 订单模块 | 创建订单、状态流转（待确认→已付款→已发货→已完成） |
| 收藏模块 | 收藏/取消收藏、收藏列表 |
| 评论模块 | 发表评论（1-5 星评分）、评论列表 |
| 后台管理 | 数据概览（分类分布饼图 + 订单状态柱状图）、用户/商品/订单/分类管理 |

## 快速启动

### 环境要求

- **JDK 17+**（后端 Spring Boot 3.3 必须）
- Maven 3.6+
- MySQL 8.0
- Node.js 18+

### 1. 初始化数据库

```bash
mysql -u root -p < sql/schema.sql
```

修改 `backend/src/main/resources/application.yml` 中的数据库连接信息。

### 2. 启动后端

确保 `JAVA_HOME` 指向 JDK 17 路径（如 `D:\java\jdk17`），否则 Maven 会因版本不兼容报错。

```bash
cd backend
# Windows：如 Maven 使用 Java 8，设置环境变量
set JAVA_HOME=D:\java\jdk17
mvn spring-boot:run
```

> 后端运行在 `http://localhost:8080/api`

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

> 前端运行在 `http://localhost:5173`，已配置 Vite 代理转发 `/api` 到后端 8080 端口。

## 演示账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin | 管理员 |
| student | 123456 | 普通用户 |

## API 接口概览

### 用户 `/user`
- `POST /login` - 登录
- `POST /register` - 注册
- `GET /info` - 获取当前用户信息
- `PUT /profile` - 更新资料
- `PUT /password` - 修改密码

### 商品 `/product`
- `GET /list` - 商品列表（分页、搜索、分类）
- `GET /detail/{id}` - 商品详情
- `GET /my` - 我的发布
- `POST /` - 发布商品
- `PUT /` - 编辑商品
- `DELETE /{id}` - 删除商品
- `PUT /{id}/off-shelf` - 下架

### 订单 `/order`
- `POST /` - 创建订单
- `GET /list` - 订单列表
- `GET /{id}` - 订单详情
- `PUT /{id}/status` - 更新状态

### 收藏 `/favorite`
- `POST /{productId}` - 收藏/取消
- `GET /list` - 收藏列表

### 评论 `/comment`
- `POST /` - 发表评论
- `GET /list/{productId}` - 评论列表

### 文件 `/file`
- `POST /upload` - 图片上传

### 后台 `/admin`（需管理员权限）
- `GET /dashboard` - 数据概览
- `GET /users` - 用户管理
- `GET /products` - 商品管理
- `GET /orders` - 订单管理
- `POST /category` - 分类管理

## 统一返回格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```
