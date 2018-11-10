package wr1ttenyu.study.netty.timeserver.protocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import wr1ttenyu.study.netty.timeserver.protocol.http.xml.HttpXmlRequest;
import wr1ttenyu.study.netty.timeserver.protocol.http.xml.HttpXmlResponse;
import wr1ttenyu.study.netty.timeserver.protocol.http.xml.OrderFactory;

public class HttpXmlClientHandler extends SimpleChannelInboundHandler<HttpXmlResponse> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        HttpXmlRequest request = new HttpXmlRequest(null, OrderFactory.create(123));
        ctx.writeAndFlush(request);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, HttpXmlResponse msg) throws Exception {
        System.out.println("The client receive response of http header is : " + msg.getHttpResponse()
                .headers().names());
        System.out.println("The client receive response of http body is : " + msg.getResult());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
