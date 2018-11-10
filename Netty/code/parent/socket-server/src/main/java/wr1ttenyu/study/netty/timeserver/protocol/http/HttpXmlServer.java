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
import wr1ttenyu.study.netty.timeserver.bean.Order;
import wr1ttenyu.study.netty.timeserver.protocol.http.xml.HttpXmlRequestDecoder;
import wr1ttenyu.study.netty.timeserver.protocol.http.xml.HttpXmlResponseEncoder;

import java.net.InetSocketAddress;

public class HttpXmlServer {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                // 采用默认值
            }
        }

        new HttpXmlServer().run(port);
    }

    public void run(final int port) throws InterruptedException {
        // 配置服务端的NIO线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new HttpXmlServer.ChildChannelHandler());
            // 绑定端口  同步等待成功
            ChannelFuture f = b.bind(new InetSocketAddress(port)).sync();
            System.out.println("HTTP订购服务器启动，网址是 : " + "http://localhost:" + port);
            // 等待服务端监听端口关闭
            f.channel().closeFuture().sync();
        } finally {
            // 优雅退出，释放线程池资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {
            socketChannel.pipeline().addLast("http-decoder", new HttpRequestDecoder());
            socketChannel.pipeline().addLast("http-aggregator", new HttpObjectAggregator(65536));
            socketChannel.pipeline().addLast("xml-decoder", new HttpXmlRequestDecoder(Order.class, true));
            socketChannel.pipeline().addLast("http-encoder", new HttpResponseEncoder());
            socketChannel.pipeline().addLast("xml-encoder", new HttpXmlResponseEncoder());
            socketChannel.pipeline().addLast("xmlServerHandler", new HttpXmlServerHandler());
        }
    }
}
