package com.studyclass.content.service;

import com.studyclass.content.model.dto.CourseTeacherInfoDto;
import com.studyclass.content.model.po.CourseTeacher;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface CourseTeacherService {

    //查
    public List<CourseTeacher> getCourseTeacher(Long courseId);
    //增加or修改
    public CourseTeacher saveCourseTeacher(CourseTeacherInfoDto teacherInfoDto);
    //删除
    public void deleteCourseTeacher(Long courseId,Long teacherId);
}
