package cn.iocoder.boot.springai.ratelimit;

/**
 * 限流类型枚举
 */
public enum RateLimitType {
    /**
     * 用户级限流
     */
    USER,
    
    /**
     * IP级限流
     */
    IP,
    
    /**
     * API级限流
     */
    API,
    
    /**
     * 全局限流
     */
    GLOBAL
}
