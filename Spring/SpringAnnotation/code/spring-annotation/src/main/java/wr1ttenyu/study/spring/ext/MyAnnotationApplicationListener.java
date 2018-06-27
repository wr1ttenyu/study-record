package wr1ttenyu.study.spring.ext;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MyAnnotationApplicationListener {

    @EventListener(classes = {ApplicationEvent.class})
    public void listen(ApplicationEvent applicationEvent) {
        System.out.println("MyAnnotationApplicationListener  接收到事件 ：" + applicationEvent);
    }

}
