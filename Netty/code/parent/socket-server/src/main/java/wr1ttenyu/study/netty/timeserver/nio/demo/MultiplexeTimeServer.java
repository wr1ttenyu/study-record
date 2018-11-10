package wr1ttenyu.study.netty.timeserver.nio.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

public class MultiplexeTimeServer implements Runnable {

    private Selector selector;

    private ServerSocketChannel serverSocketChannel;

    private volatile boolean stop;

    public MultiplexeTimeServer(int port) {
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            // endpoint -- The IP address and port number to bind to.
            // backlog -- requested maximum length of the queue of incoming connections.
            // InetSocketAddress -- 任意 ip
            // 第二个参数 是 限制正在接入队列的长度
            serverSocketChannel.socket().bind(new InetSocketAddress(port), 2);
            // 将 serverSocketChannel 注册到 selector 上 并 监听 SelectionKey.OP_ACCEPT
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("The time server is start in port : " + port);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void stop() {
        this.stop = true;
    }


    @Override
    public void run() {
        while (!stop) {
            SelectionKey key = null;
            try {

                // selector 阻塞  最长时间为1秒钟  注意下面的英文 标识了 什么时候 selector 停止阻塞 返回结果
                // It returns only after at least one channel is selected,
                // this selector's {@link #wakeup wakeup} method is invoked, the current
                // thread is interrupted, or the given timeout period expires, whichever comes first.
                selector.select(8000);
                System.out.println("The selector start," + new Date().toString());
                // 返回已经就绪的key 也可能没有就绪的
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                System.out.println("The selector ready key length : " + selectionKeys.size());
                Iterator<SelectionKey> it = selectionKeys.iterator();
                while (it.hasNext()) {
                    key = it.next();
                    it.remove();
                    try {
                        handleInput(key);
                    } catch (IOException e) {
                        if (key != null) {
                            key.cancel();
                        }
                        if (key.channel() != null) {
                            key.channel().close();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleInput(SelectionKey key) throws IOException {
        if (key.isValid()) {
            // QUA 这里是已经连接 还是未连接 就是是否已经完成了TCP 三次握手
            // ANS 这里已经完成了TCP的三次握手  当key监听的标志位就绪时  就标志已经完成了标志位代表的操作
            if (key.isAcceptable()) {
                System.out.println("Request has receive  .......");
                // 有接入请求到达  处理新接入的请求消息 完成TCP 三次握手
                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                SocketChannel sc = ssc.accept();
                sc.configureBlocking(false);
                // Add the new connection to the selector 监听可读操作位
                sc.register(selector, SelectionKey.OP_READ);
            }
            // QUA 这里是数据已经到达 还是说  未到达
            // ANS 这里是数据接收完毕 已经存储在channel里面了
            if (key.isReadable()) {
                // Read data
                System.out.println("Data has receive  .......");
                SocketChannel sc = (SocketChannel) key.channel();
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                int readBytes = sc.read(readBuffer);
                if (readBytes > 0) {
                    // 读写模式切换
                    readBuffer.flip();
                    byte[] bytes = new byte[readBuffer.remaining()];
                    readBuffer.get(bytes);
                    String body = new String(bytes, "UTF-8");
                    System.out.println("The time server receive order : " + body);
                    String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? new Date().toString() : "BAD " +
                            "ORDER";
                    doWrite(sc, currentTime);
                } else if (readBytes < 0) {
                    // 返回值为-1 表示链路已经中断了
                    key.cancel();
                    sc.close();
                } else {
                    // 读到0字节 忽略
                }
            }

        }
    }

    private void doWrite(SocketChannel channel, String response) throws IOException {
        if (response != null && response.trim().length() > 0) {
            byte[] bytes = response.getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
            writeBuffer.put(bytes);
            writeBuffer.flip();
            channel.write(writeBuffer);
            // fixme 这里多次读写客户端如何收到
            // fixme 长连接 和 短连接 如何实现
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            writeBuffer.clear();
            writeBuffer.put("123".getBytes());
            writeBuffer.flip();
            channel.write(writeBuffer);
        }
    }
}
