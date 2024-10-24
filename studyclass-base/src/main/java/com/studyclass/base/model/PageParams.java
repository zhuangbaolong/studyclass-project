package com.studyclass.base.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;

/**
 * 分页查询参数
 */
@Data
@ToString
public class PageParams {

    //当前页码
    @ApiModelProperty(value = "页码")
    private Long pageNo = 1L;
    //每页显示记录数
    @ApiModelProperty(value = "每页记录数")
    private Long pageSize = 30L;

    public PageParams(Long pageNo, Long pageSize) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }

    public PageParams() {
    }
}
