package wr1ttenyu.study.netty.timeserver;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.msgpack.MessagePack;
import wr1ttenyu.study.netty.timeserver.bean.User;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertTrue;

/**
 * Unit test for simple App.
 */
public class AppTest {
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue() {
        assertTrue(true);
    }

    @Test
    public void testMsgpack() throws IOException {
        User user = new User(1, "wr1ttenyu");
        MessagePack messagePack = new MessagePack();
        byte[] raw = messagePack.write(user);
        MessagePack msgPack = new MessagePack();
        User read = msgPack.read(raw, User.class);
        System.out.println(1);
    }

    @Test
    public void testByteBuf() throws IOException {
        ByteBuf byteBuf = Unpooled.buffer(100);
        byteBuf.writeBytes("12324789234798472398".getBytes());
        ByteBuf temp = byteBuf.readBytes(10);
        System.out.println(temp.readerIndex());
        System.out.println(temp.writerIndex());
        System.out.println(byteBuf.readerIndex());
        System.out.println(byteBuf.writerIndex());
    }

    @Test
    public void testByteBufSearch() throws IOException {
        ByteBuf byteBuf = Unpooled.buffer(100);
        byteBuf.writeByte(123);
        byteBuf.writeByte(456);
        byteBuf.writeByte(456);
        byteBuf.writeByte(456);
        byteBuf.readByte();
        int i = byteBuf.indexOf(0, 2, new Integer(123).byteValue());
        System.out.println(i);
    }

    @Test
    public void tempTest() throws IOException {
        System.out.println(tableSizeFor(19));
    }

    @Test
    public void tempSlice() throws IOException {
        /*ByteBuffer byteBuffer = ByteBuffer.allocate(100);
        byteBuffer.put("a".getBytes());
        byteBuffer.flip();
        System.out.println(byteBuffer.remaining());
        ByteBuffer slice = byteBuffer.slice();
        System.out.println(slice.remaining());
        slice.put(2, new Byte("1"));*/
        ByteBuf byteBuf = Unpooled.buffer(100);
        byteBuf.writeBytes("abcdefg".getBytes());
        ByteBuffer byteBuffer = byteBuf.nioBuffer();
        System.out.println(byteBuffer.remaining());
        System.out.println(byteBuffer.position());
        System.out.println(byteBuffer.limit());
        System.out.println(byteBuffer.capacity());
        for (int i = 0; i < 7; i++) {
            System.out.println(byteBuffer.get());
        }
        System.out.println(byteBuffer.position());
        System.out.println(byteBuffer.limit());
    }

    static final int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= 100000000) ? 100000000 : n + 1;
    }
}
