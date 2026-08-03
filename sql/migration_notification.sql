-- 用户通知系统
USE secondhand_db;

CREATE TABLE IF NOT EXISTS t_notification (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '通知ID',
    user_id     BIGINT NOT NULL COMMENT '接收用户ID',
    title       VARCHAR(100) NOT NULL COMMENT '通知标题',
    content     VARCHAR(500) NOT NULL COMMENT '通知内容',
    type        VARCHAR(30) NOT NULL COMMENT '类型',
    is_read     TINYINT NOT NULL DEFAULT 0 COMMENT '0=未读 1=已读',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT NOT NULL DEFAULT 0,
    INDEX idx_user_read (user_id, is_read),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知表';
