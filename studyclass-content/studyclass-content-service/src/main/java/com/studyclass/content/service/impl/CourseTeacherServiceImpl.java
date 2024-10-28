package com.studyclass.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyclass.base.exception.StudyClassException;
import com.studyclass.content.mapper.CourseTeacherMapper;
import com.studyclass.content.model.dto.CourseBaseInfoDto;
import com.studyclass.content.model.dto.CourseTeacherInfoDto;
import com.studyclass.content.model.po.CourseTeacher;
import com.studyclass.content.service.CourseTeacherService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CourseTeacherServiceImpl implements CourseTeacherService {

    @Autowired
    CourseTeacherMapper courseTeacherMapper;

    @Override
    public List<CourseTeacher> getCourseTeacher(Long courseId) {
        if (courseId == null) {
            StudyClassException.cast("查询课程不能为空");
        }
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId,courseId);
        List<CourseTeacher> courseTeachers = courseTeacherMapper.selectList(queryWrapper);
        return courseTeachers;
    }

    @Transactional
    @Override
    public CourseTeacher saveCourseTeacher(CourseTeacherInfoDto teacherInfoDto) {
        //新增无id
        Long id = teacherInfoDto.getId();
        if (id == null) {
            //新增
            CourseTeacher courseTeacher = new CourseTeacher();
            BeanUtils.copyProperties(teacherInfoDto,courseTeacher);
            courseTeacher.setCreateDate(LocalDateTime.now());
            int i= courseTeacherMapper.insert(courseTeacher);
            if (i <= 0) {
                StudyClassException.cast("新增失败");
            }
            return courseTeacher;
        }else {
            //修改
            CourseTeacher courseTeacher = new CourseTeacher();
            BeanUtils.copyProperties(teacherInfoDto,courseTeacher);
            int i = courseTeacherMapper.updateById(courseTeacher);
            if (i <= 0) {
                StudyClassException.cast("修改失败");
            }
            return courseTeacher;
        }
    }

    @Override
    public void deleteCourseTeacher(Long courseId, Long teacherId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId,courseId).eq(CourseTeacher::getId,teacherId);
        courseTeacherMapper.delete(queryWrapper);
    }
}
