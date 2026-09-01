-- 支付渠道与第三方交易号（支付宝沙箱 / 微信模拟支付集成）
ALTER TABLE t_order ADD COLUMN pay_channel VARCHAR(20) DEFAULT NULL COMMENT '支付渠道：ALIPAY/WECHAT' AFTER payment_time;
ALTER TABLE t_order ADD COLUMN pay_trade_no VARCHAR(64) DEFAULT NULL COMMENT '第三方支付交易号' AFTER pay_channel;
