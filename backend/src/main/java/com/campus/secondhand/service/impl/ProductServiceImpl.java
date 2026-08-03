package com.campus.secondhand.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.dto.ProductDTO;
import com.campus.secondhand.dto.ProductQueryDTO;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.mapper.ProductMapper;
import com.campus.secondhand.service.ProductService;
import com.campus.secondhand.util.UserContext;
import com.campus.secondhand.vo.ProductVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * 商品服务实现
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Override
    public void publish(ProductDTO dto) {
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
        ProductVO vo = baseMapper.selectProductDetail(id, currentUserId);
        if (vo == null) {
            throw new BusinessException("商品不存在");
        }
        Product product = getById(id);
        product.setViewCount(product.getViewCount() + 1);
        updateById(product);
        vo.setViewCount(product.getViewCount());
        return vo;
    }

    private void checkOwner(Product product) {
        if (!product.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException("无权操作此商品");
        }
    }
}
