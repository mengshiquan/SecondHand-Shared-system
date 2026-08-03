package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.secondhand.dto.ProductDTO;
import com.campus.secondhand.dto.ProductQueryDTO;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.vo.ProductVO;

public interface ProductService extends IService<Product> {

    void publish(ProductDTO dto);

    void updateProduct(ProductDTO dto);

    void deleteProduct(Long id);

    void offShelf(Long id);

    IPage<ProductVO> pageList(ProductQueryDTO query);

    ProductVO detail(Long id, Long currentUserId);
}
