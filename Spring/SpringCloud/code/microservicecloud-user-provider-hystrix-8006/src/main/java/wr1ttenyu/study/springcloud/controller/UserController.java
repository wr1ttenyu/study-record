package wr1ttenyu.study.springcloud.controller;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import wr1ttenyu.study.springcloud.entity.UUser;
import wr1ttenyu.study.springcloud.service.UserService;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/user/add")
    public boolean add(UUser user) {
        userService.insertUser(user);
        return true;
    }

    @GetMapping("/user/get/{id}")
    @HystrixCommand(fallbackMethod = "breakGetUser")
    public UUser get(@PathVariable("id") String id) {
        UUser user = userService.getUserById(id);
        if (user == null) {
            throw new RuntimeException("改id:" + id + ",没有对应数据!");
        }
        return user;
    }

    public UUser breakGetUser(@PathVariable("id") String id) {
        UUser user = new UUser();
        user.setId(id);
        user.setName("stranger, no user with id:" + id);
        return user;
    }
}
