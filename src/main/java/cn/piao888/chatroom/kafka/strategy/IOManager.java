package cn.piao888.chatroom.kafka.strategy;

import cn.piao888.chatroom.kafka.strategy.observer.Observer;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;

/**
 * Io管理器
 *
 * @Author： hongzhi.xu
 * @Date: 2022/7/7 9:00 上午
 * @Version 1.0
 */
public class IOManager {
    public List<Observer> observers = new ArrayList<>();

    public void addObservers(Observer observer) {
        observers.add(observer);
    }

    public void execute(SelectionKey selectionKey) throws IOException {
        for (Observer observer : observers) {
            if (observer.supports(selectionKey)) {
                observer.execute(selectionKey);
            }
        }
    }
}
