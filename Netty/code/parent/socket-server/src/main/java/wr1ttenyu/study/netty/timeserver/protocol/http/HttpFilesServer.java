package wr1ttenyu.study.netty.timeserver.protocol.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

public class HttpFilesServer {

    private static final String DEFAULT_URL = "/wr1ttenyu/";

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                // 采用默认值
            }
        }

        String url = DEFAULT_URL;
        new HttpFilesServer().run(port, url);
    }

    public void run(final int port, final String url) throws InterruptedException {
        // 配置服务端的NIO线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new HttpFilesServer.ChildChannelHandler(url));
            // 绑定端口  同步等待成功
            ChannelFuture f = b.bind("127.0.0.1",port).sync();
            System.out.println("HTTP文件目录服务器启动，网址是 : " + "http://127.0.0.1:"
                    + port + url);
            // 等待服务端监听端口关闭
            f.channel().closeFuture().sync();
        } finally {
            // 优雅退出，释放线程池资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {

        private String url;

        public ChildChannelHandler(String url) {
            this.url = url;
        }

        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {
            socketChannel.pipeline().addLast(new HttpRequestDecoder());
            // 解码器 将多个消息转换为单一的 FullHttpRequest 或者 FullHttpResponse
            // 因为 Http 解码器 在每个 Http 消息中 会生成多个 消息对象
            // 如 1.HttpRequest/HttpResponse 2.HttpContent 3.LastHttpContent
            socketChannel.pipeline().addLast(new HttpObjectAggregator(65536));
            socketChannel.pipeline().addLast(new HttpResponseEncoder());
            // 支持异步发送大的码流，但不占用过的内存，防止发生OOM
            socketChannel.pipeline().addLast(new ChunkedWriteHandler());
            socketChannel.pipeline().addLast(new HttpFileServerHandler(url));
        }
    }
}
