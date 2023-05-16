package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author 虎哥
 */
@Service
@RequiredArgsConstructor
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {

    private final InteractionReplyMapper replyMapper;
    private final UserClient userClient;
    private final CourseClient courseClient;
    private final SearchClient searchClient;
    private final CatalogueClient catalogueClient;
    private final CategoryCache categoryCache;

    @Override
    public void saveQuestion(QuestionFormDTO questionDTO) {
        // 1.获取当前登录的用户id
        // 2.数据封装
        // 3.写入数据库
    }

    @Override
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query) {
        // 1.参数校验，课程id和小节id不能都为空
        // 2.分页查询
        // 3.根据id查询提问者和最近一次回答的信息
        // 3.1.得到问题当中的提问者id和最近一次回答的id
        // 3.2.根据id查询最近一次回答
        // 3.3.根据id查询用户信息（提问者）
        // 4.封装VO
        return null;
    }

    @Override
    public QuestionVO queryQuestionById(Long id) {
        // 1.根据id查询数据
        // 2.数据校验
        // 3.查询提问者信息
        // 4.封装VO
        return null;
    }

    @Override
    public PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query) {
        // 1.处理课程名称，得到课程id
        // 2.分页查询

        // 3.准备VO需要的数据：用户数据、课程数据、章节数据
        // 3.1.获取各种数据的id集合
        // 3.2.根据id查询用户

        // 3.3.根据id查询课程

        // 3.4.根据id查询章节

        // 4.封装VO
        return null;
    }

    @Override
    public QuestionAdminVO queryQuestionByIdAdmin(Long id) {
        // 1.根据id查询问题
        // 2.转PO为VO
        // 3.查询提问者信息
        // 4.查询课程信息
        // 5.查询章节信息
        // 6.封装VO
        return null;
    }

    @Override
    public void hiddenQuestion(Long id, Boolean hidden) {
        // 1.更新问题
    }

    @Override
    public void updateQuestion(Long id, QuestionFormDTO questionDTO) {
        // 1.获取当前登录用户
        // 2.查询当前问题
        // 3.判断是否是当前用户的问题
        // 4.修改问题
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        // 1.获取当前登录用户
        // 2.查询当前问题
        // 3.判断是否是当前用户的问题
        // 4.删除问题
        // 5.删除答案
    }
}
