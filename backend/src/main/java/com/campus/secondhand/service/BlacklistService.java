package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.entity.User;

public interface BlacklistService {

    /** 自动扫描并拉黑违规用户（定时任务） */
    void autoScan();

    /** 小黑屋用户列表 */
    IPage<User> blacklistPage(Integer pageNum, Integer pageSize);

    /** 手动拉黑用户 */
    void manualBlacklist(Long userId, String reason, Integer days);

    /** 手动解封用户 */
    void unblacklist(Long userId);

    /** 检查用户是否在小黑屋 */
    boolean isBlacklisted(Long userId);

    /** 获取用户信息 */
    com.campus.secondhand.entity.User getUserById(Long userId);
}
