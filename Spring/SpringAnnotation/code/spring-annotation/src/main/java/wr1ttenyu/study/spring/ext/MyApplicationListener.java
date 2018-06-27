package wr1ttenyu.study.spring.ext;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class MyApplicationListener implements ApplicationListener {

    // 当容器中发布事件  会触发该方法
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        System.out.println("event........" + event);
    }
}
