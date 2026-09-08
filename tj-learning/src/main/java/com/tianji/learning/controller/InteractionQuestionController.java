package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.query.QuestionPageQuery;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;
import com.tianji.learning.service.IInteractionQuestionService;
import com.tianji.learning.domain.po.InteractionQuestion;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;

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
@RequestMapping("/questions")
public class InteractionQuestionController {

    private final IInteractionQuestionService interactionQuestionService;

    @PostMapping
    @ApiOperation("新增互动问题")
    public void saveQuestion(@RequestBody @Valid QuestionFormDTO dto){
        interactionQuestionService.saveQuestion(dto);
    }

    @GetMapping("/page")
    @ApiOperation("分页查询互动问题")
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query){
        return interactionQuestionService.queryQuestionPage(query);
    }

    @ApiOperation("修改提问")
    @PutMapping("/{id}")
    public void updateQuestion(
            @ApiParam("要修改的问题的id") @PathVariable("id") Long id,
            @RequestBody QuestionFormDTO questionDTO){
        interactionQuestionService.updateQuestion(id, questionDTO);
    }
    @GetMapping("{id}")
    @ApiOperation("查询id互动问题详情")
    public QuestionVO queryQuestionById(@PathVariable Long id){
        return interactionQuestionService.queryQuestionById(id);
    }


    @ApiOperation("根据id删除当前用户问题")
    @DeleteMapping("/{id}")
    public void deleteQuestion(
            @ApiParam(value = "问题id", example = "1") @PathVariable("id") Long id
    ){
        interactionQuestionService.deleteById(id);
    }



}
