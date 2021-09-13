package cn.piao888.chatroom.bio.myself.servers;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author： hongzhi.xu
 * @Date: 2021/3/29 下午3:25
 * @Version 1.0
 */
public class PoolsServers {
    public static ArrayList<Socket> sockets = new ArrayList<>();
    public static ExecutorService executorService = selfCreateThreadPool();

    /**
     * 创建一个核心大小为2 扩容大小为4，扩容存活时间为3秒的线程池，工作任务数为10，超出工作任务数直接丢弃。
     */
    public static ExecutorService selfCreateThreadPool() {
        ExecutorService pool = new ThreadPoolExecutor(
                2,
                4,
                3,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(10),
                new ThreadPoolExecutor.DiscardOldestPolicy());
        return pool;
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8888);
        while (true) {
            Socket socket = serverSocket.accept();
            sockets.add(socket);
            executorService.execute(() -> {
                try {
                    BufferedReader bi = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String msg = null;
                    while ((msg = bi.readLine()) != null) {
                        for (Socket socket1 : sockets) {
                            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket1.getOutputStream()));
                            bufferedWriter.write(msg + "\n");
                            bufferedWriter.flush();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
