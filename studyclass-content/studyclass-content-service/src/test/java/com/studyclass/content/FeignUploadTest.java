package com.studyclass.content;

import com.studyclass.content.config.MultipartSupportConfig;
import com.studyclass.content.feignclient.MediaServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@SpringBootTest
public class FeignUploadTest {

    @Autowired
    MediaServiceClient mediaServiceClient;

    /**
     * 远程调用上传文件
     */
    @Test
    public void test(){
        File file = new File("F:\\workspace\\project\\springcloud\\studyclass\\html\\122.html");
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        String res = mediaServiceClient.uploadFile(multipartFile, "course/121.html");
        if (res == null) {
            System.out.println("熔断降级了");
        }
    }
}
