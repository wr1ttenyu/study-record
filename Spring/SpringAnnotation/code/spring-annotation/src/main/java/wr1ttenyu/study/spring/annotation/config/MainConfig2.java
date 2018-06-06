package wr1ttenyu.study.spring.annotation.config;


import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

import wr1ttenyu.study.spring.annotation.bean.Color;
import wr1ttenyu.study.spring.annotation.bean.ColorFactoryBean;
import wr1ttenyu.study.spring.annotation.bean.Person;
import wr1ttenyu.study.spring.annotation.conditional.LinuxConditional;
import wr1ttenyu.study.spring.annotation.conditional.WindowsConditional;
import wr1ttenyu.study.spring.annotation.importRelevant.MyCustomImportBeanDefRegistrar;
import wr1ttenyu.study.spring.annotation.importRelevant.MyCustomSelector;

// 满足条件，这个配置类才能生效
@Conditional({WindowsConditional.class})
//配置类==配置文件
@Configuration // 告诉Spring这是一个配置类
@ComponentScan(value = "wr1ttenyu.study.spring.annotation"/*, useDefaultFilters = false, includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = { Person.class }) }*/)
@Import({Color.class, MyCustomSelector.class, MyCustomImportBeanDefRegistrar.class})
public class MainConfig2 {

    /**
     * Specifies the name of the scope to use for the annotated component/bean.
     * <p>
     * Defaults to an empty string ({@code ""}) which implies
     * {@link ConfigurableBeanFactory#SCOPE_SINGLETON SCOPE_SINGLETON}.
     * 
     * @since 4.2
     * @see ConfigurableBeanFactory#SCOPE_PROTOTYPE
     * @see ConfigurableBeanFactory#SCOPE_SINGLETON
     * @see org.springframework.web.context.WebApplicationContext#SCOPE_REQUEST
     * @see org.springframework.web.context.WebApplicationContext#SCOPE_SESSION
     * @see #value
     * 
     *      prototype 多实例的 singleton 单实例的 request 同一个请求创建一个实例 session
     *      同一个session创建一个实例
     * 
     *      懒加载：@Lazy 单实例Bean在容器启动时并不创建，在第一次从容器获取时才创建
     */
    @Scope
    @Bean
    public Person person() {
        Person person = new Person();
        person.setName("wr1ttenyu");
        person.setAge(26);
        return person;
    }

    /**
     * @Conditional: 按照一定的条件判断，满足条件给容器中注册Bean
     * 
     * 如果是window  创建Bill Gates
     * 如果是linux   创建linux
     */
    @Conditional({WindowsConditional.class})
    @Bean
    public Person bill() {
        return new Person("Bill Gates", 26);
    }
    
    @Conditional({LinuxConditional.class})
    @Bean
    public Person linux() {
        return new Person("linux", 26);
    }
    
    /**
     * 给容器中注册组件：
     * 1).包扫描 + 组件扫描注解(@Controller, @Service, @Repository, @Component)
     * 2).@Bean[导入第三方jar包里面的组件]
     * 3).@Import[快速给容器导入一个组件]
     *      a.@Import(要导入到容器中的组件):容器中就会自动注册这个组件,组件的id默认是全类名
     *      b.ImportSelector:返回需要导入的组件的全类名的数组
     *      c.ImportBeanDefinitionRegistrar:手动注册Bean到容器中
     * 4).使用spring提供的FactoryBean(工厂Bean)
     *      a.默认返回的Bean是调用FactoryBean.getObject()返回的对象
     *      b.要想获取 FactoryBean本身，需要在id前面加上&
     *          &factoryColor   
     */
    @Bean
    public ColorFactoryBean factoryColor() {
        return new ColorFactoryBean();
    }
    
    
}