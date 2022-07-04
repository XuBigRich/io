package cn.piao888.chatroom.shunxuIo;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 顺序读写
 * <p>
 * 顺序读写的效率一般会高于随机读写。在硬盘中，文件存储的位置是连续的
 * 那么如何将一个文件放入连续的硬盘空间呢，这里提供了Java的方案
 * <p>
 * 上面的对文件的读写都是随机读写，如果用来写比较小的日志文件还能满足要求，如果用来操作一个文件的读写，那可能带来很大的性能消耗。
 * 顺序IO的读写在中间件使用的很频繁，尤其是在队列中。几乎所有的队列（kafka,qmq等使用文件存储消息）都采用了顺序IO读写。
 * <p>
 * 与随机读写不同的是，顺序读写是优先分配一块文件空间，然后后续内容追加到对应空间内。
 * 在使用顺序IO进行文件读写时候，需要知道上次写入的地方，所以需要维护一个索引或者轮询获得一个没有写入位置。
 *
 * @Author： hongzhi.xu
 * @Date: 2022/6/30 10:04 下午
 * @Version 1.0
 */
public class Sequence {
    /**
     * 顺序写入
     *
     * @param filePath 文件路径
     * @param content  输入内容
     * @param index    起始位置
     * @return
     */
    public static long fileWrite(String filePath, String content, int index) {
        File file = new File(filePath);
        //声明一个随机文件访问类
        RandomAccessFile randomAccessTargetFile;
        //  操作系统提供的一个内存映射的机制的类
        MappedByteBuffer map;
        try {
            //读写模式随机访问文件
            randomAccessTargetFile = new RandomAccessFile(file, "rw");
            //通过所及文件访问类，获取文件通道Nio
            FileChannel targetFileChannel = randomAccessTargetFile.getChannel();
            //使用读写模式映射文件到直接内存  申请一个G（1024*1024*1024）的内存空间用户文件的读写
            map = targetFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, 20);
            //从指定位置写入
            map.position(index);
            //向内存中放入数据
            map.put(content.getBytes());
            //返回当前文件末尾的位置，便于下次继续写入
            return map.position();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }
        return 0L;
    }

    /**
     * 顺序读
     *
     * @param filePath 文件位置
     * @param index    申请内存的大小
     * @return
     */
    public static String fileRead(String filePath, long index) {
        File file = new File(filePath);
        RandomAccessFile randomAccessTargetFile;
        //  操作系统提供的一个内存映射的机制的类
        MappedByteBuffer map;
        try {
            randomAccessTargetFile = new RandomAccessFile(file, "rw");
            FileChannel targetFileChannel = randomAccessTargetFile.getChannel();
            map = targetFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, index);
            //声明一个字节数组
            byte[] byteArr = new byte[700];
            //从内存中取出index个字节放入byteArr数组
            map.get(byteArr, 0, (int) index);
            //将取出来的字符串数组 变为字符串
            return new String(byteArr);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }
        return "";
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        String filePath = "/Users/xuhongzhi/studen/io/src/main/java/cn/piao888/chatroom/shunxuIo/测试";
        File file = new File(filePath);
        if (!file.exists()) {
            file.createNewFile();
        }
        long index = Sequence.fileWrite(filePath, "你好！", 0);
//        Thread.sleep(50000);
        System.out.println(fileRead(filePath, 20));

        System.out.println(file.length());
    }
}
