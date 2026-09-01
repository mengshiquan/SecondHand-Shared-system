package com.campus.secondhand.config;

import com.campus.secondhand.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * 聊天 WebSocket 配置：/ws/chat?token=JWT 握手鉴权
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 注册 WebSocket 端点 /ws/chat，绑定 JWT 握手拦截器，允许跨域连接。
     *
     * @param registry WebSocket 处理器注册表
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(new JwtHandshakeInterceptor())
                .setAllowedOrigins("*");
    }

    /**
     * 握手阶段校验 JWT，把 userId 放进会话属性
     */
    private class JwtHandshakeInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
            String token = null;
            if (request instanceof ServletServerHttpRequest servletRequest) {
                token = servletRequest.getServletRequest().getParameter("token");
            }
            if (token == null || !jwtUtil.validateToken(token)) {
                return false;
            }
            attributes.put("userId", jwtUtil.getUserId(token));
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
        }
    }
}
