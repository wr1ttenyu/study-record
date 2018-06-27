package wr1ttenyu.study.spring.annotation.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.annotation.Transactional;
import wr1ttenyu.study.spring.annotation.service.UserService;
import wr1ttenyu.study.spring.annotation.transaction.TxConfig;

class TxTest {

    ApplicationContext applicationContext;


    @BeforeEach
    public void initMethod() {
        applicationContext = new AnnotationConfigApplicationContext(
                TxConfig.class);
    }

    @Test

    public void testTx() {
        UserService userService = applicationContext.getBean(UserService.class);
        userService.addUser();
    }

}
