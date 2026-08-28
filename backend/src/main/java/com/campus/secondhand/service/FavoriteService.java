package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.secondhand.entity.Favorite;
import com.campus.secondhand.vo.ProductVO;

public interface FavoriteService extends IService<Favorite> {

    void toggle(Long productId);

    boolean isFavorited(Long productId);

    void removeBatch(java.util.List<Long> productIds);

    IPage<ProductVO> pageList(Integer pageNum, Integer pageSize, String keyword);
}
