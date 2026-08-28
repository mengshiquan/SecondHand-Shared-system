package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.common.RedisKeyConstants;
import com.campus.secondhand.dto.ProductQueryDTO;
import com.campus.secondhand.entity.Address;
import com.campus.secondhand.entity.Cart;
import com.campus.secondhand.entity.Category;
import com.campus.secondhand.entity.Appeal;
import com.campus.secondhand.entity.Complaint;
import com.campus.secondhand.entity.Favorite;
import com.campus.secondhand.entity.Order;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.mapper.AddressMapper;
import com.campus.secondhand.mapper.AppealMapper;
import com.campus.secondhand.mapper.CartMapper;
import com.campus.secondhand.mapper.ComplaintMapper;
import com.campus.secondhand.mapper.FavoriteMapper;
import com.campus.secondhand.mapper.OrderMapper;
import com.campus.secondhand.mapper.ProductMapper;
import com.campus.secondhand.service.AdminService;
import com.campus.secondhand.service.BlacklistService;
import com.campus.secondhand.service.CategoryService;
import com.campus.secondhand.service.NotificationService;
import com.campus.secondhand.service.OrderService;
import com.campus.secondhand.service.UserService;
import com.campus.secondhand.util.RedisCacheUtil;
import com.campus.secondhand.util.UserContext;
import com.campus.secondhand.vo.DashboardVO;
import com.campus.secondhand.vo.OrderVO;
import com.campus.secondhand.vo.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
    @Autowired
    private OrderService orderService;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private FavoriteMapper favoriteMapper;
    @Autowired
    private AddressMapper addressMapper;
    @Autowired
    private CartMapper cartMapper;

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
        if ("SUPER_ADMIN".equals(user.getRole())) {
            throw new BusinessException("不能修改超级管理员状态");
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
    public IPage<OrderVO> orderPage(Integer pageNum, Integer pageSize, String status, String refundStatus) {
        checkAdmin();
        return orderMapper.selectOrderPage(new Page<>(pageNum, pageSize), null, "ADMIN", status, refundStatus);
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

    private void checkSuperAdmin() {
        if (!UserContext.isSuperAdmin()) {
            throw new BusinessException("仅超级管理员可执行此操作");
        }
    }

    // === 仲裁 + 订单/商品管理 ===

    @Override
    public void arbitrate(Long orderId, boolean refund) {
        checkAdmin();
        orderService.adminArbitrate(orderId, refund);
    }

    @Override
    public void updateOrderStatus(Long orderId, String status) {
        checkAdmin();
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        order.setStatus(status);
        orderMapper.updateById(order);
    }

    @Override
    public void deleteOrder(Long orderId) {
        checkAdmin();
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"COMPLETED".equals(order.getStatus()) && !"CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("仅已完成或已取消的订单可删除");
        }
        orderMapper.deleteById(orderId);
    }

    @Override
    public void updateProductStatus(Long productId, String status) {
        checkAdmin();
        Product product = productMapper.selectById(productId);
        if (product == null) throw new BusinessException("商品不存在");
        product.setStatus(status);
        productMapper.updateById(product);
        cacheUtil.delete(String.format(RedisKeyConstants.CACHE_PRODUCT_DETAIL, productId));
    }

    @Override
    public void deleteProduct(Long productId) {
        checkAdmin();
        productMapper.deleteById(productId);
        cacheUtil.delete(String.format(RedisKeyConstants.CACHE_PRODUCT_DETAIL, productId));
    }

    // ===== 管理员管理（仅 SUPER_ADMIN） =====

    @Override
    public IPage<User> adminPage(Integer pageNum, Integer pageSize) {
        checkSuperAdmin();
        return userService.page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<User>()
                        .in(User::getRole, "ADMIN", "SUPER_ADMIN")
                        .orderByDesc(User::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createAdmin(String username, String nickname) {
        checkSuperAdmin();
        long exists = userService.count(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (exists > 0) throw new BusinessException("用户名已存在");
        String password = cn.hutool.core.util.RandomUtil.randomString(8);
        User user = new User();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("ADMIN");
        user.setStatus(1);
        user.setVerifyStatus("APPROVED");
        userService.save(user);
        return password;
    }

    @Override
    public void updateAdmin(Long id, String nickname) {
        checkSuperAdmin();
        User user = userService.getById(id);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            throw new BusinessException("管理员不存在");
        }
        user.setNickname(nickname);
        userService.updateById(user);
    }

    @Override
    public void deleteAdmin(Long id) {
        checkSuperAdmin();
        User user = userService.getById(id);
        if (user == null) throw new BusinessException("管理员不存在");
        if ("SUPER_ADMIN".equals(user.getRole())) throw new BusinessException("不能删除超级管理员");
        if (user.getId().equals(UserContext.getUserId())) throw new BusinessException("不能删除自己");
        userService.removeById(id);
    }

    @Override
    public void updateAdminStatus(Long id, Integer status) {
        checkSuperAdmin();
        User user = userService.getById(id);
        if (user == null) throw new BusinessException("管理员不存在");
        if ("SUPER_ADMIN".equals(user.getRole())) throw new BusinessException("不能禁用超级管理员");
        if (user.getId().equals(UserContext.getUserId())) throw new BusinessException("不能禁用自己");
        user.setStatus(status);
        userService.updateById(user);
    }

    // ===== 用户管理补全 =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(String username, String password, String nickname, String role) {
        checkAdmin();
        if ("SUPER_ADMIN".equals(role)) throw new BusinessException("不能创建超级管理员");
        long exists = userService.count(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (exists > 0) throw new BusinessException("用户名已存在");
        User user = new User();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role == null ? "USER" : role);
        user.setStatus(1);
        user.setVerifyStatus("APPROVED");
        userService.save(user);
    }

    @Override
    public void updateUser(Long id, String nickname, String phone, String email) {
        checkAdmin();
        User user = userService.getById(id);
        if (user == null) throw new BusinessException("用户不存在");
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setEmail(email);
        userService.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        checkAdmin();
        User user = userService.getById(id);
        if (user == null) throw new BusinessException("用户不存在");
        if ("SUPER_ADMIN".equals(user.getRole())) throw new BusinessException("不能删除超级管理员");
        if (user.getId().equals(UserContext.getUserId())) throw new BusinessException("不能删除自己");

        // 级联清理：商品下架、未完成订单取消、收藏/地址/购物车清除；绕过 Service 层缓存清理，需手动失效详情缓存
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getUserId, id)
                .set(Product::getStatus, "OFF_SHELF"));
        productMapper.selectList(new LambdaQueryWrapper<Product>()
                        .select(Product::getId).eq(Product::getUserId, id))
                .forEach(p -> cacheUtil.delete(String.format(RedisKeyConstants.CACHE_PRODUCT_DETAIL, p.getId())));
        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .in(Order::getStatus, "PENDING", "PAID", "SHIPPED")
                .and(w -> w.eq(Order::getBuyerId, id).or().eq(Order::getSellerId, id))
                .set(Order::getStatus, "CANCELLED"));
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, id));
        addressMapper.delete(new LambdaQueryWrapper<Address>().eq(Address::getUserId, id));
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, id));

        user.setStatus(2); // 已注销，防止再次登录
        userService.updateById(user);
        userService.removeById(id);
    }

    @Override
    public void resetUserPassword(Long id) {
        checkAdmin();
        User user = userService.getById(id);
        if (user == null) throw new BusinessException("用户不存在");
        if ("SUPER_ADMIN".equals(user.getRole())) throw new BusinessException("不能重置超级管理员密码");
        user.setPassword(passwordEncoder.encode(cn.hutool.core.util.RandomUtil.randomString(8)));
        userService.updateById(user);
    }
}
