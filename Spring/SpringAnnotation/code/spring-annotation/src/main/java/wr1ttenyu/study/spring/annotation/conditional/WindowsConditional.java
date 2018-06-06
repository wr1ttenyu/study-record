package wr1ttenyu.study.spring.annotation.conditional;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class WindowsConditional implements Condition {

    /**
     * ConditionContext: 判断条件能使用的上下文环境
     * 
     * AnnotatedTypeMetadata: 注解信息
     * 
     */
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
        // 可以判斷容器中bean的注册情况，以及向容器中注册bean
        BeanDefinitionRegistry registry = context.getRegistry();
        System.err.println(registry);
        
        // 获取资源loader
        ResourceLoader resourceLoader = context.getResourceLoader();
        System.err.println(resourceLoader);
        
        String osName = environment.getProperty("os.name");
        if (osName != null && osName.contains("Windows")) {
            return true;
        }
        
        return false;
    }

}
