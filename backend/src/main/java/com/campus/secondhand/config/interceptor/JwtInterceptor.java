package com.campus.secondhand.config.interceptor;

import com.campus.secondhand.entity.User;
import com.campus.secondhand.service.BlacklistService;
import com.campus.secondhand.service.UserService;
import com.campus.secondhand.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 认证拦截器
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private BlacklistService blacklistService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** 小黑屋限制的路径前缀 */
    private static final String[] RESTRICTED_PATHS = {
        "/product/",
        "/comment/",
        "/order/",
        "/favorite/"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (!StringUtils.hasText(token) || !jwtUtil.validateToken(token)) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("message", "未登录或令牌已过期");
            result.put("data", null);
            response.getWriter().write(objectMapper.writeValueAsString(result));
            return false;
        }

        Long userId = jwtUtil.getUserId(token);
        String role = jwtUtil.getRole(token);
        request.setAttribute("userId", userId);
        request.setAttribute("role", role);

        // 小黑屋检查：对受限写入接口拦截
        if (blacklistService.isBlacklisted(userId)) {
            String uri = request.getRequestURI();
            String method = request.getMethod();
            boolean restricted = false;
            for (String rp : RESTRICTED_PATHS) {
                if (uri.contains(rp) && !"GET".equals(method)) {
                    restricted = true;
                    break;
                }
            }
            if (restricted) {
                User user = blacklistService.getUserById(userId);
                String until = user != null && user.getBlacklistUntil() != null
                    ? user.getBlacklistUntil().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    : "待定";
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                Map<String, Object> result = new HashMap<>();
                result.put("code", 403);
                result.put("message", "你的账号已被限制使用，解封时间：" + until);
                result.put("data", null);
                response.getWriter().write(objectMapper.writeValueAsString(result));
                return false;
            }
        }

        return true;
    }
}
