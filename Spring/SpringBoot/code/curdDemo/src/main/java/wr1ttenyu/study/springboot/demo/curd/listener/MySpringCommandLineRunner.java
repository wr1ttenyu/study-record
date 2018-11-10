package wr1ttenyu.study.springboot.demo.curd.listener;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class MySpringCommandLineRunner implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        System.out.println("MySpringCommandLineRunner... run ..." + Arrays.asList(args));
    }
}
