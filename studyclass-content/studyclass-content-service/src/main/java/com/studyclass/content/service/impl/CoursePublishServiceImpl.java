package com.studyclass.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.studyclass.base.exception.CommonError;
import com.studyclass.base.exception.StudyClassException;
import com.studyclass.content.config.MultipartSupportConfig;
import com.studyclass.content.feignclient.MediaServiceClient;
import com.studyclass.content.mapper.CourseBaseMapper;
import com.studyclass.content.mapper.CourseMarketMapper;
import com.studyclass.content.mapper.CoursePublishMapper;
import com.studyclass.content.mapper.CoursePublishPreMapper;
import com.studyclass.content.model.dto.CourseBaseInfoDto;
import com.studyclass.content.model.dto.CoursePreviewDto;
import com.studyclass.content.model.dto.TeachplanDto;
import com.studyclass.content.model.po.CourseBase;
import com.studyclass.content.model.po.CourseMarket;
import com.studyclass.content.model.po.CoursePublish;
import com.studyclass.content.model.po.CoursePublishPre;
import com.studyclass.content.service.CourseBaseInfoService;
import com.studyclass.content.service.CoursePublishService;
import com.studyclass.content.service.TeachplanService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
public class CoursePublishServiceImpl implements CoursePublishService {

    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    @Autowired
    TeachplanService teachplanService;

    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CoursePublishPreMapper coursePublishPreMapper;
    @Autowired
    CoursePublishMapper coursePublishMapper;

    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Autowired
    MqMessageService mqMessageService;

    @Autowired
    MediaServiceClient mediaServiceClient;

