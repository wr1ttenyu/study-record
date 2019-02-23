package wr1ttenyu.study.dubbo.service.consumer;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import wr1ttenyu.study.dubbo.service.api.DemoService;

public class Consumer {
    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new
                String[]{"dubbo-consumer.xml"});
        context.start();
        // Obtaining a remote service proxy
        DemoService demoService = (DemoService) context.getBean("demoService");
        // Executing remote methods
        String hello = demoService.sayHello("world");
        // Display the call result
        System.out.println(hello);
    }
}
