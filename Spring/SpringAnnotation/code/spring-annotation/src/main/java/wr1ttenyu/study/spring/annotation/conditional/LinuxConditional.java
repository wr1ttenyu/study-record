package wr1ttenyu.study.spring.annotation.conditional;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class LinuxConditional implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        // 获取ioc容器的beanFactory
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        System.err.println(beanFactory);
        // 获取类加载器
        ClassLoader classLoader = context.getClassLoader();
        System.err.println(classLoader);
        // 获取环境信息
        Environment environment = context.getEnvironment();
        System.err.println(environment);
        // 获取bean定义的注册类
        BeanDefinitionRegistry registry = context.getRegistry();
        System.err.println(registry);
        // 获取资源loader
        ResourceLoader resourceLoader = context.getResourceLoader();
        System.err.println(resourceLoader);
        
        String osName = environment.getProperty("os.name");
        if (osName != null && osName.contains("Linux")) {
            return true;
        }
        
        return false;
    }

}
