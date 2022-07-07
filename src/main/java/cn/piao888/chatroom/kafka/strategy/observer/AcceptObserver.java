package cn.piao888.chatroom.kafka.strategy.observer;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * @Author： hongzhi.xu
 * @Date: 2022/7/7 10:29 上午
 * @Version 1.0
 */
public class AcceptObserver implements Observer {
    @Override
    public boolean supports(SelectionKey selectionKey) {
        return selectionKey.isAcceptable();
    }

    @Override
    public void execute(SelectionKey selectionKey) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selectionKey.selector(), SelectionKey.OP_READ);
    }
}
