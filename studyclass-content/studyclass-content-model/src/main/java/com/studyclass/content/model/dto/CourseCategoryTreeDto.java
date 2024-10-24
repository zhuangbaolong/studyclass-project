package com.studyclass.content.model.dto;

import com.studyclass.content.model.po.CourseCategory;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CourseCategoryTreeDto extends CourseCategory implements Serializable {

    // 节点
    List<CourseCategoryTreeDto> childrenTreeNodes;
}
