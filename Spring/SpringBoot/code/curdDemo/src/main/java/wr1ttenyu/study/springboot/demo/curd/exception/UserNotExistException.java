package wr1ttenyu.study.springboot.demo.curd.exception;

public class UserNotExistException extends BaseException {

    public UserNotExistException() {
        super("用户不存在");
    }
}
