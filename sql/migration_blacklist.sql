-- 小黑屋功能：用户限制 + 投诉 + 申诉
-- 执行前确保已 USE secondhand_db;

USE secondhand_db;

-- t_user 新增小黑屋字段
ALTER TABLE t_user ADD COLUMN blacklist_status VARCHAR(20) DEFAULT NULL COMMENT 'NULL=正常, AUTO=系统拉黑, MANUAL=管理员拉黑';
ALTER TABLE t_user ADD COLUMN blacklist_reason VARCHAR(255) DEFAULT NULL COMMENT '拉黑原因';
ALTER TABLE t_user ADD COLUMN blacklist_until DATETIME DEFAULT NULL COMMENT '解封时间';
ALTER TABLE t_user ADD COLUMN blacklist_count INT DEFAULT 0 COMMENT '历史拉黑次数';

-- 投诉表
CREATE TABLE IF NOT EXISTS t_complaint (
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

-- 申诉表
CREATE TABLE IF NOT EXISTS t_appeal (
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
