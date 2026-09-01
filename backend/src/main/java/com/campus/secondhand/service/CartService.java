package com.campus.secondhand.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.secondhand.dto.CartCheckoutDTO;
import com.campus.secondhand.entity.Cart;
import com.campus.secondhand.vo.CartItemVO;
import com.campus.secondhand.vo.OrderVO;

import java.util.List;

public interface CartService extends IService<Cart> {

    List<CartItemVO> listMine();

    void addToCart(Long productId);

    void remove(Long id);

    void removeBatch(List<Long> ids);

    /** 移入收藏：收藏所选购物车商品并移出购物车 */
    void moveToFavorite(List<Long> ids);

    void clear();

    List<OrderVO> checkout(CartCheckoutDTO dto);
}
