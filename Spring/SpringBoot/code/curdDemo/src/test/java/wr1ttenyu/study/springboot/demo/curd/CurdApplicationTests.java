package wr1ttenyu.study.springboot.demo.curd;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import wr1ttenyu.study.springboot.demo.curd.bean.UUser;
import wr1ttenyu.study.springboot.demo.curd.component.MyRedisTemplate;
import wr1ttenyu.study.springboot.demo.curd.service.UserService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CurdApplicationTests {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MyRedisTemplate myRedisTemplate;

    @Autowired
    private StringRedisTemplate redisStringTemplate;

    @Test
    public void contextLoads() {
    }

    @Test
    public void cacheConcurrentHashMapTest() {
        for (int i = 0; i < 5; i++) {
            UUser user = userService.getUserById("1");
            System.out.println(user);
        }
    }

    /**
     *  redis 五大数据类型： string  list  set  hash  zset
     */
    @Test
    public void cacheRedisTest() {
        /*redisStringTemplate.opsForValue()
        redisStringTemplate.opsForList()
        redisStringTemplate.opsForSet()
        redisStringTemplate.opsForHash()
        redisStringTemplate.opsForZSet()*/
        /*redisStringTemplate.opsForValue().set("test01", "haha");
        System.out.println(redisStringTemplate.opsForValue().get("test01"));*/

        UUser user = userService.getUserById("1");
        /*myRedisTemplate.saveObject("user_1", user);
        UUser user_1 = myRedisTemplate.getObject("user_1", UUser.class);
        System.out.println(user_1);*/
        System.out.println(user);
    }



}
