package wr1ttenyu.study.netty.timeserver.netty.msgpack;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.msgpack.MessagePack;
import wr1ttenyu.study.netty.timeserver.bean.User;

import java.io.IOException;
import java.util.List;

public class MsgPackDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws
            Exception {
        try {
            byte[] array;
            final int length = byteBuf.readableBytes();
            // 获取byte数组
            array = new byte[length];
            byteBuf.getBytes(byteBuf.readerIndex(), array, 0, length);
            // 利用 msgPack 反序列化  加入 list 中
            MessagePack msgPack = new MessagePack();
            list.add(msgPack.read(array, User.class));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
