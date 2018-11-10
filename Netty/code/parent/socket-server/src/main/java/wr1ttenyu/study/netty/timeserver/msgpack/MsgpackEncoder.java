package wr1ttenyu.study.netty.timeserver.msgpack;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.msgpack.MessagePack;

public class MsgpackEncoder extends MessageToByteEncoder<Object> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object src, ByteBuf byteBuf) throws Exception {
        // 利用 msgpack 将要传输的对象转为 byte[] 并写入到 ByteBuf 中
        MessagePack messagePack = new MessagePack();
        byte[] raw = messagePack.write(src);
        byteBuf.writeBytes(raw);
    }
}
