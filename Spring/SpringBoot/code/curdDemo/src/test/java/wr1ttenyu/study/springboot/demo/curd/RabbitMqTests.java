package wr1ttenyu.study.springboot.demo.curd;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import wr1ttenyu.study.springboot.demo.curd.bean.UUser;
import wr1ttenyu.study.springboot.demo.curd.service.UserService;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RabbitMqTests {

    @Autowired
    private UserService userService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Test
    public void sendRabbitMqMsgTest() {
        rabbitTemplate.convertAndSend("exchange.direct", "wr1ttenyu.money","赚钱啊,快活啊！");

        Map<String, String> msg = new HashMap<>();
        msg.put("info", "test");
        msg.put("content", "赚钱啊,快活啊！");
        rabbitTemplate.convertAndSend("exchange.fanout", "",msg);
    }

    @Test
    public void receiveRabbitMqMsgTest() {
        Object o = rabbitTemplate.receiveAndConvert("wr1ttenyu.money");
        System.out.println(o);
    }

    @Test
    public void amqpAdminTest() {
        DirectExchange exchange = new DirectExchange("amqpadmin.exchange");
        amqpAdmin.declareExchange(exchange);
    }
}
