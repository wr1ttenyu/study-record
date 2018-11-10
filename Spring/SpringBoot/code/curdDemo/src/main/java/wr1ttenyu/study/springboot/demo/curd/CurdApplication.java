package wr1ttenyu.study.springboot.demo.curd;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.servlet.View;
import org.thymeleaf.spring5.view.ThymeleafView;

@MapperScan({"wr1ttenyu.study.springboot.demo.curd.dao"})
@EnableAspectJAutoProxy(exposeProxy=true)
@SpringBootApplication
public class CurdApplication {

    public static void main(String[] args) {
        SpringApplication.run(CurdApplication.class, args);
    }
}
