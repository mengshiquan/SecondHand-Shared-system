package com.campus.secondhand.controller;

import com.campus.secondhand.common.Result;
import com.campus.secondhand.dto.ChatEditDTO;
import com.campus.secondhand.dto.ChatSendDTO;
import com.campus.secondhand.service.ChatService;
import com.campus.secondhand.vo.ChatConversationVO;
import com.campus.secondhand.vo.ChatMessageVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 聊天 REST 接口：提供会话列表、消息记录、发送、已读、未读数及消息编辑/删除等能力。
 * 消息实时推送由 /ws/chat WebSocket 完成，本控制器主要负责请求落库与状态查询。
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    /**
     * 获取当前用户的会话列表（按最后消息倒序）。
     *
     * @return 每个会话包含对方用户 ID、昵称、头像、最后一条消息内容及未读数
     */
    @GetMapping("/conversations")
    public Result<List<ChatConversationVO>> conversations() {
        return Result.success(chatService.conversations());
    }

    /**
     * 获取与指定用户的消息记录，按游标分页。
     * 调用后自动将该 peerId 发来的未读消息标记为已读。
     *
     * @param peerId  对方用户 ID
     * @param beforeId 上一页最后一条消息 ID（可选，首次传空）
     * @return 按时间升序排列的消息列表
     */
    @GetMapping("/messages/{peerId}")
    public Result<List<ChatMessageVO>> messages(@PathVariable Long peerId,
                                                @RequestParam(required = false) Long beforeId) {
        return Result.success(chatService.messages(peerId, beforeId));
    }

    /**
     * 发送聊天消息。
     *
     * @param dto 接收方、关联商品及消息内容
     * @return 刚发送成功的消息视图（包含 mine=true 标识）
     */
    @PostMapping("/send")
    public Result<ChatMessageVO> send(@Valid @RequestBody ChatSendDTO dto) {
        return Result.success(chatService.send(dto));
    }

    /**
     * 将指定用户发送给当前用户的全部未读消息标记为已读。
     *
     * @param peerId 对方用户 ID
     */
    @PostMapping("/read/{peerId}")
    public Result<Void> markRead(@PathVariable Long peerId) {
        chatService.markRead(peerId);
        return Result.success();
    }

    /**
     * 获取当前用户未读消息总数，用于顶栏角标提醒。
     *
     * @return count: 未读消息总数
     */
    @GetMapping("/unread")
    public Result<Map<String, Long>> unread() {
        return Result.success(Map.of("count", chatService.unreadTotal()));
    }

    /**
     * 编辑当前用户自己发送的某条消息，并推送编辑事件给对方。
     *
     * @param id  消息 ID
     * @param dto 新的消息内容
     * @return 更新后的消息视图
     */
    @PutMapping("/message/{id}")
    public Result<ChatMessageVO> updateMessage(@PathVariable Long id, @Valid @RequestBody ChatEditDTO dto) {
        return Result.success(chatService.updateMessage(id, dto.getContent()));
    }

    /**
     * 删除当前用户自己发送的某条消息，并推送删除事件给对方。
     *
     * @param id 消息 ID
     */
    @DeleteMapping("/message/{id}")
    public Result<Void> deleteMessage(@PathVariable Long id) {
        chatService.deleteMessage(id);
        return Result.success();
    }

    /**
     * 删除当前用户与指定用户的整个会话（清空双方该会话下的全部聊天记录），
     * 并通知对方会话已被清空。
     *
     * @param peerId 对方用户 ID
     */
    @DeleteMapping("/conversation/{peerId}")
    public Result<Void> deleteConversation(@PathVariable Long peerId) {
        chatService.deleteConversation(peerId);
        return Result.success();
    }
}
