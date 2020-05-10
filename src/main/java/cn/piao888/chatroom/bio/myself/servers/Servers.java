package cn.piao888.chatroom.bio.myself.servers;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Servers {
    public static ArrayList<Socket> sockets= new ArrayList<>();
    public static void main(String[] args) throws IOException  {
        ServerSocket serverSocket=new ServerSocket(8888);
        while (true) {
            Socket socket = serverSocket.accept();
            sockets.add(socket);
            Thread thread=new Thread(()->{
                try {
                    BufferedReader bi = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String msg = null;
                    while ((msg = bi.readLine()) != null) {
                        for (Socket socket1 : sockets) {
                            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket1.getOutputStream()));
                            bufferedWriter.write(msg+"\n");
                            bufferedWriter.flush();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        );
            thread.start();
        }
    }
}
