package wr1ttenyu.study.spring.boot.starter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnWebApplication
@EnableConfigurationProperties(HelloAutoConfigProperties.class)
public class HelloAutoConfiguration {

    @Autowired
    private HelloAutoConfigProperties helloAutoConfigProperties;

    @Bean
    public HelloAutoConfigService helloAutoConfigService() {
        HelloAutoConfigService helloAutoConfigService = new HelloAutoConfigService();
        helloAutoConfigService.setHelloAutoConfigProperties(helloAutoConfigProperties);
        return helloAutoConfigService;
    }
}
