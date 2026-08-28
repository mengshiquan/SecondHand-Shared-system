# 第一批功能增强：交易基础补全 — 设计文档

> **日期**: 2026-08-28
> **范围**: 校园认证、地址管理、购物车、订单增强、超级管理员、销售统计、CRUD补全、前端页面
> **状态**: 已确认

---

## 1. 总览

| # | 模块 | 优先级 | 复杂度 |
|---|------|:------:|:------:|
| 1 | 校园身份认证 | P0 | 中 |
| 2 | 地址管理 | P0 | 低 |
| 3 | 购物车 | P0 | 中 |
| 4 | 订单增强（退款流程） | P0 | 高 |
| 5 | 超级管理员 + 管理员管理 | P1 | 中 |
| 6 | 销售统计报表 | P1 | 中 |
| 7 | CRUD 补全 | P0 | 中 |
| 8 | 前端页面适配 | P0 | 中 |

---

## 2. 校园身份认证模块

### 2.1 数据模型变更

```sql
ALTER TABLE t_user ADD COLUMN student_id    VARCHAR(20)  DEFAULT NULL COMMENT '学号';
ALTER TABLE t_user ADD COLUMN school_name   VARCHAR(100) DEFAULT NULL COMMENT '学校名称';
ALTER TABLE t_user ADD COLUMN verify_status VARCHAR(20)  DEFAULT NULL COMMENT '认证状态 PENDING/APPROVED/REJECTED';
```

### 2.2 注册流程改造

- 学号必须为纯数字 12 位（正则：`^\d{12}$`），页面不提示具体规则
- 格式合法 → 自动通过（APPROVED），格式不合法 → 注册失败
- 已有用户默认 APPROVED，仅新注册用户需认证

### 2.3 权限限制（未认证用户）

- ✅ 浏览商品列表、查看详情、查看分类
- ❌ 发布、购买、评论、收藏、加购物车

### 2.4 管理员审核

| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | /admin/users/verify | 批量审核 |
| GET | /admin/users?verifyStatus=PENDING | 查看待审核 |

---

## 3. 地址管理模块

### 3.1 数据模型

```sql
CREATE TABLE IF NOT EXISTS t_address (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL COMMENT '所属用户ID',
    receiver_name VARCHAR(50)  NOT NULL COMMENT '收货人姓名',
    phone         VARCHAR(20)  NOT NULL COMMENT '手机号',
    address       VARCHAR(255) NOT NULL COMMENT '详细地址',
    is_default    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';
```

### 3.2 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /address/list | 获取所有地址 |
| POST | /address | 新增 |
| PUT | /address/{id} | 修改 |
| DELETE | /address/{id} | 删除 |
| PUT | /address/{id}/default | 设为默认 |

### 3.3 规则

- 每用户最多 10 个地址，首个自动默认
- 设默认时取消其他默认，删默认后自动选最近一个

---

## 4. 购物车模块

### 4.1 数据模型

```sql
CREATE TABLE IF NOT EXISTS t_cart (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT   NOT NULL,
    product_id  BIGINT   NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT  NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_product (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';
```

### 4.2 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /cart/list | 列表（含商品详情+失效标记） |
| POST | /cart | 加入购物车 |
| DELETE | /cart/{id} | 移除 |
| DELETE | /cart/batch | 批量移除 |
| DELETE | /cart/clear | 清空 |
| POST | /cart/checkout | 结算（单品/批量） |

### 4.3 规则

- 同一商品不可重复加购
- 商品已失效时跳过，需认证通过才能操作
- 结算：选商品→选地址→逐件创建订单→商品锁定→清购物车项

---

## 5. 订单增强模块

### 5.1 数据模型变更

```sql
ALTER TABLE t_order ADD COLUMN address_id     BIGINT      DEFAULT NULL COMMENT '收货地址ID';
ALTER TABLE t_order ADD COLUMN refund_status  VARCHAR(20) DEFAULT NULL COMMENT '退款状态';
ALTER TABLE t_order ADD COLUMN refund_reason  VARCHAR(255) DEFAULT NULL COMMENT '退款原因';
ALTER TABLE t_order ADD COLUMN payment_time   DATETIME    DEFAULT NULL COMMENT '付款时间（预留）';
```

### 5.2 退款流程

```
买家申请退款（PAID/SHIPPED）
    ├→ 卖家同意 → 订单取消，商品恢复
    ├→ 卖家拒绝 → 买家72h内可申请仲裁
    │     └→ 管理员裁定退款/维持
    └→ 超时48h未处理 → @Scheduled 自动退款
```

### 5.3 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /order/{id}/cancel | 取消（仅PENDING） |
| POST | /order/{id}/refund | 申请退款 |
| PUT | /order/{id}/refund/handle | 卖家处理 |
| POST | /order/{id}/arbitration | 申请仲裁 |
| PUT | /admin/order/{id}/arbitration | 管理员仲裁 |
| PUT | /order/{id}/address | 修改地址（未发货） |
| DELETE | /order/{id} | 删除已完成/已取消订单 |

---

## 6. 超级管理员 + 管理员管理模块

### 6.1 角色模型

| 角色 | 说明 | 权限 |
|------|------|------|
| `SUPER_ADMIN` | 超级管理员（仅现有 admin 账号） | 全部权限 + 管理员管理 |
| `ADMIN` | 普通管理员 | 常规后台管理，无管理员管理权 |
| `USER` | 普通用户 | 前台功能 |

### 6.2 权限差异

