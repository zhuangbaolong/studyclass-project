package com.studyclass.media.service.jobhandler;

import com.studyclass.base.utils.Mp4VideoUtil;
import com.studyclass.media.mapper.MediaProcessMapper;
import com.studyclass.media.model.po.MediaProcess;
import com.studyclass.media.service.MediaFileService;
import com.studyclass.media.service.MediaProcessService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * 视频处理任务类
 *
 * @author xuxueli 2019-12-11 21:52:51
 */
@Slf4j
@Component
public class VideoTask {

    @Autowired
    MediaProcessService mediaProcessService;

    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegPath;

    @Autowired
    MediaFileService mediaFileService;

    /**
     * 2、分片广播任务
     */
    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        XxlJobHelper.log("分片参数：当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal);
        //确定cpu核心数
        int processors = Runtime.getRuntime().availableProcessors();
        //查询待处理的任务
        List<MediaProcess> mediaProcessList = mediaProcessService.getMediaProcessList(shardIndex, shardTotal, processors);
        //任务数量
        int size = mediaProcessList.size();
        if (size <= 0) {
            log.debug("取到的视频任务数：{}", size);
            return;
        }
        //创建线程池,启动多线程执行任务
        ExecutorService executorService = Executors.newFixedThreadPool(size);
        //线程计数器
        CountDownLatch countDownLatch = new CountDownLatch(size);
        mediaProcessList.forEach(mediaProcess -> {
            //将任务加入到线程池
            executorService.execute(() -> {
                try {
                    //任务执行逻辑
                    Long taskId = mediaProcess.getId();
                    //开启任务（数据库的乐观锁）
                    boolean b = mediaProcessService.startTask(taskId);
                    if (!b) {
                        log.debug("抢占任务失败,任务id:{}", taskId);
                        return;
                    }
                    //执行视频的转码
                    //todo ffmpeg的路径
                    //下载minio到本地
                    String bucket = mediaProcess.getBucket();
                    //存储路径
                    String objectName = mediaProcess.getFilePath();
                    //原始视频的md5值
                    String fileId = mediaProcess.getFileId();
                    //原始文件名称
                    String filename = mediaProcess.getFilename();
                    File originalFile  = mediaFileService.downloadFileFromMinIO(bucket, objectName);
                    if (originalFile  == null) {
                        log.debug("抢占任务失败,任务id:{},bucket:{},objectName:{}", taskId, bucket, objectName);
                        //保存任务处理失败的结果
                        mediaProcessService.saveProcessFinishStatus(taskId, "3", mediaProcess.getFileId(), null, "下载视频到本地失败");
                        return;
                    }
                    File tempFile = null;
                    try {
                        tempFile = File.createTempFile("minio", ".mp4");
                    } catch (IOException e) {
                        mediaProcessService.saveProcessFinishStatus(taskId, "3", mediaProcess.getFileId(), null, "创建临时文件失败");
                        log.debug("创建临时文件异常,{}", e.getMessage());
                        return;
                    }
                    //下载的avi绝对路径
                    String video_path = originalFile.getAbsolutePath();
                    //转换后mp4文件的名称
                    String mp4_name = fileId + ".mp4";
                    //转换后mp4文件的路径
                    String tempFilePath = tempFile.getAbsolutePath();
                    //创建工具类对象
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegPath, video_path, mp4_name, tempFilePath);
                    //开始视频转换，成功将返回success
                    String res = videoUtil.generateMp4();
                    if (!res.equals("success")) {
                        log.debug("视频转码失败:{}，bucket:{},objectName:{}", res, bucket, objectName);
                        mediaProcessService.saveProcessFinishStatus(taskId, "3", mediaProcess.getFileId(), null, res);
                        return;
                    }
                    //文件上传minio
                    //mp4在minio的存储路径
                    objectName = getFilePathByMd5(fileId, ".mp4");
                    boolean b1 = mediaFileService.addMediaFilesToMinio(tempFilePath, "video/mp4", bucket, objectName);
                    if (!b1) {
                        log.debug("上传mp4到minio失败,taskId:{},bucket:{},objectName:{}", taskId, bucket, objectName);
                        return;
                    }
//                    String url = getFilePathByMd5(fileId, ".mp4");
                    //访问url
                    String url = "/" + bucket + "/" + objectName;
                    //保存任务的处理结果
                    mediaProcessService.saveProcessFinishStatus(taskId, "2", fileId, url, "保存任务成功");
                } finally {
                    //线程计数器-1
                    countDownLatch.countDown();
                }
            });
        });
        //阻塞，最大等待时间
//        countDownLatch.await(30, TimeUnit.MINUTES);
    }

    //根据md5值得到合并后的路径地址
    private String getFilePathByMd5(String fileMd5, String fileExt) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }

}
