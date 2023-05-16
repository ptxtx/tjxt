package com.tianji.learning.controller;


import com.tianji.learning.service.ILearningRecordService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 学习记录表 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2022-12-10
 */
@RestController
@RequestMapping("/learning-records")
@Api(tags = "学习记录的相关接口")
@RequiredArgsConstructor
public class LearningRecordController {

    private final ILearningRecordService recordService;

}
