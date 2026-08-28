package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.common.RedisKeyConstants;
import com.campus.secondhand.dto.ProductQueryDTO;
import com.campus.secondhand.entity.Category;
import com.campus.secondhand.entity.Appeal;
import com.campus.secondhand.entity.Complaint;
import com.campus.secondhand.entity.Order;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.mapper.AppealMapper;
import com.campus.secondhand.mapper.ComplaintMapper;
import com.campus.secondhand.mapper.OrderMapper;
import com.campus.secondhand.mapper.ProductMapper;
import com.campus.secondhand.service.AdminService;
import com.campus.secondhand.service.BlacklistService;
import com.campus.secondhand.service.CategoryService;
import com.campus.secondhand.service.NotificationService;
import com.campus.secondhand.service.UserService;
import com.campus.secondhand.util.RedisCacheUtil;
import com.campus.secondhand.util.UserContext;
import com.campus.secondhand.vo.DashboardVO;
import com.campus.secondhand.vo.OrderVO;
import com.campus.secondhand.vo.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserService userService;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private BlacklistService blacklistService;
    @Autowired
    private ComplaintMapper complaintMapper;
    @Autowired
    private AppealMapper appealMapper;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private RedisCacheUtil cacheUtil;

    @Override
    public DashboardVO dashboard() {
        checkAdmin();
        DashboardVO vo = cacheUtil.get(RedisKeyConstants.CACHE_ADMIN_DASHBOARD);
        if (vo != null) return vo;

        vo = new DashboardVO();
        vo.setUserCount(userService.count());
        vo.setProductCount(productMapper.selectCount(null));
        vo.setOrderCount(orderMapper.selectCount(null));

        LocalDateTime start = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        vo.setTodayOrderCount(orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .between(Order::getCreateTime, start, end)));

        cacheUtil.set(RedisKeyConstants.CACHE_ADMIN_DASHBOARD, vo, RedisKeyConstants.TTL_DASHBOARD);
        return vo;
    }

    @Override
    public IPage<User> userPage(Integer pageNum, Integer pageSize, String keyword, String verifyStatus) {
        checkAdmin();
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(User::getUsername, keyword).or().like(User::getNickname, keyword);
        }
        if (verifyStatus != null && !verifyStatus.isEmpty()) {
            wrapper.eq(User::getVerifyStatus, verifyStatus);
        }
        wrapper.orderByDesc(User::getCreateTime);
        IPage<User> result = userService.page(page, wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return result;
    }

    @Override
    public void verifyUsers(List<Long> userIds, String action) {
        checkAdmin();
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException("请选择要审核的用户");
        }
        if (!"APPROVE".equals(action) && !"REJECT".equals(action)) {
            throw new BusinessException("无效的审核动作");
        }
        String target = "APPROVE".equals(action) ? "APPROVED" : "REJECTED";
        for (Long uid : userIds) {
            User user = userService.getById(uid);
            if (user != null && "PENDING".equals(user.getVerifyStatus())) {
                user.setVerifyStatus(target);
                userService.updateById(user);
            }
        }
    }

    @Override
    public void updateUserStatus(Long userId, Integer status) {
        checkAdmin();
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        userService.updateById(user);
    }

    @Override
    public IPage<ProductVO> productPage(Integer pageNum, Integer pageSize, String keyword) {
        checkAdmin();
        ProductQueryDTO query = new ProductQueryDTO();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        query.setKeyword(keyword);
        query.setStatus(null);
        return productMapper.selectProductPage(new Page<>(pageNum, pageSize), query);
    }

    @Override
    public IPage<OrderVO> orderPage(Integer pageNum, Integer pageSize, String status) {
        checkAdmin();
        return orderMapper.selectOrderPage(new Page<>(pageNum, pageSize), null, "ADMIN", status);
    }

    @Override
    public void saveCategory(Category category) {
        checkAdmin();
        if (category.getId() == null) {
            categoryService.save(category);
        } else {
            categoryService.updateById(category);
        }
    }

    @Override
    public void deleteCategory(Long id) {
        checkAdmin();
        long count = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, id));
        if (count > 0) {
            throw new BusinessException("该分类下存在商品，无法删除");
        }
        categoryService.removeById(id);
    }

    @Override
    public List<Map<String, Object>> categoryStats() {
        checkAdmin();
        List<Category> categories = categoryService.list();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Category cat : categories) {
            long count = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                    .eq(Product::getCategoryId, cat.getId()));
            Map<String, Object> item = new HashMap<>();
            item.put("name", cat.getName());
            item.put("value", count);
            result.add(item);
        }
        return result;
    }

    // === 小黑屋 ===

    @Override
    public void triggerScan() {
        checkAdmin();
        blacklistService.autoScan();
    }

    @Override
    public IPage<User> blacklistPage(Integer pageNum, Integer pageSize) {
        checkAdmin();
        return blacklistService.blacklistPage(pageNum, pageSize);
    }

    @Override
    public void manualBlacklist(Long userId, String reason, Integer days) {
        checkAdmin();
        blacklistService.manualBlacklist(userId, reason, days);
    }

    @Override
    public void unblacklist(Long userId) {
        checkAdmin();
        blacklistService.unblacklist(userId);
    }

    // === 投诉 ===

    @Override
    public IPage<Complaint> complaintPage(Integer pageNum, Integer pageSize, String status) {
        checkAdmin();
        LambdaQueryWrapper<Complaint> wrapper = new LambdaQueryWrapper<Complaint>()
                .orderByDesc(Complaint::getCreateTime);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Complaint::getStatus, status);
        }
        return complaintMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleComplaint(Long complaintId, boolean approve, String handlerNote) {
        checkAdmin();
        Complaint complaint = complaintMapper.selectById(complaintId);
        if (complaint == null || !"PENDING".equals(complaint.getStatus())) {
            throw new BusinessException("投诉不存在或已处理");
        }
        complaint.setStatus(approve ? "RESOLVED" : "DISMISSED");
        complaint.setHandlerId(UserContext.getUserId());
        complaint.setHandlerNote(handlerNote);
        complaintMapper.updateById(complaint);
        if (approve) {
            User targetUser = userService.getById(complaint.getTargetUserId());
            if (targetUser != null && !"ADMIN".equals(targetUser.getRole())) {
                blacklistService.manualBlacklist(complaint.getTargetUserId(),
                        "被投诉：" + (complaint.getReason() != null ? complaint.getReason() : "违规行为"),
                        14);
            }
            notificationService.send(complaint.getReporterId(), "投诉已处理",
                    "你投诉的用户已被处理，感谢你的反馈", "COMPLAINT_RESOLVED");
        }
    }

    // === 申诉 ===

    @Override
    public IPage<Appeal> appealPage(Integer pageNum, Integer pageSize, String status) {
        checkAdmin();
        LambdaQueryWrapper<Appeal> wrapper = new LambdaQueryWrapper<Appeal>()
                .orderByDesc(Appeal::getCreateTime);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Appeal::getStatus, status);
        }
        return appealMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleAppeal(Long appealId, boolean approve, String handlerNote) {
        checkAdmin();
        Appeal appeal = appealMapper.selectById(appealId);
        if (appeal == null || !"PENDING".equals(appeal.getStatus())) {
            throw new BusinessException("申诉不存在或已处理");
        }
        appeal.setStatus(approve ? "APPROVED" : "REJECTED");
        appeal.setHandlerId(UserContext.getUserId());
        appeal.setHandlerNote(handlerNote);
        appealMapper.updateById(appeal);
        if (approve) {
            blacklistService.unblacklist(appeal.getUserId());
            notificationService.send(appeal.getUserId(), "申诉已通过",
                    "你的申诉已通过审核，账号已恢复正常使用", "APPEAL_APPROVED");
        } else {
            notificationService.send(appeal.getUserId(), "申诉已被驳回",
                    "你的申诉已被驳回，账号限制维持不变", "APPEAL_REJECTED");
        }
    }

    // === 通知 ===
    @Override
    public java.util.Map<String, Object> getNotifications() {
        checkAdmin();
        Map<String, Object> result = new HashMap<>();
        long complaints = complaintMapper.selectCount(
                new LambdaQueryWrapper<Complaint>().eq(Complaint::getStatus, "PENDING"));
        long appeals = appealMapper.selectCount(
                new LambdaQueryWrapper<Appeal>().eq(Appeal::getStatus, "PENDING"));
        long blacklist = userService.count(
                new LambdaQueryWrapper<User>().isNotNull(User::getBlacklistStatus));
        result.put("pendingComplaints", complaints);
        result.put("pendingAppeals", appeals);
        result.put("blacklistCount", blacklist);
        result.put("total", complaints + appeals + blacklist);
        return result;
    }

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException("无管理员权限");
        }
    }
}
