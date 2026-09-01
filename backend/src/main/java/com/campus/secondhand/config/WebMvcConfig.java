package com.campus.secondhand.config;

import com.campus.secondhand.config.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：注册 JWT 登录拦截器，全局校验登录态，
 * 并放行登录注册、商品列表/详情、分类、评论、上传资源、支付回调、验证码等公开接口。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    /**
     * 配置 JWT 拦截器的拦截范围与放行路径。
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
                        "/product/list",
                        "/product/detail/**",
                        "/category/**",
                        "/comment/list/**",
                        "/uploads/**",
                        "/pay/alipay/notify",
                        "/captcha",
                        "/error"
                );
    }

}
