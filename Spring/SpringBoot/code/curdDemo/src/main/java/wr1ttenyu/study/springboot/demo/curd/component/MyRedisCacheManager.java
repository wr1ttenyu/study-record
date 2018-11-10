package wr1ttenyu.study.springboot.demo.curd.component;

import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.util.LinkedHashMap;
import java.util.Map;

public class MyRedisCacheManager extends RedisCacheManager {

    private final RedisCacheWriter cacheWriter;
    private final RedisCacheConfiguration defaultCacheConfig;
    private final Map<String, RedisCacheConfiguration> initialCacheConfiguration;
    private final boolean allowInFlightCacheCreation;

    public MyRedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration) {
        super(cacheWriter, defaultCacheConfiguration, true);
        this.cacheWriter = cacheWriter;
        this.defaultCacheConfig = defaultCacheConfiguration;
        this.initialCacheConfiguration = new LinkedHashMap<>();
        this.allowInFlightCacheCreation = true;
    }

    public Cache getCache(RedisdbNode redisDbNum, Class returnType) {
        return new MyRedisCache(redisDbNum, cacheWriter, defaultCacheConfig, returnType);
    }

    @Override
    public RedisCache getMissingCache(String name) {
        return allowInFlightCacheCreation ? createRedisCache(name, defaultCacheConfig) : null;
    }


}
