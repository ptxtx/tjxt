package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.remark.RemarkClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 互动问题的回答或评论 服务实现类
 * </p>
 *
 * @author 虎哥
 */
@Service
@RequiredArgsConstructor
public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply> implements IInteractionReplyService {

    private final IInteractionQuestionService questionService;
    private final UserClient userClient;
    private final RemarkClient remarkClient;
    private final RabbitMqHelper mqHelper;

    @Override
    @Transactional
    public void saveReply(ReplyDTO replyDTO) {
        // 1.获取登录用户
        // 2.新增回答
        // 3.累加评论数或者累加回答数
        // 3.1.判断当前回复的类型是否是回答
            // 3.2.是评论，则需要更新上级回答的评论数量
        // 3.3.尝试更新问题表中的状态、 最近一次回答、回答数量

        // 4.尝试累加积分
            // 学生才需要累加积分
    }

    @Override
    public PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery query, boolean forAdmin) {
        // 1.问题id和回答id至少要有一个，先做参数判断
        // 标记当前是查询问题下的回答
        // 2.分页查询reply
        // 3.数据处理，需要查询：提问者信息、回复目标信息、当前用户是否点赞
        // 3.1.获取提问者id 、回复的目标id、当前回答或评论id（统计点赞信息）
        // 3.2.查询目标回复，如果目标回复不是匿名，则需要查询出目标回复的用户信息
        // 3.3.查询用户
        // 3.4.查询用户点赞状态
        // 4.处理VO
            // 4.1.拷贝基础属性
            // 4.2.回复人信息
            // 4.3.如果存在评论的目标，则需要设置目标用户信息
            // 4.4.点赞状态
        return null;
    }

    @Override
    @Transactional
    public void hiddenReply(Long id, Boolean hidden) {
        // 1.查询

        // 2.隐藏回答

        // 3.隐藏评论，先判断是否是回答，回答才需要隐藏下属评论
        // 3.2.没有answerId，说明自己是回答，需要隐藏回答下的评论
    }

    @Override
    public ReplyVO queryReplyById(Long id) {
        // 1.根据id查询
        // 2.数据处理，需要查询用户信息、评论目标信息、当前用户是否点赞
        // 2.1.获取用户 id
        // 2.2.查询评论目标，如果评论目标不是匿名，则需要查询出目标回复的用户id
        // 2.3.查询用户详细
        // 2.4.查询用户点赞状态
        // 4.处理VO
        // 4.1.拷贝基础属性
        // 4.2.回复人信息
        // 4.3.目标用户
        // 4.4.点赞状态
        return null;
    }
}
