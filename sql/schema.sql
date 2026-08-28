-- 校园二手物品共享平台数据库脚本
CREATE DATABASE IF NOT EXISTS secondhand_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE secondhand_db;

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(64)  NOT NULL COMMENT '密码(BCrypt)',
    nickname    VARCHAR(50)  NOT NULL COMMENT '昵称',
    avatar      VARCHAR(255) DEFAULT NULL COMMENT '头像',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色 USER/ADMIN',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-正常 2-已注销',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    student_id    VARCHAR(20)  DEFAULT NULL COMMENT '学号',
    school_name   VARCHAR(100) DEFAULT NULL COMMENT '学校名称',
    verify_status VARCHAR(20)  DEFAULT NULL COMMENT '认证状态 PENDING/APPROVED/REJECTED',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 分类表
CREATE TABLE IF NOT EXISTS t_category (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    name        VARCHAR(50)  NOT NULL COMMENT '分类名称',
    parent_id   BIGINT       DEFAULT NULL COMMENT '父级分类ID，NULL为一级分类',
    icon        VARCHAR(100) DEFAULT NULL COMMENT '分类图标',
    sort        INT          DEFAULT 0 COMMENT '排序',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表（支持二级层级）';

-- 商品表
CREATE TABLE IF NOT EXISTS t_product (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '商品ID',
    title           VARCHAR(100)   NOT NULL COMMENT '标题',
    description     TEXT           NOT NULL COMMENT '描述',
    price           DECIMAL(10,2)  NOT NULL COMMENT '价格',
    original_price  DECIMAL(10,2)  DEFAULT NULL COMMENT '原价',
    images          TEXT           DEFAULT NULL COMMENT '图片JSON数组',
    category_id     BIGINT         NOT NULL COMMENT '分类ID',
    user_id         BIGINT         NOT NULL COMMENT '发布者ID',
    status          VARCHAR(20)    NOT NULL DEFAULT 'ON_SALE' COMMENT '状态 ON_SALE/SOLD/OFF_SHELF',
    view_count      INT            DEFAULT 0 COMMENT '浏览量',
    create_time     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_category (category_id),
    INDEX idx_user (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 订单表
CREATE TABLE IF NOT EXISTS t_order (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单ID',
    order_no    VARCHAR(32)    NOT NULL UNIQUE COMMENT '订单编号',
    product_id  BIGINT         NOT NULL COMMENT '商品ID',
    buyer_id    BIGINT         NOT NULL COMMENT '买家ID',
    seller_id   BIGINT         NOT NULL COMMENT '卖家ID',
    price       DECIMAL(10,2)  NOT NULL COMMENT '成交价格',
    status      VARCHAR(20)    NOT NULL DEFAULT 'PENDING' COMMENT '订单状态',
    remark      VARCHAR(255)   DEFAULT NULL COMMENT '备注',
    create_time DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    address_id    BIGINT       DEFAULT NULL COMMENT '收货地址ID',
    refund_status VARCHAR(20)  DEFAULT NULL COMMENT '退款状态',
    refund_reason VARCHAR(255) DEFAULT NULL COMMENT '退款原因',
    refund_time   DATETIME     DEFAULT NULL COMMENT '退款申请时间',
    payment_time  DATETIME     DEFAULT NULL COMMENT '付款时间（预留模拟支付）',
    deleted     TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_buyer (buyer_id),
    INDEX idx_seller (seller_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 收藏表
CREATE TABLE IF NOT EXISTS t_favorite (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '收藏ID',
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    product_id  BIGINT NOT NULL COMMENT '商品ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_user_product (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- 评论表
CREATE TABLE IF NOT EXISTS t_comment (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
    product_id  BIGINT       NOT NULL COMMENT '商品ID',
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    content     VARCHAR(500) NOT NULL COMMENT '评论内容',
    rating      TINYINT      NOT NULL DEFAULT 5 COMMENT '评分1-5',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 初始化数据
-- 密码使用 MD5 初始化，应用启动时 PasswordMigration 会自动迁移为 BCrypt
-- admin/admin, student/123456
INSERT INTO t_user (username, password, nickname, role, status) VALUES
('admin', '21232f297a57a5a743894a0e4a801fc3', '管理员', 'SUPER_ADMIN', 1),
('student', 'e10adc3949ba59abbe56e057f20f883e', '校园同学', 'USER', 1);

-- 存量用户默认已认证
UPDATE t_user SET verify_status = 'APPROVED' WHERE verify_status IS NULL;

-- 一级分类
INSERT INTO t_category (id, name, icon, sort) VALUES
(1, '数码电子', 'Monitor', 1),
(2, '图书教材', 'Reading', 2),
(3, '生活用品', 'House', 3),
(4, '服饰鞋包', 'ShoppingBag', 4),
(5, '运动户外', 'Football', 5),
(6, '音乐器材', 'Headset', 6),
(7, '其他闲置', 'More', 7);

-- 二级分类：数码电子 (parent_id=1)
INSERT INTO t_category (name, parent_id, icon, sort) VALUES
('手机', 1, 'Iphone', 1),
('平板电脑', 1, 'Ipad', 2),
('笔记本电脑', 1, 'Notebook', 3),
('智能手表/手环', 1, 'Watch', 4),
('移动电源/充电器', 1, 'Charger', 5),
('蓝牙耳机/音箱', 1, 'Headset', 6),
('相机/摄像机', 1, 'Camera', 7);

-- 二级分类：图书教材 (parent_id=2)
INSERT INTO t_category (name, parent_id, icon, sort) VALUES
('教材/教辅', 2, 'Document', 1),
('文学/小说', 2, 'Reading', 2),
('考试用书', 2, 'EditPen', 3),
('专业书籍', 2, 'Collection', 4),
('杂志/期刊', 2, 'Postcard', 5);

-- 二级分类：生活用品 (parent_id=3)
INSERT INTO t_category (name, parent_id, icon, sort) VALUES
('家居装饰', 3, 'Present', 1),
('厨房用品', 3, 'KnifeFork', 2),
('洗护/个护', 3, 'Soap', 3),
('收纳/整理', 3, 'Box', 4),
('日用杂货', 3, 'Goods', 5);

-- 二级分类：服饰鞋包 (parent_id=4)
INSERT INTO t_category (name, parent_id, icon, sort) VALUES
('男装', 4, 'Male', 1),
('女装', 4, 'Female', 2),
('鞋子', 4, 'Shoe', 3),
('箱包', 4, 'Briefcase', 4),
('配饰/首饰', 4, 'MagicStick', 5);

-- 二级分类：运动户外 (parent_id=5)
INSERT INTO t_category (name, parent_id, icon, sort) VALUES
('健身器材', 5, 'Bell', 1),
('球类', 5, 'Basketball', 2),
('户外装备', 5, 'Sunny', 3),
('骑行', 5, 'Bicycle', 4),
('游泳', 5, 'Ship', 5);

-- 二级分类：音乐器材 (parent_id=6)
INSERT INTO t_category (name, parent_id, icon, sort) VALUES
('吉他', 6, 'Headset', 1),
('钢琴/电子琴', 6, 'Headset', 2),
('打击乐器', 6, 'Headset', 3),
('管乐器', 6, 'Headset', 4),
('其他乐器', 6, 'Headset', 5);

-- 二级分类：其他闲置 (parent_id=7)
INSERT INTO t_category (name, parent_id, icon, sort) VALUES
('玩具/玩偶', 7, 'Present', 1),
('礼品/工艺品', 7, 'Collection', 2),
('票券/卡券', 7, 'Ticket', 3),
('其他', 7, 'More', 4);

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
