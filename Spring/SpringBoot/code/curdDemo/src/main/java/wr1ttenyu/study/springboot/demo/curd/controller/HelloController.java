package wr1ttenyu.study.springboot.demo.curd.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import wr1ttenyu.study.spring.boot.starter.HelloAutoConfigService;
import wr1ttenyu.study.springboot.demo.curd.bean.UUser;
import wr1ttenyu.study.springboot.demo.curd.bean.User;
import wr1ttenyu.study.springboot.demo.curd.service.UserService;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/hello")
public class HelloController {

    Logger logger = LoggerFactory.getLogger(HelloController.class);

    @Autowired
    private HelloAutoConfigService helloAutoConfigService;

    @Autowired
    private UserService userService;

    @GetMapping("/hello/wr1ttenyu")
    @ResponseBody
    public String hello() {
        logger.info("Hello World");
        return "hello";
    }

    @GetMapping("/success")
    public String success(HttpServletRequest request) {
        request.setAttribute("hello", "param1");

        List<String> userNames = new ArrayList<String>(5);
        userNames.add("wr1ttenyu");
        userNames.add("wwy");
        userNames.add("zhaowl");
        userNames.add("lcr");

        request.setAttribute("hello", "hello");
        request.setAttribute("userNames", userNames);

        return "success";
    }

    @GetMapping("/helloAutoConfigService")
    @ResponseBody
    public String helloAutoConfigService() {
        return helloAutoConfigService.sayHello("wr1ttenyu");
    }

    @GetMapping("/user/{id}")
    @ResponseBody
    public UUser get(@PathVariable(value = "id") String id) {
        return userService.getUserById(id);
    }

    @GetMapping("/user/add/{name}")
    @ResponseBody
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
