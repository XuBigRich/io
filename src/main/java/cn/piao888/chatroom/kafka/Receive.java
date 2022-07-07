package cn.piao888.chatroom.kafka;

import cn.piao888.chatroom.kafka.strategy.IOManager;
import cn.piao888.chatroom.kafka.strategy.observer.AcceptObserver;
import cn.piao888.chatroom.kafka.strategy.observer.ReadObserver;
import lombok.Data;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 接收线程
 * 将收到的信息放入到 缓存的文件中
 *
 * @Author： hongzhi.xu
 * @Date: 2022/7/6 11:04 下午
 * @Version 1.0
 */
@Data
public class Receive implements Runnable {

    private TargetFile file = null;
    private Selector selector = null;
    private IOManager ioManager;

    //给Io管理者添加监听器
    public Receive(Selector selector, TargetFile file) throws IOException {
        this.ioManager = new IOManager();
        this.file = file;
        this.selector = selector;
        ioManager.addObservers(new AcceptObserver());
        ioManager.addObservers(new ReadObserver(this));
    }

    @SneakyThrows
    @Override
    public void run() {
        //初始化 socket与selector
        while (true) {
            int event = selector.select(1000);
            if (event > 0) {
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    ioManager.execute(iterator.next());
                }
                selectionKeys.clear();
            }
        }
    }


}
