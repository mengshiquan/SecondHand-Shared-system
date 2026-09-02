package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String username;

    /** 密码（加密存储） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 角色：USER / ADMIN / SUPER_ADMIN */
    private String role;

    /** 状态：0-禁用 1-正常 2-已注销 */
    private Integer status;

    /** 小黑屋状态：NULL/AUTO/MANUAL */
    private String blacklistStatus;

    /** 拉黑原因 */
    private String blacklistReason;

    /** 解封时间 */
    private LocalDateTime blacklistUntil;

    /** 历史拉黑次数 */
    private Integer blacklistCount;

    /** 学号（12位纯数字） */
    private String studentId;

    /** 学校名称 */
    private String schoolName;

    /** 认证状态：PENDING/APPROVED/REJECTED */
    private String verifyStatus;

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
