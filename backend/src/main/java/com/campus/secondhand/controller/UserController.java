package com.campus.secondhand.controller;

import com.campus.secondhand.common.Result;
import com.campus.secondhand.dto.LoginDTO;
import com.campus.secondhand.dto.RegisterDTO;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.service.UserService;
import com.campus.secondhand.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户模块接口
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /** 用户登录 */
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    /** 用户注册 */
    @PostMapping("/register")
    public Result<Void> register(@Validated @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }

    /** 获取当前用户信息 */
    @GetMapping("/info")
    public Result<User> info() {
        return Result.success(userService.getCurrentUser());
    }

    /** 更新个人资料 */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody User user) {
        userService.updateProfile(user);
        return Result.success();
    }

    /** 修改密码 */
    @PutMapping("/password")
    public Result<Void> updatePassword(@RequestBody Map<String, String> params) {
        userService.updatePassword(params.get("oldPassword"), params.get("newPassword"));
        return Result.success();
    }

    /** 查询当前用户小黑屋状态 */
    @GetMapping("/blacklist-status")
    public Result<Map<String, Object>> blacklistStatus() {
        User user = userService.getCurrentUser();
        Map<String, Object> result = new java.util.HashMap<>();
        // 禁用标记：供前端顶栏展示禁用提醒（登录态残留会话场景）
        result.put("disabled", user.getStatus() != null && user.getStatus() == 0);
        if (user.getBlacklistStatus() != null) {
            result.put("blacklisted", true);
            result.put("status", user.getBlacklistStatus());
            result.put("reason", user.getBlacklistReason());
            result.put("until", user.getBlacklistUntil());
        } else {
            result.put("blacklisted", false);
        }
        return Result.success(result);
    }

    /** 注销账号 */
    @PutMapping("/deactivate")
    public Result<Void> deactivate(@RequestBody Map<String, String> params) {
        userService.deactivate(params.get("password"));
        return Result.success();
    }
}
