package com.studyclass.content;

import com.studyclass.content.model.dto.CoursePreviewDto;
import com.studyclass.content.service.CoursePublishService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.*;
import java.net.URL;
import java.util.HashMap;

@SpringBootTest
public class FreemarkerTest {

    @Autowired
    CoursePublishService coursePublishService;

    @Test
    public void testGenerateHtmlByTemplate() throws IOException, TemplateException {
        // 1. 创建一个FreeMarker配置：
        Configuration configuration = new Configuration(Configuration.getVersion());
        // 2. 告诉FreeMarker在哪里可以找到模板文件。
        String classpath = this.getClass().getResource("/").getPath();
        configuration.setDirectoryForTemplateLoading(new File("F:\\workspace\\project\\springcloud\\studyclass\\studyclass-project\\studyclass-content\\studyclass-content-service\\src\\test\\resources\\templates\\"));
        // 2.1 指定字符编码
        configuration.setDefaultEncoding("utf-8");
        // 3. 创建一个数据模型，与模板文件中的数据模型类型保持一致，这里是CoursePreviewDto类型
        CoursePreviewDto coursePreviewDto = coursePublishService.getCoursePreviewInfo(121L);
        HashMap<String, Object> map = new HashMap<>();
        map.put("model", coursePreviewDto);
        // 4. 加载模板文件
        Template template = configuration.getTemplate("course_template.ftl");
        // 5. 将数据模型应用于模板
        String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
        InputStream inputStream = IOUtils.toInputStream(content);
        FileOutputStream fileOutputStream = new FileOutputStream("F:\\workspace\\project\\springcloud\\studyclass\\html\\121.html");
        IOUtils.copy(inputStream,fileOutputStream);
    }
}
