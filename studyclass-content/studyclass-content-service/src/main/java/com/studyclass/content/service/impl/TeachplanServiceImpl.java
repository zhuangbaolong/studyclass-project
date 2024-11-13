package com.studyclass.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyclass.base.exception.StudyClassException;
import com.studyclass.content.mapper.TeachplanMapper;
import com.studyclass.content.mapper.TeachplanMediaMapper;
import com.studyclass.content.model.dto.BindTeachplanMediaDto;
import com.studyclass.content.model.dto.SaveTeachplanDto;
import com.studyclass.content.model.dto.TeachplanDto;
import com.studyclass.content.model.po.Teachplan;
import com.studyclass.content.model.po.TeachplanMedia;
import com.studyclass.content.service.TeachplanService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TeachplanServiceImpl implements TeachplanService {

    @Autowired
    TeachplanMapper teachplanMapper;

    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;

    @Override
    public List<TeachplanDto> getTeachplanTreeNodes(Long courseId) {
        List<TeachplanDto> teachplanDtos = teachplanMapper.selectTeachplanTreeNodes(courseId);
        return teachplanDtos;
    }

    /**
     * 修改保存课程（有id），新增没id
     *
     * @param saveTeachplanDto
     */
    @Override
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto) {
        //通过课程计划id来判断是新增或修改课程计划
        Long teachplanId = saveTeachplanDto.getId();
        if (teachplanId == null) {
            //新增
            Teachplan teachplan = new Teachplan();
            BeanUtils.copyProperties(saveTeachplanDto, teachplan);

            //TODO 判断是否合理：确定排序字段:同级别的章节个数+1,select count(*) from teachplan where course_id = ? and parentid = ?
            Long courseId = saveTeachplanDto.getCourseId();
            Long parentid = saveTeachplanDto.getParentid();
            LambdaQueryWrapper<Teachplan> teachplanQueryWrapper = new LambdaQueryWrapper<>();
            teachplanQueryWrapper = teachplanQueryWrapper.eq(Teachplan::getCourseId, courseId).eq(Teachplan::getParentid, parentid);
            Integer count = teachplanMapper.selectCount(teachplanQueryWrapper);
            teachplan.setOrderby(count + 1);

            teachplanMapper.insert(teachplan);
        } else {
            //修改
            //获取要修改的teachplan对象
            Teachplan teachplan = teachplanMapper.selectById(teachplanId);
            BeanUtils.copyProperties(saveTeachplanDto, teachplan);
            teachplanMapper.updateById(teachplan);
        }
    }

    @Override
    public void deleteTeachplan(Long teachplanId) {
        if (teachplanId == null) {
            StudyClassException.cast("课程计划id为空");
        }
        //需要先判断删除的是章节还是小杰
        //查询要删除的章节信息
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        // 获取章节层级
        Integer grade = teachplan.getGrade();
        if (grade == 1) {
            //判断为大章节
            //先判断该大章节下是否有小章节
            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Teachplan::getParentid, teachplanId);
            Integer count = teachplanMapper.selectCount(queryWrapper);
            if (count > 0) {
                StudyClassException.cast("课程计划信息还有子级信息，无法操作");
            }
            //该大章节无小节可以直接删除
            teachplanMapper.deleteById(teachplanId);
        } else {
            //判断为小节,删除小节信息、媒资信息
            teachplanMapper.deleteById(teachplanId);
            LambdaQueryWrapper<TeachplanMedia> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TeachplanMedia::getTeachplanId, teachplanId);
            teachplanMediaMapper.delete(wrapper);
        }
    }

    /**
     * 章节移动
     *
     * @param moveType
     * @param teachplanId
     */
    @Override
    public void moveTeachplan(String moveType, Long teachplanId) {
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        //获取层级
        Integer grade = teachplan.getGrade();
        Long courseId = teachplan.getCourseId();
        Integer orderby = teachplan.getOrderby();
        //如果是小节要判断他是哪个章节的
        Long parentid = teachplan.getParentid();
        //判断上移下移
        if ("moveup".equals(moveType)) {
            //上移
            if (grade == 1) {
                //判断能否上移
                if (orderby <= 1) {
                    StudyClassException.cast("无法上移，已经到头啦");
                }
                //大章节移动,获取上一个章节信息
                LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper
                        .eq(Teachplan::getCourseId, courseId)
                        .eq(Teachplan::getGrade, grade)
                        .eq(Teachplan::getOrderby, orderby - 1);
                Teachplan tmp = teachplanMapper.selectOne(queryWrapper);
                //交换章节排序序列
                exchangeOrderby(teachplan, tmp);
            } else {
                //判断能否上移
                if (orderby <= 1) {
                    StudyClassException.cast("无法上移，已经到头啦");
                }
                //小节上移
                LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper
                        .eq(Teachplan::getCourseId, courseId)
                        .eq(Teachplan::getParentid, parentid)
                        .eq(Teachplan::getGrade, grade)
                        .lt(Teachplan::getOrderby, orderby)
                        .orderByDesc(Teachplan::getOrderby)
                        .last("limit 1");
                Teachplan tmp = teachplanMapper.selectOne(queryWrapper);
                //交换章节排序序列
                exchangeOrderby(teachplan, tmp);
            }
        } else {
            if (grade == 1) {
                //下移
                //大章节移动,获取上一个章节信息
                LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper
                        .eq(Teachplan::getCourseId, courseId)
                        .eq(Teachplan::getGrade, grade)
                        .gt(Teachplan::getOrderby, orderby)
                        .orderByAsc(Teachplan::getOrderby)
                        .last("limit 1");
                Teachplan tmp = teachplanMapper.selectOne(queryWrapper);
                exchangeOrderby(teachplan, tmp);
            } else {
                //小节下移
                LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper
                        .eq(Teachplan::getCourseId, courseId)
                        .eq(Teachplan::getParentid, parentid)
                        .eq(Teachplan::getGrade, grade)
                        .gt(Teachplan::getOrderby, orderby)
                        .orderByAsc(Teachplan::getOrderby)
                        .last("limit 1");
                Teachplan tmp = teachplanMapper.selectOne(queryWrapper);
                //交换章节排序序列
                exchangeOrderby(teachplan, tmp);
            }
        }
    }

    private void exchangeOrderby(Teachplan teachplan, Teachplan tmp) {
        //判断能否移动
        if (tmp == null) {
            StudyClassException.cast("已经到头啦，无法移动");
        }
        Integer orderby = teachplan.getOrderby();
        Integer orderby1 = tmp.getOrderby();
        teachplan.setOrderby(orderby1);
        tmp.setOrderby(orderby);
        teachplanMapper.updateById(teachplan);
        teachplanMapper.updateById(tmp);
    }

    /**
     * 绑定媒资
     *
     * @param bindTeachplanMediaDto
     * @return
     */
    @Transactional
    @Override
    public TeachplanMedia associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto) {
        //教学计划id
        Long teachplanId = bindTeachplanMediaDto.getTeachplanId();
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if (teachplan == null) {
            StudyClassException.cast("教学计划不存在");
        }
        //先删除原有媒资,根据课程计划id
        LambdaQueryWrapper<TeachplanMedia> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TeachplanMedia::getTeachplanId, bindTeachplanMediaDto.getTeachplanId());
        teachplanMediaMapper.delete(queryWrapper);
        //添加新的媒资
        TeachplanMedia teachplanMedia = new TeachplanMedia();
        BeanUtils.copyProperties(bindTeachplanMediaDto, teachplanMedia);
        teachplanMedia.setCourseId(teachplan.getCourseId());
        teachplanMedia.setMediaFilename(bindTeachplanMediaDto.getFileName());
//        teachplanMedia.setCreateDate(LocalDateTime.now());
        teachplanMediaMapper.insert(teachplanMedia);
        return null;
    }
}