    /**
     * 获取课程预览信息
     * @param courseId 课程id
     * @return
     */
    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        //课程基本信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        coursePreviewDto.setCourseBase(courseBaseInfo);
        //课程计划
        List<TeachplanDto> teachplanTreeNodes = teachplanService.getTeachplanTreeNodes(courseId);
        coursePreviewDto.setTeachplans(teachplanTreeNodes);
        return coursePreviewDto;
    }

    /**
     * 提交审核
     * @param companyId
     * @param courseId  课程id
     */
    @Transactional
    @Override
    public void commitAudit(Long companyId, Long courseId) {
        //约束
        //如果课程审核状态为已提交不允许审核
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        if (courseBaseInfo == null) {
            StudyClassException.cast("课程找不到");
        }
        String auditStatus = courseBaseInfo.getAuditStatus();
        if (auditStatus.equals("202003")) {
            StudyClassException.cast("课程已提交等待审核");
        }
        //课程图片必须上传
        String pic = courseBaseInfo.getPic();
        if (StringUtils.isEmpty(pic)) {
            StudyClassException.cast("请求上传课程图片");
        }
        //课程计划必须存在
        List<TeachplanDto> teachplanTree = teachplanService.getTeachplanTreeNodes(courseId);
        if (teachplanTree == null || teachplanTree.size() == 0) {
            StudyClassException.cast("请填写课程计划再提交");
        }
        //查询到课程基本信息、营销信息、计划信息。。。插入到预发布表
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        BeanUtils.copyProperties(courseBaseInfo, coursePublishPre);
        //营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        String courseMarketJson = JSON.toJSONString(courseMarket);
        coursePublishPre.setMarket(courseMarketJson);
        //课程计划
        String teachplanTreeJson = JSON.toJSONString(teachplanTree);
        coursePublishPre.setTeachplan(teachplanTreeJson);
        //插入预发布表
        coursePublishPre.setStatus("202003");
        coursePublishPre.setCreateDate(LocalDateTime.now());
        CoursePublishPre coursePublishPreObj = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPreObj == null) {
            coursePublishPreMapper.insert(coursePublishPre);
        } else {
            //更新
            coursePublishPreMapper.updateById(coursePublishPre);
        }
        //课程状态更新为已提交
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setAuditStatus("202003");
        courseBaseMapper.updateById(courseBase);
    }

    /**
     * 课程发布
     * @param companyId
     * @param courseId
     */
    @Transactional
    @Override
    public void publish(Long companyId, Long courseId) {
        // 判断审核是否通过
        // 查询预发布表,如果有预发布则向发布表写入数据
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre == null) {
            StudyClassException.cast("课程无审核记录");
        }
        String status = coursePublishPre.getStatus();
        if (!status.equals("202004")) {
            StudyClassException.cast("课程未审核通过不允许发布");
        }
        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre, coursePublish);
        //课程状态改为已发布
        coursePublish.setStatus("203002");
        //先查询课程发布表
        CoursePublish coursePublishObj = coursePublishMapper.selectById(courseId);
        if (coursePublishObj == null) {
            coursePublishMapper.insert(coursePublish);
        } else {
            coursePublishMapper.updateById(coursePublish);
        }
        // 消息表写数据
        saveCoursePublishMessage(courseId);
        // 将预发布表数据删除
        coursePublishPreMapper.deleteById(courseId);
    }

    /**
     * @param courseId 课程id
     * @return void
     * @description 保存消息表记录
     * @author Mr.M
     * @date 2022/9/20 16:32
     */
    private void saveCoursePublishMessage(Long courseId) {
        MqMessage mqMessage = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if (mqMessage == null) {
            StudyClassException.cast(CommonError.UNKOWN_ERROR.getErrMessage());
        }
    }

    /**
     * @description 课程静态化
     * @param courseId  课程id
     * @return File 静态化文件
     * @author Mr.M
     * @date 2022/9/23 16:59
     */
    @Override
    public File generateCourseHtml(Long courseId) {
        // 1. 创建一个FreeMarker配置：
        Configuration configuration = new Configuration(Configuration.getVersion());
        File htmlFile = null;
        try {
            // 2. 告诉FreeMarker在哪里可以找到模板文件。
            String classpath = this.getClass().getResource("/").getPath();
//            configuration.setDirectoryForTemplateLoading(new File("F:\\workspace\\project\\springcloud\\studyclass\\studyclass-project\\studyclass-content\\studyclass-content-service\\src\\test\\resources\\templates\\"));
            configuration.setDirectoryForTemplateLoading(new File(classpath+"/templates/"));
            // 2.1 指定字符编码
            configuration.setDefaultEncoding("utf-8");
            // 3. 创建一个数据模型，与模板文件中的数据模型类型保持一致，这里是CoursePreviewDto类型
            CoursePreviewDto coursePreviewDto = this.getCoursePreviewInfo(courseId);
            HashMap<String, Object> map = new HashMap<>();
            map.put("model", coursePreviewDto);
            // 4. 加载模板文件
            Template template = configuration.getTemplate("course_template.ftl");
            // 5. 将数据模型应用于模板
            String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
            InputStream inputStream = IOUtils.toInputStream(content);
//            htmlFile = new File("F:\\workspace\\project\\springcloud\\studyclass\\html\\121.html")
            htmlFile = File.createTempFile("coursepublish",".html");
            FileOutputStream fileOutputStream = new FileOutputStream(htmlFile);
            IOUtils.copy(inputStream,fileOutputStream);
        } catch (Exception e) {
            log.error("课程静态化异常:{}",e.toString());
            StudyClassException.cast("课程静态化异常");
        }
        return htmlFile;
    }

    /**
     * @description 上传课程静态化页面
     * @param file  静态化文件
     * @return void
     * @author Mr.M
     * @date 2022/9/23 16:59
     */
    @Override
    public void uploadCourseHtml(Long courseId, File file) {
        try{
            MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
            String res = mediaServiceClient.uploadFile(multipartFile, "course/"+courseId+".html");
            if (res == null) {
                log.debug("熔断降级,课程id:{}",courseId);
                StudyClassException.cast("上传静态文件过程中存在异常。。。");
            }
        }catch (Exception e){
            e.printStackTrace();
            StudyClassException.cast("上传静态文件过程中存在异常。。。");
        }
    }

    /**
     * 发布课程信息
     * @param courseId
     * @return
     */
    public CoursePublish getCoursePublish(Long courseId){
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        return coursePublish ;
    }
}
