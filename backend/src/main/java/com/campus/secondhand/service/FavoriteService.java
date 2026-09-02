package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.secondhand.entity.Favorite;
import com.campus.secondhand.vo.ProductVO;

public interface FavoriteService extends IService<Favorite> {

    /** 当前用户切换收藏状态：未收藏则收藏，已收藏则取消。 */
    void toggle(Long productId);

    /** 确保已收藏（幂等，不会取消收藏） */
    void ensureFavorite(Long productId);

    /** 判断当前用户是否已收藏指定商品。 */
    boolean isFavorited(Long productId);

    /** 批量删除本人对指定商品的收藏，用于订单成交或商品清理。 */
    void removeBatch(java.util.List<Long> productIds);

    /** 分页查询当前用户收藏商品，支持标题模糊搜索。 */
    IPage<ProductVO> pageList(Integer pageNum, Integer pageSize, String keyword);
}
