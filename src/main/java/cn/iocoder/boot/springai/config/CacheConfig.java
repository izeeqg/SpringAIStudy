package cn.iocoder.boot.springai.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置
 * 使用Caffeine作为本地缓存，提升AI调用性能
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 配置Caffeine缓存管理器
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // 配置默认缓存策略
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .recordStats()
        );
        
        // 设置缓存名称
        cacheManager.setCacheNames("chat-responses", "rag-results", "function-results");
        
        return cacheManager;
    }

    /**
     * 专门用于聊天响应的缓存配置
     */
    @Bean("chatCache")
    public com.github.benmanes.caffeine.cache.Cache<String, Object> chatCache() {
        return Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .recordStats()
            .build();
    }

    /**
     * 专门用于RAG结果的缓存配置
     */
    @Bean("ragCache")
    public com.github.benmanes.caffeine.cache.Cache<String, Object> ragCache() {
        return Caffeine.newBuilder()
            .maximumSize(200)
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .recordStats()
            .build();
    }

    /**
     * 通用本地缓存Bean（用于分布式缓存服务的L1缓存）
     */
    @Bean("localCache")
    public com.github.benmanes.caffeine.cache.Cache<String, Object> localCache() {
        return Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(20, TimeUnit.MINUTES)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .recordStats()
            .build();
    }
}
