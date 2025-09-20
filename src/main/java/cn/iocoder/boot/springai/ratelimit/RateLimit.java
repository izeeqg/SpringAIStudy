package cn.iocoder.boot.springai.ratelimit;

import java.lang.annotation.*;

/**
 * 限流注解
 * 支持多维度限流策略
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流类型
     */
    RateLimitType type() default RateLimitType.API;

    /**
     * 限流key的SpEL表达式
     * 例如：#request.getRemoteAddr() 获取IP
     *      #authentication.name 获取用户名
     */
    String key() default "";

    /**
     * 时间窗口内允许的请求数量
     */
    int limit() default 10;

    /**
     * 时间窗口大小（秒）
     */
    int window() default 60;

    /**
     * 限流失败时的错误消息
     */
    String message() default "请求过于频繁，请稍后再试";

    /**
     * 是否启用
     */
    boolean enabled() default true;
}
