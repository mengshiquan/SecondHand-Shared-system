package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.secondhand.dto.OrderDTO;
import com.campus.secondhand.entity.Order;
import com.campus.secondhand.vo.OrderVO;

public interface OrderService extends IService<Order> {

    OrderVO createOrder(OrderDTO dto);

    void payOrder(Long id);

    void updateStatus(Long id, String status);

    IPage<OrderVO> pageList(Integer pageNum, Integer pageSize, String status);

    OrderVO detail(Long id);

    void cancelOrder(Long id);

    void applyRefund(Long id, String reason);

    void handleRefund(Long id, boolean agree);

    void applyArbitration(Long id);

    void adminArbitrate(Long id, boolean refund);

    void updateAddress(Long id, Long addressId);

    void deleteOrder(Long id);
}
