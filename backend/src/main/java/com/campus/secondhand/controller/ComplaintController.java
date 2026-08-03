package com.campus.secondhand.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.common.Result;
import com.campus.secondhand.entity.Complaint;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.mapper.ComplaintMapper;
import com.campus.secondhand.service.UserService;
import com.campus.secondhand.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/complaint")
public class ComplaintController {

    @Autowired
    private ComplaintMapper complaintMapper;
    @Autowired
    private UserService userService;

    /** 用户提交投诉 */
    @PostMapping
    public Result<Void> submit(@RequestBody Complaint complaint) {
        Long reporterId = UserContext.getUserId();
        // 校验目标用户存在
        User target = userService.getById(complaint.getTargetUserId());
        if (target == null) throw new BusinessException("目标用户不存在");
        // 不能投诉自己
        if (target.getId().equals(reporterId)) throw new BusinessException("不能投诉自己");
        // 同一用户对同一目标只能有一条待处理投诉
        long existing = complaintMapper.selectCount(new LambdaQueryWrapper<Complaint>()
                .eq(Complaint::getReporterId, reporterId)
                .eq(Complaint::getTargetUserId, complaint.getTargetUserId())
                .eq(Complaint::getStatus, "PENDING"));
        if (existing > 0) {
            throw new BusinessException("你已对该用户提交过投诉，请等待处理");
        }
        complaint.setId(null);
        complaint.setReporterId(reporterId);
        complaint.setStatus("PENDING");
        complaintMapper.insert(complaint);
        return Result.success();
    }
}
