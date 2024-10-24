package com.studyclass.content.service;

import com.studyclass.content.model.dto.CourseCategoryTreeDto;

import java.util.List;

public interface CourseCategoryService {

    public List<CourseCategoryTreeDto> queryCourseCategoryTree(String id);
}
