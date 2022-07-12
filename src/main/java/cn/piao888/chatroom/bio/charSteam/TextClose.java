package cn.piao888.chatroom.bio.charSteam;

import java.io.*;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 测试文件输入流
 *
 * @Author： hongzhi.xu
 * @Date: 2022/7/12 2:20 下午
 * @Version 1.0
 */
public class TextClose {

    public InputStream inputStream;
    public BufferedReader bufferedReader;

    public TextClose() throws IOException {
        //获取文件输入流
        inputStream = Files.newInputStream(Paths.get("/Users/xuhongzhi/studen/io/src/main/java/cn/piao888/chatroom/bio/charSteam/目标文件"));
        //将输入流 转换为字符流
        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
    }

    public void read() throws IOException {
        StringBuffer sb = new StringBuffer();
        CharBuffer charBuffer = CharBuffer.allocate(200);
        if (bufferedReader.read(charBuffer) != -1) {
            charBuffer.flip();
            sb.append(charBuffer);
            charBuffer.clear();
        }
        System.out.println(sb);
    }

    public static void main(String[] args) throws IOException {
        TextClose textClose = new TextClose();
        textClose.read();
        //当外部流被关闭
        textClose.bufferedReader.close();
        //检验内部流会不会被关闭
        System.out.println(textClose.inputStream.available());
    }
}
