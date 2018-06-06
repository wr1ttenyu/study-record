package wr1ttenyu.study.spring.annotation.test;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import wr1ttenyu.study.spring.annotation.config.MainConfigOfProfile;

class IOCTest_Profile {

    AnnotationConfigApplicationContext applicationContext;

    @Test
    public void testProfile() {
        applicationContext = new AnnotationConfigApplicationContext(MainConfigOfProfile.class);
        printBeans(applicationContext);
    }
    
    /**
     * 1. 使用命令行参数加载： -Dspring.profiles.active=Dev
     */
    @Test
    public void testProfile01() {
        applicationContext = new AnnotationConfigApplicationContext(MainConfigOfProfile.class);
        printBeans(applicationContext);
    }

    /**
     * 2. 硬编码方式指定
     */
    @Test
    public void testProfile02() {
        applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.getEnvironment().setActiveProfiles("Test");
        applicationContext.register(MainConfigOfProfile.class);
        applicationContext.refresh();
        printBeans(applicationContext);
    }

    private void printBeans(ApplicationContext applicationContext) {
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        for (String name : beanDefinitionNames) {
            System.out.println(name);
        }
    }
}
