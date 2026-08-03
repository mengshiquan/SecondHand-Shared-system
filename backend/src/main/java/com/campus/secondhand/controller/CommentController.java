package com.campus.secondhand.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.secondhand.common.Result;
import com.campus.secondhand.dto.CommentDTO;
import com.campus.secondhand.service.CommentService;
import com.campus.secondhand.vo.CommentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 评论模块接口
 */
@RestController
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    /** 发表评论 */
    @PostMapping
    public Result<Void> add(@Validated @RequestBody CommentDTO dto) {
        commentService.addComment(dto);
        return Result.success();
    }

    /** 删除评论 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success();
    }

    /** 商品评论列表 */
    @GetMapping("/list/{productId}")
    public Result<IPage<CommentVO>> list(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(commentService.pageList(productId, pageNum, pageSize));
    }
}
