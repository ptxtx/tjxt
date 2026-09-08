package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.query.QuestionAdminPageQuery;
import com.tianji.learning.query.QuestionPageQuery;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * 互动提问的问题表 控制器
 * </p>
 *
 * @author author
 */
@Api(tags = "互动问答相关接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class InteractionQuestionAdminController {

    private final IInteractionQuestionService interactionQuestionService;

    @GetMapping("/page")
    @ApiOperation("管理端分页查询互动问题")
    public PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query){
        return interactionQuestionService.queryQuestionPageAdmin(query);
    }

//    @GetMapping("/questions/{id}")
//    @ApiOperation("管理端根据id查询互动问题详情")

    @ApiOperation("管理端根据id查询互动问题")
    @GetMapping("{id}")
    public QuestionAdminVO queryQuestionByIdAdmin(@PathVariable("id") Long id){
        return interactionQuestionService.queryQuestionByIdAdmin(id);
    }

    @ApiOperation("隐藏或显示问题")
    @PutMapping("/{id}/hidden/{hidden}")
    public void hiddenQuestion(
            @ApiParam(value = "问题id", example = "1") @PathVariable("id") Long id,
            @ApiParam(value = "是否隐藏，true/false", example = "true") @PathVariable("hidden") Boolean hidden
    ){
        interactionQuestionService.hiddenQuestion(id, hidden);
    }








}
