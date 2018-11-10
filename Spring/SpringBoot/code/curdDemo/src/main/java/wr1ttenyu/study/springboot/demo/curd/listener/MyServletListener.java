package wr1ttenyu.study.springboot.demo.curd.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class MyServletListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("my context listener start...");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("my context listener end...");
    }
}