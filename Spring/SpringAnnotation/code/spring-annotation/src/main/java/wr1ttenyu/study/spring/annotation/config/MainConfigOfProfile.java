package wr1ttenyu.study.spring.annotation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringValueResolver;

import wr1ttenyu.study.spring.annotation.bean.ProfileTest;

/**
 * Profile:
 *  是spring为我们提供的可以根据当前环境来动态切换配置信息等一系列组件的功能
 * @Profile:
 *  
 *  
 * 1). 与@Bean配合，只有这个环境被激活的时候才能被注册到容器中
 * 2). 与@Configuration配合，只有这个环境被激活的时候，整个配置类才能生效
 * 
 * @author wr1ttenyu
 *
 */
@Profile("Dev")
@Import(MainConfig.class)
@Configuration
public class MainConfigOfProfile implements EmbeddedValueResolverAware {

    @Value("${name}")
    private String name;
    
    private StringValueResolver resolver;
    
    @Bean
    public ProfileTest profileNoProfile(@Value("${sex}") String sex) {
        ProfileTest profileTest = new ProfileTest();
        profileTest.setName(name);
        profileTest.setAge(resolver.resolveStringValue("${age}"));
        profileTest.setSex(sex);
        return profileTest;
    }
    
    @Profile("Dev")
    @Bean
    public ProfileTest profileDev(@Value("${sex}") String sex) {
        ProfileTest profileTest = new ProfileTest();
        profileTest.setName(name);
        profileTest.setAge(resolver.resolveStringValue("${age}"));
        profileTest.setSex(sex);
        return profileTest;
    }
    
    @Profile("Test")
    @Bean
    public ProfileTest profileTest(@Value("${sex}") String sex) {
        ProfileTest profileTest = new ProfileTest();
        profileTest.setName(name);
        profileTest.setAge(resolver.resolveStringValue("${age}"));
        profileTest.setSex(sex);
        return profileTest;
    }
    
    @Profile("Pro")
    @Bean
    public ProfileTest profilePro(@Value("${sex}") String sex) {
        ProfileTest profileTest = new ProfileTest();
        profileTest.setName(name);
        profileTest.setAge(resolver.resolveStringValue("${age}"));
        profileTest.setSex(sex);
        return profileTest;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.resolver = resolver;
    }
    
}
