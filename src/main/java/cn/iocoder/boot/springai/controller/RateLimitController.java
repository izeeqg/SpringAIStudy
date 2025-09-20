package cn.iocoder.boot.springai.controller;

import cn.iocoder.boot.springai.ratelimit.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 限流管理控制器
 * 提供限流统计和管理功能
 */
@RestController
@RequestMapping("/api/management/ratelimit")
@Tag(name = "Rate Limit Management", description = "限流管理相关接口")
public class RateLimitController {

    private final RateLimitService rateLimitService;

    public RateLimitController(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    /**
     * 获取限流统计信息
     */
    @GetMapping(path = "/stats/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "获取限流统计", description = "获取指定key的限流统计信息")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getStats(@PathVariable String key) {
        RateLimitService.RateLimitStats stats = rateLimitService.getStats(key);
        
        return Map.of(
            "key", stats.getKey(),
            "currentCount", stats.getCurrentCount(),
            "ttl", stats.getTtl(),
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 测试限流
     */
    @PostMapping(path = "/test", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "测试限流", description = "测试指定参数的限流效果")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> testRateLimit(
            @RequestParam String key,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "60") int window) {
        
        RateLimitService.RateLimitResult result = rateLimitService.isAllowed(key, limit, window);
        
        return Map.of(
            "key", key,
            "allowed", result.isAllowed(),
            "remaining", result.getRemaining(),
            "limit", result.getLimit(),
            "window", window,
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 清理本地计数器
     */
    @PostMapping(path = "/cleanup", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "清理计数器", description = "清理过期的本地限流计数器")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> cleanup() {
        try {
            rateLimitService.cleanupLocalCounters();
            return Map.of(
                "success", true,
                "message", "本地计数器清理完成",
                "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "清理失败: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
        }
    }

    /**
     * 获取限流配置信息
     */
    @GetMapping(path = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "获取限流配置", description = "获取当前系统的限流配置信息")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getConfig() {
        return Map.of(
            "supportedTypes", new String[]{"USER", "IP", "API", "GLOBAL"},
            "redisEnabled", rateLimitService != null,
            "defaultLimits", Map.of(
                "USER", Map.of("limit", 20, "window", 60),
                "IP", Map.of("limit", 100, "window", 60),
                "API", Map.of("limit", 1000, "window", 60),
                "GLOBAL", Map.of("limit", 10000, "window", 60)
            ),
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 健康检查
     */
    @GetMapping(path = "/health")
    @Operation(summary = "健康检查", description = "检查限流服务状态")
    public Map<String, Object> health() {
        return Map.of(
            "status", "ok",
            "service", "ratelimit",
            "timestamp", System.currentTimeMillis()
        );
    }
}
