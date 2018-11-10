package wr1ttenyu.study.netty.timeserver.protocol;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import wr1ttenyu.study.netty.timeserver.bean.Order;
import wr1ttenyu.study.netty.timeserver.protocol.http.xml.HttpXmlRequestEncoder;
import wr1ttenyu.study.netty.timeserver.protocol.http.xml.HttpXmlResponseDecoder;

public class HttpXmlClient {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                // 采用默认值
            }
        }

        new HttpXmlClient().connect(port, "127.0.0.1");
    }

    public void connect(int port, String host) throws Exception {
        // 配置服务端的NIO线程组
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new HttpXmlClient.ChildChannelHandler());
            // 发起异步连接操作
            ChannelFuture f = b.connect(host, port).sync();
            // 等待客户端链路关闭
            f.channel().closeFuture().sync();
        } finally {
            // 优雅退出，释放线程池资源
            group.shutdownGracefully();
        }

    }

    private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {
            socketChannel.pipeline().addLast("http-decoder", new HttpResponseDecoder());
            socketChannel.pipeline().addLast("http-aggregator", new HttpObjectAggregator(65536));
            socketChannel.pipeline().addLast("xml-decoder", new HttpXmlResponseDecoder(Order.class, true));
            socketChannel.pipeline().addLast("http-encoder", new HttpRequestEncoder());
            socketChannel.pipeline().addLast("xml-encoder", new HttpXmlRequestEncoder());
            socketChannel.pipeline().addLast("xmlClientHandler", new HttpXmlClientHandler());
        }
    }
}
