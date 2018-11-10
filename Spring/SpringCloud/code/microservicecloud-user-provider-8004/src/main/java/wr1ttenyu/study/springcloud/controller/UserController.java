package wr1ttenyu.study.springcloud.controller;

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
    public UUser get(@PathVariable("id") String id) {
        return userService.getUserById(id);
    }


}
