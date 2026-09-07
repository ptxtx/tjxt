package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.SPELUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.query.QuestionPageQuery;
import com.tianji.learning.service.IInteractionQuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author author
 */
@Service
@RequiredArgsConstructor
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {

    private final IInteractionReplyService replyService;
    private final UserClient userClient;
    @Override
    public void saveQuestion(QuestionFormDTO dto) {
        Long userId = UserContext.getUser();
        InteractionQuestion interactionQuestion = BeanUtils.copyBean(dto, InteractionQuestion.class);
        interactionQuestion.setUserId(userId);
        save(interactionQuestion);

    }

    @Override
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query) {
        //1.参数校验
        Long courseId = query.getCourseId();
        Long sectionId = query.getSectionId();
        if(courseId ==null&&sectionId==null){
            throw new BadRequestException("课程id和小节id不能同时为空");
        }
        //2.分页查询
        Page<InteractionQuestion> page = lambdaQuery()
                .select( InteractionQuestion.class,info->!info.getProperty().equals("description"))//排除字段
                .eq(query.getOnlyMine(), InteractionQuestion::getUserId, UserContext.getUser())
                .eq(courseId != null, InteractionQuestion::getCourseId, courseId)
                .eq(sectionId != null, InteractionQuestion::getSectionId, sectionId)
                .eq(InteractionQuestion::getHidden, false)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();
        if(CollUtils.isEmpty(records)){
            return PageDTO.empty(page);
        }
        //这里 数据不为空
        //3. 根据id查询提问者和最近一次问答的信息
        Set<Long> userIds=new HashSet<>();
        Set<Long> answerIds=new HashSet<>();
        //3.1 得到问题当中的提问者id和最近一次回答的id
        for(InteractionQuestion q : records){
            //注意匿名,只查询非匿名的问题
            if(!q.getAnonymity()) {
                userIds.add(q.getUserId());
            }
            answerIds.add(q.getLatestAnswerId());
        }
        //3.2 根据id查最近一次回答
        answerIds.remove(null);
        Map<Long, InteractionReply> replyMap = new HashMap<>(answerIds.size());
        if(CollUtils.isNotEmpty(answerIds)) {
            List<InteractionReply> replies = replyService.listByIds(answerIds);
            //处理成map 才方便根据id找集合中的回答
            for (InteractionReply reply : replies) {
                replyMap.put(reply.getId(), reply);
                if(!reply.getAnonymity()){
                    userIds.add(reply.getUserId());
                }
            }
        }
        //3.3 根据id查询用户信息（提问者）
        userIds.remove(null);
        Map<Long, UserDTO> userMap=new HashMap<>(userIds.size());
        if(CollUtils.isNotEmpty(userIds)) {
            List<UserDTO> users = userClient.queryUserByIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
        }

        //4.转换vo
        List<QuestionVO> voList = new ArrayList<>(records.size());
        for (InteractionQuestion q : records) {
            //4.1将po 转vo
            QuestionVO vo = BeanUtils.copyBean(q, QuestionVO.class);
            //4.2封装提问者信息
            if (!q.getAnonymity()) {
                UserDTO userDTO = userMap.get(q.getUserId());
                vo.setUserName(userDTO == null ? null : userDTO.getName());//null 防止匿名情况
                vo.setUserIcon(userDTO == null ? null : userDTO.getIcon());
            }
            //4.3 封装最新一次回答的信息
            InteractionReply latestReply = replyMap.get(q.getLatestAnswerId());
            if (latestReply != null) {
                vo.setLatestReplyContent(latestReply.getContent());
                if (!latestReply.getAnonymity()) {
                    UserDTO user = userMap.get(latestReply.getUserId());
                    vo.setLatestReplyUser(user == null ? null : user.getName());
                }
            }
            voList.add(vo);
        }
       return PageDTO.of(page,voList);

    }

    @Override
    public QuestionVO queryQuestionById(Long id) {
        //1,根据id查询
        InteractionQuestion question = getById(id);
        //2.数据校验
        if(question == null||question.getHidden()){
            return null;
        }
        //3.查询提问者信息
        UserDTO user=null;
        if(!question.getAnonymity()){
            user = userClient.queryUserById(question.getUserId());
        }
        //4.封装vo
        QuestionVO vo = BeanUtils.copyBean(question, QuestionVO.class);
        if(user!=null){
            vo.setUserName(user.getName());
            vo.setUserIcon(user.getIcon());
        }
        return vo;
    }
}
