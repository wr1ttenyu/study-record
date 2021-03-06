package wr1ttenyu.study;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@MapperScan({"wr1ttenyu.study.springcloud.dao"})
@EnableAspectJAutoProxy(exposeProxy=true)
@EnableEurekaClient
@SpringBootApplication
public class UserProvider8001_App {

    public static void main(String[] args) {
        SpringApplication.run(UserProvider8001_App.class, args);
    }
}
