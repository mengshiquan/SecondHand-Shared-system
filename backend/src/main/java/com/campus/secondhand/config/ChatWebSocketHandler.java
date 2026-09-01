package com.campus.secondhand.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 聊天 WebSocket 处理器：仅负责服务端推送，消息落库走 REST 接口
 */
@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    /** userId -> 其全部在线会话（同一用户可能多标签页） */
    private static final Map<Long, Set<WebSocketSession>> SESSIONS = new ConcurrentHashMap<>();

    /**
     * 连接建立后：从会话属性取出 userId，加入该用户的在线会话集合。
     * 若未携带 userId（握手失败）则直接关闭连接。
     *
     * @param session WebSocket 会话
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            try { session.close(CloseStatus.POLICY_VIOLATION); } catch (IOException ignored) {}
            return;
        }
        SESSIONS.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
    }

    /**
     * 连接关闭后：使用 computeIfPresent 原子清理当前会话，
     * 避免"旧会话关闭"与"新会话建连"并发时误删包含新会话的整条条目。
     *
     * @param session WebSocket 会话
     * @param status  关闭状态
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            // 原子清理：避免“旧会话关闭”与“新会话建连”并发时误删含新会话的整个条目
            SESSIONS.computeIfPresent(userId, (k, s) -> {
                s.remove(session);
                return s.isEmpty() ? null : s;
            });
        }
    }

    /**
     * 文本消息处理：本系统聊天消息走 REST 接口落库，
     * WebSocket 仅用于服务端推送，因此忽略客户端上行文本。
     *
     * @param session WebSocket 会话
     * @param message 收到的文本消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 客户端无需上行文本消息，忽略心跳外的任何内容
    }

    /**
     * 向指定用户的全部在线会话推送 JSON
     */
    public void pushToUser(Long userId, String json) {
        Set<WebSocketSession> set = SESSIONS.get(userId);
        if (set == null || set.isEmpty()) return;
        TextMessage msg = new TextMessage(json);
        for (WebSocketSession session : set) {
            try {
                if (session.isOpen()) session.sendMessage(msg);
            } catch (IOException e) {
                log.warn("WebSocket 推送失败 userId={}", userId, e);
            }
        }
    }
}
