package wr1ttenyu.study.spring.annotation.bean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class Dog {
    
    private String brand;

    private String price;

    @PostConstruct
    public void init() {
        System.out.println("Dog --- @PostConstruct init ---");
    }

    @PreDestroy
    public void destory() {
        System.out.println("Dog --- @PreDestroy destory ---");
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }
}
