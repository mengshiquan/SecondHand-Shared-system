package com.campus.secondhand.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.common.Result;
import com.campus.secondhand.entity.Appeal;
import com.campus.secondhand.mapper.AppealMapper;
import com.campus.secondhand.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/appeal")
public class AppealController {

    @Autowired
    private AppealMapper appealMapper;

    /** 用户提交申诉 */
    @PostMapping
    public Result<Void> submit(@RequestBody Appeal appeal) {
        Long userId = UserContext.getUserId();
        long existing = appealMapper.selectCount(new LambdaQueryWrapper<Appeal>()
                .eq(Appeal::getUserId, userId)
                .eq(Appeal::getStatus, "PENDING"));
        if (existing > 0) {
            throw new BusinessException("你已有一条申诉在处理中，请等待结果");
        }
        appeal.setId(null);
        appeal.setUserId(userId);
        appeal.setStatus("PENDING");
        appealMapper.insert(appeal);
        return Result.success();
    }
}
