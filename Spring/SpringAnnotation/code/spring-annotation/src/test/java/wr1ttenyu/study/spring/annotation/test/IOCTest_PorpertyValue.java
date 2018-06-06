package wr1ttenyu.study.spring.annotation.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import wr1ttenyu.study.spring.annotation.bean.Person;
import wr1ttenyu.study.spring.annotation.config.MainConfigOfPropertyValue;

class IOCTest_PorpertyValue {

    ApplicationContext applicationContext;
    
    @BeforeEach
    public void initMethod() {
        applicationContext = new AnnotationConfigApplicationContext(
                MainConfigOfPropertyValue.class);
    } 
    
    @Test
    public void testImportAnnotation() {
        printBeans(applicationContext);
        Person person = applicationContext.getBean(Person.class);
        System.out.println(person);
    }
    
    private void printBeans(ApplicationContext applicationContext) {
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        for (String name : beanDefinitionNames) {
            System.out.println(name);
        }
        
        /*((AnnotationConfigApplicationContext)applicationContext).close();*/
    }

}
