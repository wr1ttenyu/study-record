package wr1ttenyu.study.dubbo.service.impl;

import org.springframework.stereotype.Service;
import wr1ttenyu.study.dubbo.service.api.DemoService2;

@Service
public class Demo2ServiceImpl implements DemoService2 {

    @Override
    public String sayHello(String name) {
        return "hello," + name;
    }
}
