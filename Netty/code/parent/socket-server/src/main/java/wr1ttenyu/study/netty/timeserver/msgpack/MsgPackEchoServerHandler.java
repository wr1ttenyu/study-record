package wr1ttenyu.study.netty.timeserver.msgpack;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import wr1ttenyu.study.netty.timeserver.bean.User;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MsgPackEchoServerHandler extends ChannelHandlerAdapter {

    private AtomicInteger receive = new AtomicInteger(0);
    private AtomicInteger response = new AtomicInteger(0);
    List<User> users = null;
    User user;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            System.out.println("服务端接收成功:" + receive.addAndGet(1) + "次");
            user = (User) msg;
            System.out.println(user.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("服务端回写:" + response.addAndGet(1) + "次");
        ctx.writeAndFlush(user);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
