package cn.piao888.chatroom.mmap;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

/**
 * 内存映射
 * 若要进行文件的内存映射，必须有一个FileChannel  （文件通道）
 * 开启方式 ：
 * 根据映射方式来，如果MapMode是 只读类型，可以是 FileInputStream 获取的文件通道。
 * 但如果是可读可写的类型，那么需要使用RandomFileRead 的读写模式获取 文件通道
 * 优势：
 * 当数据通过内存映射的方式映射到内存后，那我们在程序读取数据的时候就可以获得很高的性能
 *
 * @Author： hongzhi.xu
 * @Date: 2022/7/6 9:55 上午
 * @Version 1.0
 */
public class MMap {
    public static String filePath = "/Users/xuhongzhi/studen/io/src/main/java/cn/piao888/chatroom/mmap/内存映射测试";
    public static File file = new File(filePath);

    /**
     * 通过内存映射 获取文件内容
     * 注意必须是通过FileInputStream  不能 通过FileOutputStream 获取 通道
     *
     * @param file
     * @throws IOException
     */
    public static void getFileContextByFileInputStream(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        FileChannel fileChannel = fileInputStream.getChannel();
        MappedByteBuffer map = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
        //只读模式映射文件到内存 ，从第3个字节开始读， 一共读9个字节  如果超过文件大小 就会报错
//        MappedByteBuffer map = fileChannel.map(FileChannel.MapMode.READ_ONLY, 3, 9);
        //不建议这么干
        byte[] bytes = new byte[(int) file.length()];
        //数组下标
        int i = 0;
        //当内存中存在数据时
        while (map.hasRemaining()) {
            bytes[i] = map.get();
            i++;
        }
        fileChannel.close();
        System.out.println(new String(bytes));
    }

    /**
     * 通过内存映射 获取文件内容
     * 通过RandomAccessFile 获得通道 ，必须含有r模式
     *
     * @param file
     * @throws IOException
     */
    public static void getFileContextByRandomAccessFile(File file) throws IOException {
        RandomAccessFile fileInputStream = new RandomAccessFile(file, "r");
        //获取通道
        FileChannel fileChannel = fileInputStream.getChannel();
        MappedByteBuffer map = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
        //只读模式映射文件到内存 ，从第3个字节开始读， 一共读9个字节  如果超过文件大小 就会报错
//        MappedByteBuffer map = fileChannel.map(FileChannel.MapMode.READ_ONLY, 3, 9);
        //不建议这么干
        byte[] bytes = new byte[(int) file.length()];
        //数组下标
        int i = 0;
        //当内存中存在数据时
        while (map.hasRemaining()) {
            bytes[i] = map.get();
            i++;
        }
        fileChannel.close();
        System.out.println(new String(bytes));
    }

    private static void writeFileContextByFileOutputStream(File file, String text) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        //获取通道
        FileChannel fileChannel = fileOutputStream.getChannel();
        MappedByteBuffer map = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, 50);
        //清除内存中的内容进行写操作
        map.clear();
        //设置内存映射内容
        map.put(text.getBytes(StandardCharsets.UTF_8));
        fileChannel.close();
    }

    private static void writeFileContextByRandomAccessFile(File file, String text) throws IOException {
        RandomAccessFile accessFile = new RandomAccessFile(file, "rw");
        //获取通道
        FileChannel fileChannel = accessFile.getChannel();
        MappedByteBuffer map = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, 50);
        //清除内存中的内容进行写操作
        map.clear();
        //设置内存映射内容
        map.put(text.getBytes(StandardCharsets.UTF_8));
        fileChannel.close();
    }

    public static void main(String[] args) throws IOException {
        getFileContextByFileInputStream(file);
        getFileContextByRandomAccessFile(file);
        //不能使用FileOutputStream 去做内存映射
        writeFileContextByFileOutputStream(file, "你好啊世界");
//        writeFileContextByRandomAccessFile(file, "你好世界");
    }


}