| 功能 | SUPER_ADMIN | ADMIN |
|------|:-----------:|:-----:|
| 管理员增删改查 | ✅ | ❌ |
| 用户管理 | ✅ | ✅ |
| 商品/订单/分类管理 | ✅ | ✅ |
| 投诉/申诉/小黑屋 | ✅ | ✅ |
| 销售统计 | ✅ | ✅ |
| 身份认证审核 | ✅ | ✅ |

### 6.3 管理员管理 API（仅 SUPER_ADMIN）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /admin/admins | 管理员列表 |
| POST | /admin/admins | 新增（自动生成8位密码） |
| PUT | /admin/admins/{id} | 修改 |
| DELETE | /admin/admins/{id} | 删除 |
| PUT | /admin/admins/{id}/status | 启用/禁用 |

### 6.4 规则

- `SUPER_ADMIN` 不可被删除/禁用/修改角色
- 普通管理员不能操作自己
- 至少保留 1 个管理员

---

## 7. 销售统计报表模块（新增）

### 7.1 功能描述

仅管理员（SUPER_ADMIN + ADMIN）可见。统计每日/每周/每月商品销售情况，支持导出为 Excel 表格。

### 7.2 统计维度

| 维度 | 说明 |
|------|------|
| 按时间 | 日/周/月切换，展示该时段内订单数量、成交金额 |
| 按商品 | 每件商品的销售次数、总成交额 |
| 按分类 | 各分类的销售占比、成交金额 |
| 按卖家 | 各卖家销售件数、成交总额 |

### 7.3 API 设计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /admin/stats/sales | 销售统计（参数：period=day/week/month, startDate, endDate） |
| GET | /admin/stats/sales/export | 导出 Excel（同参数，返回文件流） |
| GET | /admin/stats/category-sales | 分类销售统计 |
| GET | /admin/stats/seller-ranking | 卖家排行 |

### 7.4 统计数据来源

基于 `t_order` 表中 `status = 'COMPLETED'` 的订单：
- 销售数量 = 已完成订单数
- 成交金额 = SUM(price)
- 时间范围 = payment_time 或 update_time

### 7.5 导出格式

- 使用 Apache POI 或 EasyExcel 生成 `.xlsx` 文件
- 表头：日期 | 商品标题 | 卖家 | 买家 | 成交价 | 订单编号
- 支持按日期范围筛选后导出

### 7.6 前端展示

- Admin.vue 新增"销售统计"tab
- ECharts 折线图（按日/周/月销售额趋势）
- ECharts 饼图（分类占比）
- 表格展示明细 + "导出 Excel"按钮

---

## 8. CRUD 补全（现有模块增删改查完善）

### 8.1 后台-用户管理补全

**现状**：有列表 + 启用/禁用，缺增、改、删。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /admin/users | 创建用户 |
| PUT | /admin/users/{id} | 编辑用户 |
| DELETE | /admin/users/{id} | 删除用户（级联清理） |
| PUT | /admin/users/{id}/reset-password | 重置密码 |

**删除级联**：商品下架、订单取消、购物车/收藏/地址清除、状态设为已注销。

### 8.2 后台-商品管理补全

| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | /admin/products/{id}/status | 修改状态（上架/下架） |
| DELETE | /admin/products/{id} | 管理员删除 |

### 8.3 后台-订单管理补全

| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | /admin/orders/{id}/status | 管理员强制改状态 |
| DELETE | /admin/orders/{id} | 删除已完成订单 |

### 8.4 个人中心-我的收藏补全

| 方法 | 路径 | 说明 |
|------|------|------|
| DELETE | /favorite/batch | 批量取消收藏 |
| GET | /favorite/list?keyword=xxx | 搜索收藏 |

### 8.5 个人中心-我的订单补全

| 方法 | 路径 | 说明 |
|------|------|------|
| DELETE | /order/{id} | 删除已完成/已取消订单 |

### 8.6 已完整的模块

- 个人中心-我的发布 ✅
- 后台-分类管理 ✅

---

## 9. 前端页面适配

### 9.1 新增页面

| 页面 | 路径 | 说明 |
|------|------|------|
| 购物车 | /cart | 商品列表+失效标记+结算 |
| 结算页 | /checkout | 确认订单+选地址 |

### 9.2 现有页面改造

| 页面 | 改动 |
|------|------|
| Profile.vue | 新增"收货地址"tab |
| Register.vue | 新增学号+学校名称 |
| MyOrders.vue | 退款操作+删除订单 |
| ProductDetail.vue | "加入购物车"按钮 |
| Admin.vue | 管理员管理 + 销售统计 + 用户/商品/订单增删改 + 认证审核 |

### 9.3 路由

```javascript
{ path: '/cart', name: 'Cart', component: () => import('@/views/Cart.vue'), meta: { requiresAuth: true } },
{ path: '/checkout', name: 'Checkout', component: () => import('@/views/Checkout.vue'), meta: { requiresAuth: true } },
```

---

## 10. 实施顺序

```
校园认证 → 地址管理 → 购物车 → 订单增强 → 超级管理员+管理员管理 → 销售统计 → CRUD补全 → 前端
```

---

## 11. 数据库变更汇总

### 新建表
- `t_address`、`t_cart`

### 修改表
- `t_user`：+student_id, school_name, verify_status；role 值域扩展为 USER/ADMIN/SUPER_ADMIN
- `t_order`：+address_id, refund_status, refund_reason, payment_time

### 新依赖（后端）
- EasyExcel（Excel 导出）

---

## 12. 非目标（本次不做）

- 模拟支付集成（仅预留字段）
- 物流追踪
- 即时聊天
- 商品推荐算法
- 交易评价系统
