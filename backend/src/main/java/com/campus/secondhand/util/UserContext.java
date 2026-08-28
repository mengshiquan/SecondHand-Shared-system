package com.campus.secondhand.util;

import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.common.ResultCode;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 当前登录用户上下文工具
 */
public class UserContext {

    private UserContext() {
    }

    public static Long getUserId() {
        HttpServletRequest request = getRequest();
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return (Long) userId;
    }

    public static String getRole() {
        HttpServletRequest request = getRequest();
        Object role = request.getAttribute("role");
        return role != null ? role.toString() : "USER";
    }

    public static boolean isAdmin() {
        String role = getRole();
        return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
    }

    public static boolean isSuperAdmin() {
        return "SUPER_ADMIN".equals(getRole());
    }

    private static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return attributes.getRequest();
    }
}
