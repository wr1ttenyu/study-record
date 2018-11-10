package wr1ttenyu.study.netty.timeserver.netty.decoderDemo;

import wr1ttenyu.study.netty.timeserver.bean.User;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class TestJdkSerializable {

    public static void main(String[] args) throws IOException {
        User user = new User(1, "wr1ttenyu");
        ByteArrayOutputStream bos =new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(bos);
        os.writeObject(user);
        os.flush();
        os.close();
        byte[] b = bos.toByteArray();
        System.out.println("The jdk serializable length is : " + b.length);
        bos.close();
        System.out.println("-----------------------------------------");
        System.out.println("The byte array serializable length is : " + user.codeUserToArrByte().length);
    }
}
