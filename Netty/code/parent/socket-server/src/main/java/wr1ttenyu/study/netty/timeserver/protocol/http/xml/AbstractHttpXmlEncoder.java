package wr1ttenyu.study.netty.timeserver.protocol.http.xml;

import com.thoughtworks.xstream.XStream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import wr1ttenyu.study.netty.timeserver.bean.Address;
import wr1ttenyu.study.netty.timeserver.bean.Customer;
import wr1ttenyu.study.netty.timeserver.bean.Order;
import wr1ttenyu.study.netty.timeserver.bean.Shipping;

import java.nio.charset.Charset;

public abstract class AbstractHttpXmlEncoder<T> extends MessageToMessageEncoder<T> {

    final static String CHARSET_NAME = "UTF-8";
    final static Charset UTF_8 = Charset.forName(CHARSET_NAME);

    protected ByteBuf encode0(ChannelHandlerContext ctx, Object body) {
        XStream xStream = new XStream();
        xStream.setMode(XStream.NO_REFERENCES);
        // 注册使用了注解的VO
        xStream.processAnnotations(new Class[] { Order.class, Customer.class, Shipping.class, Address.class });
        String xml = xStream.toXML(body);
        ByteBuf encodeBuf = Unpooled.copiedBuffer(xml, UTF_8);
        return encodeBuf;
    }

    @Skip
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("fail to encode");
    }
}
