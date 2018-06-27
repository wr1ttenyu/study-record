package wr1ttenyu.study.spring.annotation.springmvc.service;

import org.springframework.stereotype.Service;
import wr1ttenyu.study.spring.annotation.springmvc.service.impl.IHelloService;

@Service
public class HelloService implements IHelloService {

    @Override
    public String hello() {
        return "hello";
    }
}
