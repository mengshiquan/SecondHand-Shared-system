package com.campus.secondhand.controller;

import com.campus.secondhand.common.Result;
import com.campus.secondhand.dto.CartCheckoutDTO;
import com.campus.secondhand.service.CartService;
import com.campus.secondhand.vo.CartItemVO;
import com.campus.secondhand.vo.OrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 购物车接口
 */
@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    /** 购物车列表 */
    @GetMapping("/list")
    public Result<List<CartItemVO>> list() {
        return Result.success(cartService.listMine());
    }

    /** 加入购物车 */
    @PostMapping("/{productId}")
    public Result<Void> add(@PathVariable Long productId) {
        cartService.addToCart(productId);
        return Result.success();
    }

    /** 移除 */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        cartService.remove(id);
        return Result.success();
    }

    /** 批量移除 */
    @DeleteMapping("/batch")
    public Result<Void> removeBatch(@RequestBody Map<String, List<Long>> params) {
        cartService.removeBatch(params.get("ids"));
        return Result.success();
    }

    /** 移入收藏：收藏所选商品并移出购物车 */
    @PostMapping("/move-to-favorite")
    public Result<Void> moveToFavorite(@RequestBody Map<String, List<Long>> params) {
        cartService.moveToFavorite(params.get("ids"));
        return Result.success();
    }

    /** 清空 */
    @DeleteMapping("/clear")
    public Result<Void> clear() {
        cartService.clear();
        return Result.success();
    }

    /** 结算 */
    @PostMapping("/checkout")
    public Result<List<OrderVO>> checkout(@Validated @RequestBody CartCheckoutDTO dto) {
        return Result.success(cartService.checkout(dto));
    }
}
