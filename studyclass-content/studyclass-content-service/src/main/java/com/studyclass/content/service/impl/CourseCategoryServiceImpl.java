package com.studyclass.content.service.impl;

import com.studyclass.content.mapper.CourseCategoryMapper;
import com.studyclass.content.model.dto.CourseCategoryTreeDto;
import com.studyclass.content.service.CourseCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseCategoryServiceImpl implements CourseCategoryService {

    @Autowired
    CourseCategoryMapper courseCategoryMapper;

    @Override
    public List<CourseCategoryTreeDto> queryCourseCategoryTree(String id) {
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);
        //找到每个节点子节点封装成List
        // 转成map，key是节点的id，value是courseCategoryTreeDto对象，目的是为了方便从map获取节点，filter过滤根节点;去重。
        Map<String, CourseCategoryTreeDto> courseCategoryTreeDtoMap = courseCategoryTreeDtos
                .stream()
                .filter(item->!id.equals(item.getId()))
                .collect(Collectors.toMap(key -> key.getId(), value -> value, (key1, key2) -> key1));
        //最终list
        ArrayList<CourseCategoryTreeDto> courseCategoryList = new ArrayList<>();
        //从头遍历map重新放入list中
        courseCategoryTreeDtos.stream().filter(item->!id.equals(item.getId())).forEach(item->{
            //list添加元素(父节点是1的子二级节点)
            if (item.getParentid().equals(id)){
                courseCategoryList.add(item);
            }
            //找到节点的父节点
            CourseCategoryTreeDto courseCategoryParent = courseCategoryTreeDtoMap.get(item.getParentid());
            if (courseCategoryParent != null) {
                if (courseCategoryParent.getChildrenTreeNodes() == null) {
                    //如果该节点的childrenTreeNodes属性为空要nwe一个集合，要向该集合中放它的子节点
                    courseCategoryParent.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                }
                //找到每个节点的子节点放在父节点的childrenTreeNode
                courseCategoryParent.getChildrenTreeNodes().add(item);
            }
        });
        return courseCategoryList;
    }
}
