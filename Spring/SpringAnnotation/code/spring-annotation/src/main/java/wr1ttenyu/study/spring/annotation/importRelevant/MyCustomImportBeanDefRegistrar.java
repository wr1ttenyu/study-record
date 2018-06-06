package wr1ttenyu.study.spring.annotation.importRelevant;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import wr1ttenyu.study.spring.annotation.bean.House;

public class MyCustomImportBeanDefRegistrar implements ImportBeanDefinitionRegistrar {

    /**
     * importingClassMetadata: 标注有@Import的类的所有注解信息 BeanDefinitionRegistry: Bean定义注册器
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        if (registry.containsBeanDefinition("bill")) {
            // 可以指定bean的各种配置
            BeanDefinition beanDefinition = new RootBeanDefinition(House.class);
            registry.registerBeanDefinition("house", beanDefinition);
        }
    }

}
