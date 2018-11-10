package wr1ttenyu.study.netty.timeserver.nio.demo;

public class NIOTimeServer {

    public static void main(String[] args) {
        int port = 8081;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                // 采用默认值
            }
        }

        MultiplexeTimeServer timeServer = new MultiplexeTimeServer(port);

        new Thread(timeServer, "NIO-MultiplexeTimeServer-001").start();
    }


}
