package wr1ttenyu.study.spring.annotation.springmvc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import wr1ttenyu.study.spring.annotation.springmvc.service.impl.IHelloService;

@Controller
public class HelloController {

    @Autowired
    private IHelloService helloService;

    @ResponseBody
    @GetMapping("/hello")
    public String hello() {
        return helloService.hello();
    }
}
