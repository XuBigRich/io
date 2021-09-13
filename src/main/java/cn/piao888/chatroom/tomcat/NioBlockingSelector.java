package cn.piao888.chatroom.tomcat;

import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * @Author： hongzhi.xu
 * @Date: 2021/9/13 8:02 下午
 * @Version 1.0
 */
public class NioBlockingSelector {
    public void open(Selector selector) {

    }

    public void close() {

    }

    public int write(ByteBuffer buf, SocketChannel socket,long writeTimeout) {
        return 1;
    }
    public int read(ByteBuffer buf, SocketChannel socket,long writeTimeout) {
        return 1;
    }
}
