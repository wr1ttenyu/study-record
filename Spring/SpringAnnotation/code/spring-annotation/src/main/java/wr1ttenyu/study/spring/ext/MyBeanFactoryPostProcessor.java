package wr1ttenyu.study.spring.ext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

@Component
public class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        System.out.println("MyBeanFactoryPostProcessor start.......");

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
