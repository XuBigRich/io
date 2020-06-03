package cn.piao888.chatroom.nio.chatroom.client;

import sun.nio.ch.SocketAdaptor;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * 聊天服务的客户端
 * 1. 接收消息功能
 * 2. 发送消息功能
 * 3. 退出聊天室
 */
public class ChatClient {
    private final static String DEFAULT_SERVER_HOST = "127.0.0.1";
    private final static int DEFAULT_SERVER_PORT = 8888;
    private final String QUIT = "quit";


    private final static int BUFFER = 1024;
    //设置统一编码为utf-8
    private Charset charset = Charset.forName("UTF-8");
    //用户指定服务器端口时使用
    private int port;
    //用户指定ip时使用
    private String host;
    //Socket通道
    private SocketChannel client;
    private Selector selector;

    //用于读取通道消息的缓冲区
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);
    //用于转发消息所使用的缓冲区
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);

    public boolean readToQuit(String msg) {
        return QUIT.equals(msg);
    }


    public ChatClient() {
        this(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);
    }

    public ChatClient(String host, int port) {
        this.port = port;
        this.host = host;
    }

    public synchronized void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
                System.out.println("关闭了serverSocket");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void start() throws IOException {
        client = SocketChannel.open();
        client.configureBlocking(false);
        selector.isOpen();
        client.register(selector, SelectionKey.OP_CONNECT);
        client.connect(new InetSocketAddress(host, port));
        while (true) {
            selector.select();
            Set<SelectionKey> selectionKeySet = selector.selectedKeys();
            for (SelectionKey key : selectionKeySet) {

                handls(key);
            }
            selectionKeySet.clear();
        }
    }

    private void handls(SelectionKey key) throws IOException {
        //CONNECT事件 -- 连接就绪事件
        if (key.isConnectable()) {
            SocketChannel socketChannel = (SocketChannel) key.channel();
            //判断连接动作是否已经完成
            if (socketChannel.isConnectionPending()) {
                socketChannel.finishConnect();
                new Thread(new UserInputHandler(this)).start();
            }
            client.register(selector, SelectionKey.OP_READ);
        } else
            //READ事件--服务器转发消息
            if (key.isReadable()) {
                SocketChannel socketChannel = (SocketChannel) key.channel();
                String msg = receive(socketChannel);
                if (msg.isEmpty()) {
                    System.out.println("出问题六");
                    close(selector);
                } else {
                    System.out.println(msg);
                }

            }
    }

    public void send(String msg) throws IOException {
        if (msg == null) {
            return;
        }
        wBuffer.clear();
        wBuffer.put(msg.getBytes());
        client.write(wBuffer);
        if(readToQuit(msg)){
            close(selector);
        }
    }

    private String receive(SocketChannel socketChannel) throws IOException {
        rBuffer.clear();
        while ((socketChannel.read(rBuffer)) > 0) ;
        rBuffer.flip();
        String msg = String.valueOf(charset.decode(rBuffer));
        return msg;

    }

    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();
    }

}
