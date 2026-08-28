package com.campus.secondhand.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.common.RedisKeyConstants;
import com.campus.secondhand.common.VerifyGuard;
import com.campus.secondhand.dto.OrderDTO;
import com.campus.secondhand.entity.Address;
import com.campus.secondhand.entity.Order;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.mapper.OrderMapper;
import com.campus.secondhand.service.AddressService;
import com.campus.secondhand.service.OrderService;
import com.campus.secondhand.service.ProductService;
import com.campus.secondhand.service.UserService;
import com.campus.secondhand.util.RedisLockUtil;
import com.campus.secondhand.util.UserContext;
import com.campus.secondhand.vo.OrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
    @Autowired
    private RedisLockUtil redisLockUtil;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private UserService userService;
    @Autowired
    private AddressService addressService;

    @Override
    public OrderVO createOrder(OrderDTO dto) {
        VerifyGuard.requireVerified(userService);
        Long buyerId = UserContext.getUserId();
        String lockKey = String.format(RedisKeyConstants.LOCK_ORDER_CREATE, dto.getProductId());

        // 分布式锁必须覆盖“检查→下单→下架→事务提交”全程
        // 使用 TransactionTemplate 确保事务提交在 unlock 之前
        return redisLockUtil.executeWithLock(lockKey,
                java.time.Duration.ofSeconds(10), java.time.Duration.ofSeconds(3),
                () -> transactionTemplate.execute(status -> doCreateOrder(dto, buyerId)));
    }

    private OrderVO doCreateOrder(OrderDTO dto, Long buyerId) {
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
        order.setAddressId(dto.getAddressId());
        if (dto.getAddressId() != null) {
            com.campus.secondhand.entity.Address address = addressService.getById(dto.getAddressId());
            if (address != null && address.getUserId().equals(buyerId)) {
                order.setBuyerName(address.getReceiverName());
                order.setBuyerPhone(address.getPhone());
                order.setBuyerAddress(address.getAddress());
            }
        }
        save(order);

        // CAS 条件更新：只有商品仍在售才下架（纵深防御，即使锁失效也不会超卖）
        boolean offShelfed = productService.update(new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, dto.getProductId())
                .eq(Product::getStatus, "ON_SALE")
                .set(Product::getStatus, "OFF_SHELF"));
        if (!offShelfed) {
            throw new BusinessException("该商品已被他人抢先下单");
        }

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

        order.setPaymentTime(LocalDateTime.now());
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long id) {
        Order order = getById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"PENDING".equals(order.getStatus())) throw new BusinessException("仅待付款订单可取消");
        if (!order.getBuyerId().equals(UserContext.getUserId())) throw new BusinessException("仅买家可取消");
        order.setStatus("CANCELLED");
        updateById(order);
        restoreProduct(order.getProductId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRefund(Long id, String reason) {
        Order order = getById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getBuyerId().equals(UserContext.getUserId())) throw new BusinessException("仅买家可申请退款");
        if (!"PAID".equals(order.getStatus()) && !"SHIPPED".equals(order.getStatus())) {
            throw new BusinessException("当前订单状态不支持退款");
        }
        if (order.getRefundStatus() != null && !"NONE".equals(order.getRefundStatus())) {
            throw new BusinessException("退款流程已在进行中");
        }
        order.setRefundStatus("REQUESTED");
        order.setRefundReason(reason);
        order.setRefundTime(LocalDateTime.now());
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRefund(Long id, boolean agree) {
        Order order = getById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getSellerId().equals(UserContext.getUserId())) throw new BusinessException("仅卖家可处理退款");
        if (!"REQUESTED".equals(order.getRefundStatus())) throw new BusinessException("无待处理的退款申请");
        if (agree) {
            order.setRefundStatus("SELLER_AGREED");
            order.setStatus("CANCELLED");
            updateById(order);
            restoreProduct(order.getProductId());
        } else {
            order.setRefundStatus("SELLER_REJECTED");
            updateById(order);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyArbitration(Long id) {
        Order order = getById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getBuyerId().equals(UserContext.getUserId())) throw new BusinessException("仅买家可申请仲裁");
        if (!"SELLER_REJECTED".equals(order.getRefundStatus())) throw new BusinessException("仅卖家拒绝后可申请仲裁");
        // 72小时内有效（以退款申请时间起算）
        if (order.getRefundTime() != null
                && order.getRefundTime().plusHours(72).isBefore(LocalDateTime.now())) {
            throw new BusinessException("仲裁申请已超过72小时时限");
        }
        order.setRefundStatus("ARBITRATION");
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminArbitrate(Long id, boolean refund) {
        Order order = getById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"ARBITRATION".equals(order.getRefundStatus())) throw new BusinessException("无待仲裁的退款申请");
        if (refund) {
            order.setRefundStatus("ARBITRATION_REFUND");
            order.setStatus("CANCELLED");
            updateById(order);
            restoreProduct(order.getProductId());
        } else {
            order.setRefundStatus("ARBITRATION_MAINTAIN");
            updateById(order);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAddress(Long id, Long addressId) {
        Order order = getById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getBuyerId().equals(UserContext.getUserId())) throw new BusinessException("仅买家可修改地址");
        if (!"PENDING".equals(order.getStatus()) && !"PAID".equals(order.getStatus())) {
            throw new BusinessException("仅未发货订单可修改地址");
        }
        Address address = addressService.getById(addressId);
        if (address == null || !address.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException("收货地址不存在");
        }
        order.setAddressId(addressId);
        order.setBuyerName(address.getReceiverName());
        order.setBuyerPhone(address.getPhone());
        order.setBuyerAddress(address.getAddress());
        updateById(order);
    }

    @Override
    public void deleteOrder(Long id) {
        Order order = getById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getBuyerId().equals(UserContext.getUserId())
                && !order.getSellerId().equals(UserContext.getUserId())) {
            throw new BusinessException("无权删除此订单");
        }
        if (!"COMPLETED".equals(order.getStatus()) && !"CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("仅已完成或已取消的订单可删除");
        }
        removeById(id);
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

    /** 卖家48小时未处理退款申请，自动同意退款（每10分钟扫描） */
    @Scheduled(fixedRate = 600000)
    @Transactional(rollbackFor = Exception.class)
    public void autoRefundTimeout() {
        List<Order> pending = list(new LambdaQueryWrapper<Order>()
                .eq(Order::getRefundStatus, "REQUESTED")
                .lt(Order::getRefundTime, LocalDateTime.now().minusHours(48)));
        for (Order order : pending) {
            order.setRefundStatus("SELLER_AGREED");
            order.setStatus("CANCELLED");
            updateById(order);
            restoreProduct(order.getProductId());
        }
    }

    private void restoreProduct(Long productId) {
        Product product = productService.getById(productId);
        if (product != null
                && ("OFF_SHELF".equals(product.getStatus()) || "SOLD".equals(product.getStatus()))) {
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
