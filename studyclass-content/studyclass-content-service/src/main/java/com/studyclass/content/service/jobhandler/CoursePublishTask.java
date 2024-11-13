package com.studyclass.content.service.jobhandler;

import com.studyclass.base.exception.StudyClassException;
import com.studyclass.content.feignclient.CourseIndex;
import com.studyclass.content.feignclient.SearchServiceClient;
import com.studyclass.content.mapper.CoursePublishMapper;
import com.studyclass.content.model.dto.CoursePreviewDto;
import com.studyclass.content.model.po.CoursePublish;
import com.studyclass.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 课程发布人物类
 */
@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {

    @Autowired
    CoursePublishService coursePublishService;

    @Autowired
    CoursePublishMapper coursePublishMapper;

    @Autowired
    SearchServiceClient searchServiceClient;

    @XxlJob("coursePublishJobHandler")
    public void coursePublishJobHandler() {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex(); //执行器序号，从0开始
        int shardTotal = XxlJobHelper.getShardTotal(); //执行器数量
        //调用抽象类封装的方法执行任务
        process(shardIndex, shardTotal, "course_publish", 30, 60);
        log.debug("测试任务执行中。。。。。。。。");
    }

    //执行发布任务的逻辑
    public boolean execute(MqMessage mqMessage) {
        log.debug("开始执行课程发布任务，课程id：{}", mqMessage.getBusinessKey1());
        Long courseId = Long.valueOf(mqMessage.getBusinessKey1());
        //阶段1：静态页面
        generateCourseHtml(mqMessage, courseId);
        //阶段2：向elasticsearch写索引数据
        saveCourseIndex(mqMessage, courseId);
        //redis
        return true;
    }

    //生成课程静态化页面并上传至文件系统
    public void generateCourseHtml(MqMessage mqMessage, long courseId) {
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();
        //幂等
        int stageOne = mqMessageService.getStageOne(taskId);
        if (stageOne != 0) {
            log.debug("课程静态化已完成，无需处理。");
            return;
        }
        // todo 开始进行课程静态化
        log.info("开始进行课程静态化。。。。。。。。。。。。。");
        File file = coursePublishService.generateCourseHtml(courseId);
        if (file == null) {
            StudyClassException.cast("生成的静态页面为空。。。");
        }
        //上传minio
        coursePublishService.uploadCourseHtml(courseId,file);
        // 任务完成更新任务状态。
        mqMessageService.completedStageOne(taskId);
    }

    //保存课程索引信息
    public void saveCourseIndex(MqMessage mqMessage, long courseId) {
        // 任务id
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();
        int stageTwo = mqMessageService.getStageTwo(taskId);
        if (stageTwo > 0) {
            log.debug("课程索引信息已写入,课程id:{}", courseId);
            return;
        }
        // 查询课程信息，调用搜索服务添加索引。。。
        log.info("调用搜索服务添加索引。。。。。。。。。。。。");
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        CourseIndex courseIndex = new CourseIndex();
        BeanUtils.copyProperties(coursePublish,courseIndex);
        Boolean add = searchServiceClient.add(courseIndex);
        if (!add) {
            StudyClassException.cast("远程调用搜索服务添加课程索引失败");
        }
        // 完成本阶段任务
        mqMessageService.completedStageTwo(taskId);
    }

}
