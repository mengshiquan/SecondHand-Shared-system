package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_complaint")
public class Complaint {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long targetUserId;
    private Long reporterId;
    private String reason;
    private String description;
    private String evidence;
    private String status;
    private Long handlerId;
    private String handlerNote;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}
