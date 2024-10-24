package com.studyclass.content.service;

import com.studyclass.base.model.PageParams;
import com.studyclass.base.model.PageResult;
import com.studyclass.content.model.dto.QueryCourseParamsDto;
import com.studyclass.content.model.po.CourseBase;

/**
 *
 */
public interface CourseBaseInfoService {

    //课程分页查询
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto);
}
