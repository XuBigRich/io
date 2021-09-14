package cn.piao888.chatroom.tomcat;

import java.nio.channels.SocketChannel;

/**
 * @author 许鸿志
 * @since 2021/9/14
 */
public class PollerEvent implements Runnable {

    private SocketChannel socket;
    private int interestOps;
    private NioBlockingSelector.NioSocketWrapper socketWrapper;

    public PollerEvent(SocketChannel socket, int interestOps, NioBlockingSelector.NioSocketWrapper socketWrapper) {
        this.socket = socket;
        this.interestOps = interestOps;
        this.socketWrapper = socketWrapper;
    }

    @Override
    public void run() {

    }
}
