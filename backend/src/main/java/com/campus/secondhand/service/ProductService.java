package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.secondhand.dto.ProductDTO;
import com.campus.secondhand.dto.ProductQueryDTO;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.vo.ProductVO;

public interface ProductService extends IService<Product> {

    /** 主动失效商品详情缓存（供绕过 updateById 的直更路径调用） */
    void evictDetailCache(Long productId);

    /** 买家咨询卖家：以通知形式送达卖家 */
    void contactSeller(Long productId, String message);

    /** 发布商品；要求当前用户已通过校园认证。 */
    void publish(ProductDTO dto);

    /** 更新本人商品；自动失效商品详情缓存。 */
    void updateProduct(ProductDTO dto);

    /** 逻辑删除本人商品，并清理相关收藏和购物车数据。 */
    void deleteProduct(Long id);

    /** 本人主动下架商品。 */
    void offShelf(Long id);

    /** 分页查询在售商品，支持分类、关键字、价格和排序条件。 */
    IPage<ProductVO> pageList(ProductQueryDTO query);

    /** 查询商品详情；异步累计浏览量，并返回当前用户收藏状态。 */
    ProductVO detail(Long id, Long currentUserId);
}
