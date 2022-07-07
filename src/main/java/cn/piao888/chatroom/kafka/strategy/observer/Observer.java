package cn.piao888.chatroom.kafka.strategy.observer;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * @Author： hongzhi.xu
 * @Date: 2022/7/7 9:03 上午
 * @Version 1.0
 */
public interface Observer {
    boolean supports(SelectionKey selectionKey);

    void execute(SelectionKey selector) throws IOException;
}
