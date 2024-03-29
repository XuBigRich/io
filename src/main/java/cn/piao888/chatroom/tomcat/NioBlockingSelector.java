package cn.piao888.chatroom.tomcat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author： hongzhi.xu
 * @Date: 2021/9/13 8:02 下午
 * @Version 1.0
 */
public class NioBlockingSelector {
    protected Selector sharedSelector;
    //这个类继承自Thread 他会一直检查多路复用器所监听的socket
    protected BlockPoller poller;
    private static AtomicInteger threadCounter = new AtomicInteger(0);
    private final SynchronizedStack<KeyReference> keyReferenceStack =
            new SynchronizedStack<>();

    public void open(Selector selector) {
        sharedSelector = selector;
        poller = new BlockPoller();
        //给poller 赋值一个多路复用器
        poller.selector = sharedSelector;
        poller.setDaemon(true);
        poller.setName("NioBlockingSelector.BlockPoller-" + (threadCounter.getAndIncrement()));
        //线程开始执行 这线程的使命是 不断监听多路复用器产生的事件
        poller.start();
    }

    public void close() {
        if (poller != null) {
            poller.disable();
            poller.interrupt();
            poller = null;
        }
    }

    /**
     * 实际执行读写的方法
     *
     * @param buf
     * @param socket
     * @param writeTimeout
     * @return
     */
    public int write(ByteBuffer buf, SocketChannel socket, long writeTimeout) {
        SelectionKey key = socket.keyFor(sharedSelector);
        if (key == null) System.err.println("Key no longer registered");
        KeyReference reference = keyReferenceStack.pop();
        if (reference == null) {
            reference = new KeyReference();
        }
        NioSocketWrapper att = (NioSocketWrapper) key.attachment();
        int written = 0;
        boolean timedout = false;
        int keycount = 1; //assume we can write
        long time = System.currentTimeMillis(); //start the timeout timer
        try {
            while ((!timedout) && buf.hasRemaining()) {
                if (keycount > 0) { //only write if we were registered for a write
                    int cnt = socket.write(buf); //write the data
                    if (cnt == -1)
                        System.out.println("EOFException");
                    written += cnt;
                    if (cnt > 0) {
                        time = System.currentTimeMillis(); //reset our timeout timer
                        continue; //we successfully wrote, try again without a selector
                    }
                }
                try {
                    if (att.getWriteLatch() == null || att.getWriteLatch().getCount() == 0) att.startWriteLatch(1);
                    poller.add(att, SelectionKey.OP_WRITE, reference);
                    if (writeTimeout < 0) {
                        att.awaitWriteLatch(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                    } else {
                        att.awaitWriteLatch(writeTimeout, TimeUnit.MILLISECONDS);
                    }
                } catch (InterruptedException ignore) {
                    // Ignore
                }
                if (att.getWriteLatch() != null && att.getWriteLatch().getCount() > 0) {
                    //we got interrupted, but we haven't received notification from the poller.
                    keycount = 0;
                } else {
                    //latch countdown has happened
                    keycount = 1;
                    att.resetWriteLatch();
                }

                if (writeTimeout > 0 && (keycount == 0))
                    timedout = (System.currentTimeMillis() - time) >= writeTimeout;
            } //while
            if (timedout)
                System.out.println("timedout");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            poller.remove(att, SelectionKey.OP_WRITE);
            if (timedout && reference.key != null) {
                poller.cancelKey(reference.key);
            }
            reference.key = null;
            keyReferenceStack.push(reference);
        }
        return written;
    }

    public int read(ByteBuffer buf, SocketChannel socket, long writeTimeout) {
        return 1;
    }

    /**
     * 这个类是用来检测多路复用器 的检测事件的
     */
    protected static class BlockPoller extends Thread {
        protected volatile boolean run = true;
        protected Selector selector = null;
        protected final SynchronizedQueue<Runnable> events = new SynchronizedQueue<>();
        protected final AtomicInteger wakeupCounter = new AtomicInteger(0);

        public void disable() {
            run = false;
            selector.wakeup();
        }

        public void cancelKey(final SelectionKey key) {
            wakeup();
        }

        public void remove(final NioSocketWrapper key, final int ops) {
            if (key == null) return;
            SocketChannel ch = key.getSocket();
            if (ch == null) return;

            wakeup();
        }

        public void add(final NioSocketWrapper key, final int ops, final KeyReference ref) {
            if (key == null) return;
            final SocketChannel ch = key.socketChannel;
            if (ch == null) return;
            wakeup();
        }

        public void wakeup() {
            if (wakeupCounter.addAndGet(1) == 0) selector.wakeup();
        }

        public boolean events() {
            Runnable r = null;
            //读取events队列大小，当有socket建立链接时，队列就会放入值
            int size = events.size();
            //遍历队列中的每一个事件
            for (int i = 0; i < size && (r = events.poll()) != null; i++) {
                //执行运行
                r.run();
            }
            //返回size是否大于0
            return (size > 0);
        }

        public void run() {
            while (run) {
                try {
                    //检查事件队列，并遍历
                    events();
                    int keyCount = 0;
                    try {
                        int i = wakeupCounter.get();
                        if (i > 0)
                            keyCount = selector.selectNow();
                        else {
                            wakeupCounter.set(-1);
                            //启动多路复用器的检查工作
                            keyCount = selector.select(1000);
                        }
                        wakeupCounter.set(0);
                        if (!run) break;
                    } catch (NullPointerException x) {
                        continue;
                    } catch (CancelledKeyException x) {
                        continue;
                    } catch (Throwable x) {
                        continue;
                    }

                    Iterator<SelectionKey> iterator = keyCount > 0 ? selector.selectedKeys().iterator() : null;

                    // Walk through the collection of ready keys and dispatch
                    // any active event.
                    while (run && iterator != null && iterator.hasNext()) {
                        SelectionKey sk = iterator.next();
                        NioSocketWrapper attachment = (NioSocketWrapper) sk.attachment();
                        try {
                            iterator.remove();
                            sk.interestOps(sk.interestOps() & (~sk.readyOps()));
                            if (sk.isReadable()) {
                                countDown(attachment.getReadLatch());
                            }
                            if (sk.isWritable()) {
                                countDown(attachment.getWriteLatch());
                            }
                        } catch (CancelledKeyException ckx) {
                            sk.cancel();
                            countDown(attachment.getReadLatch());
                            countDown(attachment.getWriteLatch());
                        }
                    }//while
                } catch (Throwable t) {
                }
            }
            events.clear();
            if (selector.isOpen()) {
                try {
                    // Cancels all remaining keys
                    selector.selectNow();
                } catch (Exception ignore) {
                }
            }
            try {
                selector.close();
            } catch (Exception ignore) {
            }
        }





        public void countDown(CountDownLatch latch) {
            if (latch == null) return;
            latch.countDown();
        }

        public void cancel(SelectionKey sk, NioSocketWrapper key, int ops) {
            if (sk != null) {
                sk.cancel();
                sk.attach(null);
                if (SelectionKey.OP_WRITE == (ops & SelectionKey.OP_WRITE)) countDown(key.getWriteLatch());
                if (SelectionKey.OP_READ == (ops & SelectionKey.OP_READ)) countDown(key.getReadLatch());
            }
        }

        private class RunnableAdd implements Runnable {

            private final SocketChannel ch;
            private final NioSocketWrapper key;
            private final int ops;
            private final KeyReference ref;

            public RunnableAdd(SocketChannel ch, NioSocketWrapper key, int ops, KeyReference ref) {
                this.ch = ch;
                this.key = key;
                this.ops = ops;
                this.ref = ref;
            }

            @Override
            //这个方法会被BlockPoller 类的run方法调用的events方法调用
            public void run() {
                SelectionKey sk = ch.keyFor(selector);
                try {
                    if (sk == null) {
                        sk = ch.register(selector, ops, key);
                        ref.key = sk;
                    } else if (!sk.isValid()) {
                        cancel(sk, key, ops);
                    } else {
                        sk.interestOps(sk.interestOps() | ops);
                    }
                } catch (CancelledKeyException cx) {
                    cancel(sk, key, ops);
                } catch (ClosedChannelException cx) {
                    cancel(null, key, ops);
                }
            }
        }

    }

    public static class KeyReference {
        SelectionKey key = null;

        public void finalize() {
            if (key != null && key.isValid()) {
                try {
                    key.cancel();
                } catch (Exception ignore) {
                }
            }
        }
    }

    public static class NioSocketWrapper {
        private CountDownLatch readLatch = null;
        private CountDownLatch writeLatch = null;
        private SocketChannel socketChannel;

        public NioSocketWrapper(CountDownLatch readLatch, CountDownLatch writeLatch, SocketChannel socketChannel) {
            this.readLatch = readLatch;
            this.writeLatch = writeLatch;
            this.socketChannel = socketChannel;
        }


        public SocketChannel getSocket() {
            return socketChannel;
        }

        public void setSocketChannel(SocketChannel socketChannel) {
            this.socketChannel = socketChannel;
        }

        public CountDownLatch getReadLatch() {
            return readLatch;
        }

        public void setReadLatch(CountDownLatch readLatch) {
            this.readLatch = readLatch;
        }

        public CountDownLatch getWriteLatch() {
            return writeLatch;
        }

        public void setWriteLatch(CountDownLatch writeLatch) {
            this.writeLatch = writeLatch;
        }

        public void awaitReadLatch(long timeout, TimeUnit unit) throws InterruptedException {
            awaitLatch(readLatch, timeout, unit);
        }

        public void startWriteLatch(int cnt) {
            writeLatch = startLatch(writeLatch, cnt);
        }

        protected CountDownLatch startLatch(CountDownLatch latch, int cnt) {
            if (latch == null || latch.getCount() == 0) {
                return new CountDownLatch(cnt);
            } else throw new IllegalStateException("Latch must be at count 0 or null.");
        }

        public void resetWriteLatch() {
            writeLatch = resetLatch(writeLatch);
        }

        protected CountDownLatch resetLatch(CountDownLatch latch) {
            if (latch == null || latch.getCount() == 0) return null;
            else throw new IllegalStateException("Latch must be at count 0");
        }

        public void awaitWriteLatch(long timeout, TimeUnit unit) throws InterruptedException {
            awaitLatch(writeLatch, timeout, unit);
        }

        protected void awaitLatch(CountDownLatch latch, long timeout, TimeUnit unit) throws InterruptedException {
            if (latch == null) throw new IllegalStateException("Latch cannot be null");
            // Note: While the return value is ignored if the latch does time
            //       out, logic further up the call stack will trigger a
            //       SocketTimeoutException
            latch.await(timeout, unit);
        }
    }
}
