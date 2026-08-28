package com.campus.secondhand.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度为3-20个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度为6-20个字符")
    private String password;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    private String phone;

    private String email;

    @NotBlank(message = "学号不能为空")
    @Pattern(regexp = "^\\d{12}$", message = "学号格式不正确")
    private String studentId;

    @NotBlank(message = "学校名称不能为空")
    @Size(max = 100, message = "学校名称过长")
    private String schoolName;
}
