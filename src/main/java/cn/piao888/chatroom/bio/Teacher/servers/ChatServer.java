package cn.piao888.chatroom.bio.Teacher.servers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 学习总结
 * 1. 一个方法一个功能，将所有可以封装的方法都封装起来，
 * 2. 将所有填值的东西封装为属性，保证来源来自同一个地方方便修改
 * 3. 对于异常，只要捕获就要处理，不管在什么地方
 * 4. 关于方法应该放在哪，要考虑谁应该去干这件事情，比如说关闭退出聊天，他应该服务器关注的事情 所以放在服务端
 * 5. 关于线程安全，想要保证线程的绝对安全 那么所有操作公共内存区的方法（堆内存），都要添加synchronized关键字
 */
public class ChatServer {
    private int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";

    //线程池实现的统一接口
    private ExecutorService executorService;

    private ServerSocket serverSocket;
    //存储与所有客户端连接的Socket 的Map容器 其是对象变量
    private Map<Integer, Writer> connectedClients;

    public ChatServer() {
        //对于如何选择线程池种类，需要根据需求来定，如当前需求是需要一个固定数量的线程池那么 下面这个FixedThreadPool就可以满足我们的需求
        executorService= Executors.newFixedThreadPool(3);
        connectedClients = new HashMap<>();
    }

    /**
     * 建立socket连接
     *
     * @param socket
     * @throws IOException
     */
    public synchronized void addClient(Socket socket) throws IOException {
        if (socket != null) {
            int port = socket.getPort();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            connectedClients.put(port, writer);
            System.out.println("客户端[" + port + "] 已经连接到服务器");
        }
    }

    public boolean readToQuit(String msg) {
        return QUIT.equals(msg);
    }

    /**
     * 断开连接
     *
     * @param socket
     * @throws IOException
     */
    public synchronized void removeClient(Socket socket) throws IOException {
        if (socket != null) {
            int port = socket.getPort();
            if (connectedClients.containsKey(port)) {
                //关闭writer 等同于关闭socket
                Writer writer = connectedClients.get(port);
                writer.close();
                connectedClients.remove(port);
                System.out.println("客户端[" + port + "] 已经断开连接");
            }
        }
    }

    /**
     * 发送消息，遍历所有Socket客户端对象，并发送消息给这些Socket
     *
     * @param socket 与客户端建立连接的socket
     * @param swdMsg 客户端传过来的消息
     * @throws IOException
     */
    public void forwardMessage(Socket socket, String swdMsg) throws IOException {
        int port = socket.getPort();
        for (Integer id : connectedClients.keySet()) {
            if (!id.equals(port)) {
                Writer writer = connectedClients.get(id);
                writer.write(swdMsg);
                writer.flush();
            }
        }
    }

    public synchronized void close() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
                System.out.println("关闭了serverSocket");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(DEFAULT_PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                //线程池的启动
                executorService.execute(new ChatHandler(this,socket));

                //不使用线程池的方案
                //创建ChatHandler线程
                //因为存储所有与socket客户端连接的容器 属于服务器的 对象变量 （也就是说属于一个对象）
                //把当前对象传入，让多线程操作这一个服务对象，以节约服务器资源
//                Thread thread = new Thread(new ChatHandler(this, socket));
//                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    public static void main(String[] args) {
        ChatServer chatServer=new ChatServer();
        chatServer.start();
    }
}