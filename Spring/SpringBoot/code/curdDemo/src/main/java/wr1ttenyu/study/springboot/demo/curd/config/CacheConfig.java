package wr1ttenyu.study.springboot.demo.curd.config;

import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import wr1ttenyu.study.springboot.demo.curd.component.MyCacheResolver;
import wr1ttenyu.study.springboot.demo.curd.component.MyRedisCacheManager;
import wr1ttenyu.study.springboot.demo.curd.component.MyRedisCacheWriter;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Primary
    @Bean("redisCache")
    public MyRedisCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        MyRedisCacheManager myRedisCacheManager = new MyRedisCacheManager(
                new MyRedisCacheWriter(redisConnectionFactory), determineConfiguration());
        return myRedisCacheManager;
    }

    @Bean("localCache")
    public ConcurrentMapCacheManager concurrentMapCacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        return cacheManager;
    }

    @Bean("cacheResolver")
    public MyCacheResolver myCacheResolver(MyRedisCacheManager redisCacheManager, ConcurrentMapCacheManager concurrentMapCacheManager) {
        return new MyCacheResolver(redisCacheManager, concurrentMapCacheManager);
    }

    /*@Bean
    public CacheInterceptor cacheInterceptor(){
        CacheInterceptor cacheInterceptor = new CacheInterceptor();
        cacheInterceptor.setCacheResolver();
        return cacheInterceptor;
    }*/

    private org.springframework.data.redis.cache.RedisCacheConfiguration determineConfiguration() {
        org.springframework.data.redis.cache.RedisCacheConfiguration config = org.springframework.data.redis.cache.RedisCacheConfiguration
                .defaultCacheConfig();
        config = config.serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new Jackson2JsonRedisSerializer(Object.class)));
        config = config.entryTtl(Duration.ZERO);
        config = config.disableKeyPrefix();
        return config;
    }
}
