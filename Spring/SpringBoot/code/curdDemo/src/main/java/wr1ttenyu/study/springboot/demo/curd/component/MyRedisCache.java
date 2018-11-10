package wr1ttenyu.study.springboot.demo.curd.component;

import com.alibaba.fastjson.JSONObject;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.util.Assert;

public class MyRedisCache extends RedisCache {

    /*private final String name;*/
    private final RedisdbNode redisdbNode;
    private final RedisCacheWriter cacheWriter;
    private final RedisCacheConfiguration cacheConfig;
    private final ConversionService conversionService;
    private Class returnType;

    public MyRedisCache(/*String name, */RedisdbNode redisdbNode, RedisCacheWriter cacheWriter, RedisCacheConfiguration cacheConfig, Class returnType) {
        super("123", cacheWriter, cacheConfig);
        /*this.name = name;*/
        this.redisdbNode = redisdbNode;
        this.cacheWriter = cacheWriter;
        this.cacheConfig = cacheConfig;
        this.conversionService = cacheConfig.getConversionService();
        this.returnType = returnType;
    }

    @Override
    public Object lookup(Object key) {

        byte[] value = cacheWriter.get(String.valueOf(redisdbNode.getNode()), String.valueOf(key).getBytes());

        if (value == null) {
            return null;
        }

        return JSONObject.parseObject(value, returnType);
    }


}
