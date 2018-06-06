package wr1ttenyu.study.spring.annotation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import wr1ttenyu.study.spring.annotation.aop.MathCalculator;
import wr1ttenyu.study.spring.annotation.aop.MathCalculatorLogAspects;

/**
 * 
 * AOP：
 *  指在程序运行期间动态的将某段代码切入到指定方法位置运行的编程方式
 * 
 * 1. 导入spring aop模块； spring AOP 
 *       <dependency>
 *           <groupId>org.springframework</groupId>
 *           <artifactId>spring-aspects</artifactId>
 *           <version>5.0.5.RELEASE</version>
 *       </dependency>
 *       
 * 2. 通知方法：
 *      前置通知(@Before)
 *      后置通知(@After)
 *      返回通知()
 *      异常通知()
 *      环绕通知()     
 *                                   
 * @author wr1ttenyu
 *
 */
@Configuration
@EnableAspectJAutoProxy
public class MainConfigOfAOP {
    
    @Bean
    public MathCalculator mathCalculator() {
        return new MathCalculator();
    }
    
    @Bean
    public MathCalculatorLogAspects mathCalculatorLogAspect() {
        return new MathCalculatorLogAspects();
    }
}