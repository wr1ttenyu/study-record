package wr1ttenyu.study.netty.timeserver.protocol.http.xml;

import com.thoughtworks.xstream.XStream;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import wr1ttenyu.study.netty.timeserver.bean.Address;
import wr1ttenyu.study.netty.timeserver.bean.Customer;
import wr1ttenyu.study.netty.timeserver.bean.Order;
import wr1ttenyu.study.netty.timeserver.bean.Shipping;

import java.nio.charset.Charset;

public abstract class AbstractHttpXmlDecoder<T> extends MessageToMessageDecoder<T> {

    private Class<?> clazz;
    // 是否输出码流的标志，默认为false
    private boolean isPrint;
    private final static String CHARSET_NAME = "UTF-8";
    private final static Charset UTF_8 = Charset.forName(CHARSET_NAME);

    // 当调用这个构造方法是，默认设置isPrint为false
    protected AbstractHttpXmlDecoder(Class<?> clazz) {
        this(clazz, false);
    }

    protected AbstractHttpXmlDecoder(Class<?> clazz, boolean isPrint) {
        this.clazz = clazz;
        this.isPrint = isPrint;
    }

    protected Object decode0(ChannelHandlerContext arg0, ByteBuf body) throws Exception {
        String content = body.toString(UTF_8);
        if (isPrint)
            System.out.println("The body is : " + content);
        XStream xs = new XStream();
        xs.setMode(XStream.NO_REFERENCES);
        // 注册使用了注解的VO
        xs.processAnnotations(new Class[]{Order.class, Customer.class, Shipping.class, Address.class});
        Object result = xs.fromXML(content);
        return result;
    }

    @Skip
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    }
}
