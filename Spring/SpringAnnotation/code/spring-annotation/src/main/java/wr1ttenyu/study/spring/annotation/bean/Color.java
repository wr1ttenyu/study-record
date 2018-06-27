package wr1ttenyu.study.spring.annotation.bean;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class Color implements InitializingBean, DisposableBean {

    private String name;

    private Integer order;

    public Color() {
        super();
        System.out.println("1 Color constructor is invoking ....");
    }

    public Color(String name, Integer order) {
        super();
        System.out.println("2 Color constructor is invoking ....");
        this.name = name;
        this.order = order;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    @Override
    public void destroy() throws Exception {
        System.out.println("Implement DisposableBean ---> destory!");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("Implement InitializingBean ---> afterPropertiesSet init!");
    }
}