package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.SectionType;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILeaningRecordService;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.utils.LearningRecordDelayTaskHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord> implements ILeaningRecordService {

    private final ILearningLessonService lessonService;
    private final CourseClient courseClient;

    private final LearningRecordDelayTaskHandler taskHandler;

    @Override
    public LearningLessonDTO queryLearningRecordByCourse(Long courseId) {
        Long userId = UserContext.getUser();
        LearningLesson lesson = lessonService.queryLessonByUserIdAndCourseId(userId,courseId);
        if (lesson == null) {
            return null;
        }
        //查询学习记录
        //select * from xx where lesson_id={lessonId}
        List<LearningRecord> records = lambdaQuery().eq(LearningRecord::getLessonId, lesson.getId()).list();
        //封装结果
        LearningLessonDTO dto=new LearningLessonDTO();
        dto.setId(lesson.getId());
        dto.setLatestSectionId(lesson.getLatestSectionId());
        dto.setRecords(BeanUtils.copyList(records, LearningRecordDTO.class));
        return dto;



    }

    @Override
    @Transactional
    public void addLearningRecord(LearningRecordFormDTO formDTO) {
        Long userId = UserContext.getUser();

        //处理学习记录
        boolean finished=false;
        if(formDTO.getSectionType()== SectionType.VIDEO){
            //视频
            finished=handleVideoRecord(userId,formDTO);
        }else{
            //考试
            finished=handleExamRecord(userId,formDTO);
        }
        //处理课表数据
        //交给延迟任务
        if(!finished){
            //没有新学完的小节，无需更新课表中学习进度
            return;
        }
        handleLearningLessonChanges(formDTO);
    }

    private void handleLearningLessonChanges(LearningRecordFormDTO formDTO) {
        //1.查询课表
        LearningLesson lesson = lessonService.getById(formDTO.getLessonId());
        if(lesson==null){
            throw new BizIllegalException("课程不存在，无法更新数据！");
        }
        boolean allLearned=false;
        //if(finished){
            CourseFullInfoDTO cInfo = courseClient.getCourseInfoById(lesson.getCourseId(), false, false);
            if(cInfo==null){
                throw new BizIllegalException("课程不存在，无法更新数据！");
            }
           allLearned= lesson.getLearnedSections()+1>=cInfo.getSectionNum();
        //}
        lessonService.lambdaUpdate()
                .set(lesson.getLearnedSections()==0,LearningLesson::getStatus,LessonStatus.LEARNING.getValue())
                .set(allLearned, LearningLesson::getStatus, LessonStatus.FINISHED.getValue())
                .setSql("learned_sections=learned_sections+1")
//                .set(!finished,LearningLesson::getLatestSectionId, formDTO.getSectionId())
//                .set(!finished,LearningLesson::getLatestLearnTime,formDTO.getCommitTime())
                .eq(LearningLesson::getId,lesson.getId())
                .update();

    }

    private boolean handleExamRecord(Long userId, LearningRecordFormDTO formDTO) {
        //转换DTO为PO 表单数据
        LearningRecord record = BeanUtils.copyBean(formDTO, LearningRecord.class);
        record.setUserId(userId);
        record.setFinished(true);
        record.setFinishTime(formDTO.getCommitTime());
        boolean success = save(record);
        if(!success){
            throw new DbException("新增考试记录失败！");
        }
        return true;
    }

    private boolean handleVideoRecord(Long userId, LearningRecordFormDTO formDTO) {
        //1.查询旧的学习记录，判断是否存在 不存在
        LearningRecord old=queryOldRecord(formDTO.getLessonId(), formDTO.getSectionId());
        if(old==null){
            //不存在 则新增
            LearningRecord record = BeanUtils.copyBean(formDTO, LearningRecord.class);
            record.setUserId(userId);
            record.setFinished(false);
            boolean success = save(record);
            if(!success){
                throw new DbException("新增视频记录失败！");
            }
            return false;
        }
        //存在 则更新
        //超过50% 且旧状态是未完成才是第一次学完
        boolean finished= (formDTO.getMoment()*2>=formDTO.getDuration()&&old.getFinished()==false);
        if(!finished){
            LearningRecord record = new LearningRecord();
            record.setLessonId(formDTO.getLessonId());
            record.setSectionId(formDTO.getSectionId());
            record.setMoment(formDTO.getMoment());
            record.setFinished(old.getFinished());
            record.setId(old.getId());
            taskHandler.addLearningRecordTask(record);
            return false;
        }
        boolean success = lambdaUpdate()
                .set(LearningRecord::getMoment, formDTO.getMoment())
                .set(LearningRecord::getFinished, true)
                .set(LearningRecord::getFinishTime, formDTO.getCommitTime())
                .eq(LearningRecord::getId, old.getId())
                .update();
        if(!success){
            throw new DbException("新增视频记录失败！");
        }

        //4.3清理缓存 确保一致
        taskHandler.cleanRecordCache(formDTO.getLessonId(), formDTO.getSectionId());
        return true;


    }

    private LearningRecord queryOldRecord(@NotNull(message = "课表id不能为空") Long lessonId, @NotNull(message = "节的id不能为空") Long sectionId) {
        //1.查询缓存
        LearningRecord record = taskHandler.readRecordCache(lessonId, sectionId);
        //2.如果命中 直接返回
        if(record!=null){
            return record;
        }
        //3.如果未命中 查数据库 再写入缓存
       record= lambdaQuery()
                .eq(LearningRecord::getLessonId, lessonId)
                .eq(LearningRecord::getSectionId, sectionId)
                .one();
        taskHandler.writeRecordCache(record);
        return record;
    }
}
