package cn.piao888.chatroom.tomcat;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread safe non blocking selector pool
 *
 * @version 1.0
 * @since 6.0
 */

public class NioSelectorPool {


    protected static final boolean SHARED =
            Boolean.parseBoolean(System.getProperty("org.apache.tomcat.util.net.NioSelectorShared", "true"));

    protected NioBlockingSelector blockingSelector;

    protected volatile Selector SHARED_SELECTOR;

    protected int maxSelectors = 200;
    protected long sharedSelectorTimeout = 30000;
    protected int maxSpareSelectors = -1;
    protected boolean enabled = true;
    //线程安全的计数器
    protected AtomicInteger active = new AtomicInteger(0);
    protected AtomicInteger spare = new AtomicInteger(0);
    protected ConcurrentLinkedQueue<Selector> selectors =
            new ConcurrentLinkedQueue<>();

    /**
     * 生成一个多路复用器
     *
     * @return
     * @throws IOException
     */
    protected Selector getSharedSelector() throws IOException {
        if (SHARED && SHARED_SELECTOR == null) {
            synchronized (NioSelectorPool.class) {
                if (SHARED_SELECTOR == null) {
                    SHARED_SELECTOR = Selector.open();
                }
            }
        }
        return SHARED_SELECTOR;
    }

    /**
     *  获取到多路复用器
     * @return
     * @throws IOException
     */
    public Selector get() throws IOException {
        if (SHARED) {
            return getSharedSelector();
        }
        if ((!enabled) || active.incrementAndGet() >= maxSelectors) {
            if (enabled) active.decrementAndGet();
            return null;
        }
        Selector s = null;
        try {
            s = selectors.size() > 0 ? selectors.poll() : null;
            if (s == null) {
                s = Selector.open();
            } else spare.decrementAndGet();

        } catch (NoSuchElementException x) {
            try {
                s = Selector.open();
            } catch (IOException iox) {
            }
        } finally {
            if (s == null) active.decrementAndGet();//we were unable to find a selector
        }
        return s;
    }


    public void put(Selector s) throws IOException {
        if (SHARED) return;
        if (enabled) active.decrementAndGet();
        if (enabled && (maxSpareSelectors == -1 || spare.get() < Math.min(maxSpareSelectors, maxSelectors))) {
            spare.incrementAndGet();
            selectors.offer(s);
        } else s.close();
    }

    public void close() throws IOException {
        enabled = false;
        Selector s;
        while ((s = selectors.poll()) != null) s.close();
        spare.set(0);
        active.set(0);
        if (blockingSelector != null) {
            blockingSelector.close();
        }
        if (SHARED && getSharedSelector() != null) {
            getSharedSelector().close();
            SHARED_SELECTOR = null;
        }
    }

    public void open() throws IOException {
        //设置selector为打开状态
        enabled = true;
        getSharedSelector();
        if (SHARED) {
            blockingSelector = new NioBlockingSelector();
            blockingSelector.open(getSharedSelector());
        }

    }


    public int write(ByteBuffer buf, SocketChannel socket, Selector selector,
                     long writeTimeout, boolean block) throws IOException {
        if (SHARED && block) {
            return blockingSelector.write(buf, socket, writeTimeout);
        }
        SelectionKey key = null;
        int written = 0;
        boolean timedout = false;
        int keycount = 1; //assume we can write
        long time = System.currentTimeMillis(); //start the timeout timer
        try {
            while ((!timedout) && buf.hasRemaining()) {
                int cnt = 0;
                if (keycount > 0) { //only write if we were registered for a write
                    cnt = socket.write(buf); //write the data
                    if (cnt == -1) throw new EOFException();

                    written += cnt;
                    if (cnt > 0) {
                        time = System.currentTimeMillis(); //reset our timeout timer
                        continue; //we successfully wrote, try again without a selector
                    }
                    if (cnt == 0 && (!block)) break; //don't block
                }
                if (selector != null) {
                    //register OP_WRITE to the selector
                    if (key == null) key = socket.register(selector, SelectionKey.OP_WRITE);
                    else key.interestOps(SelectionKey.OP_WRITE);
                    if (writeTimeout == 0) {
                        timedout = buf.hasRemaining();
                    } else if (writeTimeout < 0) {
                        keycount = selector.select();
                    } else {
                        keycount = selector.select(writeTimeout);
                    }
                }
                if (writeTimeout > 0 && (selector == null || keycount == 0))
                    timedout = (System.currentTimeMillis() - time) >= writeTimeout;
            }//while
            if (timedout) throw new SocketTimeoutException();
        } finally {
            if (key != null) {
                key.cancel();
                if (selector != null) selector.selectNow();//removes the key from this selector
            }
        }
        return written;
    }

    public int read(ByteBuffer buf, SocketChannel socket, Selector selector, long readTimeout) throws IOException {
        return read(buf, socket, selector, readTimeout, true);
    }

    public int read(ByteBuffer buf, SocketChannel socket, Selector selector, long readTimeout, boolean block) throws IOException {
        if (SHARED && block) {
            return blockingSelector.read(buf, socket, readTimeout);
        }
        SelectionKey key = null;
        int read = 0;
        boolean timedout = false;
        int keycount = 1; //assume we can write
        long time = System.currentTimeMillis(); //start the timeout timer
        try {
            while ((!timedout)) {
                int cnt = 0;
                if (keycount > 0) { //only read if we were registered for a read
                    cnt = socket.read(buf);
                    if (cnt == -1) {
                        if (read == 0) {
                            read = -1;
                        }
                        break;
                    }
                    read += cnt;
                    if (cnt > 0) continue; //read some more
                    if (cnt == 0 && (read > 0 || (!block))) break; //we are done reading
                }
                if (selector != null) {//perform a blocking read
                    //register OP_WRITE to the selector
                    if (key == null) key = socket.register(selector, SelectionKey.OP_READ);
                    else key.interestOps(SelectionKey.OP_READ);
                    if (readTimeout == 0) {
                        timedout = (read == 0);
                    } else if (readTimeout < 0) {
                        keycount = selector.select();
                    } else {
                        keycount = selector.select(readTimeout);
                    }
                }
                if (readTimeout > 0 && (selector == null || keycount == 0))
                    timedout = (System.currentTimeMillis() - time) >= readTimeout;
            }//while
            if (timedout) throw new SocketTimeoutException();
        } finally {
            if (key != null) {
                key.cancel();
                if (selector != null) selector.selectNow();//removes the key from this selector
            }
        }
        return read;
    }

    public void setMaxSelectors(int maxSelectors) {
        this.maxSelectors = maxSelectors;
    }

    public void setMaxSpareSelectors(int maxSpareSelectors) {
        this.maxSpareSelectors = maxSpareSelectors;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setSharedSelectorTimeout(long sharedSelectorTimeout) {
        this.sharedSelectorTimeout = sharedSelectorTimeout;
    }

    public int getMaxSelectors() {
        return maxSelectors;
    }

    public int getMaxSpareSelectors() {
        return maxSpareSelectors;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getSharedSelectorTimeout() {
        return sharedSelectorTimeout;
    }

    public ConcurrentLinkedQueue<Selector> getSelectors() {
        return selectors;
    }

    public AtomicInteger getSpare() {
        return spare;
    }
}