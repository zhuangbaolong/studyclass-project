package com.studyclass.content.api;

import com.studyclass.base.exception.ValidationGroups;
import com.studyclass.base.model.PageParams;
import com.studyclass.base.model.PageResult;
import com.studyclass.content.model.dto.*;
import com.studyclass.content.model.po.CourseBase;
import com.studyclass.content.service.CourseBaseInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Api(value = "课程信息管理",tags = "课程信息管理")
@RestController
public class CourseBaseInfoController {

    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    @ApiOperation("课程查询接口")
    @PostMapping("/course/list")
    public PageResult<CourseBase> list(PageParams pageParams, @RequestBody(required = false) QueryCourseParamsDto queryCourseParams) {
        PageResult<CourseBase> pageResult = courseBaseInfoService.queryCourseBaseList(pageParams, queryCourseParams);
        return pageResult;
    }

    @ApiOperation("添加课程")
    @PostMapping("/course")
    public CourseBaseInfoDto add(@RequestBody @Validated(ValidationGroups.Inster.class) AddCourseDto addCourseDto){
        CourseBaseInfoDto courseBase = courseBaseInfoService.createCourseBase(1232141425L, addCourseDto);
        return courseBase;
    }
    
    @ApiOperation("查看课程")
    @GetMapping("/course/{courseId}")
    public CourseBaseInfoDto getCourseBaseInfoById(@PathVariable Long courseId){
        return courseBaseInfoService.getCourseBaseInfo(courseId);
    }

    @ApiOperation("修改课程")
    @PutMapping("/course")
    public CourseBaseInfoDto modifyCourseBase(@RequestBody @Validated EditCourseDto editCourseDto){
        return courseBaseInfoService.modifyCourseBase(1232141425L,editCourseDto);
    }
    @ApiOperation("删除课程")
    @DeleteMapping("/course/{courseId}")
    public void deleteCourseBase(@PathVariable Long courseId){
        courseBaseInfoService.deleteCourseBase(1232141425L,courseId);
    }
}
