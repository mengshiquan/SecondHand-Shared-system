package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.entity.Notification;

public interface NotificationService {

    /** 发送通知 */
    void send(Long userId, String title, String content, String type);

    /** 用户通知列表 */
    IPage<Notification> listByUser(Long userId, Integer pageNum, Integer pageSize);

    /** 未读数 */
    long unreadCount(Long userId);

    /** 标为已读 */
    void markRead(Long id, Long userId);

    /** 全部已读 */
    void markAllRead(Long userId);

    /** 删除通知 */
    void delete(Long id, Long userId);

    /** 清空已读 */
    void clearRead(Long userId);
}
