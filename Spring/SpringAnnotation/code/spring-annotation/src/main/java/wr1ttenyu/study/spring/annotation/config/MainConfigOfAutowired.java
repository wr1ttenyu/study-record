package wr1ttenyu.study.spring.annotation.config;

import org.springframework.beans.factory.Aware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.*;

import wr1ttenyu.study.spring.annotation.dao.BookDao;

/**
 * 
 * 自动装配： spring利用依赖注入,完成对IOC容器中各个组件的依赖注入
 * 
 * 1.@Autowried: 自动注入 a.优先按照类型去容器中寻找 b.如果有多个的话,优先匹配id和属性名一致的 如果没有一致的则不能装配
 * c.@Qualifier配合@Autowried 在有多个类型匹配的bean时 指定要装配的bean的id
 * d.自动装配默认一定要将属性装配好，可以用 @Autowried 的required属性来调整是否一定必须 e.@Primary
 * 首选设置，没有明确指定时，优先使用拥有该注解的Bean
 * 
 * 2.Spring 还支持使用@Resource(JSR250)和@Inject(JSR330)
 * 
 * @Resource 可以和@Autowired都可以用来装配，默认按照名称优先匹配，但是不能配合@Primary一起使用
 *           没有requird=false的功能
 * @Inject 需要导入javax.inject的包，合Autowired的功能一样,没有requird=false的功能
 * 
 * 3.@Autowried:
 * Marks a constructor, field, setter method or config method as to be autowired
 * by Spring's dependency injection facilities.
 *              
 * 4.自定义组件想要使用Spring底层的一些组件（ApplicationContext, BeanFactory, 等等）
 *   自定义组件要实现xxxAware接口
 *   {@link Aware}
 *   工具类
 *   {@link ApplicationContextAware}
 *   {@link BeanNameAware}
 *   原理
 *   xxxAware 都是由对应的  xxxAwareProcesser 来实现的  也是 BeanPostProcesser的应用
 * 
 *                                   
 * @author wr1ttenyu
 *
 */
@Configuration
@ComponentScan({ "wr1ttenyu.study.spring.annotation.service", "wr1ttenyu.study.spring.annotation.dao",
        "wr1ttenyu.study.spring.annotation.controller" })
@PropertySource({"classpath:person.properties"})
public class MainConfigOfAutowired {

    @Primary
    @Bean("bookDao2")
    public BookDao bookDao() {
        BookDao bookDao = new BookDao("BookDao2");
        bookDao.setLabel("bookDao2");
        return bookDao;
    }
}