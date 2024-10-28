package com.studyclass.content.service;

import com.studyclass.content.model.dto.SaveTeachplanDto;
import com.studyclass.content.model.dto.TeachplanDto;

import java.util.List;

/**
 * 课程计划管理
 */
public interface TeachplanService {

    public List<TeachplanDto> getTeachplanTreeNodes(Long courseId);

    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto);

    public void deleteTeachplan(Long teachplanId);

    public void moveTeachplan(String moveType, Long teachplanId);
}
