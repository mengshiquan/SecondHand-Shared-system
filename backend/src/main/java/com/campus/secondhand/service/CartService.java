package com.campus.secondhand.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.secondhand.dto.CartCheckoutDTO;
import com.campus.secondhand.entity.Cart;
import com.campus.secondhand.vo.CartItemVO;
import com.campus.secondhand.vo.OrderVO;

import java.util.List;

public interface CartService extends IService<Cart> {

    /** 查询当前用户购物车，并带出商品标题、价格、图片、卖家等展示信息。 */
    List<CartItemVO> listMine();

    /** 加入购物车；同一商品只保留一条，商品必须在售且非本人发布。 */
    void addToCart(Long productId);

    /** 按 ID 删除本人购物车项。 */
    void remove(Long id);

    /** 批量删除本人购物车项。 */
    void removeBatch(List<Long> ids);

    /** 移入收藏：收藏所选购物车商品并移出购物车 */
    void moveToFavorite(List<Long> ids);

    /** 清空当前用户购物车。 */
    void clear();

    /** 根据选中的购物车项和地址批量创建订单；已售、下架或本人商品会失败。 */
    List<OrderVO> checkout(CartCheckoutDTO dto);
}
