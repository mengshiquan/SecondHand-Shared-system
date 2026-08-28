# 第一批功能增强：交易基础补全 — 设计文档

> **日期**: 2026-08-28
> **范围**: 地址管理、购物车、订单增强、校园身份认证、管理员管理、CRUD补全、前端页面
> **状态**: 已确认

---

## 1. 总览

本次增强将项目从"演示级"升级为"可实际使用级"，补全交易闭环和所有模块的完整增删改查。

### 模块清单

| # | 模块 | 优先级 | 复杂度 |
|---|------|:------:|:------:|
| 1 | 校园身份认证 | P0 | 中 |
| 2 | 地址管理 | P0 | 低 |
| 3 | 购物车 | P0 | 中 |
| 4 | 订单增强（退款流程） | P0 | 高 |
| 5 | 管理员管理 | P1 | 低 |
| 6 | CRUD 补全 | P0 | 中 |
| 7 | 前端页面适配 | P0 | 中 |

---

## 2. 校园身份认证模块

### 2.1 数据模型变更

`t_user` 表新增字段：

```sql
ALTER TABLE t_user ADD COLUMN student_id    VARCHAR(20)  DEFAULT NULL COMMENT '学号';
ALTER TABLE t_user ADD COLUMN school_name   VARCHAR(100) DEFAULT NULL COMMENT '学校名称';
ALTER TABLE t_user ADD COLUMN verify_status VARCHAR(20)  DEFAULT NULL COMMENT '认证状态 PENDING/APPROVED/REJECTED';
```

### 2.2 认证状态枚举

- `PENDING` — 待审核
- `APPROVED` — 已通过
- `REJECTED` — 已拒绝

### 2.3 注册流程改造

注册表单新增：学号、学校名称。

**自动校验规则**：
- 学号必须为纯数字，恰好 12 位（正则：`^\d{12}$`）
- 页面不提示具体格式规则，仅显示"学号格式不正确"
- 格式合法 → `verify_status = APPROVED`（自动通过）
- 格式不合法 → 注册失败，不允许提交

**权限限制**（未通过认证的用户）：
- ✅ 可以：浏览商品列表、查看商品详情、查看分类
- ❌ 不能：发布商品、购买商品、发表评论、收藏、加入购物车

### 2.4 认证校验实现

在需要认证操作的 Service 层增加校验：

```java
private void requireVerified() {
    User user = UserContext.getCurrentUser();
    if (!"APPROVED".equals(user.getVerifyStatus())) {
        throw new BusinessException("请先完成校园身份认证");
    }
}
```

### 2.5 管理员审核

| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | /admin/users/verify | 批量审核（传 userId 数组 + action） |
| GET | /admin/users?verifyStatus=PENDING | 查看待审核用户 |

### 2.6 对已有用户的影响

已有用户 `verify_status` 默认为 `APPROVED`（不做存量限制），仅新注册用户需要认证。

---

## 3. 地址管理模块

### 3.1 数据模型

```sql
CREATE TABLE IF NOT EXISTS t_address (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '地址ID',
    user_id       BIGINT       NOT NULL COMMENT '所属用户ID',
    receiver_name VARCHAR(50)  NOT NULL COMMENT '收货人姓名',
    phone         VARCHAR(20)  NOT NULL COMMENT '手机号',
    address       VARCHAR(255) NOT NULL COMMENT '详细地址',
    is_default    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认 0-否 1-是',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';
```

### 3.2 API 设计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /address/list | 获取当前用户所有地址 |
| POST | /address | 新增地址 |
| PUT | /address/{id} | 修改地址 |
| DELETE | /address/{id} | 删除地址 |
| PUT | /address/{id}/default | 设为默认地址 |

### 3.3 业务规则

- 每个用户最多 10 个地址
- 新增第一个地址时自动设为默认
- 设为默认时，自动取消其他地址的默认状态
- 删除默认地址后，自动将最近一个设为默认
- 地址只能被本人操作

---

## 4. 购物车模块

### 4.1 数据模型

```sql
CREATE TABLE IF NOT EXISTS t_cart (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '购物车项ID',
    user_id     BIGINT   NOT NULL COMMENT '用户ID',
    product_id  BIGINT   NOT NULL COMMENT '商品ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_user_product (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';
```

