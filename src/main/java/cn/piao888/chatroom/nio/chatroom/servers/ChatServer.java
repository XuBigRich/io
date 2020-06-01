package cn.piao888.chatroom.nio.chatroom.servers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.charset.Charset;


public class ChatServer {
    private  static int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private final static int BUFFER = 1024;
    //设置统一编码为utf-8
    private Charset charset=Charset.forName("UTF-8");
    //用户指定服务器端口时使用
    private int port;

    //Socket通道
    private ServerSocketChannel server;
    private Selector selector;

    //用于读取通道消息的缓冲区
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);
    //用于转发消息所使用的缓冲区
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);

    public ChatServer(){
       this(DEFAULT_PORT);
    }
    public ChatServer(int port){
        this.port=port;
    }
    private void start(){
        try {
            //既可以支持阻塞式调用，也支持非阻塞式调用
            server=ServerSocketChannel.open();
            //设置是否为非阻塞式调用
            server.configureBlocking(false);
            //设置其监听端口
            server.socket().bind(new InetSocketAddress(port));

            selector=Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public boolean readToQuit(String msg) {
        return QUIT.equals(msg);
    }


    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();

    }
}
