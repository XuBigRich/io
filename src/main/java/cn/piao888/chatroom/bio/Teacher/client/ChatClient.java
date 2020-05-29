package cn.piao888.chatroom.bio.Teacher.client;

import java.io.*;
import java.net.Socket;

/**
 * 聊天服务的客户端
 * 1. 接收消息功能
 * 2. 发送消息功能
 * 3. 退出聊天室
 */
public class ChatClient {
    private final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private final int DEFAULT_SERVER_PORT = 8888;
    private final String QUIT = "quit";
    public Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;

    /**
     * 发送消息
     *
     * @param message 将要发送的消息
     * @throws IOException
     */
    public void send(String message) throws IOException {
        if (!socket.isOutputShutdown()) {
            writer.write(message + "\n");
            writer.flush();
        }
    }

    /**
     * 接收消息
     *
     * @return 返回从服务端接收到的消息
     * @throws IOException
     */
    public String receive() throws IOException {
        String msg = null;
        if (!socket.isInputShutdown()) {
            msg = reader.readLine();
        }
        return msg;
    }

    public boolean readToQuit(String msg) {
        return QUIT.equals(msg);
    }

    /**
     * 断开连接
     *
     * @throws IOException
     */
    public synchronized void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void start()  {
        try {
            socket=new Socket(DEFAULT_SERVER_HOST,DEFAULT_SERVER_PORT);
            reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            // 处理用户输入信息
            UserInputHandler userInputHandler=new UserInputHandler(this);
            Thread thread=new Thread(userInputHandler);
            thread.start();
            //接收服务端转发的信息
            String msg=null;
            while((msg=receive())!=null){
                System.out.println(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            close();
        }
    }

    public static void main(String[] args) {
        ChatClient  chatClient=new ChatClient();
        chatClient.start();
    }

}
