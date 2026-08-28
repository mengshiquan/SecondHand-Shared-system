package com.campus.secondhand.common;

import java.time.Duration;

/**
 * Redis Key 统一常量：前缀 campus:sh:，按 lock/cache/stat 三段语义分类
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {}

    public static final String PREFIX = "campus:sh:";

    // ===== 分布式锁 =====
    /** 订单创建锁，参数: productId */
    public static final String LOCK_ORDER_CREATE    = PREFIX + "lock:order:create:%d";
    /** 收藏操作锁，参数: userId, productId */
    public static final String LOCK_FAVORITE        = PREFIX + "lock:favorite:%d:%d";
    /** 黑名单自动扫描锁（全局唯一） */
    public static final String LOCK_BLACKLIST_SCAN  = PREFIX + "lock:blacklist:scan";

    // ===== 业务缓存 =====
    /** 商品详情缓存，参数: productId */
    public static final String CACHE_PRODUCT_DETAIL  = PREFIX + "cache:product:detail:%d";
    /** 分类树缓存（全局唯一） */
    public static final String CACHE_CATEGORY_TREE   = PREFIX + "cache:category:tree";
    /** 仪表盘数据缓存（全局唯一） */
    public static final String CACHE_ADMIN_DASHBOARD = PREFIX + "cache:admin:dashboard";

    // ===== 计数器 =====
    /** 商品浏览量计数，参数: productId */
    public static final String STAT_PRODUCT_VIEW         = PREFIX + "stat:product:view:%d";
    public static final String STAT_PRODUCT_VIEW_PATTERN = PREFIX + "stat:product:view:*";
    /** 浏览去重标记，参数: productId, userId；SETNX 成功时才递增浏览量 */
    public static final String STAT_PRODUCT_VIEWED       = PREFIX + "stat:product:viewed:%d:%d";

    // ===== TTL 策略 =====
    /** 商品详情：30分钟（可被编辑/交易，短 TTL + 主动失效双保险） */
    public static final Duration TTL_PRODUCT_DETAIL = Duration.ofMinutes(30);
    /** 分类：1小时（极少变化） */
    public static final Duration TTL_CATEGORY       = Duration.ofHours(1);
    /** 仪表盘：60秒（统计容忍分钟级延迟） */
    public static final Duration TTL_DASHBOARD      = Duration.ofSeconds(60);
    /** 浏览去重窗口：24小时（同一用户同一商品 24h 内只计一次） */
    public static final Duration TTL_VIEW_DEDUP     = Duration.ofHours(24);
}
