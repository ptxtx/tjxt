package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.IdAndNumDTO;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.domain.vo.LearningPlanVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.PlanStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final CatalogueClient catalogueClient;
    private final LearningRecordMapper recordMapper;
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
        Map<Long, CourseSimpleInfoDTO> cMap = queryCourseSimpleInfoList(records);

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

    private @NonNull Map<Long, CourseSimpleInfoDTO> queryCourseSimpleInfoList(List<LearningLesson> records) {
        Set<Long> cIds = records.stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());
        List<CourseSimpleInfoDTO> cInfoList = courseClient.getSimpleInfoList(cIds);
        if(CollUtils.isEmpty(cInfoList)){
            throw new BadRequestException("课程信息不存在，无法添加到课表");
        }
        //3.3把课程集合处理成map，key是courseId，值是course本身
        Map<Long, CourseSimpleInfoDTO> cMap = cInfoList.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, cInfo -> cInfo));
        return cMap;
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

    @Override
    public LearningLesson queryLessonByUserIdAndCourseId(Long userId, Long courseId) {
        return getOne(buildUserIdAndCourseIdWrapper(userId, courseId));
    }

        @Override
        public LearningLessonVO queryMyCurrentLesson() {
            // 1.获取当前登录的用户
            Long userId = UserContext.getUser();
            // 2.查询正在学习的课程 select * from xx where user_id = #{userId} AND status = 1 order by latest_learn_time limit 1
            LearningLesson lesson = lambdaQuery()
                    .eq(LearningLesson::getUserId, userId)
                    .eq(LearningLesson::getStatus, LessonStatus.LEARNING.getValue())
                    .orderByDesc(LearningLesson::getLatestLearnTime)
                    .last("limit 1")
                    .one();
            if (lesson == null) {
                return null;
            }
            // 3.拷贝PO基础属性到VO
            LearningLessonVO vo = BeanUtils.copyBean(lesson, LearningLessonVO.class);
            // 4.查询课程信息
            CourseFullInfoDTO cInfo = courseClient.getCourseInfoById(lesson.getCourseId(), false, false);
            if (cInfo == null) {
                throw new BadRequestException("课程不存在");
            }
            vo.setCourseName(cInfo.getName());
            vo.setCourseCoverUrl(cInfo.getCoverUrl());
            vo.setSections(cInfo.getSectionNum());
            // 5.统计课表中的课程数量 select count(1) from xxx where user_id = #{userId}
            Integer courseAmount = lambdaQuery()
                    .eq(LearningLesson::getUserId, userId)
                    .count();
            vo.setCourseAmount(courseAmount);
            // 6.查询小节信息
            List<CataSimpleInfoDTO> cataInfos =
                    catalogueClient.batchQueryCatalogue(CollUtils.singletonList(lesson.getLatestSectionId()));
            if (!CollUtils.isEmpty(cataInfos)) {
                CataSimpleInfoDTO cataInfo = cataInfos.get(0);
                vo.setLatestSectionName(cataInfo.getName());
                vo.setLatestSectionIndex(cataInfo.getCIndex());
            }
            return vo;
        }

    @Override
    public void createLearningPlan(LearningPlanDTO planDTO) {
        //1.查询课表中指定课程有关数据
        Long userId = UserContext.getUser();
        LearningLesson lesson = queryLessonByUserIdAndCourseId(userId, planDTO.getCourseId());
        if(lesson==null){
            throw new BadRequestException("课程信息不存在");
        }
        LearningLesson l=new LearningLesson();
        l.setId(lesson.getId());
        l.setWeekFreq(planDTO.getFreq());
        if(lesson.getPlanStatus()==PlanStatus.NO_PLAN) {
            l.setPlanStatus(PlanStatus.PLAN_RUNNING);
        }
        updateById(l);
    }

    @Override
    public LearningPlanPageVO queryMyPlans(PageQuery query) {
        LearningPlanPageVO result=new LearningPlanPageVO();
        //1.获取当前登录用户
        Long userId = UserContext.getUser();
        //2.获取本周起始时间
        LocalDate now = LocalDate.now();
        LocalDateTime begin = DateUtils.getWeekBeginTime(now);
        LocalDateTime end = DateUtils.getWeekEndTime(now);
        //3.查询总的统计数据
        //3.1本周总已学习小结数量
        Integer weekFinished = recordMapper.selectCount(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getFinished, true)
                .gt(LearningRecord::getFinishTime, begin)
                .lt(LearningRecord::getFinishTime, end)
        );
        result.setWeekFinished(weekFinished);
        //3.2 本周总的计划学习小结数量
        Integer weekTotalPlan =getBaseMapper().queryTotalPlan(userId);
        result.setWeekTotalPlan(weekTotalPlan);
        // TODO 3.3本周学习积分

        //4.查询分页数据
        //4.1分页查询课表信息以及学习计划数量
        Page<LearningLesson> page = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getPlanStatus, PlanStatus.PLAN_RUNNING)
                .in(LearningLesson::getStatus, LessonStatus.NOT_BEGIN.getValue(), LessonStatus.LEARNING.getValue())
                .page(query.toMpPage("latest_learn_time", false));
        List<LearningLesson> records = page.getRecords();
        if(CollUtils.isEmpty(records)){
            //返回一个空的LearningPlanPageVO
            return result.pageInfo(PageDTO.empty(page));
        }
        //4.2 查询课表对应的课程信息
        Map<Long, CourseSimpleInfoDTO> cMap = queryCourseSimpleInfoList(records);
        //4.3统计每一个课程本周已学习小节数量
        List<IdAndNumDTO> list=recordMapper.countLearnedSections(userId,begin,end);
        Map<Long, Integer> countMap = IdAndNumDTO.toMap(list);
        //组装成vo
        List<LearningPlanVO> voList=new ArrayList<>(records.size());
        for(LearningLesson r : records){
            LearningPlanVO vo = BeanUtils.copyBean(r, LearningPlanVO.class);
            CourseSimpleInfoDTO cInfo = cMap.get(r.getCourseId());
            if(cInfo!=null){
                vo.setCourseName(cInfo.getName());
                vo.setSections(cInfo.getSectionNum());
            }
            vo.setWeekLearnedSections(countMap.getOrDefault(r.getId(), 0));
            voList.add(vo);

        }
        return result.pageInfo(page.getTotal(),page.getPages(),voList);
    }
}
