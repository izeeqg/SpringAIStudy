package cn.iocoder.boot.springai.controller;

import cn.iocoder.boot.springai.service.ChatService;
import cn.iocoder.boot.springai.service.SampleDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理控制器
 * 提供系统监控、缓存管理等功能
 */
@RestController
@RequestMapping("/api/management")
@Tag(name = "Management API", description = "系统管理和监控相关接口")
public class ManagementController {

    private final ChatService chatService;
    private final CacheManager cacheManager;
    
    @Autowired(required = false)
    private SampleDataService sampleDataService;

    public ManagementController(ChatService chatService, CacheManager cacheManager) {
        this.chatService = chatService;
        this.cacheManager = cacheManager;
    }

    /**
     * 获取系统状态概览
     */
    @GetMapping(path = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "系统状态", description = "获取系统整体运行状态")
    public Map<String, Object> getSystemStatus() {
        return Map.of(
            "application", "springai-app",
            "status", "running",
            "timestamp", System.currentTimeMillis(),
            "uptime", getUptime(),
            "services", Map.of(
                "chat", "active",
                "rag", "active", 
                "functions", "active",
                "cache", "active"
            )
        );
    }

    /**
     * 获取缓存统计信息
     */
    @GetMapping(path = "/cache/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "缓存统计", description = "获取所有缓存的统计信息")
    public Map<String, Object> getCacheStats() {
        Map<String, Object> cacheStats = cacheManager.getCacheNames().stream()
            .collect(Collectors.toMap(
                name -> name,
                name -> {
                    var cache = cacheManager.getCache(name);
                    return cache != null ? 
                        Map.of("name", name, "type", cache.getClass().getSimpleName()) :
                        Map.of("name", name, "status", "not_found");
                }
            ));

        return Map.of(
            "caches", cacheStats,
            "chatServiceCache", chatService.getCacheStats(),
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 清理指定缓存
     */
    @DeleteMapping(path = "/cache/{cacheName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "清理缓存", description = "清理指定名称的缓存")
    public Map<String, Object> clearCache(@PathVariable String cacheName) {
        try {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                return Map.of(
                    "success", true,
                    "message", "缓存 " + cacheName + " 已清理",
                    "cacheName", cacheName
                );
            } else {
                return Map.of(
                    "success", false,
                    "message", "缓存 " + cacheName + " 不存在",
                    "cacheName", cacheName
                );
            }
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "清理缓存失败: " + e.getMessage(),
                "cacheName", cacheName
            );
        }
    }

    /**
     * 清理所有缓存
     */
    @DeleteMapping(path = "/cache/all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "清理所有缓存", description = "清理系统中的所有缓存")
    public Map<String, Object> clearAllCaches() {
        try {
            cacheManager.getCacheNames().forEach(name -> {
                var cache = cacheManager.getCache(name);
                if (cache != null) {
                    cache.clear();
                }
            });
            
            return Map.of(
                "success", true,
                "message", "所有缓存已清理",
                "clearedCaches", cacheManager.getCacheNames()
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "清理缓存失败: " + e.getMessage()
            );
        }
    }

    /**
     * 获取系统配置信息
     */
    @GetMapping(path = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "系统配置", description = "获取系统配置信息（脱敏）")
    public Map<String, Object> getSystemConfig() {
        return Map.of(
            "profiles", System.getProperty("spring.profiles.active", "default"),
            "javaVersion", System.getProperty("java.version"),
            "springBootVersion", org.springframework.boot.SpringBootVersion.getVersion(),
            "resilience4j", Map.of(
                "rateLimiter", "enabled",
                "retry", "enabled", 
                "timeLimiter", "enabled"
            ),
            "cache", Map.of(
                "provider", "caffeine",
                "caches", cacheManager.getCacheNames()
            ),
            "ai", Map.of(
                "provider", "openai",
                "models", Map.of(
                    "chat", "gpt-4o-mini",
                    "embedding", "text-embedding-3-small"
                )
            )
        );
    }

    /**
     * 健康检查
     */
    @GetMapping(path = "/health")
    @Operation(summary = "健康检查", description = "检查管理服务状态")
    public Map<String, Object> health() {
        return Map.of(
            "status", "ok",
            "service", "management",
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 重新导入示例数据
     */
    @PostMapping(path = "/sample-data/reimport", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "重新导入示例数据", description = "重新导入系统示例文档到向量库")
    public Map<String, Object> reimportSampleData() {
        if (sampleDataService == null) {
            return Map.of(
                "success", false,
                "message", "示例数据服务未配置"
            );
        }
        return sampleDataService.reimportSampleData();
    }

    /**
     * 获取示例文档列表
     */
    @GetMapping(path = "/sample-data/documents", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "获取示例文档", description = "获取系统内置的示例文档列表")
    public Map<String, Object> getSampleDocuments() {
        if (sampleDataService == null) {
            return Map.of(
                "success", false,
                "message", "示例数据服务未配置"
            );
        }
        return sampleDataService.getSampleDocuments();
    }

    /**
     * 获取系统运行时间（简单实现）
     */
    private String getUptime() {
        long uptimeMs = System.currentTimeMillis() - getStartTime();
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        return String.format("%d小时%d分钟%d秒", hours, minutes % 60, seconds % 60);
    }

    /**
     * 获取应用启动时间（简单实现）
     */
    private long getStartTime() {
        // 这里简化处理，实际可以通过ApplicationContext获取更准确的启动时间
        return System.currentTimeMillis() - 3600000; // 假设运行了1小时
    }
}
