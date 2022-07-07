package cn.piao888.chatroom.kafka;

import lombok.Data;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

    @SneakyThrows
    @Override
    public void run() {
        while (true) {
            //当读位置与写位置不同步时
            if (file.getReadPosition() < file.getWritePosition()) {
                ByteBuffer byteBuffer;
                //这个synchronized不起作用  代码块中会有线程安全问题，造成 文件中的数据覆盖，问题
                synchronized (file) {
                    int readPosition = (int) file.getReadPosition();
                    int writePosition = (int) file.getWritePosition();
                    //原来的 读取/限制 位置
                    int oldPosition = file.byteBuffer.position();
                    int oldLimit = file.byteBuffer.limit();
                    //设置要截取的长度位置
                    file.byteBuffer.position(readPosition);
                    file.byteBuffer.limit(writePosition);
                    //截取ByteBuffer
                    byteBuffer = file.byteBuffer.slice();
                    //指针读取/限制 位置归位
                    file.byteBuffer.position(oldPosition);
                    file.byteBuffer.limit(oldLimit);
                }

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
