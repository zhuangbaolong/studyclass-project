package com.studyclass.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyclass.content.mapper.TeachplanMapper;
import com.studyclass.content.model.dto.SaveTeachplanDto;
import com.studyclass.content.model.dto.TeachplanDto;
import com.studyclass.content.model.po.Teachplan;
import com.studyclass.content.service.TeachplanService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeachplanServiceImpl implements TeachplanService {

    @Autowired
    TeachplanMapper teachplanMapper;

    @Override
    public List<TeachplanDto> getTeachplanTreeNodes(Long courseId) {
        List<TeachplanDto> teachplanDtos = teachplanMapper.selectTeachplanTreeNodes(courseId);
        return teachplanDtos;
    }

    /**
     * 修改保存课程（有id），新增没id
     * @param saveTeachplanDto
     */
    @Override
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto) {
        //通过课程计划id来判断是新增或修改课程计划
        Long teachplanId = saveTeachplanDto.getId();
        if (teachplanId == null) {
            //新增
            Teachplan teachplan = new Teachplan();
            BeanUtils.copyProperties(saveTeachplanDto,teachplan);

            //TODO 判断是否合理：确定排序字段:同级别的章节个数+1,select count(*) from teachplan where course_id = ? and parentid = ?
            Long courseId = saveTeachplanDto.getCourseId();
            Long parentid = saveTeachplanDto.getParentid();
            LambdaQueryWrapper<Teachplan> teachplanQueryWrapper = new LambdaQueryWrapper<>();
            teachplanQueryWrapper = teachplanQueryWrapper.eq(Teachplan::getCourseId,courseId).eq(Teachplan::getParentid,parentid);
            Integer count = teachplanMapper.selectCount(teachplanQueryWrapper);
            teachplan.setOrderby(count+1);

            teachplanMapper.insert(teachplan);
        }else {
            //修改
            //获取要修改的teachplan对象
            Teachplan teachplan = teachplanMapper.selectById(teachplanId);
            BeanUtils.copyProperties(saveTeachplanDto,teachplan);
            teachplanMapper.updateById(teachplan);
        }
    }
}
