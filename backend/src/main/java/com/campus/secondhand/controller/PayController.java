package com.campus.secondhand.controller;

import cn.hutool.json.JSONUtil;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.common.Result;
import com.campus.secondhand.config.AliPayConfig;
import com.campus.secondhand.entity.Order;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.service.OrderService;
import com.campus.secondhand.service.ProductService;
import com.campus.secondhand.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付控制器：支付宝沙箱扫码付 + 微信模拟收银台
 * 流程：四重校验 → 预下单生成二维码 → 前端轮询 → 回调/主动查询落单改已支付
 */
@Slf4j
@RestController
@RequestMapping("/pay")
public class PayController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private ProductService productService;
    @Autowired
    private AliPayConfig aliPayConfig;

    /**
     * 支付宝预下单：校验订单可支付后，调用支付宝沙箱生成扫码支付二维码。
     *
     * @param orderId 待支付的订单 ID
     * @return orderNo: 订单编号；qrCode: 支付宝支付二维码链接
     */
    @PostMapping("/alipay/create/{orderId}")
    public Result<Map<String, String>> alipayCreate(@PathVariable Long orderId) {
        AlipayClient client = aliPayConfig.getClient();
        if (client == null) {
            throw new BusinessException("支付宝沙箱未配置，请先在 application.yml 填入沙箱凭据");
        }
        Order order = orderService.validatePayable(orderId);
        Product product = productService.getById(order.getProductId());

        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
        request.setNotifyUrl(aliPayConfig.getNotifyUrl());
        Map<String, Object> biz = new HashMap<>();
        biz.put("out_trade_no", order.getOrderNo());
        biz.put("total_amount", order.getPrice().setScale(2, RoundingMode.HALF_UP).toPlainString());
        biz.put("subject", product != null ? product.getTitle() : "校园二手商品");
        request.setBizContent(JSONUtil.toJsonStr(biz));
        try {
            AlipayTradePrecreateResponse response = client.execute(request);
            if (!response.isSuccess()) {
                throw new BusinessException("支付宝预下单失败：" + response.getSubMsg());
            }
            Map<String, String> data = new HashMap<>();
            data.put("orderNo", order.getOrderNo());
            data.put("qrCode", response.getQrCode());
            return Result.success(data);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("支付宝沙箱预下单异常", e);
            throw new BusinessException("支付宝沙箱调用失败，请稍后重试");
        }
    }

    /**
     * 支付宝异步回调：免登录接口，验签成功后根据交易状态落单。
     * 必须为支付宝公钥验签通过且交易成功/完成才调用 markPaid。
     *
     * @param request 支付宝回调请求参数
     * @return "success" 表示处理成功，"failure" 表示验签或处理失败
     */
    @PostMapping("/alipay/notify")
    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> params.put(k, String.join(",", v)));
        try {
            boolean signOk = AlipaySignature.rsaCheckV1(params, aliPayConfig.getAlipayPublicKey(), "UTF-8", "RSA2");
            if (!signOk) {
                return "failure";
            }
            String tradeStatus = params.get("trade_status");
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                orderService.markPaid(params.get("out_trade_no"), "ALIPAY", params.get("trade_no"));
            }
            return "success";
        } catch (Exception e) {
            return "failure";
        }
    }

    /**
     * 微信模拟预下单：仅用于演示，生成一个模拟的微信支付二维码链接。
     *
     * @param orderId 待支付的订单 ID
     * @return orderNo: 订单编号；qrCode: 模拟微信支付二维码链接
     */
    @PostMapping("/wechat/create/{orderId}")
    public Result<Map<String, String>> wechatCreate(@PathVariable Long orderId) {
        Order order = orderService.validatePayable(orderId);
        Map<String, String> data = new HashMap<>();
        data.put("orderNo", order.getOrderNo());
        data.put("qrCode", "weixin://wxpay/bizpayurl?mock=" + order.getOrderNo());
        return Result.success(data);
    }

    /**
     * 微信模拟支付回调：模拟用户扫码完成付款，需当前登录用户为订单买家。
     *
     * @param orderNo 订单编号
     */
    @PostMapping("/wechat/notify/{orderNo}")
    public Result<Void> wechatNotify(@PathVariable String orderNo) {
        Order order = orderService.getByOrderNo(orderNo);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getBuyerId().equals(UserContext.getUserId())) throw new BusinessException("仅买家可完成支付");
        orderService.markPaid(orderNo, "WECHAT", "WX" + System.currentTimeMillis());
        return Result.success();
    }

    /**
     * 支付状态轮询：买家查询订单支付状态。
     * 若订单仍为待付款且支付宝已配置，主动查询支付宝交易状态并补单，
     * 作为本地无公网无法收到异步回调时的兜底机制。
     *
     * @param orderId 订单 ID
     * @return status: 订单状态；payChannel: 支付渠道
     */
    @GetMapping("/status/{orderId}")
    public Result<Map<String, Object>> status(@PathVariable Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getBuyerId().equals(UserContext.getUserId())) throw new BusinessException("仅买家可查看支付状态");

        if ("PENDING".equals(order.getStatus()) && aliPayConfig.isConfigured()) {
            try {
                AlipayTradeQueryRequest query = new AlipayTradeQueryRequest();
                query.setBizContent(JSONUtil.toJsonStr(Map.of("out_trade_no", order.getOrderNo())));
                AlipayTradeQueryResponse resp = aliPayConfig.getClient().execute(query);
                if (resp.isSuccess() && "TRADE_SUCCESS".equals(resp.getTradeStatus())) {
                    orderService.markPaid(order.getOrderNo(), "ALIPAY", resp.getTradeNo());
                    order = orderService.getById(orderId);
                }
            } catch (Exception ignored) {
                // 查询失败不中断轮询，下一轮重试
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("status", order.getStatus());
        data.put("payChannel", order.getPayChannel());
        return Result.success(data);
    }
}
