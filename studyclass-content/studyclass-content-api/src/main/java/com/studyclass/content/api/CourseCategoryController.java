package com.studyclass.content.api;

import com.studyclass.content.model.dto.CourseCategoryTreeDto;
import com.studyclass.content.model.po.CourseCategory;
import com.studyclass.content.service.CourseCategoryService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CourseCategoryController {

    @Autowired
    CourseCategoryService courseCategoryService;

    @ApiOperation("课程分类")
    @GetMapping("/course-category/tree-nodes")
    public List<CourseCategoryTreeDto> list(String id){
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryService.queryCourseCategoryTree("1");
        return courseCategoryTreeDtos;
    }
}