> 校园二手每件商品数量为 1，不需要 quantity 字段。

### 4.2 API 设计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /cart/list | 获取购物车列表（含商品详情+失效标记） |
| POST | /cart | 加入购物车 |
| DELETE | /cart/{id} | 移除购物车项 |
| DELETE | /cart/batch | 批量移除（传 id 数组） |
| DELETE | /cart/clear | 清空购物车 |
| POST | /cart/checkout | 结算下单 |

### 4.3 业务规则

- 同一商品不能重复加购（UNIQUE KEY 保证）
- 商品已购买/下架时标记"已失效"，结算时跳过
- 加购时校验：商品存在 + ON_SALE + 非自己发布
- 需要认证通过才能加购和结算

### 4.4 结算流程

```
用户选择商品（单品或批量）
    → 选择收货地址
    → 创建订单（每件商品独立订单）
    → 商品状态 → OFF_SHELF（30分钟锁定）
    → 购物车项删除
```

---

## 5. 订单增强模块（退款流程）

### 5.1 数据模型变更

```sql
ALTER TABLE t_order ADD COLUMN address_id     BIGINT      DEFAULT NULL COMMENT '收货地址ID';
ALTER TABLE t_order ADD COLUMN refund_status  VARCHAR(20) DEFAULT NULL COMMENT '退款状态';
ALTER TABLE t_order ADD COLUMN refund_reason  VARCHAR(255) DEFAULT NULL COMMENT '退款原因';
ALTER TABLE t_order ADD COLUMN payment_time   DATETIME    DEFAULT NULL COMMENT '付款时间（预留模拟支付）';
```

### 5.2 退款状态枚举

- `NONE` — 无退款
- `REQUESTED` — 买家已申请
- `SELLER_AGREED` — 卖家同意，退款完成
- `SELLER_REJECTED` — 卖家拒绝
- `ARBITRATION` — 买家申请仲裁
- `ARBITRATION_REFUND` — 管理员裁定退款
- `ARBITRATION_MAINTAIN` — 管理员裁定维持

### 5.3 订单状态机（扩展后）

```
PENDING → PAID → SHIPPED → COMPLETED
   ↓         ↓
CANCELLED   REFUND_REQUESTED
               ├→ SELLER_AGREED（订单→CANCELLED，商品→ON_SALE）
               ├→ SELLER_REJECTED
               │     └→ ARBITRATION → REFUND / MAINTAIN
               └→ 超时48h自动退款
```

### 5.4 新增 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /order/{id}/cancel | 买家取消订单（仅 PENDING） |
| POST | /order/{id}/refund | 买家申请退款（仅 PAID/SHIPPED） |
| PUT | /order/{id}/refund/handle | 卖家处理（同意/拒绝） |
| POST | /order/{id}/arbitration | 买家申请仲裁 |
| PUT | /admin/order/{id}/arbitration | 管理员仲裁 |
| PUT | /order/{id}/address | 修改收货地址（仅未发货） |

### 5.5 业务规则

- 买家取消：仅 PENDING 状态，取消后商品恢复 ON_SALE
- 退款：仅 PAID/SHIPPED 状态
- 卖家 48h 未处理 → Spring `@Scheduled` 定时任务自动同意退款
- 买家可在卖家拒绝后 72h 内申请仲裁

---

## 6. 管理员管理模块

### 6.1 功能

管理员对管理员账号进行增删改查（所有 ADMIN 角色均可操作，无超级管理员概念）。

### 6.2 API 设计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /admin/admins | 管理员列表（分页） |
| POST | /admin/admins | 新增管理员（系统生成初始密码） |
| PUT | /admin/admins/{id} | 修改管理员信息 |
| DELETE | /admin/admins/{id} | 删除管理员（逻辑删除） |
| PUT | /admin/admins/{id}/status | 启用/禁用管理员 |

### 6.3 业务规则

- 至少保留 1 个管理员
- 管理员不能删除/禁用自己
- 新增时自动生成随机 8 位密码
- 删除为逻辑删除

---

## 7. CRUD 补全（现有模块增删改查完善）

### 7.1 后台-用户管理补全

**现状**：有分页列表 + 启用/禁用，缺增、改、删。

