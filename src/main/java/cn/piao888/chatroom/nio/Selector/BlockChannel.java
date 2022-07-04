package cn.piao888.chatroom.nio.Selector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author 许鸿志
 * @since 2022/1/13
 */
public class BlockChannel {
    private ServerSocketChannel server;
    private Selector selector;

    public BlockChannel() throws IOException {
        server = ServerSocketChannel.open();
        //设置是否为阻塞式调用
        server.configureBlocking(true);
        //设置其监听端口
        server.socket().bind(new InetSocketAddress(8083));
        //初始化多路复用器
        selector = Selector.open();
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
    public void processKey(SelectionKey key) throws IOException {
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

    public static void main(String[] args) throws IOException, InterruptedException {
        BlockChannel blockChannel=new BlockChannel();
        //使用阻塞的方式 获取socket
        new Thread(() -> {
            while (true) {
                try {
                    //阻塞获取SocketChannel ，当获取到了 就 将socketChannel注册上读取事件
                    SocketChannel socketChannel = blockChannel.server.accept();
                    System.out.println("收到连接请求");
                    socketChannel.configureBlocking(false);
                    blockChannel.registerRead(socketChannel);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        //等待socketChannel 将读事件注册成功， 注册读事件 由《网络调试助手》 完成 ，且必须在四秒内（这4秒是为了让select 在阻塞前知道 有读事件被注册）
        // 必须要完成读事件注册select 才能进行阻塞休眠，否则当前select 就不会监听读事件了
        Thread.sleep(4000);
        while (true) {
            //阻塞的等待 有事件发生
            int keyCount = blockChannel.selector.select();
            System.out.println("有事件发生"+keyCount);
            //当有事件发生时，获取到事件key
            Iterator<SelectionKey> iterator =
                    keyCount > 0 ? blockChannel.selector.selectedKeys().iterator() : null;
            //迭代事件key
            while (iterator != null && iterator.hasNext()) {
                //取出发生事件的key
                SelectionKey sk = iterator.next();
                //通过key给他拿出来
                //确定好拿出来的不是一个null
                blockChannel.processKey(sk);
            }
            //必须要清空 ，否则selector.select() 会无线循环 输出 keyCount=0
            // 因为当selectedKeys集合不为空时，select()会立即返回
            blockChannel.selector.selectedKeys().clear();
        }

    }
}
