package wr1ttenyu.study.springboot.demo.curd.config;

import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.annotation.AliasFor;
import wr1ttenyu.study.springboot.demo.curd.component.RedisdbNode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.Callable;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Cacheable(cacheResolver = "cacheResolver")
public @interface MyCacheAnnotation {

    @AliasFor("cacheNames")
    String[] value() default {};

    @AliasFor("value")
    String[] cacheNames() default {};

    /**
     * 本地缓存名
     */
    String localCacheName() default "";

    String key() default "";

    String keyGenerator() default "";

    String cacheManager() default "";

    String condition() default "";

    String unless() default "";

    boolean sync() default false;

    String preFix() default "";

    RedisdbNode redisDb() default RedisdbNode.DB0;

    String cacheLevel() default "";

}
