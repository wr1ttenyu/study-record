package wr1ttenyu.study.netty.timeserver.aio;

public class AIOTimeClient {
    public static void main(String[] args) {

        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                // 采用默认值
            }
        }

        AsyncTimeClientHandler asyncTimeClientHandler = new AsyncTimeClientHandler("127.0.0.1", port);
        new Thread(asyncTimeClientHandler).start();
    }
}
