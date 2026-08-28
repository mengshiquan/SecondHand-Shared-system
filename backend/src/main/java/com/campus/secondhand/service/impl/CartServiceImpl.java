package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.common.VerifyGuard;
import com.campus.secondhand.dto.CartCheckoutDTO;
import com.campus.secondhand.dto.OrderDTO;
import com.campus.secondhand.entity.Address;
import com.campus.secondhand.entity.Cart;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.mapper.CartMapper;
import com.campus.secondhand.service.AddressService;
import com.campus.secondhand.service.CartService;
import com.campus.secondhand.service.OrderService;
import com.campus.secondhand.service.ProductService;
import com.campus.secondhand.service.UserService;
import com.campus.secondhand.util.UserContext;
import com.campus.secondhand.vo.CartItemVO;
import com.campus.secondhand.vo.OrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 购物车服务实现
 */
@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

    @Autowired
    private ProductService productService;
    @Autowired
    private UserService userService;
    @Autowired
    private AddressService addressService;
    @Autowired
    private OrderService orderService;

    @Override
    public List<CartItemVO> listMine() {
        Long userId = UserContext.getUserId();
        List<Cart> items = list(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .orderByDesc(Cart::getCreateTime));
        List<CartItemVO> result = new ArrayList<>();
        for (Cart item : items) {
            Product p = productService.getById(item.getProductId());
            CartItemVO vo = new CartItemVO();
            vo.setId(item.getId());
            vo.setProductId(item.getProductId());
            vo.setCreateTime(item.getCreateTime());
            if (p == null) {
                vo.setTitle("商品已删除");
                vo.setInvalid(true);
            } else {
                vo.setTitle(p.getTitle());
                vo.setPrice(p.getPrice());
                vo.setImages(parseImages(p.getImages()));
                vo.setInvalid(!"ON_SALE".equals(p.getStatus()));
            }
            result.add(vo);
        }
        return result;
    }

    /** 解析商品图片 JSON 数组，失败时返回空列表 */
    private List<String> parseImages(String imagesJson) {
        if (imagesJson == null || imagesJson.isBlank()) return new ArrayList<>();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(imagesJson, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void addToCart(Long productId) {
        VerifyGuard.requireVerified(userService);
        Long userId = UserContext.getUserId();
        Product product = productService.getById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!"ON_SALE".equals(product.getStatus())) {
            throw new BusinessException("商品不在售，无法加入购物车");
        }
        if (product.getUserId().equals(userId)) {
            throw new BusinessException("不能将自己发布的商品加入购物车");
        }
        long exists = count(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getProductId, productId));
        if (exists > 0) {
            throw new BusinessException("该商品已在购物车中");
        }
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setProductId(productId);
        save(cart);
    }

    @Override
    public void remove(Long id) {
        Cart cart = getOwned(id);
        removeById(cart.getId());
    }

    @Override
    public void removeBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            remove(id);
        }
    }

    @Override
    public void clear() {
        Long userId = UserContext.getUserId();
        remove(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OrderVO> checkout(CartCheckoutDTO dto) {
        VerifyGuard.requireVerified(userService);
        Long userId = UserContext.getUserId();

        Address address = addressService.getById(dto.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("收货地址不存在");
        }

        List<OrderVO> orders = new ArrayList<>();
        for (Long cartItemId : dto.getCartItemIds()) {
            Cart item = getOwned(cartItemId);
            OrderDTO orderDTO = new OrderDTO();
            orderDTO.setProductId(item.getProductId());
            orderDTO.setAddressId(dto.getAddressId());
            OrderVO vo = orderService.createOrder(orderDTO);
            removeById(item.getId());
            orders.add(vo);
        }
        return orders;
    }

    private Cart getOwned(Long id) {
        Cart cart = getById(id);
        if (cart == null || !cart.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException("购物车项不存在");
        }
        return cart;
    }
}
