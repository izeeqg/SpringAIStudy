package cn.iocoder.boot.springai.ratelimit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流服务
 * 支持基于Redis的分布式限流和本地限流降级
 */
@Service
public class RateLimitService {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    // 本地限流降级存储
    private final ConcurrentHashMap<String, WindowCounter> localCounters = new ConcurrentHashMap<>();

    // Lua脚本：滑动窗口限流
    private static final String RATE_LIMIT_SCRIPT = """
        local key = KEYS[1]
        local window = tonumber(ARGV[1])
        local limit = tonumber(ARGV[2])
        local current = tonumber(ARGV[3])
        
        -- 清理过期的记录
        redis.call('zremrangebyscore', key, '-inf', current - window * 1000)
        
        -- 获取当前窗口内的请求数
        local count = redis.call('zcard', key)
        
        if count < limit then
            -- 添加当前请求
            redis.call('zadd', key, current, current)
            redis.call('expire', key, window)
            return {1, limit - count - 1}
        else
            return {0, 0}
        end
        """;

    /**
     * 检查是否允许请求
     *
     * @param key 限流键
     * @param limit 限制数量
     * @param windowSeconds 时间窗口（秒）
     * @return RateLimitResult
     */
    public RateLimitResult isAllowed(String key, int limit, int windowSeconds) {
        if (redisTemplate != null) {
            return checkWithRedis(key, limit, windowSeconds);
        } else {
            return checkWithLocal(key, limit, windowSeconds);
        }
    }

    /**
     * 基于Redis的分布式限流
     */
    private RateLimitResult checkWithRedis(String key, int limit, int windowSeconds) {
        try {
            RedisScript<java.util.List> script = RedisScript.of(RATE_LIMIT_SCRIPT, java.util.List.class);
            long current = System.currentTimeMillis();
            
            @SuppressWarnings("unchecked")
            java.util.List<Long> result = redisTemplate.execute(
                script,
                Collections.singletonList("rate_limit:" + key),
                windowSeconds,
                limit,
                current
            );
            
            boolean allowed = result.get(0) == 1;
            long remaining = result.get(1);
            
            return new RateLimitResult(allowed, remaining, limit);
            
        } catch (Exception e) {
            System.err.println("Redis限流异常，降级到本地限流: " + e.getMessage());
            return checkWithLocal(key, limit, windowSeconds);
        }
    }

    /**
     * 本地限流降级
     */
    private RateLimitResult checkWithLocal(String key, int limit, int windowSeconds) {
        long now = System.currentTimeMillis();
        long windowMs = windowSeconds * 1000L;
        
        WindowCounter counter = localCounters.computeIfAbsent(key, k -> new WindowCounter());
        
        synchronized (counter) {
            // 清理过期窗口
            counter.cleanup(now, windowMs);
            
            // 检查当前窗口是否超限
            if (counter.getCount() >= limit) {
                return new RateLimitResult(false, 0, limit);
            }
            
            // 添加当前请求
            counter.addRequest(now);
            
            return new RateLimitResult(true, limit - counter.getCount(), limit);
        }
    }

    /**
     * 获取限流统计信息
     */
    public RateLimitStats getStats(String key) {
        if (redisTemplate != null) {
            try {
                String redisKey = "rate_limit:" + key;
                Long count = redisTemplate.opsForZSet().zCard(redisKey);
                Long ttl = redisTemplate.getExpire(redisKey);
                
                return new RateLimitStats(key, count != null ? count : 0, ttl != null ? ttl : -1);
            } catch (Exception e) {
                System.err.println("获取Redis限流统计异常: " + e.getMessage());
            }
        }
        
        WindowCounter counter = localCounters.get(key);
        if (counter != null) {
            return new RateLimitStats(key, counter.getCount(), -1);
        }
        
        return new RateLimitStats(key, 0, -1);
    }

    /**
     * 清理过期的本地计数器
     */
    public void cleanupLocalCounters() {
        long now = System.currentTimeMillis();
        localCounters.entrySet().removeIf(entry -> {
            WindowCounter counter = entry.getValue();
            synchronized (counter) {
                counter.cleanup(now, 300000); // 5分钟过期
                return counter.isEmpty();
            }
        });
    }

    /**
     * 限流结果
     */
    public static class RateLimitResult {
        private final boolean allowed;
        private final long remaining;
        private final long limit;

        public RateLimitResult(boolean allowed, long remaining, long limit) {
            this.allowed = allowed;
            this.remaining = remaining;
            this.limit = limit;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public long getRemaining() {
            return remaining;
        }

        public long getLimit() {
            return limit;
        }
    }

    /**
     * 限流统计信息
     */
    public static class RateLimitStats {
        private final String key;
        private final long currentCount;
        private final long ttl;

        public RateLimitStats(String key, long currentCount, long ttl) {
            this.key = key;
            this.currentCount = currentCount;
            this.ttl = ttl;
        }

        public String getKey() {
            return key;
        }

        public long getCurrentCount() {
            return currentCount;
        }

        public long getTtl() {
            return ttl;
        }
    }

    /**
     * 滑动窗口计数器（本地限流用）
     */
    private static class WindowCounter {
        private final ConcurrentHashMap<Long, AtomicInteger> windows = new ConcurrentHashMap<>();

        public void addRequest(long timestamp) {
            long windowStart = timestamp / 1000 * 1000; // 按秒对齐
            windows.computeIfAbsent(windowStart, k -> new AtomicInteger(0)).incrementAndGet();
        }

        public int getCount() {
            return windows.values().stream().mapToInt(AtomicInteger::get).sum();
        }

        public void cleanup(long now, long windowMs) {
            long cutoff = now - windowMs;
            windows.entrySet().removeIf(entry -> entry.getKey() < cutoff);
        }

        public boolean isEmpty() {
            return windows.isEmpty();
        }
    }
}
