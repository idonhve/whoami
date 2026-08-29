package com.whoami.common;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/admin/api/health")
    public ApiResult<Map<String, String>> health() {
        return ApiResult.ok(Map.of("status", "up"));
    }
}
