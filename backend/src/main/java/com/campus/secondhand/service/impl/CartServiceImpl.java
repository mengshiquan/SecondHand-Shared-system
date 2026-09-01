package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.common.VerifyGuard;
import com.campus.secondhand.dto.CartCheckoutDTO;
import com.campus.secondhand.dto.OrderDTO;
import com.campus.secondhand.entity.Address;
import com.campus.secondhand.entity.Cart;
import com.campus.secondhand.entity.Category;
import com.campus.secondhand.entity.Product;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.mapper.CartMapper;
import com.campus.secondhand.service.AddressService;
import com.campus.secondhand.service.CartService;
import com.campus.secondhand.service.CategoryService;
import com.campus.secondhand.service.FavoriteService;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private FavoriteService favoriteService;

    @Override
    public List<CartItemVO> listMine() {
        Long userId = UserContext.getUserId();
        List<Cart> items = list(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .orderByDesc(Cart::getCreateTime));
        if (items.isEmpty()) return new ArrayList<>();

        // 批量装配商品/卖家/分类信息，避免逐条查库
        List<Long> productIds = items.stream().map(Cart::getProductId).distinct().collect(Collectors.toList());
        Map<Long, Product> productMap = productService.listByIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Set<Long> sellerIds = productMap.values().stream().map(Product::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> nicknameMap = sellerIds.isEmpty() ? Map.of()
                : userService.listByIds(sellerIds).stream().collect(Collectors.toMap(User::getId,
                        u -> u.getNickname() != null ? u.getNickname() : String.valueOf(u.getId())));
        Set<Long> categoryIds = productMap.values().stream().map(Product::getCategoryId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> categoryNameMap = categoryIds.isEmpty() ? Map.of()
                : categoryService.listByIds(categoryIds).stream()
                        .collect(Collectors.toMap(Category::getId, Category::getName));

        List<CartItemVO> result = new ArrayList<>();
        for (Cart item : items) {
            Product p = productMap.get(item.getProductId());
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
                vo.setSellerId(p.getUserId());
                vo.setSellerNickname(nicknameMap.get(p.getUserId()));
                vo.setCategoryId(p.getCategoryId());
                vo.setCategoryName(categoryNameMap.get(p.getCategoryId()));
                vo.setInvalid(!"ON_SALE".equals(p.getStatus()));
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveToFavorite(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        Long userId = UserContext.getUserId();
        List<Cart> rows = list(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .in(Cart::getId, ids));
        for (Cart row : rows) {
            favoriteService.ensureFavorite(row.getProductId());
            baseMapper.physicalDelete(row.getUserId(), row.getProductId());
        }
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
        // 清除旧逻辑删除残留行，避免违反唯一索引 (user_id, product_id)
        baseMapper.physicalDelete(userId, productId);
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setProductId(productId);
        save(cart);
    }

    @Override
    public void remove(Long id) {
        Cart cart = getOwned(id);
        // 物理删除：避免唯一索引 (user_id, product_id) 下的逻辑删除残留行导致再次加购报错
        baseMapper.physicalDelete(cart.getUserId(), cart.getProductId());
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
        list(new LambdaQueryWrapper<Cart>().select(Cart::getProductId).eq(Cart::getUserId, userId))
                .forEach(item -> baseMapper.physicalDelete(userId, item.getProductId()));
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
            baseMapper.physicalDelete(userId, item.getProductId());
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
