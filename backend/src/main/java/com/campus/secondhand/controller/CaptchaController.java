package com.campus.secondhand.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.campus.secondhand.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码控制器：为登录/注册提供人机校验凭证。
 * 生成 4 位数字/字母验证码并以 base64 图片返回，key 存 Redis 5 分钟；
 * 校验时由 UserService 一次性消费，防止重放攻击。
 */
@RestController
@RequestMapping("/captcha")
public class CaptchaController {

    public static final String REDIS_PREFIX = "captcha:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 生成新的图形验证码。
     *
     * @return key: Redis 中验证码的唯一标识；image: PNG 图片的 base64 数据 URI
     */
    @GetMapping
    public Result<Map<String, String>> create() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 44, 4, 28);
        String key = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(REDIS_PREFIX + key, captcha.getCode(), 5, TimeUnit.MINUTES);
        return Result.success(Map.of(
                "key", key,
                "image", "data:image/png;base64," + captcha.getImageBase64()));
    }
}
