package cn.piao888.chatroom.file.jvm;

import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ForkJoinPool;

/**
 * @Author： hongzhi.xu
 * @Date: 2023/3/27 2:10 下午
 * @Version 1.0
 */
@Data
public class OssFile {
    private String fileName;
    private String filePath;
    /**
     * 偏移量
     */
    private Long fileOffset;
    private List<String> deviceIds;
    private RandomAccessFile accessFile;
    private MappedByteBuffer mappedByteBuffer;
    private String separator;
    public ByteBuffer byteNonDirectBuffer;
    public ByteBuffer byteDirectBuffer;
    private FileChannel channel;

    public OssFile(String filePath, Long fileOffset, List<String> deviceIds) throws IOException {
        final String[] split = filePath.split(File.pathSeparator);
        this.fileName = split[split.length - 1];
        this.filePath = filePath;
        this.fileOffset = fileOffset;
        this.deviceIds = deviceIds;
        this.accessFile = new RandomAccessFile(filePath, "rw");
        this.separator = System.getProperty("line.separator");
    }

    public Long contentSize(List<String> deviceIds) throws UnsupportedEncodingException {
        Long size = 0l;
        for (String d : deviceIds) {
            size += (d + separator).getBytes("UTF-8").length;
        }
        return size;
    }

    public void writeFile() {
        this.deviceIds.forEach(e -> {
            final byte[] bytes = (e + separator).getBytes(StandardCharsets.UTF_8);
            mappedByteBuffer.put(bytes);
            mappedByteBuffer.force();
            //不可以写下面的，否则会清空已经写了的数据，即使force之后
//            mappedByteBuffer.clear();
        });
//        mappedByteBuffer=null;
//        System.gc();
    }

    public void applyNonDirect() {
        this.byteNonDirectBuffer = ByteBuffer.allocate(1024000000);
        System.out.println(byteNonDirectBuffer.isDirect());
    }

    public void creteDirMap() throws IOException {
        this.channel = accessFile.getChannel();
        this.mappedByteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, this.fileOffset, contentSize(deviceIds));
        System.out.println(mappedByteBuffer.isDirect());
    }

    public void applyDirect() {
        this.byteDirectBuffer = ByteBuffer.allocateDirect(1024000000);
        System.out.println(byteDirectBuffer.isDirect());
    }

    public static void main(String[] args) throws Throwable {
        System.out.println();
        List<String> text = Arrays.asList("args",
                "111",
                "123123",
                "ssss",
                "xhz",
                "aaaaa",
                "123123ssss",
                "asdlajsln",
                "1@#!#@",
                "MMMJJJJJKKK",
                "^^^^^6",
                ")))))))",
                "SSSSSJJJ",
                "*UJUJJHH())",
                "JMMMMKKKSSS",
                "JAVA", "C#", "C++", ".NET");


        Scanner scanner = new Scanner(System.in);
        OssFile ossFile = null;
        while (scanner.hasNext()) {
            final String next = scanner.next();
            try {
                if ("1".equals(next)) {
                    ossFile = new OssFile(System.getProperty("user.dir") + File.separator + "file", new File(System.getProperty("user.dir") + File.separator + "file").length(), new ArrayList<>(text));
                }
                if ("2".equals(next)) {
                    ossFile.applyNonDirect();
                }
                if ("3".equals(next)) {
                    ossFile.byteNonDirectBuffer = null;
                }
                if ("4".equals(next)) {
                    ossFile.applyDirect();
                }

                if ("5".equals(next)) {
                    ossFile.byteDirectBuffer = null;
                }
                if ("6".equals(next)) {
                    ossFile.creteDirMap();
                }
                if ("7".equals(next)) {
                    ossFile.writeFile();
                    ossFile.accessFile.close();
                    ossFile.channel.close();

                }
                if ("8".equals(next)) {
                    ossFile.mappedByteBuffer = null;
                }
                if ("9".equals(next)) {
                    System.gc();
                }
                if ("11".equals(next)) {
                    ossFile.finalize();
                }
                if ("22".equals(next)) {
                    ossFile = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//        WriteTask writeTask = new WriteTask(ossFile, text.size());
//        ForkJoinPool pool = new ForkJoinPool();
//        pool.execute(writeTask);
//        Thread.sleep(2000l);
    }


}
