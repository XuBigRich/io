package cn.piao888.chatroom.aio.client;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @Author： hongzhi.xu
 * @Date: 2020/6/4 4:15 下午
 * @Version 1.0
 */
public class Client {
    final String LOCALHOST = "localhost";
    final int DEFAULT_PORT = 8888;
    /**
     * 异步的ServerSocketChannel
     */
    AsynchronousSocketChannel socketChannel;

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

    public void start() {
        //创建AsynchronousSocketChannel
        try {
            socketChannel = AsynchronousSocketChannel.open();
            Future<Void> future = socketChannel.connect(new InetSocketAddress(LOCALHOST, DEFAULT_PORT));
            //如果调用返回，说明客户端与服务端成功建立连接
            future.get();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String msg = bufferedReader.readLine();
                byte[] bytes=msg.getBytes();
//                ByteBuffer byteBuffer=ByteBuffer.wrap(bytes);
                ByteBuffer byteBuffer=ByteBuffer.allocate(1024);
                byteBuffer.put(bytes);
                byteBuffer.flip();
                //write调用成功后会返回函数向通道写入了多少个字节
                Future<Integer> writeResult = socketChannel.write(byteBuffer);
                //get 会阻塞
                writeResult.get();
                byteBuffer.clear();
                //视频中使用的是flip；
                byteBuffer.flip();
                Future<Integer> readResult= socketChannel.read(byteBuffer );
                //read会阻塞
                readResult.get();
                byteBuffer.clear();
                System.out.println(new String(byteBuffer.array() ));
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } finally {
            close(socketChannel);
        }
    }

    public static void main(String[] args) {
        Client client=new Client();
        client.start();
    }
}
