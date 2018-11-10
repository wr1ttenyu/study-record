package wr1ttenyu.study.netty.timeserver.msgpack;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.msgpack.MessagePack;
import wr1ttenyu.study.netty.timeserver.bean.User;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MsgPackDecoder extends MessageToMessageDecoder<ByteBuf> {

    private AtomicInteger atomicInteger = new AtomicInteger(0);

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws
            Exception {
        try {
            System.out.println("服务端解码调用:" + atomicInteger.addAndGet(1) + "次");
            byte[] array;
            final int length = byteBuf.readableBytes();
            System.out.println("服务端收到数据长度:" + length);
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
