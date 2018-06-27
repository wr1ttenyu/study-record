package wr1ttenyu.study.spring.annotation.springmvc;

import org.springframework.web.SpringServletContainerInitializer;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.AbstractContextLoaderInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;
import org.springframework.web.servlet.support.AbstractDispatcherServletInitializer;

/**
 *  1. Web容器在启动的时候，会去扫描每个jar包下面的META-INF/serivces/javax.servlet.ServletContainerInitializer
 *  2. 加载这个文件中的指定类
 *  3. springmvc的应用一启动会加载 {@link SpringServletContainerInitializer}
 *      {@link SpringServletContainerInitializer} 中指定了去加载 {@link WebApplicationInitializer} 的实现类
 *  4. @link WebApplicationInitializer} 的抽象子类
 *      {@link AbstractContextLoaderInitializer} ：添加 {@link ContextLoaderListener} 获取 RootApplicationContextInitializers
 *      {@link AbstractDispatcherServletInitializer} ：创建web的ioc容器，并注册dispatchServlet
 *          1). {@link AbstractDispatcherServletInitializer#createServletApplicationContext()} 创建web的ioc容器
 *          2). {@link AbstractDispatcherServletInitializer#createDispatcherServlet(org.springframework.web.context.WebApplicationContext)}
 *                  创建dispatchServlet
 *          3). 注册dispatchServlet
 *      {@link AbstractAnnotationConfigDispatcherServletInitializer} 注解方式配置springMVC的初始化器
 *          1). {@link AbstractAnnotationConfigDispatcherServletInitializer#createRootApplicationContext()}
 *              {@link AbstractAnnotationConfigDispatcherServletInitializer#getRootConfigClasses()}  抽象方法  子类重写
 *              传入一个配置类，创建RootApplicationContext使用
 *          2). {@link AbstractAnnotationConfigDispatcherServletInitializer#createServletApplicationContext()}
 *              {@link AbstractAnnotationConfigDispatcherServletInitializer#getServletConfigClasses()} 抽象方法  子类重写
 *              传入一个配置类，用于创建ServletApplicationContext,也就是springmvc的ioc容器
 *
 *  总结：
 *      已注解的方式来启动SpringMvc;继承{@link AbstractAnnotationConfigDispatcherServletInitializer}
 *      实现{@link AbstractAnnotationConfigDispatcherServletInitializer#getServletConfigClasses()}，指定dispatchServlet的配置信息
 *
 * ================================================================================================
 *  定制springMvc:
 *      https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-config
 *
 *
 *
 */
public class Note {
}
