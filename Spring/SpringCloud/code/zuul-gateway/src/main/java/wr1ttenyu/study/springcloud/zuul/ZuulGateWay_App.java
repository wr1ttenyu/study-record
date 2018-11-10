package wr1ttenyu.study.springcloud.zuul;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

@EnableZuulProxy
@SpringBootApplication
public class ZuulGateWay_App {

    public static void main(String[] args) {
        SpringApplication.run(ZuulGateWay_App.class, args);
    }
}
