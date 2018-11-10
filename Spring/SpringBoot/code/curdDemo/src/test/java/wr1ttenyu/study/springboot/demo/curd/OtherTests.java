package wr1ttenyu.study.springboot.demo.curd;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.test.context.junit4.SpringRunner;
import wr1ttenyu.study.springboot.demo.curd.bean.UUser;
import wr1ttenyu.study.springboot.demo.curd.service.UserService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@RunWith(SpringRunner.class)
@SpringBootTest
public class OtherTests {

    @Autowired
    private UserService userService;

    @Test
    public void cacheConcurrentHashMapTest() {
        for (int i = 0; i < 5; i++) {
            UUser user = userService.getUserById("1");
            System.out.println(user);
        }
    }

    @Test
    public void springTranstractionTest() {
        UUser userById = new UUser();
        userById.setId("1");
        userById.setName("change");
        userService.updateUser(userById);
    }


    @Test
    public void springTranstractionTest2() {
        userService.testSprintTran();
    }

    public static void main(String[] args) {
        ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>(16);
        cacheMap.get(null);
    }
}
