package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.vo.LearningLessonVO;
import io.swagger.annotations.ApiParam;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.web.bind.annotation.*;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.domain.po.LearningLesson;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * 学生课程表 控制器
 * </p>
 *
 * @author author
 */
@Api(tags = "我的课表相关接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/lessons")
public class LearningLessonController {

    private final ILearningLessonService learningLessonService;

    @GetMapping("page")
    @ApiOperation("分页查询我的课表")
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query){
        return learningLessonService.queryMyLessons(query);
    }

    @DeleteMapping("/{courseId}")
    @ApiOperation("删除指定课程信息")
    public void deleteCourseFromLesson(
            @ApiParam(value = "课程id" ,example = "1") @PathVariable("courseId") Long courseId) {
        learningLessonService.deleteCourseFromLesson(null, courseId);
    }

    @GetMapping("/{courseId}/valid")
    @ApiOperation("检查课程是否有效")
    public Long isLessonValid(  @ApiParam(value = "课程id" ,example = "1") @PathVariable("courseId") Long courseId){
        return learningLessonService.isLessonValid(courseId);
    }

    @GetMapping("{courseId}")
    @ApiOperation("查询课表中是否有该课程")
    public LearningLessonVO queryLessonByCourseId(
            @ApiParam(value = "课程id" ,example = "1") @PathVariable("courseId") Long courseId) {
        return learningLessonService.queryLessonByCourseId(courseId);
    }

    @ApiOperation("统计课程学习人数")
    @GetMapping("/{courseId}/count")
    public Integer countLearningLessonByCourse(
            @ApiParam(value = "课程id" ,example = "1") @PathVariable("courseId") Long courseId){
        return learningLessonService.countLearningLessonByCourse(courseId);
    }



}
