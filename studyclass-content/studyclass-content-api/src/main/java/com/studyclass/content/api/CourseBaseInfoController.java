package com.studyclass.content.api;

import com.studyclass.base.model.PageParams;
import com.studyclass.base.model.PageResult;
import com.studyclass.content.model.dto.CourseCategoryTreeDto;
import com.studyclass.content.model.dto.QueryCourseParamsDto;
import com.studyclass.content.model.po.CourseBase;
import com.studyclass.content.service.CourseBaseInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Api(value = "课程信息管理",tags = "课程信息管理")
@RestController
@RequestMapping
public class CourseBaseInfoController {

    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    @ApiOperation("课程查询接口")
    @PostMapping("/course/list")
    public PageResult<CourseBase> list(PageParams pageParams, @RequestBody(required = false) QueryCourseParamsDto queryCourseParams) {
        PageResult<CourseBase> pageResult = courseBaseInfoService.queryCourseBaseList(pageParams, queryCourseParams);
        return pageResult;
    }


}
