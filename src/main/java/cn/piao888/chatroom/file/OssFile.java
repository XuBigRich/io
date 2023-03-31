package cn.piao888.chatroom.file;

import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    public OssFile(String filePath, Long fileOffset, List<String> deviceIds) throws IOException {
        final String[] split = filePath.split(File.pathSeparator);
        this.fileName = split[split.length - 1];
        this.filePath = filePath;
        this.fileOffset = fileOffset;
        this.deviceIds = deviceIds;
        this.accessFile = new RandomAccessFile(filePath, "rw");
        this.separator = System.getProperty("line.separator");
        this.mappedByteBuffer = accessFile.getChannel().map(FileChannel.MapMode.READ_WRITE, this.fileOffset, contentSize(deviceIds));

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
        });
    }

    public static void main(String[] args) throws IOException, InterruptedException {
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
                "JAVA","C#","C++",".NET");

        OssFile ossFile = new OssFile(System.getProperty("user.dir") + File.separator + "file", new File(System.getProperty("user.dir") + File.separator + "file").length(), new ArrayList<>(text));
        WriteTask writeTask = new WriteTask(ossFile, text.size());
        ForkJoinPool pool = new ForkJoinPool();
        pool.execute(writeTask);
        Thread.sleep(2000l);
    }


}
