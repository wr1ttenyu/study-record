package wr1ttenyu.study.spring.annotation.config;

import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
import org.springframework.aop.framework.DefaultAdvisorChainFactory;
import org.springframework.aop.framework.DefaultAopProxyFactory;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.context.annotation.*;

import org.springframework.context.support.AbstractApplicationContext;
import wr1ttenyu.study.spring.annotation.aop.MathCalculator;
import wr1ttenyu.study.spring.annotation.aop.MathCalculatorLogAspects;

/**
 *
 * AOP：
 *  指在程序运行期间动态的将某段代码切入到指定方法位置运行的编程方式
 *
 * 1. 导入spring aop模块； spring AOP
 *       <dependency>
 *           <groupId>org.springframework</groupId>
 *           <artifactId>spring-aspects</artifactId>
 *           <version>5.0.5.RELEASE</version>
 *       </dependency>
 *
 * 2. 通知方法：
 *      前置通知(@Before)
 *      后置通知(@After)
 *      返回通知()
 *      异常通知()
 *      环绕通知()
 *
 *  AOP原理：[给容器中注册了什么组件，这个组件什么时候工作，这个组件的功能是什么]
 *      1.@EnableAspectJAutoProxy --> @Import(AspectJAutoProxyRegistrar.class)
 *          利用AspectJAutoProxyRegistrar自定义给容器注册bean：
 *          internalAutoProxyCreator=AnnotationAwareAspectJAutoProxyCreator
 *          给容器中注册一个AnnotationAwareAspectJAutoProxyCreator
 *      2.AnnotationAwareAspectJAutoProxyCreator
 *            AnnotationAwareAspectJAutoProxyCreator
 *              -> AspectJAwareAdvisorAutoProxyCreator
 *                  -> AbstractAdvisorAutoProxyCreator
 *                      -> AbstractAutoProxyCreator
 *                          implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware
 *                                  关注后置处理器（在bean初始化前后做事情）和 自动注入 BeanFactory
 *        AbstractAutoProxyCreator.setBeanFactory
 *        AbstractAutoProxyCreator.postProcessBeforeInstantiation
 *        AbstractAutoProxyCreator.postProcessAfterInstantiation
 *
 *        AbstractAdvisorAutoProxyCreator.setBeanFactory ---> initBeanFactory
 *        AnnotationAwareAspectJAutoProxyCreator.initBeanFactory
 *
 *  调用流程
 *      1. 传入配置类，创建IOC容器
 *      2. 注册配置类，调用refresh() 方法，刷新容器 {@link AnnotationConfigApplicationContext#AnnotationConfigApplicationContext(java.lang.Class[])}
 *      3. {@link org.springframework.context.support.AbstractApplicationContext#registerBeanPostProcessors(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)}
 *          a. 注册IOC容器中定义的BeanPostProcessor类型的Bean
 *          b. First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
 *          c. Next, invoke the BeanFactoryPostProcessors that implement Ordered.
 *          d. Finally, invoke all other BeanFactoryPostProcessors.
 *          e. 注册 BeanPostProcessor， 实际上就是创建 BeanPostProcessor对象，保存到容器中
 *              创建internalAutoProxyCreator的BeanPostPorcessor {@link AnnotationAwareAspectJAutoProxyCreator}
 *              1). 创建AnnotationAwareAspectJAutoProxyCreator实例
 *              2). populateBean：给Bean的各种属性赋值 {@link AbstractAutowireCapableBeanFactory#populateBean(java.lang.String, org.springframework.beans.factory.support.RootBeanDefinition, org.springframework.beans.BeanWrapper)}
 *              3). initializeBean: 初始化Bean {@link AbstractAutowireCapableBeanFactory#initializeBean(java.lang.String, java.lang.Object, org.springframework.beans.factory.support.RootBeanDefinition)}
 *                  I.调用{@link AbstractAutowireCapableBeanFactory#invokeAwareMethods(java.lang.String, java.lang.Object)}
 *                  II.调用后置处理器的BeforeInitialization {@link AbstractAutowireCapableBeanFactory#applyBeanPostProcessorsBeforeInitialization(java.lang.Object, java.lang.String)}
 *                  III.调用对象的自定义初始化方法 {@link AbstractAutowireCapableBeanFactory#invokeInitMethods(java.lang.String, java.lang.Object, org.springframework.beans.factory.support.RootBeanDefinition)}
 *                  IV.调用后置处理器的AfterInitialization {@link AbstractAutowireCapableBeanFactory#applyBeanPostProcessorsAfterInitialization(java.lang.Object, java.lang.String)}
 *              4). {@link AnnotationAwareAspectJAutoProxyCreator} 创建成功
 *          f.把创建好的BeanPostProcessor放入到BeanFactory中：
 *              {@link ConfigurableBeanFactory#addBeanPostProcessor(org.springframework.beans.factory.config.BeanPostProcessor)}
 *   ----------------------------------   以上是创建AnnotationAwareAspectJAutoProxyCreator的过程   --------------------------------------
 *     AnnotationAwareAspectJAutoProxyCreator 是 {@link InstantiationAwareBeanPostProcessor} 类型的 BeanPostProcessor
 *     // Instantiate all remaining (non-lazy-init) singletons.
 * 	   4.{@link AbstractApplicationContext#finishBeanFactoryInitialization(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)}
 *        创建IOC自带组件以外的Bean
 *        a.遍历获取容器中所有的Bean，依次创建对象{@link AbstractBeanFactory#getBean(java.lang.String)}
 *              --> {@link AbstractBeanFactory#doGetBean(java.lang.String, java.lang.Class, java.lang.Object[], boolean)}
 *              --> {@link AbstractBeanFactory#createBean(java.lang.String, org.springframework.beans.factory.support.RootBeanDefinition, java.lang.Object[])}
 *        b.创建Bean,
 *          1).先从缓存中获取当前bean，如果能获取到，说明是之前被创建过的，然后拿出来使用，保证单例Bean的唯一性，
 *          单例Bean被创建好之后都会被缓存起来
*         c. createBean流程
 *          1).Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
 *            希望后置处理器能够返回Bean的代理对象，如果不能就继续执行
 *            InstantiationAwareBeanPostProcessor 类型的 BeanPostProcessor 在 resolveBeforeInstantiation 中被调用
 *           {@link AbstractAutowireCapableBeanFactory#resolveBeforeInstantiation(java.lang.String, org.springframework.beans.factory.support.RootBeanDefinition)}
 *          2).调用doCreateBean，开始真正创建bean实例
 *            和3.e的流程是一样的
 *           {@link AbstractAutowireCapableBeanFactory#doCreateBean(java.lang.String, org.springframework.beans.factory.support.RootBeanDefinition, java.lang.Object[])}
 *         d. 总结
 *           BeanPostProcessor 是在 bean 被创建之后调用
 *           InstantiationAwareBeanPostProcessor  是在 bean 被创建之前就调用了，并试图创建出一个 bean 的代理对象出来
 *
 *      AnnotationAwareAspectJAutoProxyCreator[InstantiationAwareBeanPostProcessor]的作用：
 *          1).在每一个Bean创建之前，调用InstantiationAwareBeanPostProcessor的相关方法来创建Bean的代理对象
 *              {@link AbstractAutoProxyCreator#postProcessBeforeInstantiation(java.lang.Class, java.lang.String)}
 *              a. 判断当前bean是否在advisedBeans中（即判断当前Bean是否需要增强）
 *              b. 判断是否是基础类型的bean {@link AnnotationAwareAspectJAutoProxyCreator#isInfrastructureClass(java.lang.Class)}
 *                 判断是否需要跳过 {@link AspectJAwareAdvisorAutoProxyCreator#shouldSkip(java.lang.Class, java.lang.String)}
 *                 1）.获取所有候选的增强器（切面里面的通知方法）【List<Advisor>】
 *                     判断每一个增强器是否是 AspectJPointcutAdvisor， 如果是 就跳过
 *              c.{@link AbstractAutoProxyCreator#postProcessAfterInitialization(java.lang.Object, java.lang.String)}
 *                  --> {@link AbstractAutoProxyCreator#wrapIfNecessary(java.lang.Object, java.lang.String, java.lang.Object)}
 *                  1)获取bean的适配切面并排序
 *                      {@link AbstractAdvisorAutoProxyCreator#findEligibleAdvisors(java.lang.Class, java.lang.String)}
 *                  2) 保存 bean 到 advisedBeans， 标记Bean已被增强
 *                  3) 创建 bean 的代理对象
 *                      {@link AbstractAutoProxyCreator#createProxy(java.lang.Class, java.lang.String, java.lang.Object[], org.springframework.aop.TargetSource)}
 *                      --> {@link DefaultAopProxyFactory#createAopProxy(org.springframework.aop.framework.AdvisedSupport)}
 *                          判断是使用jdk动态代理  还是  cglib
 *                  4）给容器中返回 cglib 增强了的代理对象
 *                  5）以后通过容器获取的该对象就是 cglib 增强过后的，执行目标方法，就会执行目标对象的切面通知方法
 *
 *  3. 通知方法是如何有序执行的
 *          容器中保存了组件的代理对象，就是cglib增强之后的对象，对象里面保存了代理信息（如增强器，目标对象等）
 *      1). {@link org.springframework.aop.framework.CglibAopProxy.DynamicAdvisedInterceptor#intercept(java.lang.Object, java.lang.reflect.Method, java.lang.Object[], org.springframework.cglib.proxy.MethodProxy)}
 *          拦截目标方法的执行
 *      2). {@link org.springframework.aop.framework.AdvisedSupport#getInterceptorsAndDynamicInterceptionAdvice(java.lang.reflect.Method, java.lang.Class)}
 *          获取目标方法的拦截器链
 *          遍历所有增强器，然后将增强器封装成 MethodInterceptor
 *          {@link DefaultAdvisorChainFactory#getInterceptorsAndDynamicInterceptionAdvice(org.springframework.aop.framework.Advised, java.lang.reflect.Method, java.lang.Class)}
 *
 *
 *      3). 链有值则执行，否则直接调用目标方法
 *      4). 有值时
 *          new CglibMethodInvocation(proxy, target, method, args, targetClass, chain, methodProxy).proceed();
 *          {@link ReflectiveMethodInvocation#proceed()}
 *          在proceed()方法中，调用Interceptor的invoke()方法会导致递归调用CglibMethodInvocation的proceed()方法，
 *          按照chain中intercept的顺序，最终使得切面方法及目标方法按顺序执行
 *
 *----------------------------------------------------
 *  总结：
 *      1）. @EnableAspectJAutoProxy 开启AOP功能
 *      2）. @EnableAspectJAutoProxy  在被扫描到之后，会触发spring向容器中注入{@link AnnotationAwareAspectJAutoProxyCreator}
 *      3）. AnnotationAwareAspectJAutoProxyCreator 是一个 InstantiationAwareBeanPostProcessor  用来拦截Bean的创建 判断是否需要用切面来增强
 *      4）. CglibAopProxy.intercept();是目标方法执行的核心逻辑
 *
 *
 *
 *
 * @author wr1ttenyu
 *
 */
@Configuration
@EnableAspectJAutoProxy
public class MainConfigOfAOP {

    @Bean
    public MathCalculator mathCalculator() {
        return new MathCalculator();
    }

    @Bean
    public MathCalculatorLogAspects mathCalculatorLogAspect() {
        return new MathCalculatorLogAspects();
    }
}