package cn.piao888.chatroom.nio.chatroom.servers;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;


public class ChatServer {
    private static int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private final static int BUFFER = 1024;
    //设置统一编码为utf-8
    private Charset charset = Charset.forName("UTF-8");
    //用户指定服务器端口时使用
    private int port;

    //Socket通道
    private ServerSocketChannel server;
    private Selector selector;

    //用于读取通道消息的缓冲区
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);
    //用于转发消息所使用的缓冲区
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);

    public ChatServer() {
        this(DEFAULT_PORT);
    }

    public ChatServer(int port) {
        this.port = port;
    }

    private void start() {
        try {
            //既可以支持阻塞式调用，也支持非阻塞式调用
            server = ServerSocketChannel.open();
            //设置是否为非阻塞式调用
            server.configureBlocking(false);
            //设置其监听端口
            server.socket().bind(new InetSocketAddress(port));

            selector = Selector.open();
            //将server通道的accept事件注册至selector
            server.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("启动服务器，监听端口" + port + "...");
            while (true) {
                //阻塞式调用，如果selector所监听的通道一个监听事件都没有发生，
                // 那么select将一直阻塞 ，直到有通道发生了selector 所监听的事件，就会有返回值
                selector.select();
                //当select函数不再阻塞，说明我们监视的某个事件发生了，
                // 我们可以通过selectedKeys()得到知晓哪些通道被监听的事件发生了
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    handles(key);
                }
                //手动将selectKeys容器里面的东西做清空，如果不手动清空 ，那么下次调用select函数的时候 ，
                // 他会把新触发的selectkey 追加到 selectkeys的容器当中那样就出问题了呀
                keys.clear();

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //关闭selector.close() 他会做很多关闭，关闭所有通道，断开通道监听，关闭selector 。
            //所以使用selectot.close()一劳永逸
            close(selector);
        }
    }

    private void handles(SelectionKey key) throws IOException {
        //Accept事件-和客户端建立了连接
        if(key.isAcceptable()){
            ServerSocketChannel server= (ServerSocketChannel)key.channel();
            SocketChannel socketChannel =server.accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector,SelectionKey.OP_READ);
            System.out.println("客户端["+getClientName(socketChannel)+"]已连接。。");
        }
        //read事件-客户端传输过来了数据 给服务器端
        else if (key.isReadable()) {
               SocketChannel socketChannel=(SocketChannel)key.channel();
               String fwdMsg=receive(socketChannel);
               //如果消息为空则证明 通道出现了问题
                if(fwdMsg.isEmpty()){
                    //出现了问题就要给他断开 ,让selector不再监听这个通道
                    key.cancel();
                    //让当前调用的select函数马上返回，用以更新状态 ，这样就让selector重新审视了一下自己监听通道的最新状态
                    selector.wakeup();
                }else{
                    forwardMassage(socketChannel,fwdMsg);
                    if(readToQuit(fwdMsg)){
                         key.cancel();
                         selector.wakeup();
                         forwardMassage(socketChannel,"有个兄弟退出了");
                    }
                }

        }
    }
    public int getClientName(SocketChannel socketChannel){
       return socketChannel.socket().getPort();
    }

    private void forwardMassage(SocketChannel  client, String fwdMsg) throws IOException {

        for(SelectionKey key:selector.keys()){
          Channel socketChannel=(SocketChannel) key.channel();
          //判断如果是serverSocketChannel就 忽略对他的处理
            if ( socketChannel instanceof ServerSocketChannel) {
                continue;
            }
            //保证channel是一个正常状态，即channel没有被关闭，且selector也没有被关闭
            if(key.isValid()&&client!=socketChannel){
                //先清空缓存中的数据
                wBuffer.clear();
                wBuffer.put(charset.encode(getClientName(client)+":"+fwdMsg));
                //将buffer改为读模式，也就是上面将数据写入到Buffer里边后，再从里面读出来
                wBuffer.flip();
                while(wBuffer.hasRemaining()){
                    SocketChannel socket= (SocketChannel)socketChannel;
                    //socket 从wBuffer中读出数据，再写入到socket中
                    socket.write(wBuffer);
                }
            }
        }
    }

    private String receive(SocketChannel socketChannel) throws IOException {
        rBuffer.clear();
        StringBuffer stringBuffer=new StringBuffer();
        while((socketChannel.read(rBuffer))>0){
            rBuffer.flip();
           String msg= String.valueOf(charset.decode(rBuffer));
            stringBuffer.append(msg);
            rBuffer.clear();
        }
        return stringBuffer.toString();
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

    public boolean readToQuit(String msg) {
        return QUIT.equals(msg);
    }


    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();

    }
}
