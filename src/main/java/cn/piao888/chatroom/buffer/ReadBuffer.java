package cn.piao888.chatroom.buffer;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 通过ByteArrayOutputStream内存输出流  保存数据
 * <p>
 * 这样做的好处是当不知道byte中数据长度时，可以使用这个类去代替byte[] 数组
 *
 * @Author： hongzhi.xu
 * @Date: 2022/7/6 2:19 下午
 * @Version 1.0
 */
public class ReadBuffer {
    public static String filePath = "/Users/xuhongzhi/studen/io/src/main/java/cn/piao888/chatroom/buffer/测试buffer";
    public static File file = new File(filePath);

    public static void read(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        FileChannel fileChannel = fis.getChannel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        //内存输出流
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while (fileChannel.read(byteBuffer) != -1) {
            //重制读写指针归位到0 ，先将读写指针归位于0
            byteBuffer.flip();
            //确定内存中还有可读的数据 如果有 就放入bos中
            while (byteBuffer.hasRemaining()) {
                //将读出来的byte放入到 内存输出流中
                bos.write(byteBuffer.get());
            }
            byteBuffer.clear();
        }
        System.out.println(bos);
    }

    public static void main(String[] args) throws IOException {
        read(file);
    }
}
