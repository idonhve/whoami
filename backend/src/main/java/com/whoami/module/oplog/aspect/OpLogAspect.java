package com.whoami.module.oplog.aspect;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.whoami.module.auth.dto.LoginRequest;
import com.whoami.module.auth.entity.AdminUser;
import com.whoami.module.auth.mapper.AdminUserMapper;
import com.whoami.module.oplog.entity.AdminOpLog;
import com.whoami.module.oplog.service.OpLogService;
import com.whoami.module.oplog.support.SensitiveMasker;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志切面（Spec 06）：自动拦截全部 /admin/api/** 的 POST/PUT/DELETE，
 * 各模块管理接口无需自己埋日志；登录成功/失败以 action=LOGIN 记录。
 * 记录内容：管理员 id、HTTP 方法与路径、资源 id（@PathVariable 提取）、
 * 参数摘要（脱敏后）、IP、时间。日志写入失败只告警，绝不影响业务请求。
 */
@Aspect
@Component
public class OpLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OpLogAspect.class);

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE");
    private static final String ADMIN_API_PREFIX = "/admin/api/";
    private static final String LOGIN_URI = "/admin/api/auth/login";
    private static final int DETAIL_MAX_LENGTH = 2000;

    private final OpLogService opLogService;
    private final AdminUserMapper adminUserMapper;
    private final ObjectMapper objectMapper;

    public OpLogAspect(OpLogService opLogService, AdminUserMapper adminUserMapper, ObjectMapper objectMapper) {
        this.opLogService = opLogService;
        this.adminUserMapper = adminUserMapper;
        this.objectMapper = objectMapper;
    }

    @Pointcut("execution(* com.whoami.module..controller.*.*(..))")
    void moduleControllers() {
    }

    @AfterReturning("moduleControllers()")
    public void logSuccess(JoinPoint joinPoint) {
        record(joinPoint, null);
    }

    @AfterThrowing(pointcut = "moduleControllers()", throwing = "error")
    public void logFailure(JoinPoint joinPoint, Throwable error) {
        record(joinPoint, error);
    }

    private void record(JoinPoint joinPoint, Throwable error) {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            HttpServletRequest request = attributes.getRequest();
            String uri = request.getRequestURI();
            if (!uri.startsWith(ADMIN_API_PREFIX)) {
                return;
            }
            String method = request.getMethod();
            boolean login = LOGIN_URI.equals(uri) && "POST".equals(method);
            if (!login && !WRITE_METHODS.contains(method)) {
                return;
            }

            AdminOpLog entry = new AdminOpLog();
            entry.setAdminUserId(resolveAdminId(request, joinPoint, login));
            entry.setAction(login ? "LOGIN" : method);
            entry.setResource(uri);
            entry.setResourceId(extractResourceId(joinPoint));
            entry.setDetail(buildDetail(joinPoint, error));
            entry.setIp(resolveIp(request));
            opLogService.record(entry);
        } catch (Exception e) {
            log.warn("操作日志写入失败（不影响业务）: {}", e.getMessage());
        }
    }

    /** JWT 过滤器已放入 adminId；登录接口无 token，从登录参数反查（未知账号记 0） */
    private long resolveAdminId(HttpServletRequest request, JoinPoint joinPoint, boolean login) {
        Object adminId = request.getAttribute("adminId");
        if (adminId instanceof Long id) {
            return id;
        }
        if (login) {
            for (Object arg : joinPoint.getArgs()) {
                if (arg instanceof LoginRequest loginRequest) {
                    AdminUser user = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                            .eq(AdminUser::getUsername, loginRequest.username()));
                    return user == null ? 0L : user.getId();
                }
            }
        }
        return 0L;
    }

    /** 资源 id：取目标方法的 @PathVariable 值（多个按声明顺序逗号拼接） */
    private String extractResourceId(JoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();
        List<String> values = new ArrayList<>();
        for (int i = 0; i < parameters.length && i < args.length; i++) {
            if (parameters[i].isAnnotationPresent(PathVariable.class) && args[i] != null) {
                values.add(String.valueOf(args[i]));
            }
        }
        return values.isEmpty() ? null : String.join(",", values);
    }

    /** 参数摘要：请求体等参数序列化为 JSON 后脱敏；结果与异常信息一并记录 */
    private String buildDetail(JoinPoint joinPoint, Throwable error) {
        ObjectNode detail = objectMapper.createObjectNode();
        ArrayNode params = detail.putArray("params");
        for (Object arg : joinPoint.getArgs()) {
            if (arg == null
                    || arg instanceof HttpServletRequest
                    || arg instanceof HttpServletResponse
                    || arg instanceof BindingResult) {
                continue;
            }
            params.add(SensitiveMasker.mask(objectMapper.valueToTree(arg)));
        }
        detail.put("result", error == null ? "success" : "fail");
        if (error != null && error.getMessage() != null) {
            detail.put("error", error.getMessage());
        }
        String json = detail.toString();
        return json.length() <= DETAIL_MAX_LENGTH ? json : json.substring(0, DETAIL_MAX_LENGTH);
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
