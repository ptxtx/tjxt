package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.query.ReplyPageQuery;
import org.springframework.web.bind.annotation.*;
import com.tianji.learning.service.IInteractionReplyService;
import com.tianji.learning.domain.po.InteractionReply;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * 互动问题的回答或评论 控制器
 * </p>
 *
 * @author author
 */
@Api(tags = "InteractionReply管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/replies")
public class InteractionReplyController {

    private final IInteractionReplyService replyService;

    @ApiOperation("新增回答或评论")
    @PostMapping
    public void saveReply(@RequestBody ReplyDTO replyDTO) {
        replyService.saveReply(replyDTO);
    }

    @ApiOperation("分页查询回答或评论")
    @GetMapping("/page")
    public PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery pageQuery){
        return replyService.queryReplyPage(pageQuery, false);
    }

}
