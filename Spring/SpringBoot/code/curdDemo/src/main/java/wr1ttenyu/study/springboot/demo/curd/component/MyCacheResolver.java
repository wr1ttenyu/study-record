package wr1ttenyu.study.springboot.demo.curd.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.BasicOperation;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import wr1ttenyu.study.springboot.demo.curd.config.MyCacheAnnotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;

public class MyCacheResolver implements CacheResolver {

    private Logger Log = LoggerFactory.getLogger(MyCacheResolver.class);

    private MyRedisCacheManager redisCacheManager;

    private ConcurrentMapCacheManager concurrentMapCacheManager;

    public MyCacheResolver(MyRedisCacheManager redisCacheManager, ConcurrentMapCacheManager concurrentMapCacheManager) {
        this.redisCacheManager = redisCacheManager;
        this.concurrentMapCacheManager = concurrentMapCacheManager;
    }

    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
        Object[] args = context.getArgs();
        Method method = context.getMethod();
        BasicOperation operation = context.getOperation();
        Object target = context.getTarget();
        Class returnType = method.getReturnType();

        String cacheName;
        String key;
        String preFix;
        RedisdbNode redisDbNum = RedisdbNode.DB0;
        String cacheLevel;
        String localCacheName = null;


        Annotation[] annotations = method.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() == MyCacheAnnotation.class) {
                MyCacheAnnotation cacheInfo = (MyCacheAnnotation) annotation;
                cacheLevel = cacheInfo.cacheLevel();
                localCacheName = cacheInfo.localCacheName();
                key = cacheInfo.key();
                preFix = cacheInfo.preFix();
                redisDbNum = cacheInfo.redisDb();
                Log.info("key:{},value:{},cacheLevel:{},preFix:{},redisDbNum:{}", key, "123", cacheLevel, preFix, redisDbNum);
            }
        }

        Collection<Cache> caches = new HashSet<>();
        caches.add(redisCacheManager.getCache(redisDbNum, returnType));
        caches.add(concurrentMapCacheManager.getCache(localCacheName));
        return caches;
    }
}
