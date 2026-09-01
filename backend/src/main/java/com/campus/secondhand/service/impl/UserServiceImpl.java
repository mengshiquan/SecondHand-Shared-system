package com.campus.secondhand.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.dto.LoginDTO;
import com.campus.secondhand.dto.RegisterDTO;
import com.campus.secondhand.entity.Order;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.mapper.OrderMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.service.UserService;
import com.campus.secondhand.util.JwtUtil;
import com.campus.secondhand.util.UserContext;
import com.campus.secondhand.vo.LoginVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 校验图形验证码：一次性消费，错误/过期均要求重新刷新
     */
    private void verifyCaptcha(String captchaKey, String captchaCode) {
        String redisKey = "captcha:" + captchaKey;
        String stored = stringRedisTemplate.opsForValue().get(redisKey);
        stringRedisTemplate.delete(redisKey);
        if (stored == null) {
            throw new BusinessException("验证码已过期，请刷新后重试");
        }
        if (!stored.equalsIgnoreCase(captchaCode)) {
            throw new BusinessException("验证码错误");
        }
    }

    /**
     * 用户登录：先校验图形验证码，再验证用户名密码，最后校验账号状态。
     * 密码支持 BCrypt 新格式与 MD5 旧格式兼容，并在匹配成功后自动升级为 BCrypt。
     *
     * @param dto 登录请求（用户名、密码、验证码 key/值）
     * @return 登录成功后的 token 及用户信息
     */
    @Override
    public LoginVO login(LoginDTO dto) {
        verifyCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode());
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!verifyAndUpgradePassword(user, dto.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        if (user.getStatus() != null && user.getStatus() == 2) {
            throw new BusinessException("该账号已注销");
        }
        return buildLoginVO(user);
    }

    /**
     * 用户注册：校验图形验证码、用户名与学号唯一性（含已注销账号残留行），
     * 密码使用 BCrypt 加密，注册后需管理员审核校园身份（verifyStatus=PENDING）。
     *
     * @param dto 注册请求（用户名、密码、学号、昵称等）
     */
    @Override
    public void register(RegisterDTO dto) {
        verifyCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode());
        // 唯一性校验需覆盖已注销账号（逻辑删除残留行仍占用唯一值）
        long count = baseMapper.countByUsernameIncludeDeleted(dto.getUsername());
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        // 学号唯一：一个学号只能注册一个账号，防止冒用身份
        long studentCount = baseMapper.countByStudentIdIncludeDeleted(dto.getStudentId());
        if (studentCount > 0) {
            throw new BusinessException("该学号已被注册");
        }

        User user = new User();
        BeanUtils.copyProperties(dto, user);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole("USER");
        user.setStatus(1);
        // 注册后需管理员审核校园身份，防止校外人员冒充学生发布虚假信息
        user.setVerifyStatus("PENDING");
        save(user);
    }

    @Override
    public User getCurrentUser() {
        Long userId = UserContext.getUserId();
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public void updateProfile(User user) {
        Long userId = UserContext.getUserId();
        User exist = getById(userId);
        if (exist == null) {
            throw new BusinessException("用户不存在");
        }
        exist.setNickname(user.getNickname());
        exist.setAvatar(user.getAvatar());
        exist.setPhone(user.getPhone());
        exist.setEmail(user.getEmail());
        updateById(exist);
    }

    @Override
    public void updatePassword(String oldPassword, String newPassword) {
        Long userId = UserContext.getUserId();
        User user = getById(userId);
        if (!verifyAndUpgradePassword(user, oldPassword)) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
    }

    @Override
    public void deactivate(String password) {
        Long userId = UserContext.getUserId();
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if ("ADMIN".equals(user.getRole())) {
            throw new BusinessException("管理员账号不允许注销");
        }
        if (!verifyAndUpgradePassword(user, password)) {
            throw new BusinessException("密码错误");
        }
        // 检查未完成的订单（买家或卖家角色）
        long pendingOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .in(Order::getStatus, "PENDING", "PAID", "SHIPPED")
                .and(w -> w.eq(Order::getBuyerId, userId).or().eq(Order::getSellerId, userId)));
        if (pendingOrders > 0) {
            throw new BusinessException("你有 " + pendingOrders + " 笔未完成的订单，请先完成或取消后再注销");
        }
        user.setStatus(2);
        updateById(user);
    }

    private LoginVO buildLoginVO(User user) {
        LoginVO vo = new LoginVO();
        vo.setToken(jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole()));
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        return vo;
    }

    /**
     * 验证密码，支持 BCrypt（新）和 MD5（旧）双格式。
     * 验证成功时若为旧 MD5 格式则自动升级为 BCrypt。
     */
    private boolean verifyAndUpgradePassword(User user, String rawPassword) {
        String stored = user.getPassword();

        // BCrypt 格式（新密码或已迁移的密码）
        if (stored != null && stored.startsWith("$2a$")) {
            // 直接匹配：BCrypt(plainPassword)
            if (passwordEncoder.matches(rawPassword, stored)) {
                return true;
            }
            // 兼容迁移格式：BCrypt(MD5(plainPassword))
            String md5Hex = DigestUtil.md5Hex(rawPassword);
            if (passwordEncoder.matches(md5Hex, stored)) {
                // 升级为 BCrypt(plainPassword)，后续登录不再需要 MD5 兼容
                user.setPassword(passwordEncoder.encode(rawPassword));
                updateById(user);
                return true;
            }
            return false;
        }

        // MD5 格式（尚未被 PasswordMigration 迁移的旧密码）
        if (stored != null && stored.length() == 32) {
            if (stored.equals(DigestUtil.md5Hex(rawPassword))) {
                // 匹配成功，立即升级为 BCrypt
                user.setPassword(passwordEncoder.encode(rawPassword));
                updateById(user);
                return true;
            }
            return false;
        }

        return false;
    }
}
