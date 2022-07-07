package cn.piao888.chatroom.kafka.strategy.observer;

import cn.piao888.chatroom.kafka.Receive;
import cn.piao888.chatroom.kafka.TargetFile;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * @Author： hongzhi.xu
 * @Date: 2022/7/7 10:30 上午
 * @Version 1.0
 */
public class ReadObserver implements Observer {
    private Receive receive;
    private TargetFile file;


    public ReadObserver(Receive receive) throws IOException {
        this.receive = receive;
        this.file = receive.getFile();
    }

    @Override
    public boolean supports(SelectionKey selectionKey) {
        return selectionKey.isReadable();
    }

    @Override
    public void execute(SelectionKey selector) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selector.channel();
        //直接将数据读取到mmap内存中 ，实现零拷贝
        socketChannel.read(file.byteBuffer);
        file.byteBuffer.force();
        file.setWritePosition(file.byteBuffer.position());

    }
}
