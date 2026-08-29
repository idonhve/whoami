package com.whoami.module.auth.dto;

public record LoginResponse(String token, long expiresIn) {
}
