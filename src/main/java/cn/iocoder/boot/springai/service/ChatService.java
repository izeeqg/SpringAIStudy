package cn.iocoder.boot.springai.service;

import com.github.benmanes.caffeine.cache.Cache;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 对话服务：封装对 Spring AI ChatModel 的简单访问。
 */
@Service
public class ChatService {

    private final OpenAiChatModel chatModel;
    private final DistributedCacheService distributedCacheService;
    private final Counter aiCallCounter;
    private final Counter aiCallSuccessCounter;
    private final Counter aiCallFailureCounter;
    private final Timer aiCallTimer;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;

    public ChatService(
            OpenAiChatModel chatModel,
            DistributedCacheService distributedCacheService,
            Counter aiCallCounter,
            Counter aiCallSuccessCounter,
            Counter aiCallFailureCounter,
            Timer aiCallTimer,
            Counter cacheHitCounter,
            Counter cacheMissCounter) {
        this.chatModel = chatModel;
        this.distributedCacheService = distributedCacheService;
        this.aiCallCounter = aiCallCounter;
        this.aiCallSuccessCounter = aiCallSuccessCounter;
        this.aiCallFailureCounter = aiCallFailureCounter;
        this.aiCallTimer = aiCallTimer;
        this.cacheHitCounter = cacheHitCounter;
        this.cacheMissCounter = cacheMissCounter;
    }

    /**
     * 简单补全接口，带限流/重试/超时/缓存/指标控制。
     *
     * @param prompt 用户提示词
     * @return 模型返回的内容（结构化）
     */
    @RateLimiter(name = "chat")
    @Retry(name = "chat")
    @TimeLimiter(name = "chat")
    public Map<String, Object> complete(String prompt) {
        // 增加调用计数
        aiCallCounter.increment();
        
        // 检查分布式缓存
        String cacheKey = "chat:" + prompt.hashCode();
        @SuppressWarnings("unchecked")
        Map<String, Object> cachedResult = distributedCacheService.get(cacheKey, Map.class);
        if (cachedResult != null) {
            cacheHitCounter.increment();
            // 标记为缓存结果
            return Map.of(
                "prompt", prompt,
                "content", cachedResult.get("content"),
                "model", cachedResult.get("model"),
                "usage", cachedResult.get("usage"),
                "cached", true
            );
        }
        
        cacheMissCounter.increment();
        
        // 计时开始
        Timer.Sample sample = Timer.start();
        
        try {
            ChatResponse response = chatModel.call(prompt);
            String content = response.getResult().getOutput().getContent();
            
            Map<String, Object> result = Map.of(
                "prompt", prompt,
                "content", content,
                "model", response.getMetadata().getModel(),
                "usage", response.getMetadata().getUsage(),
                "cached", false
            );
            
            // 缓存结果到分布式缓存
            distributedCacheService.put(cacheKey, result, java.time.Duration.ofMinutes(15));
            
            // 成功计数
            aiCallSuccessCounter.increment();
            
            return result;
            
        } catch (Exception e) {
            // 失败计数
            aiCallFailureCounter.increment();
            
            return Map.of(
                "prompt", prompt,
                "error", "调用失败: " + e.getMessage(),
                "cached", false
            );
        } finally {
            // 记录响应时间
            sample.stop(aiCallTimer);
        }
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        return distributedCacheService.getStats();
    }
}


