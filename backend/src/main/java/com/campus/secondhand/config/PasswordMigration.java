package com.campus.secondhand.config;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 密码迁移组件：应用启动时自动将旧 MD5 密码迁移为 BCrypt
 * <p>
 * MD5 哈希特征：32 位十六进制字符串
 * BCrypt 哈希特征：60 位，以 $2a$ / $2b$ 开头
 * <p>
 * 迁移完成后此组件无需删除，它会自动跳过已是 BCrypt 格式的密码。
 */
@Component
public class PasswordMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PasswordMigration.class);

    @Autowired
    private UserService userService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        List<User> users = userService.list();
        int migrated = 0;

        for (User user : users) {
            String pwd = user.getPassword();
            if (pwd != null && isMd5(pwd)) {
                // MD5 是明文哈希，无法逆向；但我们可以将其作为"原始密码"重新编码
                // 这里将 MD5 值本身用 BCrypt 包裹，后续登录时走兼容逻辑
                String bcryptHash = passwordEncoder.encode(pwd);
                userService.update(new LambdaUpdateWrapper<User>()
                        .set(User::getPassword, bcryptHash)
                        .eq(User::getId, user.getId()));
                migrated++;
            }
        }

        if (migrated > 0) {
            log.info("【密码迁移】已将 {} 个用户的 MD5 密码迁移为 BCrypt", migrated);
        } else {
            log.info("【密码迁移】所有用户密码已是 BCrypt 格式，无需迁移");
        }
    }

    /**
     * 判断密码是否为旧 MD5 格式（32 位十六进制字符串）
     */
    private boolean isMd5(String password) {
        if (password.length() != 32) return false;
        return password.matches("^[0-9a-fA-F]{32}$");
    }
}
