package cn.piao888.chatroom.shunxuIo.socketRecord;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Set;

/**
 * 使用nio的方式将socket中发送的数据通过顺序读写的方式，记录到record文件里面
 * 类似于实现一个简单的kafka
 *
 * @Author： hongzhi.xu
 * @Date: 2022/7/5 2:03 下午
 * @Version 1.0
 */
public class SocketRecord {
    //选择器
    public static Selector selector;
    public static MappedByteBuffer mappedByteBuffer;
    public static String filePath = "/Users/xuhongzhi/studen/io/src/main/java/cn/piao888/chatroom/shunxuIo/socketRecord/记录";
    public static Charset charset = Charset.forName("UTF-8");

    static {
        try {
            selector = Selector.open();
            RandomAccessFile accessFile = new RandomAccessFile(filePath, "rw");
            mappedByteBuffer = accessFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, 1024 * 1024 * 1024);
            createServerSocketChannel(9999);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //需要一个ServerSocketChannel服务去监听其他客户端的链接
    private static void createServerSocketChannel(int port) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        //设置非阻塞模式
        serverSocketChannel.configureBlocking(false);
        //给ServerSocketChannel注册一个accept事件
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public static void select() throws IOException {
        while (true) {
            int selectorKeyCount = selector.select(10);
            if (selectorKeyCount > 0) {
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                selectionKeys.forEach(SocketRecord::processSelectionKey);
                selectionKeys.clear();
            }
        }
    }

    public static void processSelectionKey(SelectionKey key) {
        if (key.isAcceptable()) {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
            try {
                serverSocketChannel.accept().configureBlocking(false).register(selector, SelectionKey.OP_READ);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (key.isReadable()) {
            SocketChannel socketChannel = (SocketChannel) key.channel();
            //申请一个直接内存 大小为1024
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            try {
                while (socketChannel.read(byteBuffer) > 0) {
                    byteBuffer.flip();
                    //创建一个数组大小恰好为，byteBuffer读取到的数据数量
                    byte[] bytes = new byte[byteBuffer.remaining()];
                    //将byteBuffer中的有效数据放入到bytes数组中  这个地方与下面 put相冲突 ，因为他将使用flip状态
                    byteBuffer.get(bytes);
                    //尝试输出一下
                    System.out.println(new String(bytes));
                    //这个会将ByteBuffer全部数据都放入byteBuffer
//                    mappedByteBuffer.put(byteBuffer);
                    //只放入bytes有效的数据
                    mappedByteBuffer.put(bytes);
                    //mappedByteBuffer
                    byteBuffer.clear();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            mappedByteBuffer.force();
        }
    }


    public static void main(String[] args) throws IOException {
        select();
    }
}
