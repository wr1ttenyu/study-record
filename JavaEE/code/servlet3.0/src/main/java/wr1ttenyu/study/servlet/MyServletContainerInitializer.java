package wr1ttenyu.study.servlet;

import wr1ttenyu.study.service.IHelloService;

import javax.servlet.*;
import javax.servlet.annotation.HandlesTypes;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

/**
 * invoke {@link MyServletContainerInitializer#onStartup(java.util.Set, javax.servlet.ServletContext)}
 * when application start
 *
 * use ServletContext to register Servlet、Listener、Filter
 * 可以在以下两个位置进行注册
 * 1. ServletContainerInitializer
 * 2. {@link ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)}
 */
@HandlesTypes(value = IHelloService.class)
public class MyServletContainerInitializer implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
        for (Iterator<Class<?>> iterator = c.iterator(); iterator.hasNext(); ) {
            Class<?> next =  iterator.next();
            System.out.println(next);
        }

        // register Servlet
        ServletRegistration.Dynamic myServlet = ctx.addServlet("MyServlet", new MyServlet());
        myServlet.addMapping("/user");

        // register Listener
        ctx.addListener(MyListener.class);

        // register Filter
        FilterRegistration.Dynamic myFilter = ctx.addFilter("myFilter", MyFilter.class);
        myFilter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/user");
    }
}
