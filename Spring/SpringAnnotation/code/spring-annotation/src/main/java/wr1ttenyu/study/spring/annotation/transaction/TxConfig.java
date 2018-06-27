package wr1ttenyu.study.spring.annotation.transaction;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.aop.framework.autoproxy.InfrastructureAdvisorAutoProxyCreator;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.context.annotation.AutoProxyRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.ProxyTransactionManagementConfiguration;
import org.springframework.transaction.annotation.TransactionManagementConfigurationSelector;
import org.springframework.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import javax.sql.DataSource;

/**
 *  声明式事物
 *
 *  1. 环境搭建
 *     1）. 导入相关依赖： 数据源  数据库驱动  spring-jdbc
 *  2. 数据源配置  jdbcTemplate 操作数据
 *  3. 给方法加上@Transactional
 *  4. 添加 @EnableTransactionManagement 开启注解版事务
 *  5. 配置事务管理器
 *
 *  原理：
 *      使用@EnableTransactionManagement  利用  {@link TransactionManagementConfigurationSelector}
 *      向容器中导入组件
 *      {@link AutoProxyRegistrar} {@link ProxyTransactionManagementConfiguration}
 *   1). {@link AutoProxyRegistrar} 给容器中注册一个 {@link InfrastructureAdvisorAutoProxyCreator}
 *   2). {@link InfrastructureAdvisorAutoProxyCreator}做了什么？
 *          implements {@link SmartInstantiationAwareBeanPostProcessor}
 *          利用后置处理器机制，在对象创建以后，包装对象，返回一个代理对象（增强器），代理对象执行方法利用拦截器进行调用
 *   3). {@link ProxyTransactionManagementConfiguration}做了什么？
 *          给容器中注册事务增强器 {@link BeanFactoryTransactionAttributeSourceAdvisor}
 *          {@link BeanFactoryTransactionAttributeSourceAdvisor} 中包含 两个主要的类
 *          a.{@link ProxyTransactionManagementConfiguration} 所创建的 {@link TransactionAttributeSource} 事务注解属性
 *          b.{@link TransactionInterceptor} 事务方法拦截器  事务的实现逻辑就在
 *              {@link TransactionInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)} 中
 *
 *
 *
 *
 *
 */
@Configuration
@EnableTransactionManagement
@ComponentScan({"wr1ttenyu.study.spring.annotation.dao", "wr1ttenyu.study.spring.annotation.service"})
public class TxConfig {

    @Bean
    public DruidDataSource dataSource() {
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setUsername("root");
        druidDataSource.setPassword("a198842519");
        druidDataSource.setDriverClassName("com.mysql.jdbc.Driver");
        druidDataSource.setUrl("jdbc:mysql://47.254.36.19:3306/test");
        return druidDataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        // spring 对 @Configuration 类有特殊处理
        // 给容器中加组件的方法，多次调用，只有第一次会创建bean，剩余都是从容器中去拿
        return new JdbcTemplate(dataSource());
    }

    /**
     * 给容器中添加事务管理器
     * @return
     */
    @Bean
    public PlatformTransactionManager platformTransactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }
}
