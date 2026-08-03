package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_appeal")
public class Appeal {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String reason;
    private String status;
    private Long handlerId;
    private String handlerNote;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
