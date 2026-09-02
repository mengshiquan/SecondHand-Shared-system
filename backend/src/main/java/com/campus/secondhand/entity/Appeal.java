package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户申诉实体
 */
@Data
@TableName("t_appeal")
public class Appeal {
    /** 申诉ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申诉人用户ID */
    private Long userId;

    /** 申诉理由 */
    private String reason;

    /** 处理状态：PENDING-待处理 APPROVED-申诉成功 REJECTED-申诉失败 */
    private String status;

    /** 处理管理员ID */
    private Long handlerId;

    /** 管理员处理备注 */
    private String handlerNote;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记：0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
