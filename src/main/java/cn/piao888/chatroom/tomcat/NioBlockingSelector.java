package cn.piao888.chatroom.tomcat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
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
    //这个类继承自Thread
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
        //线程开始执行
        poller.start();
    }

    public void close() {
        if (poller != null) {
            poller.disable();
            poller.interrupt();
            poller = null;
        }
    }

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

    protected static class BlockPoller extends Thread {
        protected volatile boolean run = true;
        protected Selector selector = null;
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
