package cn.piao888.chatroom.kafka;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

/**
 * K
 *
 * @Author： hongzhi.xu
 * @Date: 2022/7/6 11:01 下午
 * @Version 1.0
 */
public class Service {
    private Selector selector;
    private TargetFile file;

    /**
     * 初始化多路复用器 和 事件处理者
     */
    public void init() throws IOException {
        //初始化一个socket通道
        ServerSocketChannel socketChannel = createSocketChannel();
        Selector selector = createSelector();
        //将SocketChannel注册到selector   accept事件
        socketChannel.register(selector, SelectionKey.OP_ACCEPT);
        this.selector = selector;
        this.file = new TargetFile();
    }

    public ServerSocketChannel createSocketChannel() throws IOException {
        ServerSocketChannel socketChannel = ServerSocketChannel.open();
        socketChannel.bind(new InetSocketAddress(9999));
        socketChannel.configureBlocking(false);
        return socketChannel;
    }

    public Selector createSelector() throws IOException {
        Selector selector = Selector.open();
        return selector;
    }

    public static void main(String[] args) throws IOException {
        Service service = new Service();
        service.init();
        Thread thread = new Thread(new Receive(service.selector, service.file));
        thread.start();
        Thread thread1 = new Thread(new Transmit(service.selector, service.file));
        thread1.start();
    }
}
