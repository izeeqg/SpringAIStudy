package cn.iocoder.boot.springai.service;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 分布式缓存服务
 * 实现L1(本地缓存) + L2(Redis分布式缓存)的多级缓存架构
 */
@Service
public class DistributedCacheService {

    private final Cache<String, Object> localCache;
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    public DistributedCacheService(@Qualifier("localCache") Cache<String, Object> localCache) {
        this.localCache = localCache;
    }

    /**
     * 获取缓存值
     * 优先从L1本地缓存获取，未命中则从L2 Redis获取
     *
     * @param key 缓存键
     * @param type 值类型
     * @return 缓存值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        // L1: 本地缓存
        Object value = localCache.getIfPresent(key);
        if (value != null) {
            return (T) value;
        }

        // L2: Redis缓存
        if (redisTemplate != null) {
            try {
                value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    // 回写到L1缓存
                    localCache.put(key, value);
                    return (T) value;
                }
            } catch (Exception e) {
                // Redis异常时降级到本地缓存
                System.err.println("Redis缓存异常，降级到本地缓存: " + e.getMessage());
            }
        }

        return null;
    }

    /**
     * 设置缓存值
     * 同时写入L1本地缓存和L2 Redis缓存
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 过期时间
     */
    public void put(String key, Object value, Duration ttl) {
        // L1: 本地缓存
        localCache.put(key, value);

        // L2: Redis缓存
        if (redisTemplate != null) {
            try {
                if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                    redisTemplate.opsForValue().set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
                } else {
                    redisTemplate.opsForValue().set(key, value);
                }
            } catch (Exception e) {
                // Redis异常时仅使用本地缓存
                System.err.println("Redis缓存写入异常，仅使用本地缓存: " + e.getMessage());
            }
        }
    }

    /**
     * 设置缓存值（默认TTL）
     */
    public void put(String key, Object value) {
        put(key, value, Duration.ofMinutes(30));
    }

    /**
     * 删除缓存
     * 同时从L1和L2删除
     *
     * @param key 缓存键
     */
    public void evict(String key) {
        // L1: 本地缓存
        localCache.invalidate(key);

        // L2: Redis缓存
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                System.err.println("Redis缓存删除异常: " + e.getMessage());
            }
        }
    }

    /**
     * 批量删除缓存
     */
    public void evictAll(String... keys) {
        for (String key : keys) {
            evict(key);
        }
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        // L1: 本地缓存
        localCache.invalidateAll();

        // L2: Redis缓存 - 不建议在生产环境使用
        if (redisTemplate != null) {
            try {
                // 这里只清除特定前缀的key，避免影响其他应用
                // redisTemplate.getConnectionFactory().getConnection().flushDb();
                System.out.println("Redis缓存清理需要手动执行，避免影响其他应用");
            } catch (Exception e) {
                System.err.println("Redis缓存清理异常: " + e.getMessage());
            }
        }
    }

    /**
     * 检查缓存是否存在
     */
    public boolean exists(String key) {
        // 先检查L1
        if (localCache.getIfPresent(key) != null) {
            return true;
        }

        // 再检查L2
        if (redisTemplate != null) {
            try {
                return Boolean.TRUE.equals(redisTemplate.hasKey(key));
            } catch (Exception e) {
                System.err.println("Redis缓存检查异常: " + e.getMessage());
            }
        }

        return false;
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = Map.of(
            "localCache", Map.of(
                "size", localCache.estimatedSize(),
                "hitCount", localCache.stats().hitCount(),
                "missCount", localCache.stats().missCount(),
                "hitRate", localCache.stats().hitRate(),
                "evictionCount", localCache.stats().evictionCount()
            ),
            "redisAvailable", redisTemplate != null
        );

        if (redisTemplate != null) {
            try {
                // 获取Redis连接信息
                return Map.of(
                    "localCache", stats.get("localCache"),
                    "redisAvailable", true,
                    "redisConnected", redisTemplate.getConnectionFactory() != null
                );
            } catch (Exception e) {
                return Map.of(
                    "localCache", stats.get("localCache"),
                    "redisAvailable", false,
                    "redisError", e.getMessage()
                );
            }
        }

        return stats;
    }

    /**
     * 获取或计算缓存值
     * 如果缓存不存在，则执行计算函数并缓存结果
     */
    public <T> T getOrCompute(String key, Class<T> type, java.util.function.Supplier<T> supplier, Duration ttl) {
        T value = get(key, type);
        if (value == null) {
            value = supplier.get();
            if (value != null) {
                put(key, value, ttl);
            }
        }
        return value;
    }

    /**
     * 预热缓存
     * 批量加载数据到缓存
     */
    public void warmUp(Map<String, Object> data, Duration ttl) {
        data.forEach((key, value) -> put(key, value, ttl));
        System.out.println("缓存预热完成，加载了 " + data.size() + " 条数据");
    }
}
