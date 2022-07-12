package cn.piao888.chatroom.kafka;

import lombok.Data;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;

/**
 * 每隔 2秒钟向 客户端返回最新获取到的消息
 *
 * @Author： hongzhi.xu
 * @Date: 2022/7/6 11:05 下午
 * @Version 1.0
 */
@Data
public class Transmit implements Runnable {
    private TargetFile file;
    private Selector selector;

    public Transmit(Selector selector, TargetFile file) {
        this.file = file;
        this.selector = selector;
    }

    public ByteBuffer getByteBufferOld(int readPosition, int writePosition) throws IOException {
        //这个synchronized不起作用  代码块中会有线程安全问题，造成 文件中的数据覆盖，问题
        //可以使用fileChannel.lock() 通道锁的方式给 文件加锁
//                synchronized (file) {
        //给通道加锁
        FileLock fileLock = file.fileChannel.lock();
        //原来的 读取/限制 位置
        int oldPosition = file.byteBuffer.position();
        int oldLimit = file.byteBuffer.limit();
        //设置要截取的长度位置
        file.byteBuffer.position(readPosition);
        file.byteBuffer.limit(writePosition);
        //截取ByteBuffer
        ByteBuffer byteBuffer = file.byteBuffer.slice();
        //指针读取/限制 位置归位
        file.byteBuffer.position(oldPosition);
        file.byteBuffer.limit(oldLimit);
        //
        fileLock.release();
        return byteBuffer;
//                }
    }

    public ByteBuffer getByteBufferNew(int readPosition, int writePosition) throws IOException {
        file.readBuffer.position(readPosition);
        file.readBuffer.limit(writePosition);
        ByteBuffer byteBuffer = file.readBuffer.slice();
        return byteBuffer;
    }

    @SneakyThrows
    @Override
    public void run() {
        while (true) {
            //当读位置与写位置不同步时
            if (file.getReadPosition() < file.getWritePosition()) {
                //获取文件读写位置
                int readPosition = (int) file.getReadPosition();
                int writePosition = (int) file.getWritePosition();
//                ByteBuffer byteBuffer = getByteBufferOld(readPosition, writePosition);
                ByteBuffer byteBuffer = getByteBufferNew(readPosition, writePosition);
                for (SelectionKey selectionKey : selector.keys()) {
                    SelectableChannel sc;
                    if ((sc = selectionKey.channel()) instanceof SocketChannel) {
                        ((SocketChannel) sc).write(byteBuffer);
                    }
                }
                //读位置与写位置同步
                file.readPosition = file.writePosition;
            }
        }
    }
}
