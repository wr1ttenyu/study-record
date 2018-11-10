package wr1ttenyu.study.springcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;

@SpringBootApplication
@EnableHystrixDashboard
public class HystrixDashboard_App {

    public static void main(String[] args) {
        SpringApplication.run(HystrixDashboard_App.class, args);
    }
}
