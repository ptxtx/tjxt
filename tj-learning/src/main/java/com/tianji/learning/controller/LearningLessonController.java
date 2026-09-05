package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.vo.LearningLessonVO;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.domain.po.LearningLesson;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

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


}
