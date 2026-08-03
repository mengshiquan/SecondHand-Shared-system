package com.campus.secondhand.vo;

import lombok.Data;

/**
 * 登录返回 VO
 */
@Data
public class LoginVO {

    private String token;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
}
