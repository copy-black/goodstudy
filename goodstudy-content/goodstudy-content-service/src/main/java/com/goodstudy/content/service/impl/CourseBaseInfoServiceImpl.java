package com.goodstudy.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.goodstudy.base.exception.GoodStudyException;
import com.goodstudy.content.mapper.CourseBaseMapper;
import com.goodstudy.content.mapper.CourseCategoryMapper;
import com.goodstudy.content.mapper.CourseMarketMapper;
import com.goodstudy.content.model.dto.AddCourseDto;
import com.goodstudy.content.model.dto.CourseBaseInfoDto;
import com.goodstudy.content.model.dto.QueryCourseParamsDto;
import com.goodstudy.content.model.po.CourseBase;
import com.goodstudy.content.model.po.CourseCategory;
import com.goodstudy.content.model.po.CourseMarket;
import com.goodstudy.content.service.CourseBaseInfoService;
import com.goodstudy.base.model.PageParams;
import com.goodstudy.base.model.PageResult;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Description: 课程信息管理业务接口实现类
 *
 * @Author: Jack
 * Date: 2023/03/26 15:03
 * Version: 1.0
 */
@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    @Autowired
    private CourseBaseMapper courseBaseMapper;

    @Autowired
    private CourseMarketMapper courseMarketMapper;

    @Autowired
    private CourseCategoryMapper courseCategoryMapper;


    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto) {
        // 构建查询条件
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName())) {
            queryWrapper.like(CourseBase::getName, queryCourseParamsDto.getCourseName());
        }
        if (StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus())) {
            queryWrapper.eq(CourseBase::getAuditStatus, queryCourseParamsDto.getAuditStatus());
        }
        if (StringUtils.isNotEmpty(queryCourseParamsDto.getPublishStatus())) {
            queryWrapper.eq(CourseBase::getStatus, queryCourseParamsDto.getPublishStatus());
        }

        // 构建分页对象
        Page<CourseBase> page = new Page(pageParams.getPageNo(), pageParams.getPageSize());

        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);
        List<CourseBase> records = pageResult.getRecords();
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<CourseBase> result = new PageResult<>(records, total, pageParams.getPageNo(), pageParams.getPageSize());
        return result;
    }

    @Transactional
    @Override
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto addCourseDto) {
        // 校验合法性
        if (StringUtils.isBlank(addCourseDto.getTeachmode())) {
            throw new GoodStudyException("课程模型不能为空");
        }
        if (StringUtils.isBlank(addCourseDto.getName())) {
            throw new GoodStudyException("课程名称不能为空");
        }
        if (StringUtils.isBlank(addCourseDto.getMt())) {
            throw new GoodStudyException("课程分类不能为空");
        }
        if (StringUtils.isBlank(addCourseDto.getSt())) {
            throw new GoodStudyException("课程分类不能为空");
        }
        if (StringUtils.isBlank(addCourseDto.getGrade())) {
            throw new GoodStudyException("课程等级不能为空");
        }
        if (StringUtils.isBlank(addCourseDto.getGrade())) {
            throw new GoodStudyException("课程等级不能为空");
        }
        if (StringUtils.isBlank(addCourseDto.getUsers())) {
            throw new GoodStudyException("适用人群不能为空");
        }
        if (StringUtils.isBlank(addCourseDto.getCharge())) {
            throw new GoodStudyException("费用不能为空");
        }

        // 新增对象
        CourseBase courseBase = new CourseBase();
        //将填写的课程信息赋值给新增对象
        BeanUtils.copyProperties(addCourseDto, courseBase);
        courseBase.setAuditStatus("202002");
        courseBase.setStatus("203001");
        courseBase.setCompanyId(companyId);
        courseBase.setCreateDate(LocalDateTime.now());
        //插入数据库
        int insert = courseBaseMapper.insert(courseBase);
        if (insert != 1) {
            throw new GoodStudyException("新增课程信息失败");
        }
        // 保存课程营销信息
        CourseMarket courseMarket = new CourseMarket();
        Long id = courseBase.getId();
        BeanUtils.copyProperties(addCourseDto, courseMarket);
        courseMarket.setId(id);
        int i = saveCourseMarket(courseMarket);
        if (i != 1) {
            throw new GoodStudyException("新增课程营销信息失败");
        }
        //从数据库查询课程的详细信息，包括两部分
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(id);
        return courseBaseInfo;
    }

    //保存课程营销信息
    private int saveCourseMarket(CourseMarket courseMarket) {
        // 收费
        String charge = courseMarket.getCharge();
        if (StringUtils.isBlank(charge)) {
            throw new GoodStudyException("收费规则没有选择");
        }
        //收费规则为收费
        if (charge.equals("201001")) {
            if (courseMarket.getPrice() == null || courseMarket.getPrice().floatValue() <= 0) {
                throw new GoodStudyException("课程为收费价格不能为空且必须大于0");
            }
        }
        CourseMarket courseMarketObj  = courseMarketMapper.selectById(courseMarket.getId());
        if (courseMarketObj  == null) {
           return courseMarketMapper.insert(courseMarket);
        } else {
            BeanUtils.copyProperties(courseMarket,courseMarketObj);
            courseMarketObj.setId(courseMarket.getId());
            return courseMarketMapper.insert(courseMarketObj);
        }
    }

    public CourseBaseInfoDto getCourseBaseInfo(Long id) {
        CourseBase courseBase = courseBaseMapper.selectById(id);
        if (courseBase == null) {
            throw new GoodStudyException("课程信息不存在");
        }
        CourseMarket courseMarket = courseMarketMapper.selectById(id);
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();

        if(courseMarket != null) {
            BeanUtils.copyProperties(courseMarket, courseBaseInfoDto);
        }
        BeanUtils.copyProperties(courseBase, courseBaseInfoDto);

        // 获得课程分类对象
        CourseCategory courseCategoryByMt = courseCategoryMapper.selectById(courseBase.getMt());
        if (courseCategoryByMt == null) {
            throw new GoodStudyException("课程分类不存在");
        }
        courseBaseInfoDto.setMtName(courseCategoryByMt.getName());
        CourseCategory courseCategoryBySt = courseCategoryMapper.selectById(courseBase.getSt());
        if (courseCategoryBySt == null) {
            throw new GoodStudyException("课程分类不存在");
        }
        courseBaseInfoDto.setStName(courseCategoryBySt.getName());
        return courseBaseInfoDto;
    }

}