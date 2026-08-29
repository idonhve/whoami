package com.whoami.module.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.whoami.common.BizException;
import com.whoami.config.JwtProperties;
import com.whoami.module.auth.dto.AdminInfo;
import com.whoami.module.auth.dto.LoginResponse;
import com.whoami.module.auth.entity.AdminUser;
import com.whoami.module.auth.mapper.AdminUserMapper;
import com.whoami.security.JwtService;
import java.time.LocalDateTime;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SECRET = "unit-test-jwt-secret-0123456789abcdef";
    private static final String CORRECT_PASSWORD = "correct-password";
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Mock
    private AdminUserMapper adminUserMapper;

    private AuthService authService;

    private static AdminUser user(long id, int failedAttempts, LocalDateTime lockedUntil) {
        AdminUser user = new AdminUser();
        user.setId(id);
        user.setUsername("admin");
        user.setPasswordHash(ENCODER.encode(CORRECT_PASSWORD));
        user.setFailedAttempts(failedAttempts);
        user.setLockedUntil(lockedUntil);
        return user;
    }

    @BeforeAll
    static void initMybatisTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), AdminUser.class);
    }

    @BeforeEach
    void setUp() {
        authService = new AuthService(adminUserMapper, new JwtService(new JwtProperties(SECRET, 7200)));
    }

    @Test
    void loginSuccessResetsCountersAndReturnsToken() {
        when(adminUserMapper.selectOne(any())).thenReturn(user(1L, 3, null));

        LoginResponse response = authService.login("admin", CORRECT_PASSWORD);

        assertThat(response.token()).isNotBlank();
        assertThat(response.expiresIn()).isEqualTo(7200);

        ArgumentCaptor<AdminUser> captor = ArgumentCaptor.forClass(AdminUser.class);
        verify(adminUserMapper).updateById(captor.capture());
        AdminUser updated = captor.getValue();
        assertThat(updated.getFailedAttempts()).isZero();
        assertThat(updated.getLockedUntil()).isNull();
        assertThat(updated.getLastLoginAt()).isNotNull();
    }

    @Test
    void wrongPasswordIncrementsCounterWithoutLock() {
        when(adminUserMapper.selectOne(any())).thenReturn(user(1L, 2, null));

        assertThatThrownBy(() -> authService.login("admin", "wrong"))
                .isInstanceOf(BizException.class)
                .extracting("status")
                .isEqualTo(401);

        ArgumentCaptor<AdminUser> captor = ArgumentCaptor.forClass(AdminUser.class);
        verify(adminUserMapper).updateById(captor.capture());
        assertThat(captor.getValue().getFailedAttempts()).isEqualTo(3);
        assertThat(captor.getValue().getLockedUntil()).isNull();
    }

    @Test
    void fifthFailureLocksAccountForTenMinutes() {
        when(adminUserMapper.selectOne(any())).thenReturn(user(1L, 4, null));

        assertThatThrownBy(() -> authService.login("admin", "wrong"))
                .isInstanceOf(BizException.class)
                .extracting("status")
                .isEqualTo(401);

        ArgumentCaptor<AdminUser> captor = ArgumentCaptor.forClass(AdminUser.class);
        verify(adminUserMapper).updateById(captor.capture());
        LocalDateTime lockedUntil = captor.getValue().getLockedUntil();
        assertThat(lockedUntil)
                .isAfter(LocalDateTime.now().plusMinutes(9))
                .isBefore(LocalDateTime.now().plusMinutes(11));
    }

    @Test
    void lockedAccountIsRejectedBeforePasswordCheck() {
        when(adminUserMapper.selectOne(any())).thenReturn(user(1L, 5, LocalDateTime.now().plusMinutes(5)));

        assertThatThrownBy(() -> authService.login("admin", CORRECT_PASSWORD))
                .isInstanceOf(BizException.class)
                .extracting("status")
                .isEqualTo(403);

        verify(adminUserMapper, never()).updateById(any(AdminUser.class));
    }

    @Test
    void unknownUserIsRejectedWith401() {
        when(adminUserMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> authService.login("nobody", "whatever"))
                .isInstanceOf(BizException.class)
                .extracting("status")
                .isEqualTo(401);
    }

    @Test
    void refreshIssuesTokenForExistingUser() {
        AdminUser user = user(7L, 0, null);
        when(adminUserMapper.selectById(7L)).thenReturn(user);

        LoginResponse response = authService.refresh(7L);

        assertThat(response.token()).isNotBlank();
    }

    @Test
    void refreshRejectsMissingUser() {
        when(adminUserMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> authService.refresh(99L))
                .isInstanceOf(BizException.class)
                .extracting("status")
                .isEqualTo(401);
    }

    @Test
    void meReturnsAdminInfo() {
        when(adminUserMapper.selectById(1L)).thenReturn(user(1L, 0, null));

        AdminInfo info = authService.me(1L);

        assertThat(info.id()).isEqualTo(1L);
        assertThat(info.username()).isEqualTo("admin");
    }
}
