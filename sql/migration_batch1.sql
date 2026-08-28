-- 第一批功能增强迁移脚本
USE secondhand_db;

-- 收货地址表
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

-- 购物车表
CREATE TABLE IF NOT EXISTS t_cart (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '购物车项ID',
    user_id     BIGINT   NOT NULL COMMENT '用户ID',
    product_id  BIGINT   NOT NULL COMMENT '商品ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_user_product (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- 用户表：校园认证字段
ALTER TABLE t_user ADD COLUMN student_id    VARCHAR(20)  DEFAULT NULL COMMENT '学号';
ALTER TABLE t_user ADD COLUMN school_name   VARCHAR(100) DEFAULT NULL COMMENT '学校名称';
ALTER TABLE t_user ADD COLUMN verify_status VARCHAR(20)  DEFAULT NULL COMMENT '认证状态 PENDING/APPROVED/REJECTED';
-- 存量用户默认已认证
UPDATE t_user SET verify_status = 'APPROVED' WHERE verify_status IS NULL;
-- admin 账号升级为超级管理员
UPDATE t_user SET role = 'SUPER_ADMIN' WHERE username = 'admin';

-- 订单表：地址 + 退款 + 预留支付字段
ALTER TABLE t_order ADD COLUMN address_id    BIGINT       DEFAULT NULL COMMENT '收货地址ID';
ALTER TABLE t_order ADD COLUMN refund_status VARCHAR(20)  DEFAULT NULL COMMENT '退款状态';
ALTER TABLE t_order ADD COLUMN refund_reason VARCHAR(255) DEFAULT NULL COMMENT '退款原因';
ALTER TABLE t_order ADD COLUMN refund_time   DATETIME     DEFAULT NULL COMMENT '退款申请时间';
ALTER TABLE t_order ADD COLUMN payment_time  DATETIME     DEFAULT NULL COMMENT '付款时间（预留模拟支付）';
