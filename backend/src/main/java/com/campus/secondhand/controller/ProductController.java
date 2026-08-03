package com.campus.secondhand.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.common.Result;
import com.campus.secondhand.dto.ProductDTO;
import com.campus.secondhand.dto.ProductQueryDTO;
import com.campus.secondhand.service.ProductService;
import com.campus.secondhand.util.UserContext;
import com.campus.secondhand.vo.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 商品模块接口
 */
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    /** 发布商品 */
    @PostMapping
    public Result<Void> publish(@Validated @RequestBody ProductDTO dto) {
        productService.publish(dto);
        return Result.success();
    }

    /** 编辑商品 */
    @PutMapping
    public Result<Void> update(@Validated @RequestBody ProductDTO dto) {
        productService.updateProduct(dto);
        return Result.success();
    }

    /** 删除商品 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.success();
    }

    /** 下架商品 */
    @PutMapping("/{id}/off-shelf")
    public Result<Void> offShelf(@PathVariable Long id) {
        productService.offShelf(id);
        return Result.success();
    }

    /** 商品分页列表（支持搜索、分类筛选） */
    @GetMapping("/list")
    public Result<IPage<ProductVO>> list(ProductQueryDTO query) {
        try {
            query.setCurrentUserId(UserContext.getUserId());
        } catch (Exception ignored) {
            // 未登录用户也可浏览
        }
        return Result.success(productService.pageList(query));
    }

    /** 我的发布 */
    @GetMapping("/my")
    public Result<IPage<ProductVO>> myProducts(ProductQueryDTO query) {
        query.setSellerId(UserContext.getUserId());
        query.setCurrentUserId(UserContext.getUserId());
        query.setStatus(null);
        return Result.success(productService.pageList(query));
    }

    /** 商品详情 */
    @GetMapping("/detail/{id}")
    public Result<ProductVO> detail(@PathVariable Long id) {
        Long currentUserId = null;
        try {
            currentUserId = UserContext.getUserId();
        } catch (Exception ignored) {
        }
        return Result.success(productService.detail(id, currentUserId));
    }
}
