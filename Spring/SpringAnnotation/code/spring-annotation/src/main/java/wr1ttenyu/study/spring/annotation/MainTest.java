package wr1ttenyu.study.spring.annotation;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import wr1ttenyu.study.spring.annotation.bean.Person;
import wr1ttenyu.study.spring.annotation.config.MainConfig;
import wr1ttenyu.study.spring.annotation.config.MainConfig2;

/**
 * @author wr1ttenyu
 */
public class MainTest {

    public static void main(String[] args) {

        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
                MainConfig.class);
        Person person = applicationContext.getBean(Person.class);
        System.out.println(person);

        // beanName默认为@Bean作用的方法名，也可以通过@Bean的参数来指定
        String[] beanNames = applicationContext.getBeanNamesForType(Person.class);
        for (String name : beanNames) {
            System.out.println(name);
        }
        
        String[] allBeanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : allBeanNames) {
            System.out.println(beanName);
        }

        applicationContext.close();
    }

}