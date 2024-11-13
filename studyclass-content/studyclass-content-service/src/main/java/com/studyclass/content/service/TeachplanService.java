package com.studyclass.content.service;

import com.studyclass.content.model.dto.BindTeachplanMediaDto;
import com.studyclass.content.model.dto.SaveTeachplanDto;
import com.studyclass.content.model.dto.TeachplanDto;
import com.studyclass.content.model.po.TeachplanMedia;

import java.util.List;

/**
 * 课程计划管理
 */
public interface TeachplanService {

    public List<TeachplanDto> getTeachplanTreeNodes(Long courseId);

    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto);

    public void deleteTeachplan(Long teachplanId);

    public void moveTeachplan(String moveType, Long teachplanId);

    /**
     * @description 教学计划绑定媒资
     * @param bindTeachplanMediaDto
     * @return com.xuecheng.content.model.po.TeachplanMedia
     * @author Mr.M
     * @date 2022/9/14 22:20
     */
    public TeachplanMedia associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto);
}
