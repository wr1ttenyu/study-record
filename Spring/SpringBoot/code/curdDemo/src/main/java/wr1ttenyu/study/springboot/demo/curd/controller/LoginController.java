package wr1ttenyu.study.springboot.demo.curd.controller;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.Map;

@Controller
public class LoginController {

    @PostMapping("/user/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session, Map<String, Object> msg) {
        if(!StringUtils.isEmpty(username) && "123456".equals(password)) {
            session.setAttribute("loginUser",username);
            //登陆成功，防止表单重复提交，可以重定向到主页
            return "redirect:/main.html";
        } else {
            msg.put("msg", "用户名密码错误");
            return "login";
        }
    }
}
