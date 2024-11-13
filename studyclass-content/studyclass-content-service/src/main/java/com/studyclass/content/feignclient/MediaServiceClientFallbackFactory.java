package com.studyclass.content.feignclient;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
public class MediaServiceClientFallbackFactory implements FallbackFactory<MediaServiceClient> {
    /**
     * 拿到熔断异常信息 throwable
     * @param throwable
     * @return
     */
    @Override
    public MediaServiceClient create(Throwable throwable) {
        return new MediaServiceClient() {
            //发生熔断，上游服务调用此方法执行降级逻辑
            @Override
            public String uploadFile(MultipartFile upload, String objectName) {
                log.debug("远程调用接口发生熔断:{}",throwable.toString(),throwable);
                return null;
            }
        };
    }
}
