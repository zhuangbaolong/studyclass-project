package com.studyclass.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyclass.content.model.dto.CourseCategoryTreeDto;
import com.studyclass.content.model.po.CourseCategory;

import java.util.List;

/**
 * <p>
 * 课程分类 Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface CourseCategoryMapper extends BaseMapper<CourseCategory> {

    //使用递归查询课程分类
    public List<CourseCategoryTreeDto> selectTreeNodes(String id);
}
