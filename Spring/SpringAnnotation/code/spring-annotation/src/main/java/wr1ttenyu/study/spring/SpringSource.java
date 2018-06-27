package wr1ttenyu.study.spring;

import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.*;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
/*import org.springframework.context.support.PostProcessorRegistrationDelegate;*/
import org.springframework.core.PriorityOrdered;

public class SpringSource {

    /**
     *  spring
     *  通过{@link org.springframework.context.support.AbstractApplicationContext#refresh()} 方法 来完成容器的创建和刷新
     *  1.{@link AbstractApplicationContext#prepareRefresh()} 刷新前预处理
     *      1.1 {@link AbstractApplicationContext#initPropertySources()}
     *              Initialize any placeholder property sources in the context environment.
     *              预留给子类重写的
     *     1.2  getEnvironment().validateRequiredProperties();
     *              Validate that all properties marked as required are resolvable
     * 		        see ConfigurablePropertyResolver#setRequiredProperties
     * 	   1.3  this.earlyApplicationEvents = new LinkedHashSet<>();
     * 	            Allow for the collection of early ApplicationEvents,
     * 		        to be published once the multicaster is available...
     *
     *  2.{@link AbstractApplicationContext#obtainFreshBeanFactory()} 获取BeanFactory
     *     2.1 {@link AbstractApplicationContext#refreshBeanFactory()} 刷新BeanFactory
     *     2.2 {@link AbstractApplicationContext#getBeanFactory()}
     *          返回  {@link GenericApplicationContext#GenericApplicationContext()} 创建的 {@link DefaultListableBeanFactory}
     *
     *  3.{@link AbstractApplicationContext#prepareBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)}
     *          Prepare the bean factory for use in this context.
     *          Configure the factory's standard context characteristics,
     * 	        such as the context's ClassLoader and post-processors.
     *
     * 	4.{@link AbstractApplicationContext#postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)}
     * 	        Allows post-processing of the bean factory in context subclasses.
     * 	        BeanFactory创建完成并预准备完成之后的后置处理工作，预留给子类进行扩展的，目前为空
     *
     * 	-------------------------------------------------   BeanFactory的创建及预准备工作完成   -------------------------------------------------
     *
     *  5. {@link AbstractApplicationContext#invokeBeanFactoryPostProcessors(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)}
     *          Invoke factory processors registered as beans in the context
     *          在BeanFactory标准初始化之后执行 BeanFactoryPostProcessors
     *          两个重要接口：
     *          {@link BeanDefinitionRegistryPostProcessor}  和  {@link BeanFactoryPostProcessor}
     *      5.1 {@link PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors(org.springframework.beans.factory.config.ConfigurableListableBeanFactory, java.util.List)}
     *          1). 先获取所有 BeanDefinitionRegistryPostProcessor
     *          2). 对实现了 {@link PriorityOrdered} 的BeanDefinitionRegistryPostProcessor 进行排序 并且 先于其他的  先执行
     *          3). 执行 {@link BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry(org.springframework.beans.factory.support.BeanDefinitionRegistry)}
     *          4). Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
     *          5). Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
     *          6). 记录哪些 BeanDefinitionRegistryPostProcessors 已经执行了
     *          7). 一样的逻辑来执行 BeanFactoryPostProcessors 并且忽略执行第六步已经执行的 BeanDefinitionRegistryPostProcessors
     *              因为  BeanDefinitionRegistryPostProcessors  implements  BeanFactoryPostProcessors
     *
     *  6. {@link AbstractApplicationContext#registerBeanPostProcessors(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)}
     *          Register bean processors that intercept bean creation.
     *          BeanPostProcessor 及其 子接口 ：
     *          @see BeanPostProcessor
     *          @see DestructionAwareBeanPostProcessor
     *          @see InstantiationAwareBeanPostProcessor
     *          @see SmartInstantiationAwareBeanPostProcessor
     *          @see MergedBeanDefinitionPostProcessor
     *          不同接口的调用时机是不一样的
     *          1).找到所有BeanPostProcessor 按照优先级  注册
     *          {@link PostProcessorRegistrationDelegate#registerBeanPostProcessors(org.springframework.beans.factory.config.ConfigurableListableBeanFactory, java.util.List)}
     *          后置处理器都支持 PriorityOrdered 和 Ordered  PriorityOrdered > Ordered > the rest
     *          2).最后一步
     *          // Re-register post-processor for detecting inner beans as ApplicationListeners,
     * 		    // moving it to the end of the processor chain (for picking up proxies etc).
     * 		    beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
     * 		    帮助检测出ApplicationListener的Bean 并注册Listener
     *
     *  7. {@link AbstractApplicationContext#initMessageSource()}
     *     Initialize message source for this context.
     *     判断容器中是否有MessageSource,有则赋值给 {@link AbstractApplicationContext#messageSource}, 无则创建一个并注册到容器中
     *          MessageSource 跟国际化功能有关
     *
     *  8. {@link AbstractApplicationContext#initApplicationEventMulticaster()}  初始化事件派发器
     *      Initialize event multicaster for this context.
     *      也是判断容器中是否有事件派发器，如果有的话就从容器中拿，没有的话，就创建一个，{@link SimpleApplicationEventMulticaster}
     *      然后注册到容器中
     *
     *  9. {@link AbstractApplicationContext#onRefresh()}
     *      留给子类重写，自定义操作
     *
     *  10. {@link AbstractApplicationContext#registerListeners()}
     *      1. check for listener beans and register them.
     *      2. Publish early application events now that we finally have a multicaster...
     *
     *  11. ***重要***
     *      {@link AbstractApplicationContext#finishBeanFactoryInitialization(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)}
     *      Instantiate all remaining (non-lazy-init) singletons.
     *      核心步骤：
     *      {@link ConfigurableListableBeanFactory#preInstantiateSingletons()}
     *      1. 遍历所有bean的定义信息
     *      2. !bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit() 满足前述条件的bean 才执行创建
     *      3. 判断是否是 spring factory bean ------  {@link AbstractBeanFactory#isFactoryBean(java.lang.String)}
     *      4. 不是FactoryBean  执行  {@link AbstractBeanFactory#getBean(java.lang.String)}  创建bean实例
     *          1). Eagerly check singleton cache for manually registered singletons.  所有被创建的单实例bean都会被缓存起来
     *          2). 缓存中拿不到  开始创建
     *          3). {@link AbstractBeanFactory#markBeanAsCreated(java.lang.String)}  标记bean 已经被创建  防并发
     *          4). 获取bean的定义信息
     *          5). {@link AbstractBeanDefinition#getDependsOn()}  获取依赖的bean  先创建依赖的Bean
     *          6). {@link AbstractAutowireCapableBeanFactory#createBean(java.lang.String, org.springframework.beans.factory.support.RootBeanDefinition, java.lang.Object[])}
     *              6.1). {@link AbstractAutowireCapableBeanFactory#resolveBeforeInstantiation(java.lang.String, org.springframework.beans.factory.support.RootBeanDefinition)}
     *                  Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
     *                  InstantiationAwareBeanPostProcessor  在bean创建之前执行
     *              6.2). {@link AbstractAutowireCapableBeanFactory#doCreateBean(java.lang.String, org.springframework.beans.factory.support.RootBeanDefinition, java.lang.Object[])
     *                  如果6.1 没有返回代理对象 则调用  doCreateBean
     *                  1). {@link AbstractAutowireCapableBeanFactory#createBeanInstance(java.lang.String, org.springframework.beans.factory.support.RootBeanDefinition, java.lang.Object[])}
     *                      创建bean实例
     *                  2). {@link AbstractAutowireCapableBeanFactory#applyMergedBeanDefinitionPostProcessors(org.springframework.beans.factory.support.RootBeanDefinition, java.lang.Class, java.lang.String)}
     *                      调用MergedBeanDefinitionPostProcessors
     *                  3). {@link AbstractAutowireCapableBeanFactory#populateBean(java.lang.String, org.springframework.beans.factory.support.RootBeanDefinition, org.springframework.beans.BeanWrapper)}
     *                      给bean属性赋值
     *                      -------在赋值之前
     *                      1.调用{@link InstantiationAwareBeanPostProcessor} 的 {@link InstantiationAwareBeanPostProcessor#postProcessAfterInstantiation(java.lang.Object, java.lang.String)}
     *                      2.调用{@link InstantiationAwareBeanPostProcessor} 的 {@link InstantiationAwareBeanPostProcessor#postProcessPropertyValues(org.springframework.beans.PropertyValues, java.beans.PropertyDescriptor[], java.lang.Object, java.lang.String)}
     *                      -------赋值
     *                      3.调用{@link AbstractAutowireCapableBeanFactory#applyPropertyValues(java.lang.String, org.springframework.beans.factory.config.BeanDefinition, org.springframework.beans.BeanWrapper, org.springframework.beans.PropertyValues)}
     *                          为属性赋值
     *                  4). {@link AbstractAutowireCapableBeanFactory#initializeBean(java.lang.String, java.lang.Object, org.springframework.beans.factory.support.RootBeanDefinition)}
     *                      1. {@link AbstractAutowireCapableBeanFactory#invokeAwareMethods(java.lang.String, java.lang.Object)}
     *                      2. {@link AbstractAutowireCapableBeanFactory#applyBeanPostProcessorsBeforeInitialization(java.lang.Object, java.lang.String)}
     *                          执行所有 BeanPostProcessors 的 {@link BeanPostProcessor#postProcessBeforeInitialization(java.lang.Object, java.lang.String)}
     *                      3. {@link AbstractAutowireCapableBeanFactory#invokeInitMethods(java.lang.String, java.lang.Object, org.springframework.beans.factory.support.RootBeanDefinition)}
     *                          执行初始化方法
     *                          检测是否是 {@link InitializingBean} 或者 {@link AbstractBeanDefinition#getInitMethodName()}
     *                      4. {@link AbstractAutowireCapableBeanFactory#applyBeanPostProcessorsAfterInitialization(java.lang.Object, java.lang.String)}
     *                          执行所有 BeanPostProcessors 的 {@link BeanPostProcessor#postProcessAfterInitialization(java.lang.Object, java.lang.String)}
     *                  5). {@link AbstractBeanFactory#registerDisposableBeanIfNecessary(java.lang.String, java.lang.Object, org.springframework.beans.factory.support.RootBeanDefinition)}
     *                      Register bean as disposable.
     *      5. {@link org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#addSingleton(java.lang.String, java.lang.Object)}
     *          将创建的bean加入到容器中
     *          其实ioc容器  就是  一系列的map  保存bean的各种信息  和  bean实例
     *      6. 所有bean的创建完之后
     *          检测bean是否是 {@link SmartInitializingSingleton}
     *          如果是的  就调用  {@link SmartInitializingSingleton#afterSingletonsInstantiated()} 方法
     *
     *  12. 最后一步 {@link AbstractApplicationContext#finishRefresh()}
     *      1. {@link AbstractApplicationContext#initLifecycleProcessor()} 注册 {@link LifecycleProcessor}
     *      2. 调用 {@link LifecycleProcessor} 的 {@link LifecycleProcessor#onRefresh()} 方法
     *      3. 发布 {@link ContextRefreshedEvent} ----- publishEvent(new ContextRefreshedEvent(this));
     *      4. LiveBeansView.registerApplicationContext(this);
     *         ---- Participate in LiveBeansView MBean, if active.
     *
     *  总结：
     *      1). spring容器在启动的时候，先会保存所有注册进来的 bean 的定义信息
     *          1.xml注册
     *          2.注解注册  @Bean @Compotent @Service .....
     *      2). spring会在合适的时机创建这些bean
     *          1.用到这个bean的时候就创建
     *          2.统一创建剩下的所有bean
     *      3).后置处理器(spring 很重要的工作环节)
     *          1.每一个bean创建完成都会使用 PostProcessor 来进行处理，来增强 bean 的功能
     *             例如:
     *             {@link AutowiredAnnotationBeanPostProcessor} ： 处理@Autowired自动注入
     *             {@link AnnotationAwareAspectJAutoProxyCreator} ： 做 Aop 功能
     *             ......
     *      4).事件驱动模型：
     *          {@link ApplicationListener} 事件监听器
     *          {@link ApplicationEventMulticaster} 事件多播器
     *          {@link ApplicationEvent} 事件基类
     *
     *
     */
    public void spirngSource() {

    }

}
