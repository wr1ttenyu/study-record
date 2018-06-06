package wr1ttenyu.study.spring.annotation.aop;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class MathCalculatorLogAspects {

    /**
     * 定义切面
     * 1. 内部引用  方法名
     * 2. 外部引用  方法全类名
     */
    @Pointcut("execution(public int wr1ttenyu.study.spring.annotation.aop.MathCalculator.*(..))")
    public void pointCut() {}

    @Before("pointCut()")
    public void calculatorStart() {
        System.out.println("计算开始。。。。");
    }

    @After("wr1ttenyu.study.spring.annotation.aop.MathCalculatorLogAspects.pointCut()")
    public void calculatorEnd() {
        System.out.println("计算结束。。。。");
    }

    @AfterReturning("pointCut()")
    public void calculatorReturn() {
        System.out.println("计算返回。。。。");
    }

    @AfterThrowing("pointCut()")
    public void calculatorException() {
        System.out.println("计算异常。。。。");
    }
}
