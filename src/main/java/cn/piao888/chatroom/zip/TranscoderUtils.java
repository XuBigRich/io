package cn.piao888.chatroom.zip;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * @author 许鸿志
 * @since 2022/8/9
 */
public class TranscoderUtils {


    public static File download(String filePath, String fileName) throws IOException {

        HttpURLConnection uc = null;
        try {
            URL url = new URL(filePath);
            uc = (HttpURLConnection) url.openConnection();
            // 设置的值 doInput领域本 URLConnection指定值。
            uc.setDoInput(true);
            // 打开与此URL引用的资源的通信链接，如果此类连接尚未建立。
            uc.connect();
            // 获取服务端的字节输入流
            InputStream inputStream = uc.getInputStream();
            Files.copy(inputStream, Paths.get(fileName));
            return new File(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (uc != null) {
                uc.disconnect();
            }
        }
        throw new IOException("获取zip压缩包异常！");
    }


    //解压文件 到临时目录
    public static List<File> getFile(String filePath) throws IOException {
        Long currentTime = System.currentTimeMillis();
        //创建临时文件夹
        Path path = Files.createTempDirectory(currentTime+ "");
        //创建下载路径
        String fileName = path.toString() + File.separator + currentTime + ".zip";
        //进行压缩包下载
        File file = download(filePath, fileName);
        //对下载下来的文件解压缩
        ZipFile zipFile = new ZipFile(file, Charset.forName("gbk"));
        Enumeration enumeration = zipFile.entries();
        while (enumeration.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) enumeration.nextElement();
            byte[] bytes = new byte[1024];
            InputStream inputStream = zipFile.getInputStream(zipEntry);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int i = -1;
            while ((i = inputStream.read(bytes)) != -1) {
                byteArrayOutputStream.write(bytes, 0, i);
            }
//            Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxrwxrwx");
//            FileAttribute<Set<PosixFilePermission>> fileAttributes = PosixFilePermissions
//                    .asFileAttribute(permissions);
//            //创建临时文件夹
//            //linux
//            Path files = Files.createTempFile(path+File.separator, null,null,fileAttributes);
            //创建临时文件夹
            Path files = Files.createTempFile(path, null,".jpg");
            //向临时文件夹写入文件
            Files.write(files, byteArrayOutputStream.toByteArray(), new OpenOption[] {StandardOpenOption.WRITE});
        }
        return Arrays.asList(path.toFile().listFiles());
    }

    /**
     * @param readImage 读取一维码图片名
     * @return void
     */
    public static String readCode(File readImage) throws Exception {
        try {
            BufferedImage image = ImageIO.read(readImage);
            if (image == null) {
                return null;
            }
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Map<DecodeHintType, Object> hints = new HashMap<DecodeHintType, Object>();
            hints.put(DecodeHintType.CHARACTER_SET, "gbk");
            hints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

            Result result = new MultiFormatReader().decode(bitmap, hints);
            return result.getText();
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new Exception("内部异常");
    }

    /**
     * 解析读取二维码
     *
     * @param readImage 二维码图片
     * @return
     */
    public static String decodeQRcode(File readImage) {
        BufferedImage image;
        String qrCodeText = null;
        try {
            image = ImageIO.read(readImage);
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            Binarizer binarizer = new HybridBinarizer(source);
            BinaryBitmap binaryBitmap = new BinaryBitmap(binarizer);
            Map<DecodeHintType, Object> hints = new HashMap<DecodeHintType, Object>();
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
            // 对图像进行解码
            Result result = new MultiFormatReader().decode(binaryBitmap, hints);
            qrCodeText = result.getText();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        return qrCodeText;
    }

    public static void main(String[] args) throws Exception {
        List<File> files = getFile("http://127.0.0.1:8081/down?fileName=Desktop.zip");
        for (File file : files) {
            if(file.getName().contains(".jpg")){
                System.out.println(readCode(file));
            }
//            System.out.println(file.getName());
        }
    }
}
