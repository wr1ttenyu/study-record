package wr1ttenyu.study.spring.annotation.bean;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class MyCustomBeanPostProcessor implements BeanPostProcessor {

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        System.out.println("MyCustomBeanPostProcessor --> postProcessBeforeInitialization --> beanName: " + beanName);
        return bean;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        System.out.println("MyCustomBeanPostProcessor --> postProcessAfterInitialization --> beanName: " + beanName);
        return bean;
    }

}
