package cn.piao888.chatroom.kafka;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @Author： hongzhi.xu
 * @Date: 2022/7/7 8:36 上午
 * @Version 1.0
 */
@Getter
@Setter
@ToString
public class TargetFile extends File {
    //当前写位置
    public volatile long writePosition;
    //当前读位置
    public volatile long readPosition;
    //映射的内存
    public MappedByteBuffer byteBuffer;
    //用于读写的内存映射
    public MappedByteBuffer readBuffer;
    //当前文件的文件通道
    public FileChannel fileChannel;
    //默认存储路径
    private static String filePath = "/Users/xuhongzhi/studen/io/src/main/java/cn/piao888/chatroom/kafka/follow";

    public TargetFile() throws IOException {
        super(filePath);
        RandomAccessFile randomAccessFile = new RandomAccessFile(this, "rw");
        this.fileChannel = randomAccessFile.getChannel();
        //映射一个512Mb的内存大小到bytebuffer
        this.byteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, 1024 * 1024 * 512);
        this.readBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, 1024 * 1024 * 512);
    }

}
