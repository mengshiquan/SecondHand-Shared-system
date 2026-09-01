package com.campus.secondhand.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝沙箱支付配置
 * 凭据留空视为未配置：支付宝选项不可用，微信模拟支付不受影响
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "pay.alipay")
public class AliPayConfig {

    /** 沙箱应用 APPID */
    private String appId = "";

    /** 应用私钥（RSA2） */
    private String privateKey = "";

    /** 支付宝公钥（验签用） */
    private String alipayPublicKey = "";

    /** 沙箱网关 */
    private String gateway = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";

    /** 异步回调地址（需公网可达，本地调试由 /pay/status 主动查询兜底） */
    private String notifyUrl = "http://localhost:8080/api/pay/alipay/notify";

    private volatile AlipayClient client;

    public boolean isConfigured() {
        return notBlank(appId) && notBlank(privateKey) && notBlank(alipayPublicKey);
    }

    /** 懒加载 AlipayClient，未配置时返回 null */
    public AlipayClient getClient() {
        if (!isConfigured()) {
            return null;
        }
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = new DefaultAlipayClient(gateway, appId, privateKey,
                            "json", "UTF-8", alipayPublicKey, "RSA2");
                }
            }
        }
        return client;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
