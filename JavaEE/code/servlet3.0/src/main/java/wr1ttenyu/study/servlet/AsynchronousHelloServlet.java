package wr1ttenyu.study.servlet;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(value = "/async", asyncSupported = true)
public class AsynchronousHelloServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 1. asyncSupported = true 支持异步模式
        // 2. 开启异步模式
        System.out.println(Thread.currentThread() + " 主线程 start... ==> " + System.currentTimeMillis());
        AsyncContext asyncContext = req.startAsync();
        // 3. 开始异步业务逻辑
        asyncContext.start(() -> {
            try {
                System.out.println(Thread.currentThread() + " 副线程 start... ==> " + System.currentTimeMillis());
                sayHello();
                asyncContext.complete();
                ServletResponse response = asyncContext.getResponse();
                response.getWriter().write("hello async....");
                System.out.println(Thread.currentThread() + " 副线程 end... ==> " + System.currentTimeMillis());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        System.out.println(Thread.currentThread() + " 主线程 end... ==> " + System.currentTimeMillis());
    }

    public void sayHello() throws Exception {
        System.out.println(Thread.currentThread() + " processing... ==> " + System.currentTimeMillis());
        Thread.sleep(3000);
    }
}
