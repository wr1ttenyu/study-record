package wr1ttenyu.study.servlet;


import javax.servlet.*;
import java.io.IOException;

public class MyFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        System.out.println("MyFilter work ......");
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}
