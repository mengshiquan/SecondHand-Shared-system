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

    void publish(ProductDTO dto);

    void updateProduct(ProductDTO dto);

    void deleteProduct(Long id);

    void offShelf(Long id);

    IPage<ProductVO> pageList(ProductQueryDTO query);

    ProductVO detail(Long id, Long currentUserId);
}