**新增 API**：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /admin/users | 手动创建用户（用户名+密码+昵称+角色） |
| PUT | /admin/users/{id} | 编辑用户信息（昵称/邮箱/手机） |
| DELETE | /admin/users/{id} | 删除用户（逻辑删除+级联清理） |
| PUT | /admin/users/{id}/reset-password | 重置密码 |

**删除用户级联处理**：
- 该用户的商品全部下架
- 未完成订单取消
- 购物车清空、收藏清除、地址删除
- 账号状态设为已注销（防止再次登录）
- 不能删除自己，不能删除最后一个管理员

### 7.2 后台-商品管理补全

**现状**：有分页列表，缺改、删。

**新增 API**：

| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | /admin/products/{id}/status | 修改商品状态（上架/下架/强制删除） |
| DELETE | /admin/products/{id} | 管理员删除商品（逻辑删除） |

### 7.3 后台-订单管理补全

**现状**：有分页列表，缺改、删。

**新增 API**：

| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | /admin/orders/{id}/status | 管理员修改订单状态（强制完成/取消） |
| DELETE | /admin/orders/{id} | 管理员删除订单（仅已完成的，逻辑删除） |

### 7.4 个人中心-我的收藏补全

**现状**：有列表 + 取消收藏（单个），缺批量操作和搜索。

**新增/增强**：

| 方法 | 路径 | 说明 |
|------|------|------|
| DELETE | /favorite/batch | 批量取消收藏 |
| GET | /favorite/list?keyword=xxx | 支持关键词搜索收藏列表 |

### 7.5 个人中心-我的订单补全

**现状**：有列表/详情/状态更新，缺删除。

**新增**：

| 方法 | 路径 | 说明 |
|------|------|------|
| DELETE | /order/{id} | 删除已完成/已取消的订单（逻辑删除，仅本人） |

### 7.6 个人中心-我的发布补全

**现状**：已有完整增删改查 ✅（发布/编辑/下架/删除/列表），无需补充。

### 7.7 后台-分类管理

**现状**：已有完整增删改查 ✅（新增/编辑/删除/列表），无需补充。

---

## 8. 前端页面适配

### 8.1 新增页面

| 页面 | 路径 | 说明 |
|------|------|------|
| 购物车页面 | /cart | 商品列表+失效标记+单品/批量结算 |
| 结算页面 | /checkout | 确认订单+选择收货地址 |

### 8.2 现有页面改造

| 页面 | 改动 |
|------|------|
| Profile.vue | 新增"收货地址"tab（地址 CRUD） |
| Register.vue | 新增学号+学校名称字段 |
| MyOrders.vue | 新增退款操作+退款状态+删除已完成订单 |
| ProductDetail.vue | 新增"加入购物车"按钮 |
| Admin.vue | 新增：管理员管理、用户增删改、商品管理增删、订单管理增删、认证审核 |

### 8.3 路由新增

```javascript
{ path: '/cart', name: 'Cart', component: () => import('@/views/Cart.vue'), meta: { requiresAuth: true } },
{ path: '/checkout', name: 'Checkout', component: () => import('@/views/Checkout.vue'), meta: { requiresAuth: true } },
```

---

## 9. 依赖关系与实施顺序

```
校园认证（改造注册流程）
    ↓
地址管理（独立，无前置依赖）
    ↓
购物车（依赖：地址管理，用于结算时选地址）
    ↓
订单增强（依赖：地址管理，用于订单关联地址）
    ↓
管理员管理 + CRUD补全（独立，无前置依赖）
    ↓
前端页面（依赖：以上所有后端完成）
```

建议实施顺序：**认证 → 地址 → 购物车 → 订单 → 管理员+CRUD → 前端**

---

## 10. 数据库变更汇总

### 新建表
- `t_address`（收货地址）
- `t_cart`（购物车）

### 修改表
- `t_user`：新增 student_id, school_name, verify_status
- `t_order`：新增 address_id, refund_status, refund_reason, payment_time

### 索引
- `t_address`: idx_user(user_id)
- `t_cart`: uk_user_product(user_id, product_id)

---

## 11. 非目标（本次不做）

- 模拟支付集成（仅预留字段）
- 物流追踪
- 即时聊天
- 商品推荐算法
- 交易评价系统
