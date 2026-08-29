package com.whoami.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.whoami.common.BizException;
import com.whoami.module.auth.dto.AdminInfo;
import com.whoami.module.auth.dto.LoginResponse;
import com.whoami.module.auth.entity.AdminUser;
import com.whoami.module.auth.mapper.AdminUserMapper;
import com.whoami.security.JwtService;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    static final int MAX_FAILED_ATTEMPTS = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(10);

    private final AdminUserMapper adminUserMapper;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(AdminUserMapper adminUserMapper, JwtService jwtService) {
        this.adminUserMapper = adminUserMapper;
        this.jwtService = jwtService;
    }

    public LoginResponse login(String username, String password) {
        AdminUser user = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, username));
        if (user == null) {
            throw new BizException(401, "用户名或密码错误");
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            long minutes = Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes() + 1;
            throw new BizException(403, "账号已锁定，请约 " + minutes + " 分钟后重试");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            registerFailure(user);
            throw new BizException(401, "用户名或密码错误");
        }
        registerSuccess(user);
        return new LoginResponse(jwtService.issue(user.getId()), jwtService.expiresIn());
    }

    public LoginResponse refresh(long adminId) {
        return new LoginResponse(jwtService.issue(loadUser(adminId).getId()), jwtService.expiresIn());
    }

    public AdminInfo me(long adminId) {
        AdminUser user = loadUser(adminId);
        return new AdminInfo(user.getId(), user.getUsername());
    }

    private AdminUser loadUser(long adminId) {
        AdminUser user = adminUserMapper.selectById(adminId);
        if (user == null) {
            throw new BizException(401, "账号不存在或已删除");
        }
        return user;
    }

    private void registerFailure(AdminUser user) {
        int attempts = (user.getFailedAttempts() == null ? 0 : user.getFailedAttempts()) + 1;
        AdminUser update = new AdminUser();
        update.setId(user.getId());
        update.setFailedAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            update.setLockedUntil(LocalDateTime.now().plus(LOCK_DURATION));
        }
        adminUserMapper.updateById(update);
    }

    private void registerSuccess(AdminUser user) {
        AdminUser update = new AdminUser();
        update.setId(user.getId());
        update.setFailedAttempts(0);
        update.setLockedUntil(null);
        update.setLastLoginAt(LocalDateTime.now());
        adminUserMapper.updateById(update);
    }
}
