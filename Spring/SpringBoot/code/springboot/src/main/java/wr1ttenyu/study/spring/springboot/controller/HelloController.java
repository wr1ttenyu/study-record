package wr1ttenyu.study.spring.springboot.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HelloController {

    Logger logger = LoggerFactory.getLogger(HelloController.class);

    @GetMapping
    @ResponseBody
    public String hello() {
        logger.info("Hello World");
        return "hello";
    }
}
