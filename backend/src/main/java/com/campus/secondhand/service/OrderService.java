package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.secondhand.dto.OrderDTO;
import com.campus.secondhand.entity.Order;
import com.campus.secondhand.vo.OrderVO;

import java.util.Map;

public interface OrderService extends IService<Order> {

    /** 创建订单：使用 Redis 分布式锁和 CAS 扣库存，防止同一商品被重复下单。 */
    OrderVO createOrder(OrderDTO dto);
    
    /** 按订单号查询订单 */
    Order getByOrderNo(String orderNo);

    /** 支付前置校验（存在/归属/待付款/未超时），返回订单 */
    Order validatePayable(Long orderId);

    /** 支付回调/主动查询落单：CAS 改已支付 + 商品售罄 + 通知卖家，幂等 */
    void markPaid(String orderNo, String channel, String tradeNo);

    /** 买家或卖家更新订单状态；状态流转、角色和前置条件由服务实现校验。 */
    void updateStatus(Long id, String status);

    /** 分页查询当前用户作为买家或卖家参与的订单。 */
    IPage<OrderVO> pageList(Integer pageNum, Integer pageSize, String status);

    /** 当前用户相关订单的状态计数（待付款/已付款/已发货），用于前端角标提醒 */
    Map<String, Long> statusCounts();

    /** 查询订单详情；只有买家、卖家或管理员可访问。 */
    OrderVO detail(Long id);

    /** 买家取消待付款订单，取消后释放商品库存状态。 */
    void cancelOrder(Long id);

    /** 买家对已付款订单申请退款，进入卖家处理流程。 */
    void applyRefund(Long id, String reason);

    /** 卖家处理退款申请；同意退款或拒绝后进入对应状态。 */
    void handleRefund(Long id, boolean agree);

    /** 卖家拒绝退款后，买家申请平台仲裁。 */
    void applyArbitration(Long id);

    /** 管理员仲裁退款纠纷：通过则退款，拒绝则维持交易。 */
    void adminArbitrate(Long id, boolean refund);

    /** 买家更新待付款订单的收货地址。 */
    void updateAddress(Long id, Long addressId);

    /** 逻辑删除当前用户可见的已完成/已取消订单。 */
    void deleteOrder(Long id);
}
