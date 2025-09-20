package cn.iocoder.boot.springai.service;

import io.micrometer.core.instrument.Counter;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 流式对话服务
 * 实现基于SSE的实时流式响应
 */
@Service
public class StreamingChatService {

    private final OpenAiChatModel chatModel;
    private final Counter streamConnectionCounter;
    private final ScheduledExecutorService executorService;
    
    // 活跃连接管理
    private final ConcurrentHashMap<String, SseEmitter> activeConnections = new ConcurrentHashMap<>();

    public StreamingChatService(OpenAiChatModel chatModel, Counter aiCallCounter) {
        this.chatModel = chatModel;
        this.streamConnectionCounter = aiCallCounter; // 复用计数器
        this.executorService = Executors.newScheduledThreadPool(10);
        
        // 定期清理过期连接
        this.executorService.scheduleAtFixedRate(this::cleanupExpiredConnections, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * 流式对话
     *
     * @param prompt 用户提示词
     * @param connectionId 连接ID
     * @return SSE发射器
     */
    public SseEmitter streamChat(String prompt, String connectionId) {
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
        
        // 注册连接
        activeConnections.put(connectionId, emitter);
        streamConnectionCounter.increment();
        
        // 设置连接事件处理
        emitter.onCompletion(() -> {
            activeConnections.remove(connectionId);
            System.out.println("SSE连接完成: " + connectionId);
        });
        
        emitter.onTimeout(() -> {
            activeConnections.remove(connectionId);
            System.out.println("SSE连接超时: " + connectionId);
        });
        
        emitter.onError((throwable) -> {
            activeConnections.remove(connectionId);
            System.err.println("SSE连接错误: " + connectionId + ", " + throwable.getMessage());
        });

        // 异步处理对话
        executorService.submit(() -> processStreamingChat(emitter, prompt, connectionId));
        
        return emitter;
    }

    /**
     * 处理流式对话
     */
    private void processStreamingChat(SseEmitter emitter, String prompt, String connectionId) {
        try {
            // 发送开始事件
            emitter.send(SseEmitter.event()
                .name("start")
                .data(Map.of(
                    "message", "开始处理对话",
                    "prompt", prompt,
                    "timestamp", System.currentTimeMillis()
                )));

            // 模拟流式响应（实际应使用OpenAI的流式API）
            String response = simulateStreamingResponse(emitter, prompt);
            
            // 发送完成事件
            emitter.send(SseEmitter.event()
                .name("complete")
                .data(Map.of(
                    "message", "对话完成",
                    "fullResponse", response,
                    "timestamp", System.currentTimeMillis()
                )));
            
            emitter.complete();
            
        } catch (Exception e) {
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(Map.of(
                        "error", "处理失败: " + e.getMessage(),
                        "timestamp", System.currentTimeMillis()
                    )));
                emitter.completeWithError(e);
            } catch (IOException ioException) {
                System.err.println("发送错误事件失败: " + ioException.getMessage());
            }
        }
    }

    /**
     * 模拟流式响应
     * 注意：这里是模拟实现，实际应该使用OpenAI的流式API
     */
    private String simulateStreamingResponse(SseEmitter emitter, String prompt) throws IOException, InterruptedException {
        // 实际调用ChatModel获取完整响应
        ChatResponse chatResponse = chatModel.call(prompt);
        String fullContent = chatResponse.getResult().getOutput().getContent();
        
        // 将响应分块发送，模拟流式效果
        String[] words = fullContent.split(" ");
        StringBuilder currentChunk = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            currentChunk.append(words[i]).append(" ");
            
            // 每5个词或到达末尾时发送一个chunk
            if ((i + 1) % 5 == 0 || i == words.length - 1) {
                emitter.send(SseEmitter.event()
                    .name("chunk")
                    .data(Map.of(
                        "content", currentChunk.toString().trim(),
                        "index", i / 5,
                        "done", i == words.length - 1,
                        "timestamp", System.currentTimeMillis()
                    )));
                
                currentChunk.setLength(0);
                
                // 模拟网络延迟
                Thread.sleep(100);
            }
        }
        
        return fullContent;
    }

    /**
     * 获取活跃连接统计
     */
    public Map<String, Object> getConnectionStats() {
        return Map.of(
            "activeConnections", activeConnections.size(),
            "connectionIds", activeConnections.keySet(),
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 关闭指定连接
     */
    public boolean closeConnection(String connectionId) {
        SseEmitter emitter = activeConnections.remove(connectionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name("close")
                    .data(Map.of("message", "连接被服务端关闭")));
                emitter.complete();
                return true;
            } catch (IOException e) {
                System.err.println("关闭连接失败: " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * 向指定连接发送消息
     */
    public boolean sendMessage(String connectionId, String message) {
        SseEmitter emitter = activeConnections.get(connectionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name("message")
                    .data(Map.of(
                        "message", message,
                        "timestamp", System.currentTimeMillis()
                    )));
                return true;
            } catch (IOException e) {
                // 连接可能已断开，移除它
                activeConnections.remove(connectionId);
                System.err.println("发送消息失败，移除连接: " + connectionId);
            }
        }
        return false;
    }

    /**
     * 广播消息到所有连接
     */
    public int broadcast(String message) {
        int successCount = 0;
        for (Map.Entry<String, SseEmitter> entry : activeConnections.entrySet()) {
            if (sendMessage(entry.getKey(), message)) {
                successCount++;
            }
        }
        return successCount;
    }

    /**
     * 清理过期连接
     */
    private void cleanupExpiredConnections() {
        activeConnections.entrySet().removeIf(entry -> {
            try {
                // 发送心跳检测
                entry.getValue().send(SseEmitter.event()
                    .name("ping")
                    .data(Map.of("timestamp", System.currentTimeMillis())));
                return false;
            } catch (IOException e) {
                // 连接已断开
                System.out.println("清理断开的连接: " + entry.getKey());
                return true;
            }
        });
    }

    /**
     * 关闭服务时清理资源
     */
    public void shutdown() {
        // 关闭所有活跃连接
        activeConnections.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("shutdown")
                    .data(Map.of("message", "服务正在关闭")));
                emitter.complete();
            } catch (IOException e) {
                System.err.println("关闭连接失败: " + id);
            }
        });
        
        activeConnections.clear();
        executorService.shutdown();
    }
}
