package wr1ttenyu.study.springboot.demo.curd.component;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MyRedisTemplate {

    private Logger Log = LoggerFactory.getLogger(MyRedisTemplate.class);

    private StringRedisTemplate stringRedisTemplate;

    public MyRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public <T> T getObject(String key, Class<T> t) {
        String result = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isEmpty(result)) {
            return null;
        } else {
            try {
                T obj = JSONObject.parseObject(result, t);
                return obj;
            } catch (Exception e) {
                Log.error("redis-json2obj-反序列化失败, key:{}, value:{}, errorInfo:{}", key, result, e.getLocalizedMessage());
                return null;
            }
        }
    }

    public void saveObject(String key, Object obj) {
        stringRedisTemplate.opsForValue().set(key, JSONObject.toJSONString(obj));
    }
}
