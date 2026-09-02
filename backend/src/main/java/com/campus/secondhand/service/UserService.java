package com.campus.secondhand.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.secondhand.dto.LoginDTO;
import com.campus.secondhand.dto.RegisterDTO;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.vo.LoginVO;

public interface UserService extends IService<User> {

    /** 登录并返回 JWT 与脱敏后的用户信息。 */
    LoginVO login(LoginDTO dto);

    /** 注册普通用户；用户名和学号全局唯一，校园认证初始为待审核。 */
    void register(RegisterDTO dto);

    /** 读取当前登录用户，返回前清除密码。 */
    User getCurrentUser();

    /** 更新当前用户昵称、头像、手机号和邮箱。 */
    void updateProfile(User user);

    /** 校验旧密码后更新密码，并自动把旧 MD5 密码升级为 BCrypt。 */
    void updatePassword(String oldPassword, String newPassword);

    /** 注销当前用户；要求密码正确且名下没有待处理订单。 */
    void deactivate(String password);
}
