package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.*;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.query.QuestionAdminPageQuery;
import com.tianji.learning.query.QuestionPageQuery;
import com.tianji.learning.service.IInteractionQuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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


    private final UserClient userClient;

    private final InteractionReplyMapper replyMapper;

    private final CatalogueClient catalogueClient;
    private final CourseClient courseClient;
    private final SearchClient searchClient;
    private final CategoryCache categoryCache;
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
            List<InteractionReply> replies = replyMapper.selectBatchIds(answerIds);
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

    @Override
    public PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query) {
       //1. 处理课程名称 得到课程id
        List<Long> courseIds=null;
        if(StringUtils.isNotBlank(query.getCourseName()) ){
            courseIds = searchClient.queryCoursesIdByName(query.getCourseName());
            if(CollUtils.isEmpty(courseIds)){
                return PageDTO.empty(0L,0L);//0条 0页
            }
        }

        //2.分页查询
        Integer status = query.getStatus();
        LocalDateTime begin = query.getBeginTime();
        LocalDateTime end = query.getEndTime();
        Page<InteractionQuestion> page = lambdaQuery()
                .in(courseIds != null, InteractionQuestion::getCourseId, courseIds)
                .eq(status != null, InteractionQuestion::getStatus, status)
                .between(begin != null && end != null, InteractionQuestion::getUpdateTime, begin, end)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();
        if(CollUtils.isEmpty(records)){
            return PageDTO.empty(page);
        }

        //3.准备
        //3.1 获取各种数据的id集合
        Set<Long> userIds=new HashSet<>();
        Set<Long> cIds=new HashSet<>();
        Set<Long> cataIds=new HashSet<>();

        for(InteractionQuestion r:records){
            cIds.add(r.getCourseId());
            cataIds.add(r.getChapterId());
            cataIds.add(r.getSectionId());
            userIds.add(r.getUserId());
        }
        //3.2 根据id查询用户、
        List<UserDTO> users = userClient.queryUserByIds(userIds);
        Map<Long, UserDTO> userMap=new HashMap<>();
        if(CollUtils.isNotEmpty(users)) {
            userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
        }
        //3.3根据id查询课程
        List<CourseSimpleInfoDTO> cInfos = courseClient.getSimpleInfoList(cIds);
        Map<Long, CourseSimpleInfoDTO> cInfoMap=new HashMap<>();
        if(CollUtils.isNotEmpty(cInfos)) {
            cInfoMap = cInfos.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        }
        //3.4根据id查询章节
        List<CataSimpleInfoDTO> catas = catalogueClient.batchQueryCatalogue(cataIds);
        Map<Long, String> cataMap=new HashMap<>();
        if(CollUtils.isNotEmpty(catas)) {
            cataMap = catas.stream().collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
        }
        //4.封装
        List<QuestionAdminVO> voList = new ArrayList<>(records.size());
        for (InteractionQuestion r : records) {
            QuestionAdminVO vo = BeanUtils.copyBean(r, QuestionAdminVO.class);
            UserDTO user = userMap.get(r.getUserId());
            if(user != null) {
                vo.setUserName(user.getName());
            }
            CourseSimpleInfoDTO cInfo = cInfoMap.get(r.getCourseId());
            if(cInfo != null) {
                vo.setCourseName(cInfo.getName());
                vo.setCategoryName(categoryCache.getCategoryNames(cInfo.getCategoryIds()));
            }
            if(cataMap.get(r.getChapterId()) != null) {
                vo.setChapterName(cataMap.get(r.getChapterId()));
            }
            if(cataMap.get(r.getSectionId()) != null) {
                vo.setSectionName(cataMap.get(r.getSectionId()));
            }
            voList.add(vo);
        }

        return PageDTO.of(page,voList);
    }
    @Override
    public QuestionAdminVO queryQuestionByIdAdmin(Long id) {
        // 1.根据id查询问题
        InteractionQuestion question = getById(id);
        if (question == null) {
            return null;
        }
        // 2.转PO为VO
        QuestionAdminVO vo = BeanUtils.copyBean(question, QuestionAdminVO.class);
        // 3.查询提问者信息
        UserDTO user = userClient.queryUserById(question.getUserId());
        if (user != null) {
            vo.setUserName(user.getName());
            vo.setUserIcon(user.getIcon());
        }
        // 4.查询课程信息
        CourseFullInfoDTO cInfo = courseClient.getCourseInfoById(
                question.getCourseId(), false, true);
        if (cInfo != null) {
            // 4.1.课程名称信息
            vo.setCourseName(cInfo.getName());
            // 4.2.分类信息
            vo.setCategoryName(categoryCache.getCategoryNames(cInfo.getCategoryIds()));
            // 4.3.教师信息
            List<Long> teacherIds = cInfo.getTeacherIds();
            List<UserDTO> teachers = userClient.queryUserByIds(teacherIds);
            if(CollUtils.isNotEmpty(teachers)) {
                vo.setTeacherName(teachers.stream()
                        .map(UserDTO::getName).collect(Collectors.joining("/")));
            }
        }
        // 5.查询章节信息
        List<CataSimpleInfoDTO> catas = catalogueClient.batchQueryCatalogue(
                List.of(question.getChapterId(), question.getSectionId()));
        Map<Long, String> cataMap = new HashMap<>(catas.size());
        if (CollUtils.isNotEmpty(catas)) {
            cataMap = catas.stream()
                    .collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
        }
        vo.setChapterName(cataMap.getOrDefault(question.getChapterId(), ""));
        vo.setSectionName(cataMap.getOrDefault(question.getSectionId(), ""));
        // 6.封装VO
        return vo;
    }

    @Override
    public void hiddenQuestion(Long id, Boolean hidden) {
        // 1.更新问题
        InteractionQuestion question = new InteractionQuestion();
        question.setId(id);
        question.setHidden(hidden);
        updateById(question);
    }

    @Override
    public void updateQuestion(Long id, QuestionFormDTO questionDTO) {
        // 1.获取当前登录用户
        Long userId = UserContext.getUser();
        // 2.查询当前问题
        InteractionQuestion q = getById(id);
        if (q == null) {
            throw new BadRequestException("问题不存在");
        }
        // 3.判断是否是当前用户的问题
        if (!q.getUserId().equals(userId)) {
            // 不是，抛出异常
            throw new BadRequestException("无权修改他人的问题");
        }
        // 4.修改问题
        InteractionQuestion question = BeanUtils.toBean(questionDTO, InteractionQuestion.class);
        question.setId(id);
        updateById(question);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        // 1.获取当前登录用户
        Long userId = UserContext.getUser();
        // 2.查询当前问题
        InteractionQuestion q = getById(id);
        if (q == null) {
            return;
        }
        // 3.判断是否是当前用户的问题
        if (!q.getUserId().equals(userId)) {
            // 不是，抛出异常
            throw new BadRequestException("无权删除他人的问题");
        }
        // 4.删除问题
        removeById(id);
        // 5.删除答案
        replyMapper.delete(
                new QueryWrapper<InteractionReply>().lambda().eq(InteractionReply::getQuestionId, id)
        );
    }
}
