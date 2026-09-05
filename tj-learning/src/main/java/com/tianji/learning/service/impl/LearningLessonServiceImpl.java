package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 学生课程表 服务实现类
 * </p>
 *
 * @author author
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {
    private final CourseClient courseClient;
    @Override
    @Transactional//批处理 加一个事务吧！
    public void addUserLessons(Long userId, List<Long> courseIds) {
        //1.查询课程有效期
        List<CourseSimpleInfoDTO> cInfoList = courseClient.getSimpleInfoList(courseIds);
        if(CollUtils.isEmpty(cInfoList)){
            log.error("课程有效期查询结果为空", courseIds);
            return;
        }
        List<LearningLesson> list=new ArrayList<>();
        for (CourseSimpleInfoDTO cInfo : cInfoList) {
            LearningLesson lesson=new LearningLesson();
            Integer validDuration = cInfo.getValidDuration();//月
            if (validDuration!=null&&validDuration>0) {
                LocalDateTime now = LocalDateTime.now();
                lesson.setCreateTime(now);
                lesson.setUpdateTime(now);
                lesson.setExpireTime(now.plusMonths(validDuration));

            }
            lesson.setUserId(userId);
            lesson.setCourseId(cInfo.getId());
            list.add(lesson);

        }
        saveBatch(list);//mp批量保存操作
    }

    @Override
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
        //1.获取当前登录用户
        Long userId = UserContext.getUser();

        //2.分页查询 查询到的是po
        //select * from learning_lesson where user_id={userId} order by latest_learn_time limit 0,5
        Page<LearningLesson> page = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .page(query.toMpPage("latest_learn_time", false));

        //3.查询课程信息 拼接成VO
        List<LearningLesson> records = page.getRecords();
        if(CollUtils.isEmpty(records)){
            log.error("课程信息不存在，无法添加到课表");
            return PageDTO.empty(page);
        }
        Set<Long> cIds = records.stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());
        List<CourseSimpleInfoDTO> cInfoList = courseClient.getSimpleInfoList(cIds);
        if(CollUtils.isEmpty(cInfoList)){
            throw new BadRequestException("课程信息不存在，无法添加到课表");
        }
        //3.3把课程集合处理成map，key是courseId，值是course本身
        Map<Long, CourseSimpleInfoDTO> cMap = cInfoList.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, cInfo -> cInfo));

        List<LearningLessonVO> list=new ArrayList<>(records.size());
        //4.封装VO，返回
        for (LearningLesson learningLesson : records) {
            LearningLessonVO vo = BeanUtil.toBean(learningLesson, LearningLessonVO.class);
            CourseSimpleInfoDTO cInfo = cMap.get(learningLesson.getCourseId());
            vo.setCourseName(cInfo.getName());
            vo.setCourseCoverUrl(cInfo.getCoverUrl());
            vo.setSections(cInfo.getSectionNum());
            list.add(vo);
        }
        return new PageDTO<>(page.getTotal(),page.getPages(),list);
    }

    @Override
    public void deleteCourseFromLesson(Long userId, Long courseId) {
    // 1.获取当前登录用户
        if (userId == null) {
            userId = UserContext.getUser();
        }
        // 2.删除课程
        remove(buildUserIdAndCourseIdWrapper(userId, courseId));
    }

    private LambdaQueryWrapper<LearningLesson> buildUserIdAndCourseIdWrapper(Long userId, Long courseId) {
        LambdaQueryWrapper<LearningLesson> queryWrapper = new QueryWrapper<LearningLesson>()
                .lambda()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId);
        return queryWrapper;
    }

    @Override
    public Long isLessonValid(Long courseId) {
        Long userId = UserContext.getUser();
        if(userId==null){
            return null;
        }
        //2.查询课程信息
        LearningLesson lesson = getOne(buildUserIdAndCourseIdWrapper(userId, courseId));
        if (lesson == null) {
            return null;
        }
        return lesson.getId();
    }

    @Override
    public LearningLessonVO queryLessonByCourseId(Long courseId) {
        Long userId = UserContext.getUser();
        LearningLesson lesson = getOne(buildUserIdAndCourseIdWrapper(userId, courseId));
        if (lesson == null) {
            return null;
        }
        return BeanUtils.copyBean(lesson, LearningLessonVO.class);
    }

    @Override
    public Integer countLearningLessonByCourse(Long courseId) {
        // select count(1) from xx where course_id = #{cc} AND status in (0, 1, 2)
        return lambdaQuery()
                .eq(LearningLesson::getCourseId, courseId)
                .in(LearningLesson::getStatus,
                        LessonStatus.NOT_BEGIN.getValue(),
                        LessonStatus.LEARNING.getValue(),
                        LessonStatus.FINISHED.getValue())
                .count();
    }
}
