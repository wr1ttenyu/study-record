package wr1ttenyu.study.springboot.demo.curd.webComponent;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import wr1ttenyu.study.springboot.demo.curd.exception.BaseException;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
public class MyExceptionHandler {

    @ExceptionHandler
    public String handleMyBaseException(BaseException ex, HttpServletRequest request) {
        request.setAttribute("javax.servlet.error.status_code", 500);
        //转发到/error
        return "forward:/error";
    }

    @ExceptionHandler
    public String handle(Exception ex, HttpServletRequest request) {
        request.setAttribute("javax.servlet.error.status_code", 400);
        //转发到/error
        return "forward:/error";
    }
}
