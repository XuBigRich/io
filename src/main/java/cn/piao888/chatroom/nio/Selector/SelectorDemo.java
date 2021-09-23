package cn.piao888.chatroom.nio.Selector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

/**
 * @author 许鸿志
 * @since 2021/9/23
 */
public class SelectorDemo {
    private ServerSocketChannel server;
    private Selector selector;

    public SelectorDemo() throws IOException {
        server = ServerSocketChannel.open();
        //设置是否为非阻塞式调用
        server.configureBlocking(false);
        //设置其监听端口
        server.socket().bind(new InetSocketAddress(8080));
        //初始化多路复用器
        selector = Selector.open();
    }

    public void accept() throws IOException {
        while (true) {
            int keyCount = selector.select();
            Iterator<SelectionKey> iterator =
                    keyCount > 0 ? selector.selectedKeys().iterator() : null;
            while (iterator != null && iterator.hasNext()) {
                //取出发生事件的key
                SelectionKey sk = iterator.next();
                //通过key给他拿出来
                //确定好拿出来的不是一个null
                processKey(sk);
            }
        }
    }

    public void processKey(SelectionKey key) throws IOException {
        if (key.isAcceptable()) {
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            SocketChannel socketChannel = server.accept();
            socketChannel.configureBlocking(false);
//            printOps(socketChannel);
            registerRead(socketChannel);
            printOps(socketChannel);
            registerWrite(socketChannel);
            printOps(socketChannel);
            registerConnect(socketChannel);
            printOps(socketChannel);
        }
    }

    /**
     * 输出这个socketChannel所被 多路复用器监听的事件
     *
     * @param socketChannel
     * @throws ClosedChannelException
     */
    public void printOps(SocketChannel socketChannel) throws ClosedChannelException {
        //interestOps这个方法实际就是返回的SelectionKey的 interestOps属性
        int ops = socketChannel.keyFor(selector).interestOps();
        //判断是否是连接信息就是使用readyOps() & OP_ACCEPT 查看是否等于0
        //readyOps() 方法实际上读取的就是interestOps属性，OP_ACCEPT是1 << 4;
        //因此实际上 是 interestOps & 8
        socketChannel.keyFor(selector).isAcceptable();
        System.out.println(ops);
    }

    /**
     * 注册事件
     *
     * @throws ClosedChannelException
     */
    public void registerAccept() throws ClosedChannelException {
        //interestOps这个方法实际就是返回的SelectionKey的 interestOps属性
        server.register(selector, SelectionKey.OP_ACCEPT);
    }

    /**
     * 注册读事件
     *
     * @param socketChannel
     * @throws ClosedChannelException
     */
    public void registerRead(SocketChannel socketChannel) throws ClosedChannelException {
        socketChannel.register(selector, SelectionKey.OP_READ);
    }

    /**
     * 注册写事件
     *
     * @param socketChannel
     * @throws ClosedChannelException
     */
    public void registerWrite(SocketChannel socketChannel) throws ClosedChannelException {
        socketChannel.register(selector, SelectionKey.OP_WRITE);
    }

    /**
     * 注册连接事件
     *
     * @param socketChannel
     * @throws ClosedChannelException
     */
    public void registerConnect(SocketChannel socketChannel) throws ClosedChannelException {
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
    }

    public static void main(String[] args) throws IOException {
        SelectorDemo selectorDemo = new SelectorDemo();
        selectorDemo.registerAccept();
        selectorDemo.accept();
    }
}
