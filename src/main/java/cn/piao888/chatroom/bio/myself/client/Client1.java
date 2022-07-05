package cn.piao888.chatroom.bio.myself.client;

import java.io.*;
import java.net.Socket;

public class Client1 {
    public static void main(String[] args) throws IOException {
        String ip = "127.0.0.1";
        int port = 9999;
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
            new Thread(() -> {
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
                //读取服务器返回消息
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
