package wr1ttenyu.study.dubbo.service.impl;

import org.springframework.stereotype.Service;
import wr1ttenyu.study.dubbo.service.api.DemoService;

@Service
public class DemoServiceImpl implements DemoService {

    @Override
    public String sayHello(String name) {
        return "hello," + name;
    }
}
