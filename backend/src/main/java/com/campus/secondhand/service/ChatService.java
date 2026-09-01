package com.campus.secondhand.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.secondhand.dto.ChatSendDTO;
import com.campus.secondhand.entity.ChatMessage;
import com.campus.secondhand.vo.ChatConversationVO;
import com.campus.secondhand.vo.ChatMessageVO;

import java.util.List;

public interface ChatService extends IService<ChatMessage> {

    /** 我的会话列表（按最后消息倒序） */
    List<ChatConversationVO> conversations();

    /** 与某人的消息记录（游标分页：beforeId 为空取最近一页，否则取其之前的一页），同时把对方发来的标记已读 */
    List<ChatMessageVO> messages(Long peerId, Long beforeId);

    /** 发送消息并实时推送给对方 */
    ChatMessageVO send(ChatSendDTO dto);

    /** 把某人发来的消息标记已读 */
    void markRead(Long peerId);

    /** 未读消息总数（顶栏角标） */
    long unreadTotal();

    /** 编辑自己发送的消息，实时同步给对方 */
    ChatMessageVO updateMessage(Long id, String content);

    /** 删除自己发送的消息，实时同步给对方 */
    void deleteMessage(Long id);

    /** 删除会话（清空双方聊天记录），实时通知对方 */
    void deleteConversation(Long peerId);
}
