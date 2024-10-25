package com.studyclass.content.api;

import com.studyclass.content.model.dto.SaveTeachplanDto;
import com.studyclass.content.model.dto.TeachplanDto;
import com.studyclass.content.service.TeachplanService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程计划管理
 */
@RestController
public class TeachplanController {

    @Autowired
    TeachplanService teachplanService;
    
    //查询课程计划
    @ApiOperation("课程计划树形结构")
    @GetMapping("/teachplan/{courseId}/tree-nodes")
    public List<TeachplanDto> getTeachplanTreeNodes(@PathVariable Long courseId){
        return teachplanService.getTeachplanTreeNodes(courseId);
    }

    @ApiOperation("课程计划添加、修改")
    @PostMapping("/teachplan")
    public void saveTeachplan(@RequestBody SaveTeachplanDto teachplanDto){
        teachplanService.saveTeachplan(teachplanDto);
    }
}
