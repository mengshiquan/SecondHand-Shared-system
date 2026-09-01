package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.secondhand.dto.OrderDTO;
import com.campus.secondhand.entity.Order;
import com.campus.secondhand.vo.OrderVO;

import java.util.Map;

public interface OrderService extends IService<Order> {

    OrderVO createOrder(OrderDTO dto);
    
    /** 按订单号查询订单 */
    Order getByOrderNo(String orderNo);

    /** 支付前置校验（存在/归属/待付款/未超时），返回订单 */
    Order validatePayable(Long orderId);

    /** 支付回调/主动查询落单：CAS 改已支付 + 商品售罄 + 通知卖家，幂等 */
    void markPaid(String orderNo, String channel, String tradeNo);

    void updateStatus(Long id, String status);

    IPage<OrderVO> pageList(Integer pageNum, Integer pageSize, String status);

    /** 当前用户相关订单的状态计数（待付款/已付款/已发货），用于前端角标提醒 */
    Map<String, Long> statusCounts();

    OrderVO detail(Long id);

    void cancelOrder(Long id);

    void applyRefund(Long id, String reason);

    void handleRefund(Long id, boolean agree);

    void applyArbitration(Long id);

    void adminArbitrate(Long id, boolean refund);

    void updateAddress(Long id, Long addressId);

    void deleteOrder(Long id);
}
