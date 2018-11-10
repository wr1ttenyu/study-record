package wr1ttenyu.study.spring.annotation.springmvc.ext;

import org.springframework.web.servlet.AsyncHandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MyAsyncIntercept implements AsyncHandlerInterceptor {

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) {
        System.out.println(Thread.currentThread() + " AsyncHandlerInterceptor  看看啥时调用的... ==> " + System.currentTimeMillis());
    }
}
