package wr1ttenyu.study.spring.ext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class MyBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        System.out.println("my postProcessBeanDefinitionRegistry start.......");

        int beanDefinitionCount = registry.getBeanDefinitionCount();
        System.out.println("print bean definition count num : " + beanDefinitionCount);

        String[] beanDefinitionNames = registry.getBeanDefinitionNames();
        System.out.println("print bean definition name start ....");
        for (int i = 0; i < beanDefinitionNames.length; i++) {
            String beanDefinitionName = beanDefinitionNames[i];
            System.out.println("bean definition name ：" + beanDefinitionName);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        System.out.println("my postProcessBeanFactory start.......");

        int beanDefinitionCount = beanFactory.getBeanDefinitionCount();
        System.out.println("print bean definition count num : " + beanDefinitionCount);

        String[] beanDefinitionNames = beanFactory.getBeanDefinitionNames();
        System.out.println("print bean definition name start ....");
        for (int i = 0; i < beanDefinitionNames.length; i++) {
            String beanDefinitionName = beanDefinitionNames[i];
            System.out.println("bean definition name ：" + beanDefinitionName);
        }
    }

}
