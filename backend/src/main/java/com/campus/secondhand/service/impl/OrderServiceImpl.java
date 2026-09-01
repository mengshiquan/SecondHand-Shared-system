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
import com.campus.secondhand.service.NotificationService;
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
import org.springframework.transaction.TransactionDefinition;

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
    @Autowired
    private NotificationService notificationService;

    /**
     * 创建订单：对同一商品加分布式锁，防止并发超卖。
     * 锁覆盖"检查→下单→下架→事务提交"全程，并通过 TransactionTemplate
     * 保证事务提交后才释放锁。
     *
     * @param dto 订单信息（商品、买家信息、地址等）
     * @return 订单详情视图
     */
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

    /**
     * 订单创建核心逻辑：校验商品状态、预留冲突，取消历史未支付订单，
     * 保存订单后通过 CAS 条件更新将商品下架（仅 ON_SALE 时才允许下架），
     * 最后通知卖家。CAS 是防止超卖的纵深防御。
     *
     * @param dto     订单信息
     * @param buyerId 当前买家用户 ID
     * @return 订单详情视图
     */
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
        // CAS 走 Wrapper 更新，绕过 updateById 的缓存清理，需主动失效详情缓存，避免详情接口读到脏缓存
        productService.evictDetailCache(dto.getProductId());

        // 通知卖家有新订单，便于及时处理
        notificationService.send(order.getSellerId(), "收到新订单",
                "买家拍下了你的商品「" + product.getTitle() + "」，请及时查看订单详情。", "ORDER");

        return baseMapper.selectOrderDetail(order.getId());
    }

    @Override
    public Order getByOrderNo(String orderNo) {
        return getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
    }

    /**
     * 校验订单是否允许支付：存在、属于当前买家、状态为待付款。
     * 若订单已过期则自动取消并恢复商品上架。
     *
     * @param orderId 订单 ID
     * @return 可支付的订单
     */
    @Override
    public Order validatePayable(Long orderId) {
        Order order = getById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getBuyerId().equals(UserContext.getUserId())) throw new BusinessException("仅买家可支付自己的订单");
        if (!"PENDING".equals(order.getStatus())) throw new BusinessException("订单状态不允许支付");

        // 检查是否过期
        if (order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
            order.setStatus("CANCELLED");
            updateById(order);
            restoreProduct(order.getProductId());
            throw new BusinessException("订单已超时，请重新下单");
        }
        return order;
    }

    /**
     * 标记订单已支付：处理支付宝/微信回调或轮询补单。
     * 对超时订单先以 REQUIRES_NEW 独立事务取消并恢复库存，再抛异常；
     * 正常订单通过 CAS 条件更新仅将 PENDING 改为 PAID，天然防重复支付。
     *
     * @param orderNo 订单编号
     * @param channel 支付渠道（ALIPAY/WECHAT）
     * @param tradeNo 第三方支付流水号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markPaid(String orderNo, String channel, String tradeNo) {
        Order order = getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) throw new BusinessException("订单不存在");

        // 待付款但已过期：按取消处理，防止超时订单被支付
        // 取消+恢复库存放进 REQUIRES_NEW 独立事务先落库，避免外层事务回滚把取消也撤销导致订单僵死
        if ("PENDING".equals(order.getStatus())
                && order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
            TransactionTemplate requiresNew = new TransactionTemplate(transactionTemplate.getTransactionManager());
            requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            requiresNew.executeWithoutResult(s -> {
                order.setStatus("CANCELLED");
                updateById(order);
                restoreProduct(order.getProductId());
            });
            throw new BusinessException("订单已超时，请重新下单");
        }

        // CAS 条件更新：仅待付款可改已支付，天然防并发重复支付
        boolean ok = update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, order.getId())
                .eq(Order::getStatus, "PENDING")
                .set(Order::getStatus, "PAID")
                .set(Order::getPaymentTime, LocalDateTime.now())
                .set(Order::getPayChannel, channel)
                .set(Order::getPayTradeNo, tradeNo));
        if (!ok) {
            // 回调与轮询同时到达导致的已付款视为幂等成功，其他状态抛异常
            Order now = getById(order.getId());
            if (now != null && "PAID".equals(now.getStatus())) return;
            throw new BusinessException("订单状态不允许支付");
        }

        // 商品标记为已售
        Product product = productService.getById(order.getProductId());
        if (product != null) {
            product.setStatus("SOLD");
            productService.updateById(product);
        }

        // 通知卖家尽快发货
        notificationService.send(order.getSellerId(), "买家已付款",
                "订单 " + order.getOrderNo() + " 买家已付款，请尽快发货。", "ORDER");
    }

    /**
     * 更新订单状态：校验权限、状态机流转合法性及退款冲突，
     * 取消时恢复商品上架，确认收货时标记商品已售出，并通知相关方。
     *
     * @param id     订单 ID
     * @param status 目标状态（PAID/SHIPPED/COMPLETED/CANCELLED）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String status) {
        Order order = getById(id);
        if (order == null) throw new BusinessException("订单不存在");

        checkOrderPermission(order, status);

        // 退款进行中（待处理/仲裁中）暂停订单正常流转，避免“一边发货一边退款”的状态冲突
        if (("SHIPPED".equals(status) || "COMPLETED".equals(status))
                && ("REQUESTED".equals(order.getRefundStatus()) || "ARBITRATION".equals(order.getRefundStatus()))) {
            throw new BusinessException("退款处理中，请等待退款处理完成后再操作");
        }

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

        // 订单状态变更通知对方
        if ("SHIPPED".equals(status)) {
            notificationService.send(order.getBuyerId(), "订单已发货",
                    "订单 " + order.getOrderNo() + " 卖家已发货，请注意查收并确认收货。", "ORDER");
        } else if ("COMPLETED".equals(status)) {
            notificationService.send(order.getSellerId(), "订单已完成",
                    "买家已确认收货，订单 " + order.getOrderNo() + " 交易完成。", "ORDER");
        } else if ("CANCELLED".equals(status)) {
            notificationService.send(order.getSellerId(), "订单已取消",
                    "买家取消了订单 " + order.getOrderNo() + "，商品已恢复上架。", "ORDER");
        }
    }

    @Override
    public IPage<OrderVO> pageList(Integer pageNum, Integer pageSize, String status) {
        Page<OrderVO> page = new Page<>(pageNum, pageSize);
        return baseMapper.selectOrderPage(page, UserContext.getUserId(), UserContext.getRole(), status, null);
    }

    @Override
    public Map<String, Long> statusCounts() {
        Long uid = UserContext.getUserId();
        Map<String, Long> result = new LinkedHashMap<>();
        // 与订单列表口径一致：当前用户作为买家或卖家的订单（退款进行中的归入退款/售后，不计入状态页签）
        for (String s : Arrays.asList("PENDING", "PAID", "SHIPPED", "COMPLETED")) {
            long c = count(new LambdaQueryWrapper<Order>()
                    .eq(Order::getStatus, s)
                    .and(w -> w.eq(Order::getBuyerId, uid).or().eq(Order::getSellerId, uid))
                    .and(w -> w.isNull(Order::getRefundStatus)
                            .or().notIn(Order::getRefundStatus, Arrays.asList("REQUESTED", "ARBITRATION"))));
            result.put(s, c);
        }
        // 个人中心首页展示的订单总数
        result.put("TOTAL", count(new LambdaQueryWrapper<Order>()
                .and(w -> w.eq(Order::getBuyerId, uid).or().eq(Order::getSellerId, uid))));
        // 行动视角待办：首页提醒条与“我的订单”角标按“谁该行动”拆分
        result.put("BUYER_PENDING", count(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, "PENDING").eq(Order::getBuyerId, uid)));
        result.put("BUYER_SHIPPED", count(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, "SHIPPED").eq(Order::getBuyerId, uid)
                .and(w -> w.isNull(Order::getRefundStatus)
                        .or().notIn(Order::getRefundStatus, Arrays.asList("REQUESTED", "ARBITRATION")))));
        result.put("SELLER_PAID", count(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, "PAID").eq(Order::getSellerId, uid)
                .and(w -> w.isNull(Order::getRefundStatus)
                        .or().notIn(Order::getRefundStatus, Arrays.asList("REQUESTED", "ARBITRATION")))));
        result.put("SELLER_REFUND", count(new LambdaQueryWrapper<Order>()
                .eq(Order::getRefundStatus, "REQUESTED").eq(Order::getSellerId, uid)));
        // 退款/售后进行中的订单数（已完结的退款不再计数，角标处理后自动消失）
        result.put("REFUND", count(new LambdaQueryWrapper<Order>()
                .in(Order::getRefundStatus, Arrays.asList("REQUESTED", "ARBITRATION"))
                .and(w -> w.eq(Order::getBuyerId, uid).or().eq(Order::getSellerId, uid))));
        return result;
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

        notificationService.send(order.getSellerId(), "订单已取消",
                "买家取消了订单 " + order.getOrderNo() + "，商品已恢复上架。", "ORDER");
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

        // 通知卖家及时处理退款申请
        notificationService.send(order.getSellerId(), "收到退款申请",
                "买家对订单 " + order.getOrderNo() + " 申请退款，原因：" + reason + "，请及时处理。", "ORDER");
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
            notificationService.send(order.getBuyerId(), "卖家同意退款",
                    "订单 " + order.getOrderNo() + " 卖家已同意退款，订单已取消。", "ORDER");
        } else {
            order.setRefundStatus("SELLER_REJECTED");
            updateById(order);
            notificationService.send(order.getBuyerId(), "卖家拒绝退款",
                    "订单 " + order.getOrderNo() + " 卖家拒绝了退款申请，如有异议可在72小时内申请平台仲裁。", "ORDER");
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

        notificationService.send(order.getSellerId(), "买家申请仲裁",
                "订单 " + order.getOrderNo() + " 的退款争议已提交平台仲裁，请留意仲裁结果。", "ORDER");
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
            notificationService.send(order.getBuyerId(), "仲裁结果：退款",
                    "平台仲裁判定订单 " + order.getOrderNo() + " 退款，订单已取消。", "ORDER");
            notificationService.send(order.getSellerId(), "仲裁结果：退款",
                    "平台仲裁判定订单 " + order.getOrderNo() + " 退款，订单已取消。", "ORDER");
        } else {
            order.setRefundStatus("ARBITRATION_MAINTAIN");
            updateById(order);
            notificationService.send(order.getBuyerId(), "仲裁结果：维持原订单",
                    "平台仲裁判定订单 " + order.getOrderNo() + " 维持原交易，请继续完成交易。", "ORDER");
            notificationService.send(order.getSellerId(), "仲裁结果：维持原订单",
                    "平台仲裁判定订单 " + order.getOrderNo() + " 维持原交易，请继续完成交易。", "ORDER");
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

    /**
     * 恢复商品上架：取消或退款后将商品重新置为 ON_SALE，
     * 仅当商品当前为 OFF_SHELF 或 SOLD 时才执行，避免误操作。
     *
     * @param productId 商品 ID
     */
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
