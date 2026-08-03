---
name: db-migration
description: Database schema changes for SecondHand project. Triggers when adding tables, columns, indexes, or seed data to secondhand_db.
---

# Database Migration — 校园二手平台

## Database Info
- Name: `secondhand_db`
- Engine: InnoDB, charset: `utf8mb4`, collation: `utf8mb4_general_ci`
- Migration files: `sql/` directory at project root

## Table Naming Conventions
- All tables: `t_` prefix (e.g., `t_user`, `t_product`, `t_order`)
- Primary key: `id BIGINT AUTO_INCREMENT PRIMARY KEY`
- Timestamps: `create_time DATETIME DEFAULT CURRENT_TIMESTAMP`
- Update tracking: `update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`
- Soft delete: `deleted TINYINT NOT NULL DEFAULT 0`
- Comments: `COMMENT 'xxx'` on every column

## Current Schema (5 tables + subcategory data)

### t_user — 用户表
```sql
id, username(VARCHAR 50 UNIQUE), password(VARCHAR 64 MD5), nickname, avatar, phone, email,
role(VARCHAR 20 DEFAULT 'USER'), status(TINYINT DEFAULT 1), create_time, update_time, deleted
```

### t_category — 商品分类表（二级层级）
```sql
id, name(VARCHAR 50), parent_id(BIGINT NULL→一级), icon, sort(INT), create_time, deleted
```
7 first-level categories: 数码电子/图书教材/生活用品/服饰鞋包/运动户外/音乐器材/其他闲置

### t_product — 商品表
```sql
id, title, description(TEXT), price(DECIMAL 10,2), original_price, images(TEXT JSON array),
category_id, user_id, status(VARCHAR 20 DEFAULT 'ON_SALE'), view_count, create_time, update_time, deleted
```
Status values: `ON_SALE` / `SOLD` / `OFF_SHELF`

### t_order — 订单表
```sql
id, order_no(VARCHAR 32 UNIQUE), product_id, buyer_id, seller_id, price, status(VARCHAR 20 DEFAULT 'PENDING'),
remark, create_time, update_time, deleted
```
Status values: `PENDING` / `PAID` / `SHIPPED` / `COMPLETED` / `CANCELLED`

### t_favorite — 收藏表
```sql
id, user_id, product_id, create_time, deleted
UNIQUE KEY uk_user_product (user_id, product_id)
```

### t_comment — 评论表
```sql
id, product_id, user_id, content(VARCHAR 500), rating(TINYINT 1-5 DEFAULT 5), create_time, deleted
```

## Adding a New Migration

### 1. Create SQL file: `sql/migration_<desc>.sql`
```sql
-- 描述变更内容和原因
USE secondhand_db;

ALTER TABLE t_xxx ADD COLUMN new_column VARCHAR(50) DEFAULT NULL COMMENT '说明';
-- or
CREATE TABLE IF NOT EXISTS t_xxx (...);
```

### 2. Create Entity class
- Package: `com.campus.secondhand.entity`
- `@TableName("t_xxx")`, `@Data`, `@TableId(type = IdType.AUTO)`, `@TableLogic`

### 3. Create Mapper interface
- Package: `com.campus.secondhand.mapper`
- `extends BaseMapper<Xxx>`

### 4. Update MyBatis-Plus config if needed
- `config/MybatisPlusConfig.java` for pagination plugin, logic delete config

## Entity ↔ Table Mapping Rules
| SQL Column | Java Field | Annotation |
|-----------|-----------|------------|
| `BIGINT AUTO_INCREMENT` | `Long id` | `@TableId(type = IdType.AUTO)` |
| `TINYINT DEFAULT 0` | `Integer deleted` | `@TableLogic` |
| `create_time` | `LocalDateTime createTime` | `@TableField(fill = FieldFill.INSERT)` |
| `update_time` | `LocalDateTime updateTime` | `@TableField(fill = FieldFill.INSERT_UPDATE)` |
| `DECIMAL(10,2)` | `BigDecimal` | (no annotation needed) |
| `VARCHAR(N)` | `String` | (no annotation needed) |

## Seed Data Pattern
```sql
INSERT INTO t_xxx (col1, col2) VALUES
('val1', 'val2'),
('val3', 'val4');
```
- Always use explicit column lists (never `INSERT INTO t_xxx VALUES(...)`)
- Admin user: username=`admin`, role=`ADMIN`
- Category icons use Element Plus icon names (e.g., `Monitor`, `Reading`)

## Status Color Mapping (shared across frontend)
Use for status tags in both Admin.vue and MyOrders.vue:
| Status | Tag Type | Color | CSS Class |
|--------|----------|-------|-----------|
| ON_SALE / PENDING | `warning` | #F59E0B | `order-PENDING` |
| PAID | `primary` | #3B82F6 | `order-PAID` |
| SHIPPED | `success` | #10B981 | `order-SHIPPED` |
| COMPLETED | `success` | #10B981 | `order-COMPLETED` |
| CANCELLED / OFF_SHELF | `info` | #9CA3AF | `order-CANCELLED` |
