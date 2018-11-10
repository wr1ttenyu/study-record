package wr1ttenyu.study.netty.timeserver.bean;

import org.msgpack.annotation.Message;

import java.nio.ByteBuffer;

@Message
public class User /*implements Serializable */{

    private Integer id;

    private String userName;

    public User() {
    }

    public User(Integer id, String userName) {
        this.id = id;
        this.userName = userName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public byte[] codeUserToArrByte() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        byte[] value = userName.getBytes();
        buffer.putInt(value.length);
        buffer.put(value);
        buffer.putInt(id);
        buffer.flip();
        value = null;
        byte[] result = new byte[buffer.remaining()];
        buffer.get(result);
        return result;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                '}';
    }
}
