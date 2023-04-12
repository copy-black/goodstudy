package com.goodstudy.content.service;

import com.goodstudy.content.model.dto.CoursePreviewDto;

/**
 * Description: 课程预览、发布接口
 *
 * @Author: Jack
 * Date: 2023/04/12 20:00
 * Version: 1.0
 */
public interface CoursePublishService {

    /**
     * Description: 获取课程预览信息
     *
     * @param courseId java.lang.Long
     * @return com.goodstudy.content.model.dto.CoursePreviewDto
     * @author Jack
     * @date 2023/4/12 20:00
     * @update_by Jack
     * @update_at 2023/4/12 20:00
     * @creed Talk is cheap, show me the comment !!!
     */
    public CoursePreviewDto getCoursePreviewInfo(Long courseId);
}
