package cn.piao888.chatroom.bio.Teacher.servers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * nio look
 * 如果客户的io 流被关闭了 那么读到的就是null值
 */
public class ChatHandler implements Runnable {
    private ChatServer chatServer;
    private Socket socket;

    /**
     * 创建ChatHandler
     *
     * @param chatServer 聊天服务
     * @param socket     与客户端建立连接的socket
     */
    public ChatHandler(ChatServer chatServer, Socket socket) {
        this.chatServer = chatServer;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            //存储了新上线用户
            chatServer.addClient(socket);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg;
            //哪个线程的客户端发送消息，哪个线程的服务端就会读到信息，然后停止阻塞，给其他线程socket发送消息
            while ((msg = reader.readLine()) != null) {
                String fwdMsg = "客户端[" + socket.getPort() + "]:" + msg + "\n";
                System.out.println();
                chatServer.forwardMessage(socket, fwdMsg);
                //检查y用户是否准备退出
                if (chatServer.readToQuit(msg)) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                chatServer.removeClient(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
