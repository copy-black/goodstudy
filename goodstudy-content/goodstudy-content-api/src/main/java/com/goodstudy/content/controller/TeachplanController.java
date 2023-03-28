package com.goodstudy.content.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Description: 课程计划接口
 *
 * @Author: Jack
 * Date: 2023/03/28 21:55
 * Version: 1.0
 */
@Api(value = "课程计划接口", tags = "课程计划接口")
@RestController
public class TeachplanController {

    /**
     * 查询课程计划树形结构
     *
     * @param
     * @return void
     * @author Jack
     * @date 2023/3/28 21:58
     * @update_by Jack
     * @update_at 2023/3/28 21:58
     * @creed Talk is cheap, show me the comment !!!
     */
    @ApiOperation("查询课程计划树形结构")
    @ApiImplicitParam(value = "courseId", name = "课程Id", required = true, dataType = "Long", paramType = "path")
    @GetMapping("/teachplan/{courseId}/tree-nodes")
    public void getTreeNodes(@PathVariable Long courseId) {

    }

}