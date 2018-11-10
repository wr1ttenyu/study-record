package wr1ttenyu.study.spring.annotation.springmvc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.PostConstruct;
import javax.servlet.AsyncListener;
import java.util.concurrent.*;

@Controller
public class AsyncController {

    ExecutorService executorServic;

    @PostConstruct
    private void init() {
        executorServic = Executors.newFixedThreadPool(5);
    }

    /**
     * Callable processing:
     *
     *  1. Controller returns a Callable.
     *
     *  2. Spring MVC calls request.startAsync() and submits the Callable to a TaskExecutor for processing in a separate thread.
     *
     *  3. Meanwhile the DispatcherServlet and all Filter’s exit the Servlet container thread but the response remains open.
     *
     *  4. Eventually the Callable produces a result and Spring MVC dispatches the request back to the Servlet container to complete processing.
     *
     *  5. The DispatcherServlet is invoked again and processing resumes with the asynchronously produced return value from the Callable.
     *
     *  Intercept by myFirstIntercept...../asyncHello
     *  =========================== 目标controller执行开始 ======================
     *  主线程 start... Thread[http-apr-8080-exec-5,5,main] ==> 1530175899882
     *  主线程 end... Thread[http-apr-8080-exec-5,5,main] ==> 1530175899886
     *  =========================== 目标controller执行结束 ======================
     *
     *  =========================== callable执行开始 ======================
     *  副线程 start... Thread[MvcAsync1,5,main] ==> 1530175899928
     *  Thread[MvcAsync1,5,main] processing... ==> 1530175899928
     *  副线程 end... Thread[MvcAsync1,5,main] ==> 1530175902928
     *  =========================== callable执行结束 ======================
     *
     *  Intercept by myFirstIntercept...../asyncHello
     *  postHandle by myFirstIntercept.....
     *  afterCompletion by myFirstIntercept.....
     *
     *  从上面的打印结果看：
     *      interceptor 的
     *      {@link HandlerInterceptor#postHandle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.web.servlet.ModelAndView)}
     *      和 {@link HandlerInterceptor#afterCompletion(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object, java.lang.Exception)}
     *      并没有在 the DispatcherServlet and all Filter’s exit the Servlet container thread 的时候被调用
     *  那么如何拦截：
     *      使用异步拦截器
     *      1. 原生API的 {@link AsyncListener}
     *      2. 实现springmvc的 {@link AsyncHandlerInterceptor}
     */
    @ResponseBody
    @GetMapping("/asyncHello")
    public Callable<String> asyncHello() {
        System.out.println("主线程 start... " + Thread.currentThread() + " ==> " + System.currentTimeMillis());

        Callable<String> callable = new Callable<String>() {
            @Override
            public String call() throws Exception {
                System.out.println("副线程 start... " + Thread.currentThread() + " ==> " + System.currentTimeMillis());
                sayHello();
                System.out.println("副线程 end... " + Thread.currentThread() + " ==> " + System.currentTimeMillis());
                return "springMvc Async";
            }
        };

        System.out.println("主线程 end... " + Thread.currentThread() + " ==> " + System.currentTimeMillis());

        return callable;
    }

    @GetMapping("/quotes")
    @ResponseBody
    public DeferredResult<String> quotes() {
        System.out.println("主线程 start... " + Thread.currentThread() + " ==> " + System.currentTimeMillis());
        DeferredResult<String> deferredResult = new DeferredResult<String>();

        executorServic.execute(() -> {
            System.out.println("副线程 start... " + Thread.currentThread() + " ==> " + System.currentTimeMillis());
            try {
                sayHello();
                deferredResult.setResult("springMvc DeferredResult Async");
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("副线程 end... " + Thread.currentThread() + " ==> " + System.currentTimeMillis());
        });

        System.out.println("主线程 end... " + Thread.currentThread() + " ==> " + System.currentTimeMillis());
        return deferredResult;
    }

    public void sayHello() throws Exception {
        System.out.println(Thread.currentThread() + " processing... ==> " + System.currentTimeMillis());
        Thread.sleep(3000);
    }
}
