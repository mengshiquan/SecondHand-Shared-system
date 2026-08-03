package com.campus.secondhand.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.dto.OrderDTO;
import com.campus.secondhand.entity.Order;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.mapper.OrderMapper;
import com.campus.secondhand.service.OrderService;
import com.campus.secondhand.service.ProductService;
import com.campus.secondhand.util.UserContext;
import com.campus.secondhand.vo.OrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 订单服务实现
 * 状态流转: PENDING(待付款) → PAID(已付款) → SHIPPED(已发货) → COMPLETED(已完成)
 *                      ↘ CANCELLED(已取消)
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final Map<String, List<String>> STATUS_FLOW = new HashMap<>();

    static {
        STATUS_FLOW.put("PENDING", Arrays.asList("PAID", "CANCELLED"));
        STATUS_FLOW.put("PAID", Arrays.asList("SHIPPED"));
        STATUS_FLOW.put("SHIPPED", Arrays.asList("COMPLETED"));
        STATUS_FLOW.put("COMPLETED", Arrays.asList());
        STATUS_FLOW.put("CANCELLED", Arrays.asList());
    }

    @Autowired
    private ProductService productService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(OrderDTO dto) {
        Long buyerId = UserContext.getUserId();
        Product product = productService.getById(dto.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!"ON_SALE".equals(product.getStatus())) {
            throw new BusinessException("商品不在售");
        }
        if (product.getUserId().equals(buyerId)) {
            throw new BusinessException("不能购买自己发布的商品");
        }

        // 检查是否有其他人已预留此商品
        long activeReserve = count(new LambdaQueryWrapper<Order>()
                .eq(Order::getProductId, dto.getProductId())
                .eq(Order::getStatus, "PENDING")
                .gt(Order::getExpireTime, LocalDateTime.now()));
        if (activeReserve > 0) {
            throw new BusinessException("该商品已被他人预留，请稍后再试");
        }

        // 取消自己同一商品之前的未支付订单
        List<Order> oldPending = list(new LambdaQueryWrapper<Order>()
                .eq(Order::getProductId, dto.getProductId())
                .eq(Order::getBuyerId, buyerId)
                .eq(Order::getStatus, "PENDING"));
        for (Order old : oldPending) {
            old.setStatus("CANCELLED");
            updateById(old);
        }

        Order order = new Order();
        order.setOrderNo("ORD" + IdUtil.getSnowflakeNextIdStr());
        order.setProductId(dto.getProductId());
        order.setBuyerId(buyerId);
        order.setSellerId(product.getUserId());
        order.setPrice(product.getPrice());
        order.setBuyerName(dto.getBuyerName());
        order.setBuyerPhone(dto.getBuyerPhone());
        order.setBuyerAddress(dto.getBuyerAddress());
        order.setStatus("PENDING");
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));
        order.setRemark(dto.getRemark());
        save(order);

        // 下架商品（预留）
        product.setStatus("OFF_SHELF");
        productService.updateById(product);

        return baseMapper.selectOrderDetail(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long id) {
        Order order = getById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"PENDING".equals(order.getStatus())) throw new BusinessException("订单状态不允许付款");
        if (!order.getBuyerId().equals(UserContext.getUserId())) throw new BusinessException("仅买家可付款");

        // 检查是否过期
        if (order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
            order.setStatus("CANCELLED");
            updateById(order);
            restoreProduct(order.getProductId());
            throw new BusinessException("订单已超时，请重新下单");
        }

        order.setStatus("PAID");
        updateById(order);

        // 商品标记为已售
        Product product = productService.getById(order.getProductId());
        product.setStatus("SOLD");
        productService.updateById(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String status) {
        Order order = getById(id);
        if (order == null) throw new BusinessException("订单不存在");

        checkOrderPermission(order, status);

        List<String> allowed = STATUS_FLOW.get(order.getStatus());
        if (allowed == null || !allowed.contains(status)) {
            throw new BusinessException("不允许的状态流转");
        }

        order.setStatus(status);
        updateById(order);

        // 取消 → 恢复商品
        if ("CANCELLED".equals(status)) {
            restoreProduct(order.getProductId());
        }
        // 确认收货 → 标记售出
        if ("COMPLETED".equals(status)) {
            Product product = productService.getById(order.getProductId());
            product.setStatus("SOLD");
            productService.updateById(product);
        }
    }

    @Override
    public IPage<OrderVO> pageList(Integer pageNum, Integer pageSize, String status) {
        Page<OrderVO> page = new Page<>(pageNum, pageSize);
        return baseMapper.selectOrderPage(page, UserContext.getUserId(), UserContext.getRole(), status);
    }

    @Override
    public OrderVO detail(Long id) {
        OrderVO vo = baseMapper.selectOrderDetail(id);
        if (vo == null) throw new BusinessException("订单不存在");
        if (!UserContext.isAdmin()
                && !vo.getBuyerId().equals(UserContext.getUserId())
                && !vo.getSellerId().equals(UserContext.getUserId())) {
            throw new BusinessException("无权查看此订单");
        }
        return vo;
    }

    /** 定时清理过期待付款订单（每5分钟） */
    @Scheduled(fixedRate = 300000)
    @Transactional(rollbackFor = Exception.class)
    public void cancelExpiredOrders() {
        List<Order> expired = list(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, "PENDING")
                .lt(Order::getExpireTime, LocalDateTime.now()));
        for (Order order : expired) {
            order.setStatus("CANCELLED");
            updateById(order);
            restoreProduct(order.getProductId());
        }
    }

    private void restoreProduct(Long productId) {
        Product product = productService.getById(productId);
        if (product != null && "OFF_SHELF".equals(product.getStatus())) {
            product.setStatus("ON_SALE");
            productService.updateById(product);
        }
    }

    private void checkOrderPermission(Order order, String newStatus) {
        Long userId = UserContext.getUserId();
        if (UserContext.isAdmin()) return;

        if ("PAID".equals(newStatus) && !order.getBuyerId().equals(userId))
            throw new BusinessException("仅买家可付款");
        if ("SHIPPED".equals(newStatus) && !order.getSellerId().equals(userId))
            throw new BusinessException("仅卖家可发货");
        if ("COMPLETED".equals(newStatus) && !order.getBuyerId().equals(userId))
            throw new BusinessException("仅买家可确认收货");
        if ("CANCELLED".equals(newStatus)
                && !order.getBuyerId().equals(userId)
                && !order.getSellerId().equals(userId))
            throw new BusinessException("无权取消订单");
    }
}
