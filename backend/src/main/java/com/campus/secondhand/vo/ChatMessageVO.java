package com.campus.secondhand.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息视图
 */
@Data
public class ChatMessageVO {

    private Long id;
    private Long senderId;
    private Long receiverId;
    private Long productId;
    /** 关联商品标题（首条消息上下文展示） */
    private String productTitle;
    private String content;
    private LocalDateTime createTime;
    /** 是否为我发送 */
    private Boolean mine;
}
