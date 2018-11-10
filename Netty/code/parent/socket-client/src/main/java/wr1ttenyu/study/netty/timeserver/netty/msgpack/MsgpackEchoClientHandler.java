package wr1ttenyu.study.netty.timeserver.netty.msgpack;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import wr1ttenyu.study.netty.timeserver.bean.User;

public class MsgpackEchoClientHandler extends ChannelHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        /*User[] userInfos = userInfo();
        for (User user : userInfos) {
            ctx.writeAndFlush(user);
        }*/
        /*ctx.flush();*/

        for (int i = 0; i < 5; i++) {
            User user = new User(i, "degula-tonis-wr1ttenyu" + i);
            ctx.writeAndFlush(user);
        }
        /*ctx.flush();*/
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("Client receive the msgpack message : " + msg);
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    private User[] userInfo() {
        User[] userInfos = new User[50];
        User user = null;
        for (int i = 0; i < 50; i++) {
            user = new User(i, "wr1ttenyu" + i);
            userInfos[i] = user;
        }
        return userInfos;
    }
}
