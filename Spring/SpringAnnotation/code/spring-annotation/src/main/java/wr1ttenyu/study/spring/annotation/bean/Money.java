package wr1ttenyu.study.spring.annotation.bean;

import org.springframework.stereotype.Component;

@Component
public class Money {

    private Integer num;

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }
}
