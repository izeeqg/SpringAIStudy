package cn.iocoder.boot.springai.controller;

import cn.iocoder.boot.springai.ratelimit.RateLimit;
import cn.iocoder.boot.springai.ratelimit.RateLimitType;
import cn.iocoder.boot.springai.service.StreamingChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

/**
 * 流式对话控制器
 * 提供基于SSE的实时流式对话接口
 */
@RestController
@RequestMapping("/api/streaming")
@Validated
@Tag(name = "Streaming Chat API", description = "流式对话相关接口")
public class StreamingChatController {

    private final StreamingChatService streamingChatService;

    public StreamingChatController(StreamingChatService streamingChatService) {
        this.streamingChatService = streamingChatService;
    }

    /**
     * 流式对话接口
     */
    @GetMapping(path = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式对话", description = "基于SSE的实时流式对话")
    @RateLimit(type = RateLimitType.USER, limit = 10, window = 60, message = "流式对话请求过于频繁，每分钟最多10次")
    public SseEmitter streamChat(
            @RequestParam @NotBlank String prompt,
            @RequestParam(required = false) String connectionId) {
        
        // 如果未提供connectionId，自动生成一个
        if (connectionId == null || connectionId.trim().isEmpty()) {
            connectionId = UUID.randomUUID().toString();
        }
        
        return streamingChatService.streamChat(prompt, connectionId);
    }

    /**
     * 获取连接统计信息
     */
    @GetMapping(path = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "连接统计", description = "获取当前活跃的流式连接统计")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getConnectionStats() {
        return streamingChatService.getConnectionStats();
    }

    /**
     * 关闭指定连接
     */
    @DeleteMapping(path = "/connections/{connectionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "关闭连接", description = "关闭指定的流式连接")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> closeConnection(@PathVariable String connectionId) {
        boolean closed = streamingChatService.closeConnection(connectionId);
        
        return Map.of(
            "success", closed,
            "connectionId", connectionId,
            "message", closed ? "连接已关闭" : "连接不存在或已关闭",
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 向指定连接发送消息
     */
    @PostMapping(path = "/connections/{connectionId}/message", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "发送消息", description = "向指定连接发送消息")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> sendMessage(
            @PathVariable String connectionId,
            @RequestParam @NotBlank String message) {
        
        boolean sent = streamingChatService.sendMessage(connectionId, message);
        
        return Map.of(
            "success", sent,
            "connectionId", connectionId,
            "message", sent ? "消息已发送" : "连接不存在或发送失败",
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 广播消息到所有连接
     */
    @PostMapping(path = "/broadcast", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "广播消息", description = "向所有活跃连接广播消息")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> broadcast(@RequestParam @NotBlank String message) {
        int successCount = streamingChatService.broadcast(message);
        
        return Map.of(
            "success", true,
            "message", "广播完成",
            "successCount", successCount,
            "broadcastMessage", message,
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 健康检查
     */
    @GetMapping(path = "/health")
    @Operation(summary = "健康检查", description = "检查流式服务状态")
    public Map<String, Object> health() {
        Map<String, Object> stats = streamingChatService.getConnectionStats();
        
        return Map.of(
            "status", "ok",
            "service", "streaming",
            "activeConnections", stats.get("activeConnections"),
            "timestamp", System.currentTimeMillis()
        );
    }
}
