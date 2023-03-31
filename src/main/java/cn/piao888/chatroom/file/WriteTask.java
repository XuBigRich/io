package cn.piao888.chatroom.file;

import cn.piao888.chatroom.file.OssFile;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;

/**
 * @Author： hongzhi.xu
 * @Date: 2023/3/28 3:43 下午
 * @Version 1.0
 */
public class WriteTask extends RecursiveAction {
    public static Integer threads = Runtime.getRuntime().availableProcessors() * 2;
    //总数据数
    public Integer count;
    public ArrayList<WriteTask> writeTasks;
    public OssFile ossFile;
    public Long offset;


    public WriteTask(OssFile ossFile, Integer count) {
        this.count = count;
        this.ossFile = ossFile;
        this.offset = ossFile.getFileOffset();
    }


    @SneakyThrows
    @Override
    protected void compute() {
        //计算每个线程要处理的任务量
        Integer taskCount = count / threads;
        //要么要处理的数据小于 cpu个数  ，要么 将数据按照cpu等分
        if (ossFile.getDeviceIds().size() <= 2 * WriteTask.threads) {
            ossFile.writeFile();
        } else {
            this.writeTasks = new ArrayList<>();

            for (int i = 0; i < threads - 1; i++) {
                final List<String> deviceIds = this.ossFile.getDeviceIds().subList(i * taskCount, (i + 1) * taskCount);
                this.writeTasks.add(new WriteTask(new OssFile(ossFile.getFilePath(), offset, deviceIds), count));
                offset += ossFile.contentSize(deviceIds);
            }
            final List<String> deviceId = this.ossFile.getDeviceIds().subList((threads - 1) * taskCount, count);
            this.writeTasks.add(new WriteTask(new OssFile(ossFile.getFilePath(), offset, deviceId), count));
        }
        if (writeTasks != null && !writeTasks.isEmpty()) {
            /**invokeAll 执行子任务下的compute 方法**/
            for (WriteTask writeTask : invokeAll(writeTasks)) {
                //这个地方就是让子任务先执行，然后获取子任务的返回值，如果无返回值这个地方可以不加
                writeTask.join();
            }
        }
    }
}
