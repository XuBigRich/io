package cn.piao888.chatroom.aio.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hongzhi.xu
 */
public class Server {
    final int DEFAULT_PORT = 8888;
    /**
     * 异步的ServerSocketChannel
     */
    AsynchronousServerSocketChannel serverSocketChannel;

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

    /**
     * start函数运行于主线程
     */
    public void start() {
        try {
            /**
             * 当通过静态函数生成AsynchronousServerSocketChannel对象时，在底层同时也会生成一个AsynchronousChannelGroup对象
             * AsynchronousChannelGroup  他是与serverSocketChannel绑定在一起的，（注意在没有指定 AsynchronousChannelGroup时绑定系统默认的group）
             * 如果你对线程池大小有要求，你也可以指定group
             *  他的意义类似于一个线程池，提供一些异步通道共享的系统资源
             *  如线程中有一条或者多条异步的系统通道时,那么我们就需要一些额外的线程用来调用CompletionHandler的回调函数
             *  所以我们需要AsynchronousChannelGroup 它里面所包含的线程去处理这些回调。
             *
             *    当有回调时，系统就去AsynchronousChannelGroup这个线程池中寻找可用的线程资源，来进行回调的执行
             */
            serverSocketChannel = AsynchronousServerSocketChannel.open();
            //绑定监听端口
            serverSocketChannel.bind(new InetSocketAddress(DEFAULT_PORT));
            System.out.println("服务器已经启动成功正在监听：" + DEFAULT_PORT + "端口");
            //accept有连个重载函数  一个是无参的 一个是有两个参数的
            // accept(A attachment, CompletionHandler<AsynchronousSocketChannel,? super A> handler);
            //前面的attachment函数代表了为 执行函数提供信息的一个Object对象
//            serverSocketChannel.accept();
            while (true) {
                //AcceptHandler是在其他线程中调用的
                serverSocketChannel.accept(null, new AcceptHandler());
                //因为accept是非阻塞式的，他会立即返回可能会造成因主线程执行完毕，导致的进程结束
                // 所以通过添加while循环与read搭配使用，accept就不会立即结束祝线程
                System.in.read();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(serverSocketChannel);
        }
    }

    /**
     * 用于系统内核回调  回调函数是使用的其他线程的，不是主线程
     */
    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {
        //调用成功要做的事情
        @Override
        public void completed(AsynchronousSocketChannel result, Object attachment) {
            //这次调用只是代表着 上次的accept调用完成，我们还需要继续告诉系统内核，继续去监听有没有新的请求要连接到客户端
            //翻译成白话就是，告诉通道说好了现在你需要继续监听下一个会和你建立连接的客户端，发送过来的连接请求
            if (serverSocketChannel.isOpen()) {
                serverSocketChannel.accept(null, this);
            }

            //针对刚刚建立好连接的channel 我们开始 读他发给的消息
            AsynchronousSocketChannel clientChannel = result;
            if (clientChannel != null && clientChannel.isOpen()) {
                ClientHandler handler = new ClientHandler(clientChannel);
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                Map<String, Object> info = new HashMap<>();
                info.put("type", "read");
                info.put("buffer", buffer);
                //我们需要异步的调用读取
                clientChannel.read(buffer, info, handler);
            }
        }

        //调用失败要做的事情
        @Override
        public void failed(Throwable exc, Object attachment) {
            //处理错误
        }

        private class ClientHandler implements CompletionHandler<Integer, Object> {
            private AsynchronousSocketChannel channel;

            public ClientHandler(AsynchronousSocketChannel channel) {
                this.channel = channel;
            }

            @Override
            public void completed(Integer result, Object attachment) {
                Map<String, Object> info = (Map<String, Object>) attachment;
                String type = (String) info.get("type");
                if ("read".equals(type)) {
                    ByteBuffer byteBuffer = (ByteBuffer) info.get("buffer");
                    byteBuffer.flip();
                    info.put("type", "write");
                    channel.write(byteBuffer, info, this);
                    byteBuffer.clear();
                }
                if ("write".equals(type)) {
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    info.put("type", "read");
                    info.put("buffer", buffer);
                    channel.read(buffer, info, this);
                }
            }

            @Override
            public void failed(Throwable exc, Object attachment) {

            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}
