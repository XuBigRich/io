package cn.piao888.chatroom.bio.Teacher.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class UserInputHandler implements Runnable {
    private ChatClient chatClient;

    public UserInputHandler(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void run() {
      /*  try {
            chatClient.receive();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        try {
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String input = consoleReader.readLine();
                chatClient.send(input);
                if(chatClient.readToQuit(input)){
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
