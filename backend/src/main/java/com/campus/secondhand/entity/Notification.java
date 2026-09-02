package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户通知实体
 */
@Data
@TableName("t_notification")
public class Notification {
    /** 通知ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收用户ID */
    private Long userId;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 通知类型：ORDER-订单 VERIFY-认证 CONTACT-咨询 COMPLAINT_RESOLVED-投诉处理 APPEAL_APPROVED-申诉成功 UNBLACKLIST-解除拉黑 */
    private String type;

    /** 阅读状态：0-未读 1-已读 */
    private Integer isRead;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除标记：0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
