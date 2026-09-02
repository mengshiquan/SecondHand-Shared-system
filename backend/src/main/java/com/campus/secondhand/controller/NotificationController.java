package com.campus.secondhand.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.common.Result;
import com.campus.secondhand.entity.Notification;
import com.campus.secondhand.service.NotificationService;
import com.campus.secondhand.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户通知中心接口
 */
@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /** 分页查询当前用户通知。 */
    @GetMapping("/list")
    public Result<IPage<Notification>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(notificationService.listByUser(UserContext.getUserId(), pageNum, pageSize));
    }

    /** 查询当前用户未读通知数量。 */
    @GetMapping("/unread-count")
    public Result<Map<String, Object>> unreadCount() {
        return Result.success(Map.of("count", notificationService.unreadCount(UserContext.getUserId())));
    }

    /** 将指定本人通知标记为已读。 */
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id, UserContext.getUserId());
        return Result.success();
    }

    /** 将当前用户全部通知标记为已读。 */
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead(UserContext.getUserId());
        return Result.success();
    }

    /** 删除指定本人通知。 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        notificationService.delete(id, UserContext.getUserId());
        return Result.success();
    }

    /** 清空当前用户所有已读通知。 */
    @DeleteMapping("/clear")
    public Result<Void> clearRead() {
        notificationService.clearRead(UserContext.getUserId());
        return Result.success();
    }
}
