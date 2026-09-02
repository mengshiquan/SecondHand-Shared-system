package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.entity.Category;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.vo.DashboardVO;
import com.campus.secondhand.vo.OrderVO;
import com.campus.secondhand.vo.ProductVO;

public interface AdminService {

    /** 后台仪表盘统计，使用 Redis 缓存；数据变更后由相关业务负责失效。 */
    DashboardVO dashboard();

    /** 分页查询普通用户，支持用户名/昵称关键字和认证状态筛选。 */
    IPage<User> userPage(Integer pageNum, Integer pageSize, String keyword, String verifyStatus);

    /** 批量审核身份认证 */
    void verifyUsers(java.util.List<Long> userIds, String action);

    /** 启用或禁用普通用户/管理员，超级管理员状态不可修改。 */
    void updateUserStatus(Long userId, Integer status);

    /** 分页查询商品，支持关键字、分类和状态筛选。 */
    IPage<ProductVO> productPage(Integer pageNum, Integer pageSize, String keyword, Long categoryId, Long parentCategoryId, String status);

    /** 分页查询全部订单，支持订单状态和退款状态筛选。 */
    IPage<OrderVO> orderPage(Integer pageNum, Integer pageSize, String status, String refundStatus);

    /** 新增或编辑分类；系统最多保留两级，且上级必须是一级分类。 */
    void saveCategory(Category category);

    /** 逻辑删除空分类；分类下仍有商品或子分类时禁止删除。 */
    void deleteCategory(Long id);

    /** 统计每个分类的商品数量，用于后台图表。 */
    java.util.List<java.util.Map<String, Object>> categoryStats();

    // === 小黑屋 ===
    /** 立即执行一次违规自动扫描并进入小黑屋。 */
    void triggerScan();

    /** 分页查询当前处于小黑屋中的用户。 */
    com.baomidou.mybatisplus.core.metadata.IPage<User> blacklistPage(Integer pageNum, Integer pageSize);

    /** 管理员手动拉黑用户。 */
    void manualBlacklist(Long userId, String reason, Integer days);

    /** 管理员手动解除用户拉黑并恢复登录能力。 */
    void unblacklist(Long userId);

    // === 投诉 ===
    /** 分页查询用户投诉，支持处理状态筛选。 */
    com.baomidou.mybatisplus.core.metadata.IPage<com.campus.secondhand.entity.Complaint> complaintPage(Integer pageNum, Integer pageSize, String status);

    /** 处理投诉：成立时按规则拉黑被投诉人，驳回时维持原状态。 */
    void handleComplaint(Long complaintId, boolean approve, String handlerNote);

    // === 申诉 ===
    /** 分页查询用户申诉，支持处理状态筛选。 */
    com.baomidou.mybatisplus.core.metadata.IPage<com.campus.secondhand.entity.Appeal> appealPage(Integer pageNum, Integer pageSize, String status);

    /** 处理申诉：通过时解除小黑屋，驳回时维持账号限制。 */
    void handleAppeal(Long appealId, boolean approve, String handlerNote);

    // === 通知 ===
    /** 汇总后台待处理事项：投诉、申诉、小黑屋和退款仲裁。 */
    java.util.Map<String, Object> getNotifications();

    // === 仲裁 + 订单/商品管理 ===
    /** 管理员仲裁退款纠纷；退款通过则退款，未通过则维持交易。 */
    void arbitrate(Long orderId, boolean refund);

    /** 管理员直接修改订单状态，仅用于后台纠错或运营处理。 */
    void updateOrderStatus(Long orderId, String status);

    /** 删除已完成或已取消订单，其他状态禁止删除。 */
    void deleteOrder(Long orderId);

    /** 管理员调整商品状态并立即失效商品详情缓存。 */
    void updateProductStatus(Long productId, String status);

    /** 管理员逻辑删除商品并失效商品详情缓存。 */
    void deleteProduct(Long productId);

    // ===== 管理员管理（仅 SUPER_ADMIN） =====
    /** 管理员列表；普通管理员可读，写操作由具体方法限制为超管。 */
    IPage<User> adminPage(Integer pageNum, Integer pageSize);

    /** 超级管理员创建管理员账号，返回初始明文密码。 */
    String createAdmin(String username, String nickname);

    /** 超级管理员更新管理员昵称。 */
    void updateAdmin(Long id, String nickname);

    /** 超级管理员删除管理员；不允许删除超级管理员或自己。 */
    void deleteAdmin(Long id);

    /** 超级管理员启用/禁用管理员；不允许禁用超级管理员或自己。 */
    void updateAdminStatus(Long id, Integer status);

    // ===== 用户管理补全 =====
    /** 后台创建用户或管理员；校园认证直接置为已通过，不允许创建超级管理员。 */
    void createUser(String username, String password, String nickname, String role);

    /** 后台更新用户昵称、手机号和邮箱，超级管理员信息不可修改。 */
    void updateUser(Long id, String nickname, String phone, String email);

    /** 级联清理并删除用户；不允许删除超级管理员或自己。 */
    void deleteUser(Long id);

    /** 重置密码：新密码为空时用默认密码，返回明文新密码供管理员告知用户 */
    String resetUserPassword(Long id, String newPassword);

    /** 调整用户角色（仅超管）：USER ↔ ADMIN */
    void updateUserRole(Long id, String role);
}
