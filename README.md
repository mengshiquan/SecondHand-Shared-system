# 校园二手物品共享平台

前后端分离的校园二手交易平台，后端基于 Spring Boot 3.3 + Java 17 + MyBatis-Plus + JWT + Redis，前端基于 Vue 3 + Vite + Element Plus + Pinia + ECharts。支付集成支付宝沙箱扫码与微信模拟收银台。

## 项目结构

```
SecondHand-Shared-system/
├── backend/          # Spring Boot 后端
├── frontend/         # Vue 3 前端
├── sql/              # 数据库脚本（schema + 增量 migration）
├── docs/             # 设计文档/论文资料
└── README.md
```

## 技术栈

### 后端
- Spring Boot 3.3.5（最低 JDK 17）
- MyBatis-Plus 3.5.9
- MySQL 8.0
- Redis（Lettuce：分类树/商品详情缓存、分布式锁、频控、令牌黑名单）
- JJWT 0.12.6（HMAC-SHA384）
- spring-security-crypto（BCrypt 密码哈希）
- EasyExcel 3.3.4（表格导出）
- alipay-sdk-java 4.39.x（支付宝沙箱扫码支付）
- Hutool 5.8.25、Lombok

### 前端
- Vue 3 + Vite
- Element Plus + Icons
- Pinia 状态管理
- Vue Router 4
- Axios
- ECharts（数据图表）
- qrcode（支付二维码渲染）

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
| 用户模块 | 登录/注册（学号+学校）、个人资料、修改密码、校园认证申请、收货地址管理、消息通知中心 |
| 商品模块 | 发布/编辑/删除/下架、分页列表、关键词搜索、两级分类筛选、排序（综合/人气/价格升降）、收藏、咨询卖家（日限频控） |
| 购物车 | 加购、按卖家分组、全选/单选、失效商品管理、移入收藏、结算下单 |
| 订单模块 | 下单（30 分钟预留超时自动取消）、支付（支付宝沙箱扫码 / 微信模拟收银台）、发货、确认收货、退款/售后、平台仲裁、订单删除 |
| 支付模块 | 支付宝沙箱 precreate 扫码付（RSA2 签名、异步回调验签、轮询主动查询补单）、微信模拟收银台（模拟扫码回调）、CAS 幂等落单 |
| 后台管理 | 数据概览图表、用户管理（认证审核/角色/禁用/小黑屋）、商品/订单/分类管理、投诉与申诉处理、仲裁判定、销售统计、用户/商品/订单/销售 Excel 导出 |

### 订单状态机

```
PENDING(待付款) → PAID(已付款) → SHIPPED(已发货) → COMPLETED(已完成)
       ↘ CANCELLED(已取消)
退款状态：NONE → REQUESTED → SELLER_AGREED / SELLER_REJECTED → ARBITRATION → ARBITRATION_REFUND / ARBITRATION_MAINTAIN
```

## 快速启动

### 环境要求

- **JDK 17+**（Spring Boot 3.3 必须）
- Maven 3.6+
- MySQL 8.0
- Redis 6+
- Node.js 18+

### 1. 初始化数据库

```bash
mysql -u root -p < sql/schema.sql
# schema 之后按顺序补三块增量（通知、投诉/小黑屋、支付渠道）
mysql -u root -p < sql/migration_notification.sql
mysql -u root -p < sql/migration_blacklist.sql
mysql -u root -p < sql/migration_pay_channel.sql
```

> `migration_batch1.sql`、`migration_subcategories.sql` 的内容已合并进 `schema.sql`，新库不要重复执行。老库升级按需挑选 migration 执行。

修改 `backend/src/main/resources/application.yml` 中的数据库/Redis 连接信息（均支持环境变量覆盖：`DB_USERNAME`、`DB_PASSWORD`、`REDIS_HOST` 等）。

### 2. 启动后端

确保 `JAVA_HOME` 指向 JDK 17 路径（如 `D:\java\jdk17`），否则 Maven 会因版本不兼容报错。

```bash
cd backend
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

## 支付配置（支付宝沙箱）

`application.yml` 中的 `pay.alipay` 段：

```yaml
pay:
  alipay:
    app-id: ${ALIPAY_APP_ID:}          # 沙箱 APPID
    private-key: ${ALIPAY_PRIVATE_KEY:}      # 应用私钥（RSA2）
    alipay-public-key: ${ALIPAY_PUBLIC_KEY:} # 支付宝公钥（验签）
    gateway: https://openapi-sandbox.dl.alipaydev.com/gateway.do
    notify-url: http://localhost:8080/api/pay/alipay/notify
```

- 三个凭据从 [支付宝开放平台沙箱控制台](https://open.alipay.com/develop/sandbox/app) 领取，建议用环境变量注入，不要提交到公开仓库
- 留空视为未配置：前端支付宝选项提示不可用，**微信模拟支付不受影响**
- 本地无公网时异步回调不可达，由 `/pay/status` 轮询接口主动调用 `alipay.trade.query` 补单；部署公网后回调自然生效
- 微信支付无个人开放沙箱（需营业执照+商户号），本项目采用模拟收银台：模拟二维码 + 模拟扫码回调端点，接口形态与真实微信 notify 对齐

## 演示账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin | 管理员 |
| student | 123456 | 普通用户 |

## API 接口概览

### 用户 `/user`
- `POST /login` / `POST /register`
- `GET /info` / `PUT /profile` / `PUT /password`

### 商品 `/product`
- `GET /list`（分页/搜索/分类/排序）、`GET /detail/{id}`、`GET /my`
- `POST /`、`PUT /`、`DELETE /{id}`、`PUT /{id}/off-shelf`

### 购物车 `/cart`
- `POST /{productId}` 加购、`GET /list`、`DELETE /{id}`、`DELETE /batch`
- `POST /move-to-favorite`、`DELETE /clear`、`POST /checkout` 结算

### 地址 `/address`
- `GET /list`、`POST /`、`PUT /{id}`、`DELETE /{id}`、`PUT /{id}/default`

### 订单 `/order`
- `POST /` 创建、`GET /list`、`GET /{id}`、`GET /status-counts`
- `PUT /{id}/status`（发货/收货）、`POST /{id}/cancel`、`PUT /{id}/address`
- `POST /{id}/refund` 申请退款、`PUT /{id}/refund/handle` 卖家处理、`POST /{id}/arbitration` 申请仲裁、`DELETE /{id}`

### 支付 `/pay`
- `POST /alipay/create/{orderId}` 支付宝预下单（返回沙箱二维码）
- `POST /alipay/notify` 支付宝异步回调（免登录，验签）
- `POST /wechat/create/{orderId}` 微信模拟预下单
- `POST /wechat/notify/{orderNo}` 微信模拟扫码回调（免登录）
- `GET /status/{orderId}` 支付状态轮询（含支付宝主动查询补单）

### 收藏 `/favorite` · 评论 `/comment` · 通知 `/notification`
- `POST /{productId}` 收藏/取消、`GET /list`
- `POST /` 发表评论、`GET /list/{productId}`
- `GET /list`、`GET /unread-count`、`PUT /{id}/read`、`PUT /read-all`

### 文件 `/file`
- `POST /upload` 图片上传

### 后台 `/admin`（需管理员权限）
- `GET /dashboard` 数据概览、销售统计
- 用户管理（含 `PUT /users/verify` 认证审核）、商品/订单/分类管理
- 投诉/申诉处理、`PUT /order/{id}/arbitration` 仲裁判定
- `GET /export/users|products|orders` 表格导出（EasyExcel）

## 统一返回格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```
