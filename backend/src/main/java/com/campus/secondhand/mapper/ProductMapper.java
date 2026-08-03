package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.dto.ProductQueryDTO;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.vo.ProductVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    IPage<ProductVO> selectProductPage(Page<ProductVO> page, @Param("query") ProductQueryDTO query);

    ProductVO selectProductDetail(@Param("id") Long id, @Param("userId") Long userId);
}
