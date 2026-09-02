package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.secondhand.dto.CommentDTO;
import com.campus.secondhand.entity.Comment;
import com.campus.secondhand.vo.CommentVO;

public interface CommentService extends IService<Comment> {

    /** 当前用户对指定商品发表评论和评分；需完成交易且不能重复评价。 */
    void addComment(CommentDTO dto);

    /** 删除本人或管理员指定的评论。 */
    void deleteComment(Long id);

    /** 分页查询商品评论，附带评论人昵称与头像。 */
    IPage<CommentVO> pageList(Long productId, Integer pageNum, Integer pageSize);
}
