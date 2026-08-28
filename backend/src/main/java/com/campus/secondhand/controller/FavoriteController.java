package com.campus.secondhand.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.common.Result;
import com.campus.secondhand.service.FavoriteService;
import com.campus.secondhand.vo.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 收藏模块接口
 */
@RestController
@RequestMapping("/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    /** 收藏/取消收藏 */
    @PostMapping("/{productId}")
    public Result<Void> toggle(@PathVariable Long productId) {
        favoriteService.toggle(productId);
        return Result.success();
    }

    /** 是否已收藏 */
    @GetMapping("/{productId}")
    public Result<Boolean> isFavorited(@PathVariable Long productId) {
        return Result.success(favoriteService.isFavorited(productId));
    }

    /** 我的收藏列表 */
    @GetMapping("/list")
    public Result<IPage<ProductVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(favoriteService.pageList(pageNum, pageSize, keyword));
    }

    /** 批量取消收藏 */
    @DeleteMapping("/batch")
    public Result<Void> removeBatch(@RequestBody java.util.Map<String, java.util.List<Long>> params) {
        favoriteService.removeBatch(params.get("productIds"));
        return Result.success();
    }
}
