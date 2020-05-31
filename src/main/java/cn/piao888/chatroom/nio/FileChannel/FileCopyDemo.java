package cn.piao888.chatroom.nio.FileChannel;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

interface FileCopyRunner {
    void copyFile(File source, File target) throws IOException;
}

public class FileCopyDemo {
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        File source = new File("/Users/xuhongzhi/studen/io/src/main/java/cn/piao888/chatroom/nio/nio说明");
        File target = new File("/Users/xuhongzhi/studen/io/src/main/resources/nio说明");
        //===========stream 流方式的io拷贝===========
        //不使用Buffer缓存
        FileCopyRunner noBufferStreamCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File target) {
                FileInputStream fileInputStream = null;
                FileOutputStream fileOutputStream = null;
                try {
                    fileInputStream = new FileInputStream(source);
                    fileOutputStream = new FileOutputStream(target);
                    int i = 0;
                    byte[] bytes = new byte[1024];
                    //每次读会从流中 将字节存入bytes缓存中
                    while ((i = fileInputStream.read(bytes, 0, bytes.length)) != -1) {
                        //每次写会将bytes写到流中
                        fileOutputStream.write(bytes);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(fileInputStream);
                    close(fileOutputStream);
                }
            }
        };
//        noBufferStreamCopy.copyFile(source,target);
        //使用Buffer缓存
        FileCopyRunner BufferStreamCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File target) {
                FileInputStream fileInputStream = null;
                FileOutputStream fileOutputStream = null;
                try {
                    fileInputStream = new FileInputStream(source);
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                    fileOutputStream = new FileOutputStream(target);
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

                    byte[] bytes = new byte[1024];
                    int result;
                    while ((result = fileInputStream.read(bytes)) != -1) {
                        bufferedOutputStream.write(bytes, 0, result);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(fileInputStream);
                    close(fileOutputStream);
                }

            }
        };

        //===========channel 通道方式的io拷贝===========
        //通道使用buffer的文件拷贝
        FileCopyRunner nioBufferCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File target) {
                FileChannel fin = null;
                FileChannel fout = null;
                try {
                    fin = new FileInputStream(source).getChannel();
                    fout = new FileOutputStream(target).getChannel();
                    //通道中的数据需要缓存入Buffer中，才可进行操作，
                    // 而且有一个小技巧
                    // 通道是读通道时，对Buffer的操作是一个写操作
                    // 通道是写通道时，对Buffer的操作是一个读操作
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    while ((fin.read(byteBuffer)) != -1) {
                        //将Buffer状态改为写状态
                        byteBuffer.flip();
                        //fout.write()方法不能保证 一次性全部写入通道，所以需要使用hasRemaining方法判断一下 以保证数据完整性
                        while (byteBuffer.hasRemaining()) {
                            fout.write(byteBuffer);
                        }
                        //将Buffer状态改为读状态
                        byteBuffer.clear();
                    }
//                    错误示范  这样会一直 一直去写，造成了 《nio说明》这个文件非常非常非常大，在短时间内达到了11个G 只因为没有把byteBuffer转为写状态
                    /*while ((fin.read(byteBuffer)) != -1) {
                        fout.write(byteBuffer);
                    }*/
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(fin);
                    close(fout);
                }
            }
        };
        nioBufferCopy.copyFile(source, target);

        //通道间的文件拷贝  (不使用Buffer)
        FileCopyRunner nioTransferCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File target) {
                FileChannel fin = null;
                FileChannel fout = null;
                try {
                    fin = new FileInputStream(source).getChannel();
                    fout = new FileOutputStream(target).getChannel();
                    long transferred = 0L;
                    long size = fin.size();
                    while (transferred != size) {
                        //fin.size() 可以知道源文件byte的长度
                        transferred += fin.transferTo(0, size, fout);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(fin);
                    close(fout);
                }

            }
        };
//        nioTransferCopy.copyFile(source, target);
    }
}
