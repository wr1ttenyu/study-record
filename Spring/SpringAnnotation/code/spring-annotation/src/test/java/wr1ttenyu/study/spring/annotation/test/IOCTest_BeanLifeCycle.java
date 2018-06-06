package wr1ttenyu.study.spring.annotation.test;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.Environment;

import wr1ttenyu.study.spring.annotation.bean.Person;
import wr1ttenyu.study.spring.annotation.config.MainConfigOfLifeCycle;

class IOCTest_BeanLifeCycle {

    ApplicationContext applicationContext;
    
    @BeforeEach
    public void initMethod() {
        applicationContext = new AnnotationConfigApplicationContext(
                MainConfigOfLifeCycle.class);
    } 
    
    @Test
    public void testAnnotationConditional() {
        Environment environment = applicationContext.getEnvironment();
        String osName = environment.getProperty("os.name");
        System.out.println("osName-->" + osName);
        
        String[] namesForType = applicationContext.getBeanNamesForType(Person.class);
        for (String name : namesForType) {
            System.out.println(name);
        }

        Map<String, Person> persons = applicationContext.getBeansOfType(Person.class);
        System.out.println(persons);
    }
    
    @Test
    public void testImportAnnotation() {
        printBeans(applicationContext);
        
    }
    
    private void printBeans(ApplicationContext applicationContext) {
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        for (String name : beanDefinitionNames) {
            System.out.println(name);
        }
        
        ((AnnotationConfigApplicationContext)applicationContext).close();
    }

}
