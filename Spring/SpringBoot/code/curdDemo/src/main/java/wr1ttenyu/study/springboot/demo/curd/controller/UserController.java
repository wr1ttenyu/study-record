package wr1ttenyu.study.springboot.demo.curd.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import wr1ttenyu.study.springboot.demo.curd.bean.UUser;
import wr1ttenyu.study.springboot.demo.curd.dao.UserDao;
import wr1ttenyu.study.springboot.demo.curd.service.UserService;

import java.util.Date;

@Controller
@ResponseBody
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/user/{id}")
    public UUser get(@PathVariable(value = "id") String id) {
        return userService.getUserById(id);
    }

    @GetMapping("/user/delete/{id}")
    public String delete(@PathVariable(value = "id") String id) {
        userService.deleteUser(id);
        return "SUCCESS";
    }

    @GetMapping("/user/add/{name}")
    public UUser post(@PathVariable(value = "name") String name) {
        UUser user = new UUser();
        user.setName(name);
        Date gmtCreate = new Date();
        user.setGmtCreate(gmtCreate);
        user.setGmtModified(gmtCreate);
        userService.insertUser(user);
        return user;
    }

}
