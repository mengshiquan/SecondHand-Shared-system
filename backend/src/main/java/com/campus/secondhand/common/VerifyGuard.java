package com.campus.secondhand.common;

import com.campus.secondhand.entity.User;
import com.campus.secondhand.service.UserService;
import com.campus.secondhand.util.UserContext;

/**
 * 校园身份认证守卫：未通过认证的用户禁止执行写操作
 */
public final class VerifyGuard {

    private VerifyGuard() {}

    public static void requireVerified(UserService userService) {
        Long userId = UserContext.getUserId();
        User user = userService.getById(userId);
        if (user == null || !"APPROVED".equals(user.getVerifyStatus())) {
            throw new BusinessException("请先完成校园身份认证");
        }
    }
}
