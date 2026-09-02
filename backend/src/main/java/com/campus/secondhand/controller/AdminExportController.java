package com.campus.secondhand.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.entity.Category;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.mapper.OrderMapper;
import com.campus.secondhand.service.CategoryService;
import com.campus.secondhand.service.ProductService;
import com.campus.secondhand.service.UserService;
import com.campus.secondhand.util.UserContext;
import com.campus.secondhand.vo.OrderVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 后台管理表格导出：用户/商品/订单三张表一键导出 Excel，便于离线统计
 */
@RestController
@RequestMapping("/admin/export")
public class AdminExportController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private UserService userService;
    @Autowired
    private ProductService productService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private OrderMapper orderMapper;

    /** 导出全部用户数据为 Excel。 */
    @GetMapping("/users")
    public void exportUsers(HttpServletResponse response) throws Exception {
        checkAdmin();
        List<User> users = userService.list(new LambdaQueryWrapper<User>().orderByAsc(User::getId));
        List<List<Object>> rows = users.stream().map(u -> row(
                u.getId(), u.getUsername(), u.getNickname(), roleText(u.getRole()),
                verifyText(u.getVerifyStatus()),
                u.getStatus() != null && u.getStatus() == 1 ? "正常" : "禁用",
                fmt(u.getCreateTime()))).collect(Collectors.toList());
        write(response, "用户管理",
                head("ID", "用户名", "昵称", "角色", "认证状态", "账号状态", "注册时间"), rows);
    }

    /** 导出全部商品数据为 Excel，并带出分类和卖家名称。 */
    @GetMapping("/products")
    public void exportProducts(HttpServletResponse response) throws Exception {
        checkAdmin();
        List<Product> products = productService.list(new LambdaQueryWrapper<Product>().orderByDesc(Product::getCreateTime));
        Map<Long, String> catNames = categoryService.list().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
        Map<Long, String> sellerNames = userService.list().stream()
                .collect(Collectors.toMap(User::getId,
                        u -> u.getNickname() != null ? u.getNickname() : u.getUsername(), (a, b) -> a));
        List<List<Object>> rows = products.stream().map(p -> row(
                p.getId(), p.getTitle(),
                catNames.getOrDefault(p.getCategoryId(), "-"),
                p.getPrice(), p.getOriginalPrice(),
                productText(p.getStatus()),
                sellerNames.getOrDefault(p.getUserId(), "-"),
                p.getViewCount(), fmt(p.getCreateTime()))).collect(Collectors.toList());
        write(response, "商品管理",
                head("ID", "标题", "分类", "售价", "原价", "状态", "卖家", "浏览量", "发布时间"), rows);
    }

    /** 导出全部订单数据为 Excel，并带出买家、卖家和状态中文说明。 */
    @GetMapping("/orders")
    public void exportOrders(HttpServletResponse response) throws Exception {
        checkAdmin();
        List<OrderVO> orders = orderMapper
                .selectOrderPage(new Page<>(1, 100000), null, "ADMIN", null, null).getRecords();
        List<List<Object>> rows = orders.stream().map(o -> row(
                o.getOrderNo(), o.getProductTitle(), o.getPrice(),
                o.getBuyerNickname(), o.getSellerName(),
                orderText(o.getStatus()), refundText(o.getRefundStatus()),
                fmt(o.getCreateTime()))).collect(Collectors.toList());
        write(response, "订单管理",
                head("订单号", "商品", "金额", "买家", "卖家", "订单状态", "退款状态", "创建时间"), rows);
    }

    // ===== 工具方法 =====

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException("无管理员权限");
        }
    }

    private void write(HttpServletResponse response, String sheet, List<List<String>> head, List<List<Object>> rows) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String filename = URLEncoder.encode(sheet + "_" + System.currentTimeMillis(), StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename=" + filename + ".xlsx");
        EasyExcel.write(response.getOutputStream()).head(head).sheet(sheet).doWrite(rows);
    }

    private List<List<String>> head(String... cols) {
        return Arrays.stream(cols).map(c -> new ArrayList<>(List.of(c))).collect(Collectors.toList());
    }

    private List<Object> row(Object... cells) {
        return new ArrayList<>(Arrays.asList(cells));
    }

    private String fmt(LocalDateTime t) {
        return t == null ? "-" : t.format(FMT);
    }

    private String roleText(String role) {
        if ("SUPER_ADMIN".equals(role)) return "超级管理员";
        if ("ADMIN".equals(role)) return "管理员";
        return "普通用户";
    }

    private String verifyText(String s) {
        if ("PENDING".equals(s)) return "待审核";
        if ("APPROVED".equals(s)) return "已认证";
        if ("REJECTED".equals(s)) return "已拒绝";
        return "未提交";
    }

    private String productText(String s) {
        if ("ON_SALE".equals(s)) return "在售";
        if ("SOLD".equals(s)) return "已售";
        if ("OFF_SHELF".equals(s)) return "已下架";
        return s == null ? "-" : s;
    }

    private String orderText(String s) {
        Map<String, String> map = Map.of(
                "PENDING", "待付款", "PAID", "已付款", "SHIPPED", "已发货",
                "COMPLETED", "已完成", "CANCELLED", "已取消");
        return map.getOrDefault(s, s);
    }

    private String refundText(String s) {
        if (s == null || "NONE".equals(s)) return "-";
        Map<String, String> map = Map.of(
                "REQUESTED", "退款待处理", "SELLER_AGREED", "卖家已同意", "SELLER_REJECTED", "卖家已拒绝",
                "ARBITRATION", "仲裁中", "ARBITRATION_REFUND", "仲裁退款", "ARBITRATION_MAINTAIN", "仲裁维持");
        return map.getOrDefault(s, s);
    }
}
