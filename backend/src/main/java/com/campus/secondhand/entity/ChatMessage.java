package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 买卖家聊天消息
 */
@Data
@TableName("t_chat_message")
public class ChatMessage {

    private Long id;

    /** 发送者用户ID */
    private Long senderId;

    /** 接收者用户ID */
    private Long receiverId;

    /** 关联商品ID */
    private Long productId;

    /** 消息内容 */
    private String content;

    /** 0-未读 1-已读 */
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
