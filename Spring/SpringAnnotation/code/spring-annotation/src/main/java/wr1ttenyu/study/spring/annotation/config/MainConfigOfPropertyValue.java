package wr1ttenyu.study.spring.annotation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import wr1ttenyu.study.spring.annotation.bean.Person;

/**
 * @author wr1ttenyu
 */
@Configuration
// 导入外部资源文件，保存kv键值对到运行环境的变量池中
@PropertySource(value= {"classpath:/person.properties"})
public class MainConfigOfPropertyValue {

    @Bean
    public Person person() {
        return new Person();
    }
    
}
