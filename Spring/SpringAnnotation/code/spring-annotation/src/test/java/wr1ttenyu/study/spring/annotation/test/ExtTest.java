package wr1ttenyu.study.spring.annotation.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import wr1ttenyu.study.spring.annotation.aop.MathCalculator;
import wr1ttenyu.study.spring.annotation.config.MainConfigOfAOP;
import wr1ttenyu.study.spring.annotation.service.UserService;
import wr1ttenyu.study.spring.annotation.transaction.TxConfig;
import wr1ttenyu.study.spring.ext.ExtConfig;

class ExtTest {

    ApplicationContext applicationContext;

    @BeforeEach
    public void initMethod() {
        applicationContext = new AnnotationConfigApplicationContext(
                ExtConfig.class);
    }

    @Test
    public void testExt() {
        applicationContext.publishEvent(new ApplicationEvent(new String("my event")) {});
        ((AnnotationConfigApplicationContext)applicationContext).close();
    }

    private void printBeans(ApplicationContext applicationContext) {
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        for (String name : beanDefinitionNames) {
            System.out.println(name);
        }
    }

}
