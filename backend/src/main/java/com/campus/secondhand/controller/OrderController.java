package com.campus.secondhand.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.common.Result;
import com.campus.secondhand.dto.OrderDTO;
import com.campus.secondhand.service.OrderService;
import com.campus.secondhand.vo.OrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 订单模块接口
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /** 创建订单 */
    @PostMapping
    public Result<OrderVO> create(@Validated @RequestBody OrderDTO dto) {
        return Result.success(orderService.createOrder(dto));
    }

    /** 订单列表 */
    @GetMapping("/list")
    public Result<IPage<OrderVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {
        return Result.success(orderService.pageList(pageNum, pageSize, status));
    }

    /** 当前用户订单状态计数（待付款/已付款/已发货），用于角标提醒 */
    @GetMapping("/status-counts")
    public Result<Map<String, Long>> statusCounts() {
        return Result.success(orderService.statusCounts());
    }

    /** 订单详情 */
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.success(orderService.detail(id));
    }

    /** 更新订单状态（发货、收货、取消） */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> params) {
        orderService.updateStatus(id, params.get("status"));
        return Result.success();
    }

    /** 买家取消订单 */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return Result.success();
    }

    /** 买家申请退款 */
    @PostMapping("/{id}/refund")
    public Result<Void> applyRefund(@PathVariable Long id, @RequestBody Map<String, String> params) {
        orderService.applyRefund(id, params.get("reason"));
        return Result.success();
    }

    /** 卖家处理退款 */
    @PutMapping("/{id}/refund/handle")
    public Result<Void> handleRefund(@PathVariable Long id, @RequestBody Map<String, Boolean> params) {
        orderService.handleRefund(id, Boolean.TRUE.equals(params.get("agree")));
        return Result.success();
    }

    /** 买家申请仲裁 */
    @PostMapping("/{id}/arbitration")
    public Result<Void> arbitration(@PathVariable Long id) {
        orderService.applyArbitration(id);
        return Result.success();
    }

    /** 修改收货地址 */
    @PutMapping("/{id}/address")
    public Result<Void> updateAddress(@PathVariable Long id, @RequestBody Map<String, Long> params) {
        orderService.updateAddress(id, params.get("addressId"));
        return Result.success();
    }

    /** 删除已完成/已取消订单 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return Result.success();
    }
}
