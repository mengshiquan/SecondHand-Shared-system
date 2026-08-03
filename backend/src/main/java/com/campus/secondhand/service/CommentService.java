package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.secondhand.dto.CommentDTO;
import com.campus.secondhand.entity.Comment;
import com.campus.secondhand.vo.CommentVO;

public interface CommentService extends IService<Comment> {

    void addComment(CommentDTO dto);

    void deleteComment(Long id);

    IPage<CommentVO> pageList(Long productId, Integer pageNum, Integer pageSize);
}
