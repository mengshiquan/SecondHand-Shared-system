package com.campus.secondhand.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.common.RedisKeyConstants;
import com.campus.secondhand.common.VerifyGuard;
import com.campus.secondhand.dto.ProductDTO;
import com.campus.secondhand.dto.ProductQueryDTO;
import com.campus.secondhand.entity.Favorite;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.mapper.FavoriteMapper;
import com.campus.secondhand.mapper.ProductMapper;
import com.campus.secondhand.service.ProductService;
import com.campus.secondhand.service.UserService;
import com.campus.secondhand.util.RedisCacheUtil;
import com.campus.secondhand.util.UserContext;
import com.campus.secondhand.vo.ProductVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collections;

/**
 * 商品服务实现
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Autowired
    private RedisCacheUtil cacheUtil;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private FavoriteMapper favoriteMapper;
    @Autowired
    private UserService userService;

    /** Lua 脚本：key 不存在时以 DB 值为基准初始化并自增，避免并发初始化覆盖计数 */
    private static final DefaultRedisScript<Long> INIT_INCR_SCRIPT = new DefaultRedisScript<>(
            "local v = redis.call('GET', KEYS[1]); " +
            "if not v then v = ARGV[1]; redis.call('SET', KEYS[1], v); end; " +
            "return redis.call('INCR', KEYS[1])", Long.class);

    @Override
    public void publish(ProductDTO dto) {
        VerifyGuard.requireVerified(userService);
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        product.setImages(JSONUtil.toJsonStr(dto.getImages()));
        product.setUserId(UserContext.getUserId());
        product.setStatus("ON_SALE");
        product.setViewCount(0);
        save(product);
    }

    @Override
    public void updateProduct(ProductDTO dto) {
        Product product = getById(dto.getId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        checkOwner(product);
        product.setTitle(dto.getTitle());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setOriginalPrice(dto.getOriginalPrice());
        product.setCategoryId(dto.getCategoryId());
        if (dto.getImages() != null) {
            product.setImages(JSONUtil.toJsonStr(dto.getImages()));
        }
        updateById(product);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = getById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!UserContext.isAdmin()) {
            checkOwner(product);
        }
        removeById(id);
    }

    @Override
    public void offShelf(Long id) {
        Product product = getById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        checkOwner(product);
        product.setStatus("OFF_SHELF");
        updateById(product);
    }

    @Override
    public IPage<ProductVO> pageList(ProductQueryDTO query) {
        Page<ProductVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        // 公开列表默认只展示在售商品
        if ((query.getStatus() == null || query.getStatus().isEmpty()) && query.getSellerId() == null) {
            query.setStatus("ON_SALE");
        }
        return baseMapper.selectProductPage(page, query);
    }

    @Override
    public ProductVO detail(Long id, Long currentUserId) {
        String cacheKey = String.format(RedisKeyConstants.CACHE_PRODUCT_DETAIL, id);
        ProductVO vo = cacheUtil.get(cacheKey);

        if (vo == null) {
            // 缓存未命中：查库并缓存（剥离 favorited 用户维度字段）
            vo = baseMapper.selectProductDetail(id, currentUserId);
            if (vo == null) {
                throw new BusinessException("商品不存在");
            }
            Boolean favorited = vo.getFavorited();
            vo.setFavorited(null);  // 不进缓存，防止收藏状态跨用户泄漏
            cacheUtil.set(cacheKey, vo, RedisKeyConstants.TTL_PRODUCT_DETAIL);
            vo.setFavorited(favorited);
        } else {
            // 缓存命中：单独重算用户维度字段
            vo.setFavorited(currentUserId != null && favoriteMapper.selectCount(
                    new LambdaQueryWrapper<Favorite>()
                            .eq(Favorite::getUserId, currentUserId)
                            .eq(Favorite::getProductId, id)) > 0);
        }

        // 浏览量：SETNX 去重 + INCR 原子递增（同一用户同一商品 24h 只计一次）
        try {
            Product p = getById(id);
            String viewKey = String.format(RedisKeyConstants.STAT_PRODUCT_VIEW, id);

            boolean shouldIncr = true;
            if (currentUserId != null) {
                String dedupKey = String.format(RedisKeyConstants.STAT_PRODUCT_VIEWED, id, currentUserId);
                Boolean isNew = stringRedisTemplate.opsForValue()
                        .setIfAbsent(dedupKey, "1", RedisKeyConstants.TTL_VIEW_DEDUP);
                shouldIncr = Boolean.TRUE.equals(isNew);
            }

            if (shouldIncr) {
                Long view = stringRedisTemplate.execute(INIT_INCR_SCRIPT,
                        Collections.singletonList(viewKey),
                        String.valueOf(p.getViewCount() == null ? 0 : p.getViewCount()));
                if (view != null) vo.setViewCount(view.intValue());
            }
        } catch (Exception e) {
            log.warn("浏览量计数失败，保留原值。productId={}", id, e);
        }
        return vo;
    }

    private void checkOwner(Product product) {
        if (!product.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException("无权操作此商品");
        }
    }

    // ===== 缓存统一失效：覆写 IService 通用方法，所有状态变更自动清除缓存 =====

    @Override
    public boolean updateById(Product entity) {
        boolean ok = super.updateById(entity);
        if (ok) cacheUtil.delete(String.format(RedisKeyConstants.CACHE_PRODUCT_DETAIL, entity.getId()));
        return ok;
    }

    @Override
    public boolean removeById(Serializable id) {
        boolean ok = super.removeById(id);
        if (ok) cacheUtil.delete(String.format(RedisKeyConstants.CACHE_PRODUCT_DETAIL, id));
        return ok;
    }

    /** 每 5 分钟将 Redis 浏览量回写 MySQL（SCAN 不用 KEYS，避免阻塞） */
    @Scheduled(fixedRate = 300000)
    public void syncViewCountToDb() {
        try (Cursor<String> cursor = stringRedisTemplate.scan(ScanOptions.scanOptions()
                .match(RedisKeyConstants.STAT_PRODUCT_VIEW_PATTERN).count(500).build())) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                String v = stringRedisTemplate.opsForValue().get(key);
                if (v == null) continue;
                long productId = Long.parseLong(key.substring(key.lastIndexOf(':') + 1));
                update(new LambdaUpdateWrapper<Product>()
                        .eq(Product::getId, productId)
                        .setSql("view_count = " + Long.parseLong(v)));
            }
        } catch (Exception e) {
            log.warn("浏览量回写失败，下轮重试", e);
        }
    }
}
