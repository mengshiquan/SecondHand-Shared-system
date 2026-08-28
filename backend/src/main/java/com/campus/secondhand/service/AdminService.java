package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.entity.Category;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.vo.DashboardVO;
import com.campus.secondhand.vo.OrderVO;
import com.campus.secondhand.vo.ProductVO;

public interface AdminService {

    DashboardVO dashboard();

    IPage<User> userPage(Integer pageNum, Integer pageSize, String keyword, String verifyStatus);

    /** 批量审核身份认证 */
    void verifyUsers(java.util.List<Long> userIds, String action);

    void updateUserStatus(Long userId, Integer status);

    IPage<ProductVO> productPage(Integer pageNum, Integer pageSize, String keyword);

    IPage<OrderVO> orderPage(Integer pageNum, Integer pageSize, String status, String refundStatus);

    void saveCategory(Category category);

    void deleteCategory(Long id);

    java.util.List<java.util.Map<String, Object>> categoryStats();

    // === 小黑屋 ===
    void triggerScan();
    com.baomidou.mybatisplus.core.metadata.IPage<User> blacklistPage(Integer pageNum, Integer pageSize);
    void manualBlacklist(Long userId, String reason, Integer days);
    void unblacklist(Long userId);

    // === 投诉 ===
    com.baomidou.mybatisplus.core.metadata.IPage<com.campus.secondhand.entity.Complaint> complaintPage(Integer pageNum, Integer pageSize, String status);
    void handleComplaint(Long complaintId, boolean approve, String handlerNote);

    // === 申诉 ===
    com.baomidou.mybatisplus.core.metadata.IPage<com.campus.secondhand.entity.Appeal> appealPage(Integer pageNum, Integer pageSize, String status);
    void handleAppeal(Long appealId, boolean approve, String handlerNote);

    // === 通知 ===
    java.util.Map<String, Object> getNotifications();

    // === 仲裁 + 订单/商品管理 ===
    void arbitrate(Long orderId, boolean refund);

    void updateOrderStatus(Long orderId, String status);

    void deleteOrder(Long orderId);

    void updateProductStatus(Long productId, String status);

    void deleteProduct(Long productId);

    // ===== 管理员管理（仅 SUPER_ADMIN） =====
    IPage<User> adminPage(Integer pageNum, Integer pageSize);

    String createAdmin(String username, String nickname);

    void updateAdmin(Long id, String nickname);

    void deleteAdmin(Long id);

    void updateAdminStatus(Long id, Integer status);

    // ===== 用户管理补全 =====
    void createUser(String username, String password, String nickname, String role);

    void updateUser(Long id, String nickname, String phone, String email);

    void deleteUser(Long id);

    void resetUserPassword(Long id);
}
