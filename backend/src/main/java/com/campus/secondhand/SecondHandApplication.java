package com.campus.secondhand;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园二手物品共享平台启动类
 */
@SpringBootApplication
@MapperScan("com.campus.secondhand.mapper")
@EnableScheduling
public class SecondHandApplication {

    public static void main(String[] args) {

        SpringApplication.run(SecondHandApplication.class, args);
    }
}
