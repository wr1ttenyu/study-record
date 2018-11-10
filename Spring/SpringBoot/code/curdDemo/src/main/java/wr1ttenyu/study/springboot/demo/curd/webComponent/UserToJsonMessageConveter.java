package wr1ttenyu.study.springboot.demo.curd.webComponent;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;
import wr1ttenyu.study.springboot.demo.curd.bean.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class UserToJsonMessageConveter implements HttpMessageConverter<User> {
    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        if(clazz == User.class) {
            mediaType = MediaType.APPLICATION_JSON;
            return true;
        }
        return false;
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        List<MediaType> support = new ArrayList<>(5);
        support.add(MediaType.TEXT_HTML);
        return support;
    }

    @Override
    public User read(Class<? extends User> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return null;
    }

    @Override
    public void write(User user, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        String message = "My user HttpMessageConverter has worked, the user info : " + user.toString();
        outputMessage.getBody().write(message.getBytes());
    }
}
