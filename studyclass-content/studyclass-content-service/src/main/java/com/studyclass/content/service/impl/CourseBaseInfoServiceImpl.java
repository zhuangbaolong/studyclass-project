package com.studyclass.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studyclass.base.exception.StudyClassException;
import com.studyclass.base.model.PageParams;
import com.studyclass.base.model.PageResult;
import com.studyclass.content.mapper.*;
import com.studyclass.content.model.dto.AddCourseDto;
import com.studyclass.content.model.dto.CourseBaseInfoDto;
import com.studyclass.content.model.dto.EditCourseDto;
import com.studyclass.content.model.dto.QueryCourseParamsDto;
import com.studyclass.content.model.po.*;
import com.studyclass.content.service.CourseBaseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Slf4j
@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    @Resource
    CourseBaseMapper courseBaseMapper;

    @Resource
    CourseMarketMapper courseMarketMapper;

    @Resource
    CourseCategoryMapper courseCategoryMapper;

    @Autowired
    CourseTeacherMapper courseTeacherMapper;

    @Autowired
    TeachplanMapper teachplanMapper;

    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;


    /**
     * @param pageParams           分页查询参数
     * @param queryCourseParamsDto 查询条件
     * @return 查询结果
     */
    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto) {
        // 拼装查询条件
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        // 根据名称模糊查询,在sql拼接 course_base.name like '%值%'
        queryWrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()), CourseBase::getName, queryCourseParamsDto.getCourseName());
        // 审核状态查询 course_base.audit_status = ?
        queryWrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()), CourseBase::getAuditStatus, queryCourseParamsDto.getAuditStatus());
        // 发布状态
        queryWrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getPublishStatus()), CourseBase::getStatus, queryCourseParamsDto.getPublishStatus());
        // 创建page分页参数,参数：当前页码，每页记录数
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        //查询
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);
        // 参数：List<T> items,long counts,long page,long pageSize
        PageResult<CourseBase> courseBasePageResult = new PageResult<>(pageResult.getRecords(), pageResult.getTotal(), pageParams.getPageNo(), pageParams.getPageSize());
        return courseBasePageResult;
    }

    /**
     * 添加课程
     */
    @Transactional
    @Override
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto addCourseDto) {
        //参数的合法性校验
        //合法性校验
        if (StringUtils.isBlank(addCourseDto.getName())) {
//            throw new RuntimeException("课程名称为空");
            StudyClassException.cast("课程名称不为空");
        }

        if (StringUtils.isBlank(addCourseDto.getMt())) {
            throw new RuntimeException("课程分类为空");
        }

        if (StringUtils.isBlank(addCourseDto.getSt())) {
            throw new RuntimeException("课程分类为空");
        }

        if (StringUtils.isBlank(addCourseDto.getGrade())) {
            throw new RuntimeException("课程等级为空");
        }

        if (StringUtils.isBlank(addCourseDto.getTeachmode())) {
            throw new RuntimeException("教育模式为空");
        }

        if (StringUtils.isBlank(addCourseDto.getUsers())) {
            throw new RuntimeException("适应人群为空");
        }

        if (StringUtils.isBlank(addCourseDto.getCharge())) {
            throw new RuntimeException("收费规则为空");
        }
        //向课程基本信息表course_base写入数据
        CourseBase courseBaseNew = new CourseBase();
        BeanUtils.copyProperties(addCourseDto, courseBaseNew);
        courseBaseNew.setCompanyId(companyId);
        //默认提交
        courseBaseNew.setCreateDate(LocalDateTime.now());
        courseBaseNew.setCreatePeople("zb;");
        courseBaseNew.setAuditStatus("202002");
        courseBaseNew.setStatus("203001");
        //插入数据库
        int insert = courseBaseMapper.insert(courseBaseNew);
        if (insert <= 0) {
            throw new RuntimeException("添加课程失败");
        }
        //向课程营销表course_market写入数据
        CourseMarket courseMarketNew = new CourseMarket();
        BeanUtils.copyProperties(addCourseDto, courseMarketNew);
        //课程id
        Long courseId = courseBaseNew.getId();
        courseMarketNew.setId(courseId);
        //保存营销信息
        saveCourseMarket(courseMarketNew);
        //从数据查询课程详细信息，包括两部分
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseId);
        return courseBaseInfo;
    }

    /**
     * 查询课程信息
     *
     * @param courseId
     * @return
     */
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId) {
        //从课程基本信息表查询
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null) {
            return null;
        }
        //从课程营销表查询
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        //组装在一起
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase, courseBaseInfoDto);
        if (courseMarket != null) {
            BeanUtils.copyProperties(courseMarket, courseBaseInfoDto);
        }
        //TODO 通过courseCategoryMapper查询分类信息，将分类名称放在info对象中
        return courseBaseInfoDto;
    }

    /**
     * 保存课程营销信息
     *
     * @param courseMarketNew
     * @return
     */
    public int saveCourseMarket(CourseMarket courseMarketNew) {
        //参数的合法性校验
        String charge = courseMarketNew.getCharge();
        if (StringUtils.isEmpty(charge)) {
            throw new RuntimeException("收费规则为空");
        }
        if (charge.equals("201001")) {
            if (courseMarketNew.getPrice() == null || courseMarketNew.getPrice().floatValue() <= 0) {
//                throw new RuntimeException("价格不为空且不小于0");
                StudyClassException.cast("价格不为空且不小于0");
            }
        }
        Long id = courseMarketNew.getId();
        CourseMarket courseMarket = courseMarketMapper.selectById(id);
        if (courseMarket == null) {
            int insert = courseMarketMapper.insert(courseMarketNew);
            return insert;
        } else {
            BeanUtils.copyProperties(courseMarketNew, courseMarket);
            courseMarket.setId(courseMarketNew.getId());
            int i = courseMarketMapper.updateById(courseMarket);
            return i;
        }
    }

    /**
     * 修改课程
     *
     * @param editCourseDto
     * @return
     */
    @Transactional
    @Override
    public CourseBaseInfoDto modifyCourseBase(Long companyId, EditCourseDto editCourseDto) {
        Long courseId = editCourseDto.getId();
        //查询课程
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null) {
            StudyClassException.cast("课程不存在");
        }
        //数据合法性校验
        //本机构只能修改本机构信息
        if (!companyId.equals(courseBase.getCompanyId())) {
            StudyClassException.cast("本机构只能修改本机构课程");
        }
        //封装数据
        BeanUtils.copyProperties(editCourseDto, courseBase);
        //修改时间
        courseBase.setChangeDate(LocalDateTime.now());
        //修改
        int i = courseBaseMapper.updateById(courseBase);
        if (i <= 0) {
            StudyClassException.cast("修改课程失败");
        }
        //查询课程返回数据
        CourseBaseInfoDto courseBaseInfoDto = getCourseBaseInfo(courseId);
        return courseBaseInfoDto;
    }

    @Transactional
    @Override
    public void deleteCourseBase(Long companyId,Long courseId) {
        // 课程基本信息、课程营销信息course_market、课程计划teachplan、课程计划关联信息teachplan_media、课程师资course_teacher
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (!companyId.equals(courseBase.getCompanyId()))
            StudyClassException.cast("只能删除本机构的课程");
        //删除师资
        LambdaQueryWrapper<CourseTeacher> courseTeacherQueryWrapper = new LambdaQueryWrapper<>();
        courseTeacherQueryWrapper.eq(CourseTeacher::getCourseId,courseId);
        courseTeacherMapper.delete(courseTeacherQueryWrapper);
        //删除课程计划/媒资相关信息
        LambdaQueryWrapper<TeachplanMedia> teachplanMediaWrapper = new LambdaQueryWrapper<>();
        teachplanMediaWrapper.eq(TeachplanMedia::getCourseId,courseId);
        teachplanMediaMapper.delete(teachplanMediaWrapper);
        LambdaQueryWrapper<Teachplan> teachplanWrapper = new LambdaQueryWrapper<>();
        teachplanWrapper.eq(Teachplan::getCourseId,courseId);
        teachplanMapper.delete(teachplanWrapper);
        //删除课程营销信息、课程信息
        LambdaQueryWrapper<CourseMarket> courseMarketWrapper = new LambdaQueryWrapper<>();
        courseMarketWrapper.eq(CourseMarket::getId,courseId);
        courseMarketMapper.delete(courseMarketWrapper);
        LambdaQueryWrapper<CourseBase> courseBaseWrapper = new LambdaQueryWrapper<>();
        courseBaseWrapper.eq(CourseBase::getId,courseId);
        courseBaseMapper.delete(courseBaseWrapper);
    }
}
