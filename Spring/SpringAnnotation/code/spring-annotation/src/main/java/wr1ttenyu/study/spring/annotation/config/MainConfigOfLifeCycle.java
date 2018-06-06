package wr1ttenyu.study.spring.annotation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import wr1ttenyu.study.spring.annotation.bean.Car;
import wr1ttenyu.study.spring.annotation.bean.Dog;
import wr1ttenyu.study.spring.annotation.bean.MyCustomBeanPostProcessor;

/**
 * bean的生命周期：
 *      bean的创建---初始化---销毁的过程
 * 容器管理bean的生命周期，
 * 我们可以自定义初始化和销毁方法由容器来调用
 * 
 * 构造（对象创建）
 *      单实例，在容器启动的时候创建
 *      多实例，在获取的时候创建
 * 
 * 初始化：
 *      对象创建完成并完成属性赋值的时候，调用初始化
 * 销毁：
 *      单实例，容器关闭的时候
 *      多实例，容器不会管理这个bean，也就不会调用这个bean的销毁方法
 *      
 * 1).指定bean的初始化和销毁方法
 *      指定init-method和destory-method
 * 2).通过让Bean实现InitializingBean来实现初始化逻辑
 *    通过让Bean实现DisposableBean来实现销毁逻辑
 * 3).可以使用JSR250
 *          @PostConstruct 对象创建完成并完成属性赋值的时候，调用初始化
 *          @PreDestroy The PreDestroy annotation is used on methods as a callback notification to
 *      signal that the instance is in the process of being removed by the container
 * 4).BeanPostProcessor[interface]         
 *      method1--->postProcessBeforeInitialization:
 *          Apply this BeanPostProcessor to the given new bean instance <i>before</i> any bean
 *          initialization callbacks (like InitializingBean's {@code afterPropertiesSet}
 *          or a custom init-method). The bean will already be populated with property values
 *      method2--->postProcessAfterInitialization:
 *          Apply this BeanPostProcessor to the given new bean instance <i>after</i> any bean
 *          initialization callbacks (like InitializingBean's {@code afterPropertiesSet}
 *          or a custom init-method). The bean will already be populated with property values.
 *     BeanPostProcessor执行关键点
 *      org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.configureBean(Object, String)
 *      --->
 *      org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(String, Object, RootBeanDefinition)
 * 
 * Spring底层对BeanPostProcessor 的使用：
 *      bean赋值，注入其他组件，@Autowoired，生命周期注解功能， @Async等
 * 
 * @author wr1ttenyu
 */
@Configuration
public class MainConfigOfLifeCycle {

    @Bean(initMethod="init", destroyMethod="destory")
    public Car lkss() {
        return new Car();
    }
    
    @Bean
    public Dog dog() {
        return new Dog();
    }
    
    @Bean
    public MyCustomBeanPostProcessor myCustomBeanPostProcessor() {
        return new MyCustomBeanPostProcessor();
    }
    
}