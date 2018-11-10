package wr1ttenyu.study.springcloud.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import wr1ttenyu.study.springcloud.entity.UUser;
import wr1ttenyu.study.springcloud.fegin.service.UserFeginService;

@RestController
public class UserController_Consumer {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private UserFeginService userFeginService;

    private final String USER_SERVICE_NAME = "http://MICROSERVICECLOUD-USER-PROVIDER";

    @GetMapping("/user/get/{id}")
    public UUser get(@PathVariable String id) {
        UUser user = restTemplate.getForObject(USER_SERVICE_NAME + "/user/get/" + id, UUser.class, id);
        return user;
    }

    @GetMapping("/user/service/get/{id}")
    public UUser serviceGet(@PathVariable String id) {
        UUser user = userFeginService.getUserById(id);
        return user;
    }

}
