-- 买卖家实时聊天消息表
CREATE TABLE IF NOT EXISTS t_chat_message (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id   BIGINT       NOT NULL COMMENT '发送者用户ID',
    receiver_id BIGINT       NOT NULL COMMENT '接收者用户ID',
    product_id  BIGINT       DEFAULT NULL COMMENT '关联商品ID',
    content     VARCHAR(500) NOT NULL COMMENT '消息内容',
    is_read     TINYINT      NOT NULL DEFAULT 0 COMMENT '0-未读 1-已读',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT      DEFAULT 0,
    KEY idx_sender (sender_id),
    KEY idx_receiver (receiver_id)
) COMMENT = '买卖家聊天消息';
