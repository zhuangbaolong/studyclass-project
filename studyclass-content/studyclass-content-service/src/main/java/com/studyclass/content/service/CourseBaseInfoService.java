package com.studyclass.content.service;

import com.studyclass.base.model.PageParams;
import com.studyclass.base.model.PageResult;
import com.studyclass.content.model.dto.AddCourseDto;
import com.studyclass.content.model.dto.CourseBaseInfoDto;
import com.studyclass.content.model.dto.EditCourseDto;
import com.studyclass.content.model.dto.QueryCourseParamsDto;
import com.studyclass.content.model.po.CourseBase;

/**
 *
 */
public interface CourseBaseInfoService {

    //课程分页查询
    public PageResult<CourseBase> queryCourseBaseList(Long companyId, PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto);

    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto addCourseDto);

    public CourseBaseInfoDto getCourseBaseInfo(Long courseId);

    public CourseBaseInfoDto modifyCourseBase(Long companyId, EditCourseDto editCourseDto);

    public void deleteCourseBase(Long companyId, Long courseId);
}
