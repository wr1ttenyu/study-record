package wr1ttenyu.study.netty.timeserver.aio.demo;

public class AIOTimeServer {

    public static void main(String[] args) {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                // 采用默认值
            }
        }

        AsyncTimeServerHandler asyncTimeServerHandler = new AsyncTimeServerHandler(port);
        new Thread(asyncTimeServerHandler, "AIO-AsyncTimeServerHandler-001").start();
    }

}
