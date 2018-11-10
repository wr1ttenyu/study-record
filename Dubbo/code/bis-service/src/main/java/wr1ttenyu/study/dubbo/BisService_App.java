package wr1ttenyu.study.dubbo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan({"wr1ttenyu.study.dubbo.dao"})
@SpringBootApplication
public class BisService_App {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(BisService_App.class, args);
    }

}