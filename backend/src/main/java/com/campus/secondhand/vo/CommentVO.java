package com.campus.secondhand.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论展示 VO
 */
@Data
public class CommentVO {

    private Long id;
    private Long productId;
    private Long userId;
    private String nickname;
    private String avatar;
    private String content;
    private Integer rating;
    private LocalDateTime createTime;
}
