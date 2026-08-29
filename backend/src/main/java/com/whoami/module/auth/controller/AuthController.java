package com.whoami.module.auth.controller;

import com.whoami.common.ApiResult;
import com.whoami.module.auth.dto.AdminInfo;
import com.whoami.module.auth.dto.LoginRequest;
import com.whoami.module.auth.dto.LoginResponse;
import com.whoami.module.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResult.ok(authService.login(request.username(), request.password()));
    }

    @PostMapping("/refresh")
    public ApiResult<LoginResponse> refresh(@RequestAttribute("adminId") long adminId) {
        return ApiResult.ok(authService.refresh(adminId));
    }

    @GetMapping("/me")
    public ApiResult<AdminInfo> me(@RequestAttribute("adminId") long adminId) {
        return ApiResult.ok(authService.me(adminId));
    }
}
