package com.studyclass.content.feignclient;

import org.springframework.web.multipart.MultipartFile;

/**
 * 熔断后降级
 */
public class MediaServiceClientFallback implements MediaServiceClient{

    @Override
    public String uploadFile(MultipartFile upload, String objectName) {
        return null;
    }
}
