package com.campus.secondhand.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 验证码 key（对应 Redis 中的 captcha:{key}） */
    @NotBlank(message = "请刷新验证码")
    private String captchaKey;

    /** 用户输入的验证码 */
    @NotBlank(message = "请输入验证码")
    private String captchaCode;
}
