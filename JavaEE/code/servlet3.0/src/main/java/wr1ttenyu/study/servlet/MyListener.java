package wr1ttenyu.study.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class MyListener implements ServletContextListener {
    // 监听ServletContext容器启动初始化
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("MyListener ServletContext start....");
    }

    // 监听ServletContext容器销毁
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("MyListener ServletContext detoryed....");
    }
}
