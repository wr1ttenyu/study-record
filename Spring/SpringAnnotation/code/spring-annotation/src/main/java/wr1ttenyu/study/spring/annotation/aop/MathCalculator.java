package wr1ttenyu.study.spring.annotation.aop;

public class MathCalculator {

    public int add(int i, int j) {
        System.out.println("结算中。。。");
        return i + j;
    }
    
    public int div(int i, int j) {
        return i/j;
    }
    
}
