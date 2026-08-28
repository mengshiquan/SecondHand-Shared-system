package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.common.RedisKeyConstants;
import com.campus.secondhand.entity.*;
import com.campus.secondhand.mapper.CommentMapper;
import com.campus.secondhand.mapper.OrderMapper;
import com.campus.secondhand.mapper.ProductMapper;
import com.campus.secondhand.service.BlacklistService;
import com.campus.secondhand.service.NotificationService;
import com.campus.secondhand.service.UserService;
import com.campus.secondhand.util.RedisLockUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BlacklistServiceImpl implements BlacklistService {

    private static final Logger log = LoggerFactory.getLogger(BlacklistServiceImpl.class);

    @Autowired
    private UserService userService;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private RedisLockUtil redisLockUtil;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private static final int BASE_DAYS = 14;

    @Override
    @Scheduled(cron = "0 0 3 * * ?")
    public void autoScan() {
        // 非阻塞抢锁：定时任务与手动触发不会重叠执行
        String token = redisLockUtil.tryLock(RedisKeyConstants.LOCK_BLACKLIST_SCAN,
                java.time.Duration.ofMinutes(10));
        if (token == null) {
            log.info("黑名单扫描任务已在执行中，本轮跳过");
            return;
        }
        try {
            transactionTemplate.executeWithoutResult(status -> doAutoScan());
        } finally {
            redisLockUtil.unlock(RedisKeyConstants.LOCK_BLACKLIST_SCAN, token);
        }
    }

    /** 原 autoScan 逻辑体（在事务内执行） */
    private void doAutoScan() {
        LocalDateTime now = LocalDateTime.now();
        // 记录本轮解封的用户ID，避免立即重新拉黑
        Set<Long> justUnblacklisted = new HashSet<>();

        // 解封到期用户
        List<User> expired = userService.list(new LambdaQueryWrapper<User>()
                .isNotNull(User::getBlacklistUntil)
                .le(User::getBlacklistUntil, now));
        for (User u : expired) {
            unblacklist(u);
            justUnblacklisted.add(u.getId());
        }

        // 只查询非管理员、非已有的黑名单用户
        List<User> allUsers = userService.list(new LambdaQueryWrapper<User>()
                .isNull(User::getBlacklistStatus)
                .ne(User::getRole, "ADMIN"));
        // 排除刚解封的用户
        allUsers.removeIf(u -> justUnblacklisted.contains(u.getId()));
        if (allUsers.isEmpty()) return;

        // 只查近期有1星的评论（90天内，既减少数据量也避免历史违规被反复处罚）
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        List<Comment> allComments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .le(Comment::getRating, 1)
                        .ge(Comment::getCreateTime, ninetyDaysAgo));
        if (allComments.isEmpty()) return;

        // 只查已成交订单（用于买家购买数判断）
        List<Order> completedOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, "COMPLETED"));

        // 按被评论商品所属卖家统计
        Set<Long> relevantProductIds = allComments.stream()
                .map(Comment::getProductId).collect(Collectors.toSet());
        Map<Long, Long> productOwner = new HashMap<>();
        for (Product p : productMapper.selectBatchIds(relevantProductIds)) {
            productOwner.put(p.getId(), p.getUserId());
        }

        Map<Long, List<Comment>> sellerComments = new HashMap<>();
        for (Comment c : allComments) {
            Long ownerId = productOwner.get(c.getProductId());
            if (ownerId != null) {
                sellerComments.computeIfAbsent(ownerId, k -> new ArrayList<>()).add(c);
            }
        }

        // 按评论发出者统计（只统计发出的1星）
        Map<Long, List<Comment>> userOneStarComments = allComments.stream()
                .filter(c -> c.getRating() != null && c.getRating() == 1)
                .collect(Collectors.groupingBy(Comment::getUserId));

        for (User user : allUsers) {
            // 曾受过处罚的用户，只统计解封后的新评论
            int prevCount = user.getBlacklistCount() != null ? user.getBlacklistCount() : 0;
            LocalDateTime since = prevCount > 0 && user.getUpdateTime() != null
                    ? user.getUpdateTime() : ninetyDaysAgo;

            // 卖家判定：收到 1 星 >= 5 且 占比 > 30%
            List<Comment> received = sellerComments.getOrDefault(user.getId(), Collections.emptyList());
            if (prevCount > 0) {
                final LocalDateTime cutoff = since;
                received = received.stream().filter(c -> c.getCreateTime() != null
                        && c.getCreateTime().isAfter(cutoff)).collect(Collectors.toList());
            }
            if (received.size() >= 5) {
                long oneStar = received.stream().filter(c -> c.getRating() != null && c.getRating() == 1).count();
                if (oneStar >= 5 && (double) oneStar / received.size() > 0.3) {
                    blacklist(user, "AUTO", buildReason("卖家", oneStar, received.size()), BASE_DAYS);
                    continue;
                }
            }

            // 买家判定：发出的 1 星 > 80% 且购买订单数 = 0
            List<Comment> givenOneStar = userOneStarComments.getOrDefault(user.getId(), Collections.emptyList());
            if (prevCount > 0) {
                final LocalDateTime cutoff = since;
                givenOneStar = givenOneStar.stream().filter(c -> c.getCreateTime() != null
                        && c.getCreateTime().isAfter(cutoff)).collect(Collectors.toList());
            }
            if (!givenOneStar.isEmpty()) {
                // 获取该用户所有评论数（不仅是1星）
                long totalGiven = allComments.stream()
                        .filter(c -> Objects.equals(c.getUserId(), user.getId())).count();
                long orderCount = completedOrders.stream()
                        .filter(o -> Objects.equals(o.getBuyerId(), user.getId())).count();
                if (totalGiven > 0 && (double) givenOneStar.size() / totalGiven > 0.8 && orderCount == 0) {
                    blacklist(user, "AUTO", buildBuyerReason(givenOneStar.size(), totalGiven), BASE_DAYS);
                }
            }
        }
    }

    @Override
    public IPage<User> blacklistPage(Integer pageNum, Integer pageSize) {
        return userService.page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<User>().isNotNull(User::getBlacklistStatus));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualBlacklist(Long userId, String reason, Integer days) {
        User user = userService.getById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        if ("ADMIN".equals(user.getRole())) throw new BusinessException("管理员账号不受限制");
        blacklist(user, "MANUAL", reason != null ? reason : "管理员手动拉黑",
                days != null && days > 0 ? days : BASE_DAYS);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unblacklist(Long userId) {
        User user = userService.getById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        unblacklist(user);
    }

    @Override
    public boolean isBlacklisted(Long userId) {
        User user = userService.getById(userId);
        return user != null && user.getBlacklistStatus() != null;
    }

    @Override
    public User getUserById(Long userId) {
        return userService.getById(userId);
    }

    // ========== 内部方法 ==========

    private void blacklist(User user, String type, String reason, int days) {
        int count = user.getBlacklistCount() != null ? user.getBlacklistCount() : 0;
        int actualDays = days * (int) Math.pow(2, count);
        LocalDateTime until = LocalDateTime.now().plusDays(actualDays);
        userService.update(new LambdaUpdateWrapper<User>()
                .set(User::getBlacklistStatus, type)
                .set(User::getBlacklistReason, reason)
                .set(User::getBlacklistUntil, until)
                .set(User::getUpdateTime, LocalDateTime.now())
                .eq(User::getId, user.getId()));
        // 发送通知
        String prefix = "AUTO".equals(type) ? "系统检测到违规行为，" : "管理员已将";
        String suffix = "AUTO".equals(type) ? "，你已被限制使用" : "的账号限制使用";
        notificationService.send(user.getId(), "账号已被限制",
                prefix + reason + suffix + "至 " + until.toLocalDate(), type);
    }

    private void unblacklist(User user) {
        int newCount = (user.getBlacklistCount() != null ? user.getBlacklistCount() : 0) + 1;
        userService.update(new LambdaUpdateWrapper<User>()
                .setSql("blacklist_status = NULL")
                .setSql("blacklist_reason = NULL")
                .setSql("blacklist_until = NULL")
                .set(User::getBlacklistCount, newCount)
                .set(User::getUpdateTime, LocalDateTime.now())
                .eq(User::getId, user.getId()));
        notificationService.send(user.getId(), "账号已恢复正常",
                "你的账号限制已解除，可以正常使用平台功能了", "UNBLACKLIST");
    }

    private String buildReason(String role, long oneStar, long total) {
        return String.format("%s收到%d条1星差评（共%d条评论），超过30%%阈值，系统自动拉黑", role, oneStar, total);
    }

    private String buildBuyerReason(long oneStar, long total) {
        return String.format("买家发出%d条1星差评（共%d条评论），超过80%%阈值且无购买记录，系统自动拉黑", oneStar, total);
    }
}
