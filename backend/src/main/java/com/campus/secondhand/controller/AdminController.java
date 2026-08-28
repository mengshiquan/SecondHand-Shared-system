package com.campus.secondhand.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.common.Result;
import com.campus.secondhand.entity.Appeal;
import com.campus.secondhand.entity.Category;
import com.campus.secondhand.entity.Complaint;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.service.AdminService;
import com.campus.secondhand.service.BlacklistService;
import com.campus.secondhand.vo.DashboardVO;
import com.campus.secondhand.vo.OrderVO;
import com.campus.secondhand.vo.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 后台管理模块接口
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;
    @Autowired
    private BlacklistService blacklistService;

    /** 仪表盘数据 */
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        return Result.success(adminService.dashboard());
    }

    /** 用户管理列表 */
    @GetMapping("/users")
    public Result<IPage<User>> users(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String verifyStatus) {
        return Result.success(adminService.userPage(pageNum, pageSize, keyword, verifyStatus));
    }

    /** 批量审核身份认证 */
    @PutMapping("/users/verify")
    public Result<Void> verifyUsers(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Long> userIds = ((List<Number>) params.get("userIds"))
                .stream().map(Number::longValue).collect(java.util.stream.Collectors.toList());
        adminService.verifyUsers(userIds, (String) params.get("action"));
        return Result.success();
    }

    /** 启用/禁用用户 */
    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        adminService.updateUserStatus(id, params.get("status"));
        return Result.success();
    }

    /** 商品管理列表 */
    @GetMapping("/products")
    public Result<IPage<ProductVO>> products(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(adminService.productPage(pageNum, pageSize, keyword));
    }

    /** 订单管理列表 */
    @GetMapping("/orders")
    public Result<IPage<OrderVO>> orders(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {
        return Result.success(adminService.orderPage(pageNum, pageSize, status));
    }

    /** 各分类商品数量统计 */
    @GetMapping("/category-stats")
    public Result<List<Map<String, Object>>> categoryStats() {
        return Result.success(adminService.categoryStats());
    }

    /** 新增/编辑分类 */
    @PostMapping("/category")
    public Result<Void> saveCategory(@RequestBody Category category) {
        adminService.saveCategory(category);
        return Result.success();
    }

    /** 删除分类 */
    @DeleteMapping("/category/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        adminService.deleteCategory(id);
        return Result.success();
    }

    /** 通知中心 */
    @GetMapping("/notifications")
    public Result<Map<String, Object>> notifications() {
        return Result.success(adminService.getNotifications());
    }

    // === 小黑屋 ===

    /** 触发自动扫描（手动调用） */
    @PostMapping("/blacklist/scan")
    public Result<Void> triggerScan() {
        adminService.triggerScan();
        return Result.success();
    }

    /** 小黑屋用户列表 */
    @GetMapping("/blacklist")
    public Result<IPage<User>> blacklist(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(adminService.blacklistPage(pageNum, pageSize));
    }

    /** 手动拉黑用户 */
    @PutMapping("/users/{id}/blacklist")
    public Result<Void> manualBlacklist(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        adminService.manualBlacklist(id,
                (String) params.get("reason"),
                params.get("days") != null ? ((Number) params.get("days")).intValue() : null);
        return Result.success();
    }

    /** 手动解封用户 */
    @PutMapping("/users/{id}/unblacklist")
    public Result<Void> unblacklist(@PathVariable Long id) {
        adminService.unblacklist(id);
        return Result.success();
    }

    // === 投诉 ===

    @GetMapping("/complaints")
    public Result<IPage<Complaint>> complaints(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {
        return Result.success(adminService.complaintPage(pageNum, pageSize, status));
    }

    @PutMapping("/complaints/{id}/handle")
    public Result<Void> handleComplaint(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        adminService.handleComplaint(id,
                Boolean.TRUE.equals(params.get("approve")),
                (String) params.get("handlerNote"));
        return Result.success();
    }

    // === 申诉 ===

    @GetMapping("/appeals")
    public Result<IPage<Appeal>> appeals(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {
        return Result.success(adminService.appealPage(pageNum, pageSize, status));
    }

    @PutMapping("/appeals/{id}/handle")
    public Result<Void> handleAppeal(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        adminService.handleAppeal(id,
                Boolean.TRUE.equals(params.get("approve")),
                (String) params.get("handlerNote"));
        return Result.success();
    }

    // === 仲裁 + 订单/商品管理 ===

    /** 管理员仲裁退款 */
    @PutMapping("/order/{id}/arbitration")
    public Result<Void> arbitrate(@PathVariable Long id, @RequestBody Map<String, Boolean> params) {
        adminService.arbitrate(id, Boolean.TRUE.equals(params.get("refund")));
        return Result.success();
    }

    /** 管理员修改订单状态 */
    @PutMapping("/orders/{id}/status")
    public Result<Void> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> params) {
        adminService.updateOrderStatus(id, params.get("status"));
        return Result.success();
    }

    /** 管理员删除订单 */
    @DeleteMapping("/orders/{id}")
    public Result<Void> deleteOrder(@PathVariable Long id) {
        adminService.deleteOrder(id);
        return Result.success();
    }

    /** 管理员修改商品状态 */
    @PutMapping("/products/{id}/status")
    public Result<Void> updateProductStatus(@PathVariable Long id, @RequestBody Map<String, String> params) {
        adminService.updateProductStatus(id, params.get("status"));
        return Result.success();
    }

    /** 管理员删除商品 */
    @DeleteMapping("/products/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        adminService.deleteProduct(id);
        return Result.success();
    }
}
