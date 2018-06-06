package wr1ttenyu.study.spring.annotation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Controller;

import wr1ttenyu.study.spring.annotation.bean.Person;
import wr1ttenyu.study.spring.annotation.service.BookService;

// 配置类==配置文件
@Configuration // 告诉Spring这是一个配置类
// compoentScan是扫描符合规则的包下面的所有类
@ComponentScan(value = "wr1ttenyu.study.spring.annotation", useDefaultFilters = false, includeFilters = {
        @Filter(type = FilterType.ANNOTATION, classes = { Controller.class }),
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = { BookService.class }),
        @Filter(type = FilterType.CUSTOM, classes = { MyCustomTypeFilter.class }) })
// ComponentScan.Filter Type
// FilterType.ANNOTATION 使用注解
// FilterType.ASSIGNABLE_TYPE 使用自定义类型
// FilterType.ASPECTJ 使用切面
// FilterType.REGEX 正则表达式
// FilterType.CUSTOM 自定义过滤器
public class MainConfig {

    @Bean
    public Person person() {
        Person person = new Person();
        person.setName("wr1ttenyu");
        person.setAge(26);
        return person;
    }
}