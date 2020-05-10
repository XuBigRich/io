package cn.piao888.chatroom.bio.myself.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class Client {
    public static void main(String[] args) throws IOException {
        String ip = "127.0.0.1";
        int port = 8888;
        Socket socket = null;
        BufferedWriter write = null;
        BufferedReader reader = null;
        try {
            socket = new Socket(ip, port);
            reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            write = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream()));

            BufferedReader consoleReader = new BufferedReader(
                    new InputStreamReader(System.in));

            BufferedReader finalReader = reader;
            //开启线程单独 接收消息
            Socket finalSocket = socket;
            new Thread(() -> {
                System.out.println(finalSocket.getPort());
                String msg = null;
                try {
                    while ((msg = finalReader.readLine()) != null) {
                        System.out.println(msg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            while (true) {
                String consoleContent = consoleReader.readLine();
//			发送消息给服务器
                write.write(consoleContent + "\n");
                write.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (write != null) {
                try {
                    write.close();
                    System.out.println("关闭socket");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
