package com.studyclass.content.api;

import com.studyclass.content.model.dto.BindTeachplanMediaDto;
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

    @ApiOperation("删除课程计划")
    @DeleteMapping("/teachplan/{teachplanId}")
    public void deleteTeachplan(@PathVariable Long teachplanId){
        teachplanService.deleteTeachplan(teachplanId);
    }

    @ApiOperation("移动课程计划")
    @PostMapping("/teachplan/{moveType}/{teachplanId}")
    public void movedownTeachplan(@PathVariable String moveType,@PathVariable Long teachplanId){
        teachplanService.moveTeachplan(moveType,teachplanId);
    }

    @ApiOperation(value = "课程计划和媒资信息绑定")
    @PostMapping("/teachplan/association/media")
    public void associationMedia(@RequestBody BindTeachplanMediaDto bindTeachplanMediaDto){
        teachplanService.associationMedia(bindTeachplanMediaDto);
    }
}
