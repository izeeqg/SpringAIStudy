package cn.iocoder.boot.springai.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 指标配置
 * 定义业务相关的自定义指标
 */
@Configuration
public class MetricsConfig {

    /**
     * AI调用次数计数器
     */
    @Bean
    public Counter aiCallCounter(MeterRegistry meterRegistry) {
        return Counter.builder("ai.calls.total")
            .description("Total AI API calls")
            .tag("application", "springai-app")
            .register(meterRegistry);
    }

    /**
     * AI调用成功次数计数器
     */
    @Bean
    public Counter aiCallSuccessCounter(MeterRegistry meterRegistry) {
        return Counter.builder("ai.calls.success")
            .description("Successful AI API calls")
            .tag("application", "springai-app")
            .register(meterRegistry);
    }

    /**
     * AI调用失败次数计数器
     */
    @Bean
    public Counter aiCallFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("ai.calls.failure")
            .description("Failed AI API calls")
            .tag("application", "springai-app")
            .register(meterRegistry);
    }

    /**
     * AI调用响应时间计时器
     */
    @Bean
    public Timer aiCallTimer(MeterRegistry meterRegistry) {
        return Timer.builder("ai.calls.duration")
            .description("AI API call duration")
            .tag("application", "springai-app")
            .register(meterRegistry);
    }

    /**
     * RAG查询计数器
     */
    @Bean
    public Counter ragQueryCounter(MeterRegistry meterRegistry) {
        return Counter.builder("rag.queries.total")
            .description("Total RAG queries")
            .tag("application", "springai-app")
            .register(meterRegistry);
    }

    /**
     * 函数调用计数器
     */
    @Bean
    public Counter functionCallCounter(MeterRegistry meterRegistry) {
        return Counter.builder("function.calls.total")
            .description("Total function calls")
            .tag("application", "springai-app")
            .register(meterRegistry);
    }

    /**
     * 缓存命中率计数器
     */
    @Bean
    public Counter cacheHitCounter(MeterRegistry meterRegistry) {
        return Counter.builder("cache.hits")
            .description("Cache hits")
            .tag("application", "springai-app")
            .register(meterRegistry);
    }

    /**
     * 缓存未命中计数器
     */
    @Bean
    public Counter cacheMissCounter(MeterRegistry meterRegistry) {
        return Counter.builder("cache.misses")
            .description("Cache misses")
            .tag("application", "springai-app")
            .register(meterRegistry);
    }
}
