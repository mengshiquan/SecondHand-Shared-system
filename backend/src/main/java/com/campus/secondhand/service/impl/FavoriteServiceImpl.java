package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.entity.Favorite;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.mapper.FavoriteMapper;
import com.campus.secondhand.service.FavoriteService;
import com.campus.secondhand.service.ProductService;
import com.campus.secondhand.util.UserContext;
import com.campus.secondhand.vo.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    @Autowired
    private ProductService productService;

    @Override
    public void toggle(Long productId) {
        Long userId = UserContext.getUserId();
        Product product = productService.getById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        Favorite favorite = getOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId));
        if (favorite != null) {
            removeById(favorite.getId());
        } else {
            Favorite newFavorite = new Favorite();
            newFavorite.setUserId(userId);
            newFavorite.setProductId(productId);
            save(newFavorite);
        }
    }

    @Override
    public boolean isFavorited(Long productId) {
        return count(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, UserContext.getUserId())
                .eq(Favorite::getProductId, productId)) > 0;
    }

    @Override
    public IPage<ProductVO> pageList(Integer pageNum, Integer pageSize) {
        Page<ProductVO> page = new Page<>(pageNum, pageSize);
        return baseMapper.selectFavoritePage(page, UserContext.getUserId());
    }
}
