package wr1ttenyu.study.netty.timeserver.utils;

public interface ResultHandler<T> {
    public void handle(T result);
}