package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.common.VerifyGuard;
import com.campus.secondhand.dto.CommentDTO;
import com.campus.secondhand.entity.Comment;
import com.campus.secondhand.mapper.CommentMapper;
import com.campus.secondhand.service.CommentService;
import com.campus.secondhand.service.UserService;
import com.campus.secondhand.util.UserContext;
import com.campus.secondhand.vo.CommentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Autowired
    private UserService userService;

    @Override
    public void addComment(CommentDTO dto) {
        VerifyGuard.requireVerified(userService);
        Comment comment = new Comment();
        comment.setProductId(dto.getProductId());
        comment.setUserId(UserContext.getUserId());
        comment.setContent(dto.getContent());
        comment.setRating(dto.getRating());
        save(comment);
    }

    @Override
    public void deleteComment(Long id) {
        Comment comment = getById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        if (!comment.getUserId().equals(UserContext.getUserId()) && !UserContext.isAdmin()) {
            throw new BusinessException("无权删除此评论");
        }
        removeById(id);
    }

    @Override
    public IPage<CommentVO> pageList(Long productId, Integer pageNum, Integer pageSize) {
        Page<CommentVO> page = new Page<>(pageNum, pageSize);
        return baseMapper.selectCommentPage(page, productId);
    }
}
