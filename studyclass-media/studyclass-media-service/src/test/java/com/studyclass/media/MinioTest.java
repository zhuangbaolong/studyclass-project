package com.studyclass.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import io.minio.errors.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MinioTest {

    static MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://192.168.88.130:9090")
                    .credentials("minioadmin", "minioadmin")
                    .build();


    //上传文件
    @Test
    public void testUpload() {
        try {
            //根据扩展名取出mimeType
            ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".jpg");
            //通用mimeType，字节流
            String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            if (extensionMatch != null) {
                mimeType = extensionMatch.getMimeType();
            }
            UploadObjectArgs testbucket = UploadObjectArgs.builder()
                    .bucket("studyclassbucket")
                    //.object("test001.mp4")
                    .filename("C:\\Users\\32944\\Pictures\\Saved Pictures\\keep.jpg")
                    .object("test01/keep.jpg")//文件目录
                    .contentType(mimeType)//默认根据扩展名确定文件内容类型，也可以指定
                    .build();
            minioClient.uploadObject(testbucket);
            // TODO 再下载和本地上传文件进行md5校验，校验文件上传完整性
            System.out.println("上传成功");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("上传失败");
        }
    }

    @Test
    public void delete() {
        try {
            RemoveObjectArgs deletefile = RemoveObjectArgs.builder()
                    .bucket("studyclassbucket")
                    .object("keep.jpg")
                    .build();
            minioClient.removeObject(deletefile);
            System.out.println("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("删除失败");
        }
    }

    @Test
    public void download() {
        try {
            GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    .bucket("studyclassbucket")
                    .object("test01/keep.jpg")
                    .build();
            //查询远程服务获取到的流对象
            FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
            //指定输出流
            FileOutputStream fileOutputStream = new FileOutputStream(new File("F:\\workspace\\project\\springcloud\\studyclass\\keep.jpg"));
            IOUtils.copy(inputStream, fileOutputStream);

            //md5校验文件的完整性: TODO 本地原文件流与下载的文件流校验
//            String source_input = DigestUtils.md5Hex(inputStream);
//            String source_output = DigestUtils.md5Hex(new FileInputStream(new File("F:\\workspace\\project\\springcloud\\studyclass\\keep.jpg")));
//            if (source_input.equals(source_output)){
//                System.out.println("下载成功");
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //分块文件上传到minio
    @Test
    public void uploadChunk() {
        String chunkFolderPath = "F:\\workspace\\project\\springcloud\\studyclass\\video\\chunk\\";
        File chunkFolder = new File(chunkFolderPath);
        //分块
        File[] files = chunkFolder.listFiles();
        List<File> fileList = Arrays.asList(files);
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Integer.parseInt(o1.getName()) - Integer.parseInt(o2.getName());
            }
        });
        System.out.println(fileList);
        for (File file:
                fileList) {
//            System.out.println(file.getName());
            try {
                UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                        .bucket("studyclassbucket")
                        .object("chunk/" + file.getName())
                        .filename(file.getAbsolutePath()).build();
                System.out.println(uploadObjectArgs.object());
                minioClient.uploadObject(uploadObjectArgs);
//                System.out.println("上传分块成功" + file.getName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//        for (int i = 0; i < fileList.size(); i++) {
//            System.out.println(fileList.get(i).getAbsolutePath());
//        }
//        for (int i = 0; i < fileList.size(); i++) {
//            try {
//                UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
//                        .bucket("studyclassbucket")
//                        .object("chunk/" + i)
//                        .filename(fileList.get(i).getAbsolutePath()).build();
//                minioClient.uploadObject(uploadObjectArgs);
//                System.out.println("上传分块成功"+i);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
    }

    //调用minio接口合并分块
    @Test
    public void testMerge() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
//        List<ComposeSource> sources = null;
//        for (int i = 0; i < 18; i++) {
//            //指定分块信息
//            ComposeSource composeSource = ComposeSource.builder()
//                    .bucket("studyclassbucket")
//                    .object("chunk/" + i)
//                    .build();
//            sources.add(composeSource);
//            System.out.println(sources.get(i));
//        }
        List<ComposeSource> sources = Stream.iterate(0, i -> ++i)
                .limit(18)
                .map(i -> ComposeSource.builder()
                        .bucket("studyclassbucket")
                        .object("chunk/".concat(Integer.toString(i)))
                        .build())
                .collect(Collectors.toList());
        //指定合并后的objectName等信息
        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs
                .builder()
                .bucket("studyclassbucket")
                .object("merge01.mp4")
                .sources(sources)
                .build();
        minioClient.composeObject(composeObjectArgs);
    }
    //批量清除分块
}
