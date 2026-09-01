package com.campus.secondhand.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 跨域配置：源白名单通过 application.yml 的 cors.allowed-origins 配置，多个源用逗号分隔。
 * <p>
 * 默认 * 表示允许任意源（开发/演示环境需支持局域网 IP、内网穿透域名访问）；
 * 生产部署可通过环境变量收紧为精确域名：CORS_ALLOWED_ORIGINS=https://example.com,https://admin.example.com
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 用模式匹配而非精确白名单：既支持 * 通配（兼容 allowCredentials），也支持精确域名
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        config.setAllowedOriginPatterns(origins);

        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
