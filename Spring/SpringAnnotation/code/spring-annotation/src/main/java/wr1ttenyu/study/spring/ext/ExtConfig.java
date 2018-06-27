package wr1ttenyu.study.spring.ext;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListenerMethodProcessor;
import org.springframework.context.support.AbstractApplicationContext;
import wr1ttenyu.study.spring.annotation.bean.Car;
import wr1ttenyu.study.spring.annotation.bean.Color;

/**
 *  1. BeanPostProcessor 和 BeanFactoryPostProcessor 的区别
 *  {@link BeanPostProcessor} bean后置处理器 在bean的初始化前后进行调用
 *  {@link BeanFactoryPostProcessor} beanFactory的后置处理器
 *         Modify the application context's internal bean factory after its standard initialization.
 *         All bean definitions will have been loaded, but no beans will have been instantiated yet.
 *
 *  2. BeanFactoryPostProcessor 的调用位置
 *      {@link AbstractApplicationContext#invokeBeanFactoryPostProcessors(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)}
 *      直接在 BeanFactory 中获取 BeanFactoryPostProcessors 的实现类，并调用容器中的 BeanFactoryPostProcessors 的方法
 *
 *  3. {@link BeanDefinitionRegistryPostProcessor}
 *      先于 BeanFactoryPostProcessor执行  可以用来想spring ioc 容器中添加组件
 *      主要方法：
 *         {@link BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry(org.springframework.beans.factory.support.BeanDefinitionRegistry)}
 *          Modify the application context's internal bean definition registry after its
 * 	        standard initialization. All regular bean definitions will have been loaded,
 * 	        but no beans will have been instantiated yet. This allows for adding further
 * 	        bean definitions before the next post-processing phase kicks in.
 * 	    调用位置：
 *         {@link AbstractApplicationContext#invokeBeanFactoryPostProcessors(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)}
 *          直接在 BeanFactory 中获取 BeanDefinitionRegistryPostProcessor 的实现类，
 *          并调依次先调用所有 BeanDefinitionRegistryPostProcessor 的 postProcessBeanDefinitionRegistry 方法
 *          再调用所有 postProcessBeanFactory 方法
 *
 *  4. {@link ApplicationListener} : 监听容器中发布的事件。事件驱动模型开发。
 *          public interface ApplicationListener<E extends ApplicationEvent>
 *              监听ApplicationEvent类型的事件
 *     事件驱动开发步骤：
 *          1）.写一个监听器来监听某个事件（implements ApplicationEvent  或者使用  @EvenListener）
 *              @EvenListener 使用 {@link EventListenerMethodProcessor} 来处理
 *          2）.把监听器加入到容器
 *          3）.容器发布该事件，我们就能监听到
 *          4）.如何自己发布一个事件
 *              applicationContext.publishEvent(new ApplicationEvent(new String("my event")) {});
 *
 *     原理:
 *          事件发布流程？
 *          1.ContextRefreshedEvent {@link AbstractApplicationContext#finishRefresh()} 容器刷新完成
 *          2.调用 {@link AbstractApplicationContext#publishEvent(org.springframework.context.ApplicationEvent)} 发布事件
 *
 *          多播器：
 *          在 {@link AbstractApplicationContext#publishEvent(org.springframework.context.ApplicationEvent)} 方法中
 *          通过获取事件多播器来发布事件 {@link AbstractApplicationContext#getApplicationEventMulticaster()}
 *          多播器的创建过程：
 *              {@link AbstractApplicationContext#refresh()}
 *                  --> {@link AbstractApplicationContext#initApplicationEventMulticaster()}
 *
 *          监听器的注册： 即  多播器  是如何知道要通知哪些  {@link ApplicationListener}
 *              {@link AbstractApplicationContext#refresh()}
 *                 --> {@link AbstractApplicationContext#registerListeners()}
 *
 *
 */
@Configuration
@ComponentScan("wr1ttenyu.study.spring.ext")
public class ExtConfig {

    @Bean
    public Color color() {
        Color color = new Color("red", 1);
        return color;
    }
}
