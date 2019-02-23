package writtenyu.study.javase.bean;

import java.io.Serializable;

public class User<T> implements Cloneable, Serializable {

    private String name;

    private T t;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User clone() {
        try {
           return (User)super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String toString() {
        super.toString();
        return "User{" +
                "name='" + name + '\'' +
                '}';
    }

    public T getT() {
        return t;
    }

    public void setT(T t) {
        this.t = t;
    }
}
