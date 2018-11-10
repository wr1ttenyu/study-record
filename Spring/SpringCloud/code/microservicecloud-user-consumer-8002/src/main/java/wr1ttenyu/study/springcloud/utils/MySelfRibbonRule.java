package wr1ttenyu.study.springcloud.utils;

import com.netflix.loadbalancer.IRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MySelfRibbonRule {

    @Bean
    public IRule myRule() {
        return new MyRibbonRule();
    }
}