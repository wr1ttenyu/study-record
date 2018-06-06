package wr1ttenyu.study.spring.annotation.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import wr1ttenyu.study.spring.annotation.config.MainConfigOfAutowired;
import wr1ttenyu.study.spring.annotation.service.BookService;

class IOCTest_Autowried {

    ApplicationContext applicationContext;
    
    @BeforeEach
    public void initMethod() {
        applicationContext = new AnnotationConfigApplicationContext(
                MainConfigOfAutowired.class);
    } 
    
    @Test
    public void testImportAnnotation() {
        printBeans(applicationContext);
        BookService bean = applicationContext.getBean(BookService.class);
        bean.testAutowried();
    }
    
    private void printBeans(ApplicationContext applicationContext) {
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        for (String name : beanDefinitionNames) {
            System.out.println(name);
        }
    }
}
