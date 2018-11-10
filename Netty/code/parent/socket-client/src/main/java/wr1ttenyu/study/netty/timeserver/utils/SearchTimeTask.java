package wr1ttenyu.study.netty.timeserver.utils;

import wr1ttenyu.study.netty.timeserver.nio.TimeClientHandler;

import java.util.concurrent.Callable;

public class SearchTimeTask implements Callable<String> {

    private TimeClientHandler timeClientHandler = new TimeClientHandler("127.0.0.1", 8080);

    @Override
    public String call() throws Exception {
        timeClientHandler.run();
        return "success";
    }
}
