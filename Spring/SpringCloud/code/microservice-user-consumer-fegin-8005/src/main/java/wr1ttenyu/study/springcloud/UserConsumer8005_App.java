package wr1ttenyu.study.springcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableEurekaClient
@EnableFeignClients(basePackages="wr1ttenyu.study.springcloud")
@ComponentScan("wr1ttenyu.study.springcloud")
public class UserConsumer8005_App {

    public static void main(String[] args) {
        SpringApplication.run(UserConsumer8005_App.class, args);
    }
}
