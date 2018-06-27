package wr1ttenyu.study.spring.annotation.springmvc;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;
import wr1ttenyu.study.spring.annotation.springmvc.config.RootConfig;
import wr1ttenyu.study.spring.annotation.springmvc.config.WebConfig;

public class MyAnnotationConfigDispatcherServletInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
    /**
     * 获取根容器的配置文件：spring的配置文件 根容器
     *
     * @return
     */
    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[]{RootConfig.class};
    }

    /**
     * 获取web容器的配置类：springmvc的配置文件 子容器
     *
     * @return
     */
    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[]{WebConfig.class};
    }

    /**
     * 获取DispatcherServlet的映射信息
     *
     * @return
     */
    @Override
    protected String[] getServletMappings() {
        // "/" : 拦截所有请求(包括静态资源  xx.js xx.img....) , 但是不包括*.jsp
        // "/*" : 拦截所有请求, 包括 *.jsp， jsp是tomcat的jsp引擎解析的
        return new String[]{"/"};
    }
}
