package writtenyu.study.javase.bean;

import java.util.Date;

public class SubUser extends User<Date> {


    public Date getT() {
        return new Date();
    }

    public void setT(Date t) {
        System.out.println("setT");
    }

    public void setName(Integer name) {
    }
}
