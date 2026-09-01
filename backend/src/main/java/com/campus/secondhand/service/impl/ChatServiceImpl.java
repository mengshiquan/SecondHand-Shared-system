package com.campus.secondhand.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.config.ChatWebSocketHandler;
import com.campus.secondhand.dto.ChatSendDTO;
import com.campus.secondhand.entity.ChatMessage;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.mapper.ChatMessageMapper;
import com.campus.secondhand.mapper.ProductMapper;
import com.campus.secondhand.service.ChatService;
import com.campus.secondhand.service.UserService;
import com.campus.secondhand.util.UserContext;
import com.campus.secondhand.vo.ChatConversationVO;
import com.campus.secondhand.vo.ChatMessageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 聊天服务实现：负责消息持久化、会话聚合、已读计数及在线推送。
 * 依赖 ChatWebSocketHandler 向接收方实时推送新消息、编辑、删除、清空事件。
 */
@Service
public class ChatServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatService {

    @Autowired
    private UserService userService;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    /**
     * 聚合当前用户的会话列表。
     * 按最后消息倒序，利用 LinkedHashMap 保持首次出现顺序（msgs 已倒序）。
     *
     * @return 会话视图列表
     */
    @Override
    public List<ChatConversationVO> conversations() {
        Long uid = UserContext.getUserId();
        List<ChatMessage> msgs = list(new LambdaQueryWrapper<ChatMessage>()
                .and(w -> w.eq(ChatMessage::getSenderId, uid).or().eq(ChatMessage::getReceiverId, uid))
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT 500"));
        // 按对方用户分组，msgs 已倒序，首条即最后消息
        Map<Long, ChatConversationVO> map = new LinkedHashMap<>();
        for (ChatMessage m : msgs) {
            Long peer = m.getSenderId().equals(uid) ? m.getReceiverId() : m.getSenderId();
            ChatConversationVO vo = map.computeIfAbsent(peer, k -> {
                ChatConversationVO c = new ChatConversationVO();
                c.setPeerId(k);
                c.setUnread(0);
                return c;
            });
            if (vo.getLastContent() == null) {
                vo.setLastContent(m.getContent());
                vo.setLastTime(m.getCreateTime());
            }
            if (m.getReceiverId().equals(uid) && m.getIsRead() == 0) {
                vo.setUnread(vo.getUnread() + 1);
            }
        }
        if (!map.isEmpty()) {
            Map<Long, User> users = userService.listByIds(map.keySet()).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
            map.values().forEach(c -> {
                User u = users.get(c.getPeerId());
                if (u != null) {
                    c.setPeerName(u.getNickname());
                    c.setPeerAvatar(u.getAvatar());
                }
            });
        }
        return new ArrayList<>(map.values());
    }

    /** 单页消息数 */
    private static final int PAGE_SIZE = 100;

    /**
     * 查询与指定用户的消息记录（游标分页）。
     * 倒序取出一页后翻转为升序展示，并自动将对方消息标记为已读。
     *
     * @param peerId   对方用户 ID
     * @param beforeId 上一页最后消息 ID，为空则取最新一页
     * @return 按时间升序排列的消息视图列表
     */
    @Override
    public List<ChatMessageVO> messages(Long peerId, Long beforeId) {
        Long uid = UserContext.getUserId();
        List<ChatMessage> msgs = list(new LambdaQueryWrapper<ChatMessage>()
                .and(w -> w.eq(ChatMessage::getSenderId, uid).eq(ChatMessage::getReceiverId, peerId))
                .or(w -> w.eq(ChatMessage::getSenderId, peerId).eq(ChatMessage::getReceiverId, uid))
                .lt(beforeId != null, ChatMessage::getId, beforeId)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT " + PAGE_SIZE));
        // 倒序取出后翻转为升序展示
        Collections.reverse(msgs);
        markRead(peerId);
        Map<Long, String> titles = productTitles(msgs);
        return msgs.stream().map(m -> toVO(m, uid, titles)).collect(Collectors.toList());
    }

    /**
     * 发送聊天消息，保存入库并实时推送给接收方。
     *
     * @param dto 包含接收方、关联商品及消息内容
     * @return 发送方视角的消息视图
     */
    @Override
    public ChatMessageVO send(ChatSendDTO dto) {
        Long uid = UserContext.getUserId();
        if (dto.getReceiverId().equals(uid)) {
            throw new BusinessException("不能给自己发送消息");
        }
        ChatMessage m = new ChatMessage();
        m.setSenderId(uid);
        m.setReceiverId(dto.getReceiverId());
        m.setProductId(dto.getProductId());
        m.setContent(dto.getContent().trim());
        m.setIsRead(0);
        save(m);

        // 推送给接收方（mine 以接收方视角计算）
        Map<Long, String> titles = m.getProductId() != null
                ? productTitles(List.of(m)) : Map.of();
        ChatMessageVO receiverView = toVO(m, dto.getReceiverId(), titles);
        chatWebSocketHandler.pushToUser(dto.getReceiverId(),
                JSONUtil.toJsonStr(Map.of("type", "chat", "message", receiverView)));
        return toVO(m, uid, titles);
    }

    /**
     * 将 peerId 发送给当前用户的全部未读消息标记为已读。
     *
     * @param peerId 对方用户 ID
     */
    @Override
    public void markRead(Long peerId) {
        Long uid = UserContext.getUserId();
        update(new LambdaUpdateWrapper<ChatMessage>()
                .eq(ChatMessage::getSenderId, peerId)
                .eq(ChatMessage::getReceiverId, uid)
                .eq(ChatMessage::getIsRead, 0)
                .set(ChatMessage::getIsRead, 1));
    }

    /**
     * 统计当前用户所有未读消息总数。
     *
     * @return 未读消息数
     */
    @Override
    public long unreadTotal() {
        return count(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getReceiverId, UserContext.getUserId())
                .eq(ChatMessage::getIsRead, 0));
    }

    /**
     * 编辑当前用户自己发送的消息，保存后推送编辑事件给对方。
     *
     * @param id      消息 ID
     * @param content 新内容
     * @return 更新后的消息视图
     */
    @Override
    public ChatMessageVO updateMessage(Long id, String content) {
        Long uid = UserContext.getUserId();
        ChatMessage m = getById(id);
        if (m == null) throw new BusinessException("消息不存在或已删除");
        if (!m.getSenderId().equals(uid)) throw new BusinessException("只能编辑自己发送的消息");
        m.setContent(content.trim());
        updateById(m);
        Map<Long, String> titles = productTitles(List.of(m));
        chatWebSocketHandler.pushToUser(m.getReceiverId(),
                JSONUtil.toJsonStr(Map.of("type", "chat_edit", "message", toVO(m, m.getReceiverId(), titles))));
        return toVO(m, uid, titles);
    }

    /**
     * 删除当前用户自己发送的消息，并推送删除事件给对方。
     *
     * @param id 消息 ID
     */
    @Override
    public void deleteMessage(Long id) {
        Long uid = UserContext.getUserId();
        ChatMessage m = getById(id);
        if (m == null) return;
        if (!m.getSenderId().equals(uid)) throw new BusinessException("只能删除自己发送的消息");
        removeById(id);
        chatWebSocketHandler.pushToUser(m.getReceiverId(),
                JSONUtil.toJsonStr(Map.of("type", "chat_delete", "id", id)));
    }

    /**
     * 删除当前用户与指定用户的全部聊天记录，并通知对方会话已清空。
     *
     * @param peerId 对方用户 ID
     */
    @Override
    public void deleteConversation(Long peerId) {
        Long uid = UserContext.getUserId();
        remove(new LambdaQueryWrapper<ChatMessage>()
                .and(w -> w.eq(ChatMessage::getSenderId, uid).eq(ChatMessage::getReceiverId, peerId))
                .or(w -> w.eq(ChatMessage::getSenderId, peerId).eq(ChatMessage::getReceiverId, uid)));
        chatWebSocketHandler.pushToUser(peerId,
                JSONUtil.toJsonStr(Map.of("type", "chat_clear", "peerId", uid)));
    }

    /**
     * 批量查询消息关联的商品标题，用于组装消息视图。
     *
     * @param msgs 消息列表
     * @return 商品 ID 到标题的映射
     */
    private Map<Long, String> productTitles(List<ChatMessage> msgs) {
        Set<Long> pids = msgs.stream().map(ChatMessage::getProductId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (pids.isEmpty()) return Map.of();
        return productMapper.selectBatchIds(pids).stream()
                .collect(Collectors.toMap(Product::getId, Product::getTitle));
    }

    /**
     * 将 ChatMessage 转换为 ChatMessageVO，并根据 viewerId 判定消息是否为自己发送。
     *
     * @param m         消息实体
     * @param viewerId  当前查看者用户 ID
     * @param titles    商品标题缓存
     * @return 消息视图
     */
    private ChatMessageVO toVO(ChatMessage m, Long viewerId, Map<Long, String> titles) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(m.getId());
        vo.setSenderId(m.getSenderId());
        vo.setReceiverId(m.getReceiverId());
        vo.setProductId(m.getProductId());
        vo.setProductTitle(m.getProductId() != null ? titles.get(m.getProductId()) : null);
        vo.setContent(m.getContent());
        vo.setCreateTime(m.getCreateTime());
        vo.setMine(m.getSenderId().equals(viewerId));
        return vo;
    }
}
