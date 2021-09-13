package cn.piao888.chatroom.tomcat;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.charset.Charset;

/**
 * @author 许鸿志
 * @since 2021/9/13
 */
public class NoBackChatServer {
    private static int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private final static int BUFFER = 1024;
    /* 设置统一编码为utf-8*/
    private Charset charset = Charset.forName("UTF-8");
    //用户指定服务器端口时使用
    private int port;

    //Socket通道
    private ServerSocketChannel server;
    /*多路复用器*/
    private NioSelectorPool selectorPool = new NioSelectorPool();

    //用于读取通道消息的缓冲区
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);
    //用于转发消息所使用的缓冲区
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);

    public NoBackChatServer() throws IOException {
        server = ServerSocketChannel.open();
        server.socket().setReuseAddress(true);
        SocketAddress addr = new InetSocketAddress(DEFAULT_PORT);
        //地址和最大连接数
        server.socket().bind(addr, 1);
        //设置为阻塞式调用
        server.configureBlocking(true);
        //创建多路复用器
        selectorPool.open();
    }
}
