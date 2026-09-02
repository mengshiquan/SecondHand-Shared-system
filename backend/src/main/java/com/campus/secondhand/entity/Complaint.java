package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户投诉实体
 */
@Data
@TableName("t_complaint")
public class Complaint {
    /** 投诉ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 被投诉用户ID */
    private Long targetUserId;

    /** 投诉人用户ID */
    private Long reporterId;

    /** 投诉原因 */
    private String reason;

    /** 详细描述 */
    private String description;

    /** 证据图片URL，多个值用英文逗号分隔 */
    private String evidence;

    /** 处理状态：PENDING-待处理 RESOLVED-投诉成立 DISMISSED-投诉驳回 */
    private String status;

    /** 处理管理员ID */
    private Long handlerId;

    /** 管理员处理备注 */
    private String handlerNote;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除标记：0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
