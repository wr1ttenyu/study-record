package wr1ttenyu.study.spring.annotation.bean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Car {

    private String brand;
    
    private String price;
    
    @Autowired
    private Money money;

    public void init() {
        System.out.println("car --- init ---");
    }
    
    public void destory() {
        System.out.println("car --- destory ---");
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