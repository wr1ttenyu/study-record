package wr1ttenyu.study.springcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import wr1ttenyu.study.springcloud.utils.MySelfRibbonRule;

@SpringBootApplication
@EnableEurekaClient
@RibbonClient(name = "MICROSERVICECLOUD-USER-PROVIDER", configuration = MySelfRibbonRule.class)
public class UserConsumer8002_App {

    public static void main(String[] args) {
        SpringApplication.run(UserConsumer8002_App.class, args);
    }
}
