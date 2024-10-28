package com.studyclass.content.api;

import com.studyclass.content.model.dto.CourseTeacherInfoDto;
import com.studyclass.content.model.po.CourseTeacher;
import com.studyclass.content.service.CourseTeacherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(value = "课程教师管理",tags = "课程教师管理")
@RestController
public class CourseTeacherController {

    @Autowired
    CourseTeacherService teacherService;

    @ApiOperation("查询课程教师")
    @GetMapping("/courseTeacher/list/{courseId}")
    public List<CourseTeacher> getCourseplanTeacher(@PathVariable Long courseId){
        return teacherService.getCourseTeacher(courseId);
    }

    @ApiOperation("添加课程教师")
    @PostMapping("/courseTeacher")
    public CourseTeacher addCourseplanTeacher(@RequestBody CourseTeacherInfoDto teacherInfoDto){
        return teacherService.saveCourseTeacher(teacherInfoDto);
    }

    @ApiOperation("修改课程教师")
    @PutMapping("/courseTeacher")
    public CourseTeacher updateCourseplanTeacher(@RequestBody CourseTeacherInfoDto teacherInfoDto){
        return teacherService.saveCourseTeacher(teacherInfoDto);
    }

    @ApiOperation("删除课程教师")
    @DeleteMapping("/courseTeacher/course/{courseId}/{teacherId}")
    public void deleteCourseplanTeacher(@PathVariable Long courseId,@PathVariable Long teacherId){
        teacherService.deleteCourseTeacher(courseId,teacherId);
    }
}
