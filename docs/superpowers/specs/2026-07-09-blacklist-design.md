# 小黑屋功能设计文档

> 创建日期: 2026-07-09 | 状态: 待实现

## 一、概述

对信誉低的卖家和恶意差评用户实施账号限制。支持系统自动判定拉黑、用户投诉、用户申诉、管理员审核四种场景。

### 核心目标
- 自动识别恶意用户并限制其行为
- 提供投诉渠道让普通用户举报违规行为
- 给被拉黑用户提供申诉途径
- 管理员拥有最终裁定权

---

## 二、数据模型

### 2.1 t_user 新增字段

```sql
ALTER TABLE t_user ADD COLUMN blacklist_status VARCHAR(20) DEFAULT NULL COMMENT 'NULL=正常, AUTO=系统拉黑, MANUAL=管理员拉黑';
ALTER TABLE t_user ADD COLUMN blacklist_reason VARCHAR(255) DEFAULT NULL COMMENT '拉黑原因';
ALTER TABLE t_user ADD COLUMN blacklist_until DATETIME DEFAULT NULL COMMENT '解封时间';
ALTER TABLE t_user ADD COLUMN blacklist_count INT DEFAULT 0 COMMENT '历史拉黑次数';
```

### 2.2 投诉表 t_complaint

```sql
CREATE TABLE t_complaint (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '投诉ID',
    target_user_id  BIGINT NOT NULL COMMENT '被投诉用户ID',
    reporter_id     BIGINT NOT NULL COMMENT '投诉人ID',
    reason          VARCHAR(50) NOT NULL COMMENT '投诉原因',
    description     TEXT COMMENT '详细描述',
    evidence        TEXT COMMENT '证据图片URL（逗号分隔）',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RESOLVED/DISMISSED',
    handler_id      BIGINT DEFAULT NULL COMMENT '处理人ID',
    handler_note    VARCHAR(255) DEFAULT NULL COMMENT '处理备注',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    INDEX idx_target (target_user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户投诉表';
```

### 2.3 申诉表 t_appeal

```sql
CREATE TABLE t_appeal (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '申诉ID',
    user_id         BIGINT NOT NULL COMMENT '申诉人ID',
    reason          TEXT NOT NULL COMMENT '申诉理由',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    handler_id      BIGINT DEFAULT NULL COMMENT '处理人ID',
    handler_note    VARCHAR(255) DEFAULT NULL COMMENT '处理备注',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    INDEX idx_user (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户申诉表';
```

---

## 三、自动判定规则

### 3.1 定时任务（每日一次）

**卖家判定：**
- 条件：收到的 1 星评论数 ≥ 5 **且** 1 星占比 > 30%
- 动作：自动拉黑 14 天（`blacklist_status='AUTO'`）

**买家判定：**
- 条件：发出的 1 星评论占比 > 80% **且** 购买订单数 = 0
- 动作：自动拉黑 14 天（`blacklist_status='AUTO'`）

**到期解封：**
- 条件：`blacklist_until IS NOT NULL AND blacklist_until <= NOW()`
- 动作：`blacklist_status=NULL, blacklist_count+=1, blacklist_until=NULL`

### 3.2 累犯翻倍

| 历史拉黑次数 | 封禁时长 |
|-------------|---------|
| 0 次 | 14 天 |
| 1 次 | 28 天 |
| 2 次 | 56 天 |
| 3 次 | 112 天 |
| n 次 | 14 × 2^n 天 |

---

## 四、接口设计

### 4.1 定时任务/管理员接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/admin/blacklist/scan` | 触发一次自动扫描（测试用，定时任务同逻辑） |
| GET | `/admin/blacklist` | 小黑屋用户列表（分页） |
| PUT | `/admin/users/{id}/unblacklist` | 手动解封 |
| PUT | `/admin/users/{id}/blacklist` | 手动拉黑（管理员主动操作） |
| GET | `/admin/complaints` | 投诉列表（分页，按状态筛选） |
| PUT | `/admin/complaints/{id}/handle` | 处理投诉（通过→拉黑被投诉人，驳回→关闭投诉） |
| GET | `/admin/appeals` | 申诉列表（分页，按状态筛选） |
| PUT | `/admin/appeals/{id}/handle` | 处理申诉（通过→解封，驳回→维持） |

### 4.2 用户接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/complaint` | 提交投诉（需登录，同一用户对同一目标限一次） |
| POST | `/appeal` | 提交申诉（需登录，只能有一条待处理的申诉） |
| GET | `/user/blacklist-status` | 查询当前用户小黑屋状态 |

### 4.3 权限限制

小黑屋用户在以下接口被拦截（返回 403 + 解封时间）：

- POST `/product` → 发布商品
- PUT `/product` → 编辑商品
- POST `/comment` → 发表评论
- POST `/order` → 创建订单
- POST `/favorite/{id}` → 收藏

不受影响的操作：登录、浏览、查看订单、提交申诉。

---

## 五、实现层

### 5.1 后端

| 层 | 文件 |
|----|------|
| Entity | `BlacklistInfo.java`（t_user 新增字段的 DTO）、`Complaint.java`、`Appeal.java` |
| Mapper | `ComplaintMapper.java`、`AppealMapper.java` |
| Service | `BlacklistService.java` + impl（定时扫描逻辑） |
| Controller | `ComplaintController.java`、`AppealController.java` |
| Interceptor | 在 `JwtInterceptor` 中增加黑名单状态检查 |

### 5.2 前端

| 页面 | 改动 |
|------|------|
| Admin.vue | 新标签页"小黑屋"（拉黑用户列表 + 手动操作）+ "投诉/申诉"（审核列表） |
| AppHeader.vue | 小黑屋用户顶部红色警告条 + 申诉按钮 |
| ProductDetail.vue | 小黑屋用户隐藏发布/评论/购买，显示限制提示 |
| 投诉入口 | 用户页面/商品详情页增加"投诉该用户"按钮，弹出投诉表单 |

---

## 六、前端组件设计

### 6.1 警告条（AppHeader 内）
- 位置：Header 下方（全宽红色条）
- 显示：`"你的账号已被限制，解封时间：2026-07-23 14:30"` + 申诉按钮
- 申诉点击：弹出申诉弹窗（理由 textarea + 提交按钮）

### 6.2 投诉弹窗
- 触发："投诉该用户"按钮（卖家卡片旁 / 评论区用户名旁）
- 表单：投诉原因下拉（恶意评论 / 虚假描述 / 骚扰行为 / 其他）+ 文字描述 + 截图上传（可选）
- 提交后提示"投诉已提交，管理员将尽快处理"

### 6.3 后台管理
- 小黑屋标签页：el-table 展示拉黑用户（头像、昵称、拉黑原因、拉黑方式、剩余天数、历史次数、手动解封按钮）
- 投诉/申诉标签页：el-tabs 切换投诉列表和申诉列表，每条有处理/驳回按钮

---

## 七、注意事项

- 定时任务需在主类加 `@EnableScheduling`，`application.yml` 可配置 cron 表达式
- 阈值（1星≥5条、占比>30%等）建议配置在 `application.yml` 中，便于调整
- 自动扫描需避免对已在黑名单中的用户重复拉黑
- 投诉去重：同一用户对同一目标只能有一条 PENDING 投诉
- 申诉去重：同一用户只能有一条 PENDING 申诉
- 前端路由守卫不拦截黑名单用户（黑名单不是完全封禁）
- 投诉入口：卖家卡片区 + 每条评论的用户名旁
