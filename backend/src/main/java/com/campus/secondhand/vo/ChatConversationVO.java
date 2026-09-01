package com.campus.secondhand.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表视图
 */
@Data
public class ChatConversationVO {

    private Long peerId;
    private String peerName;
    private String peerAvatar;
    private String lastContent;
    private LocalDateTime lastTime;
    private Integer unread;
}
