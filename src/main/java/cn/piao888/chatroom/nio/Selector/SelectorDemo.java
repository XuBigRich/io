package cn.piao888.chatroom.nio.Selector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
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
        //设置是否为阻塞式调用
        server.configureBlocking(false);
        //设置其监听端口
        server.socket().bind(new InetSocketAddress(8888));
        //初始化多路复用器
        selector = Selector.open();
    }

    public void accept() throws IOException {
        while (true) {
            int keyCount = selector.select();
            Set<SelectionKey>  selectionKeys=selector.selectedKeys();
            Iterator<SelectionKey> iterator =
                    keyCount > 0 ? selectionKeys.iterator() : null;
            while (iterator != null && iterator.hasNext()) {
                //取出发生事件的key
                SelectionKey sk = iterator.next();
                //通过key给他拿出来
                //确定好拿出来的不是一个null
                processKey(sk);
            }
            //让当前调用的select函数马上返回，用以更新状态 ，这样就让selector重新审视了一下自己监听通道的最新状态
//            selector.wakeup();
            //让事件key清空
            selectionKeys.clear();
        }
    }

    public void processKey(SelectionKey key) throws IOException {
        if (key.isAcceptable()) {
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            SocketChannel socketChannel = server.accept();
            socketChannel.configureBlocking(false);
//            printOps(socketChannel);
            //注册读事件
            registerRead(socketChannel);
            printOps(socketChannel);
//            //**********注册写事件与连接事件  (写事件与连接事件有个大坑 会造成select 状态无法重置 一直返回select=0 ，所以 平时给注释掉)*****************
            // 原因
//            其实select()是否返回与selectedKeys集合有关。当selectedKeys集合不为空时，select()会立即返回（不会阻塞），
//            但是其返回值是发生改变的keys数量，即新的就绪通道数量，这里不可能是1。因此我的这个场景下，第一条消息会产生一个新的key，我处理完没有将其删除
//            所以收第二条消息时，认定这个key没有发生改变，就会导致select()返回0，从而导致无限循环。
//            registerWrite(socketChannel);
//            printOps(socketChannel);
            //注册连接事件
            registerConnect(socketChannel);
            printOps(socketChannel);
        }
        if (key.isReadable()) {
            SocketChannel socketChannel = (SocketChannel) key.channel();
            ByteBuffer rBuffer = ByteBuffer.allocate(1024);
            StringBuffer stringBuffer=new StringBuffer();
            if (socketChannel.read(rBuffer) != -1) {
                //只要是读操作，就一定要先将模式转变 ，这个地方我踩坑了
                rBuffer.flip();
                String msg = String.valueOf(Charset.forName("UTF-8").decode(rBuffer));
                stringBuffer.append(msg);
                rBuffer.clear();
            }
            System.out.println(stringBuffer);
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
//        将ServerSocketChannel的 连接事件注册给多路复用器
        selectorDemo.registerAccept();

        selectorDemo.accept();



    }
}
