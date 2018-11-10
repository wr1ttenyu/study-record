package wr1ttenyu.study.spring.annotation.springmvc.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.*;
import wr1ttenyu.study.spring.annotation.springmvc.ext.MyAsyncIntercept;
import wr1ttenyu.study.spring.annotation.springmvc.ext.MyFirstIntercept;

import java.util.List;

// useDefaultFilters = false 禁用默认的过滤器  才能让 includeFilters 生效
@ComponentScan(value = "wr1ttenyu.study.spring.annotation.springmvc",
        includeFilters = {@ComponentScan.Filter(type = FilterType.ANNOTATION, classes = {Controller.class})},
        useDefaultFilters = false)
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    // 定制试图解析器
    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.jsp("/WEB-INF/views/", ".jsp");
    }

    // 静态资源访问交给中间件
    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MyFirstIntercept()).addPathPatterns("/**");
        registry.addInterceptor(new MyAsyncIntercept()).addPathPatterns("/**");
    }
}
