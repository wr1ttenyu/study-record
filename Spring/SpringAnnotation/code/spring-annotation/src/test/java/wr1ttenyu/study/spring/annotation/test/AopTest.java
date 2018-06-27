package wr1ttenyu.study.spring.annotation.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import wr1ttenyu.study.spring.annotation.aop.MathCalculator;
import wr1ttenyu.study.spring.annotation.config.MainConfigOfAOP;

class AopTest {

    ApplicationContext applicationContext;
    
    @BeforeEach
    public void initMethod() {
        applicationContext = new AnnotationConfigApplicationContext(
                MainConfigOfAOP.class);
    }
    
    @Test
    public void testAopAnnotation() {
        MathCalculator bean = applicationContext.getBean(MathCalculator.class);
        bean.add(1, 1);
    }
    
    private void printBeans(ApplicationContext applicationContext) {
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        for (String name : beanDefinitionNames) {
            System.out.println(name);
        }
    }
}
