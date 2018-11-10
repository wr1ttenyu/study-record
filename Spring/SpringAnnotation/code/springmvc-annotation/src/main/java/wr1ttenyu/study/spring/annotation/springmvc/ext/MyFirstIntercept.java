package wr1ttenyu.study.spring.annotation.springmvc.ext;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class MyFirstIntercept implements HandlerInterceptor {

    /**
     * 在目标方法之前执行
     * @param request
     * @param response
     * @param handler
     * @return
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        System.out.println("Intercept by myFirstIntercept....." + request.getRequestURI());
        /*PrintWriter writer = response.getWriter();
        writer.print("Intercept by myFirstIntercept.....");
        writer.flush();
        writer.close();*/
        return true;
    }

    /**
     * 目标方法执行正确之后执行
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        System.out.println("postHandle by myFirstIntercept.....");
    }

    /**
     * 响应成功后执行
     * @param request
     * @param response
     * @param handler
     * @param ex
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        System.out.println("afterCompletion by myFirstIntercept.....");
    }
}
