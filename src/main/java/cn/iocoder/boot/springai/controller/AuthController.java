package cn.iocoder.boot.springai.controller;

import cn.iocoder.boot.springai.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 * 处理用户登录、注册、token刷新等认证相关请求
 */
@RestController
@RequestMapping("/api/auth")
@Validated
@Tag(name = "Authentication API", description = "用户认证相关接口")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录
     */
    @PostMapping(path = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回JWT token")
    public Map<String, Object> login(
            @RequestParam @NotBlank(message = "用户名不能为空") String username,
            @RequestParam @NotBlank(message = "密码不能为空") String password) {
        
        return authService.login(username, password);
    }

    /**
     * 用户注册
     */
    @PostMapping(path = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "用户注册", description = "注册新用户账户")
    public Map<String, Object> register(
            @RequestParam @NotBlank(message = "用户名不能为空") 
            @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间") String username,
            @RequestParam @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间") String password,
            @RequestParam @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确") String email) {
        
        return authService.register(username, password, email);
    }

    /**
     * 刷新token
     */
    @PostMapping(path = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "刷新Token", description = "使用刷新token获取新的访问token")
    public Map<String, Object> refreshToken(
            @RequestParam @NotBlank(message = "刷新token不能为空") String refreshToken) {
        
        return authService.refreshToken(refreshToken);
    }

    /**
     * 验证token
     */
    @PostMapping(path = "/validate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "验证Token", description = "验证访问token是否有效")
    public Map<String, Object> validateToken(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        
        if (!StringUtils.hasText(token)) {
            return Map.of(
                "valid", false,
                "message", "未提供token"
            );
        }
        
        return authService.validateToken(token);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping(path = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "获取当前用户", description = "获取当前登录用户的详细信息")
    public Map<String, Object> getCurrentUser(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        
        if (!StringUtils.hasText(token)) {
            return Map.of(
                "success", false,
                "message", "未提供token"
            );
        }
        
        return authService.getCurrentUser(token);
    }

    /**
     * 用户登出（客户端处理）
     */
    @PostMapping(path = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "用户登出", description = "用户登出（主要由客户端清除token）")
    public Map<String, Object> logout() {
        // JWT是无状态的，服务端无需处理登出
        // 客户端需要清除本地存储的token
        return Map.of(
            "success", true,
            "message", "登出成功，请清除本地token"
        );
    }

    /**
     * 健康检查
     */
    @GetMapping(path = "/health")
    @Operation(summary = "健康检查", description = "检查认证服务状态")
    public Map<String, Object> health() {
        return Map.of(
            "status", "ok",
            "service", "authentication",
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 从请求中提取token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
}
