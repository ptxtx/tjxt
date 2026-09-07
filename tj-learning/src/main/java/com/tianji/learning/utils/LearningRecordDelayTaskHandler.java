package com.tianji.learning.utils;

import com.tianji.common.utils.JsonUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.DelayQueue;


@Slf4j
@RequiredArgsConstructor
@Component
public class LearningRecordDelayTaskHandler {

    private final StringRedisTemplate redisTemplate;
    private final DelayQueue<DelayTask<RecordTaskData>> queue=new DelayQueue<>();
    private final LearningRecordMapper recordMapper;
    private final ILearningLessonService lessonService;



    private final static String RECORD_KEY_TEMPLATE="learning:record:{}";

    private static volatile boolean begin=true;
    @PostConstruct
    public void init(){
        //不能直接调handleDelayTask  因为init方法是spring生命周期的一部分，一旦调用 死循环 Spring生命周期被阻塞
        CompletableFuture.runAsync(this::handleDelayTask);

    }
    @PreDestroy//在容器销毁之前调用这个函数
    public void preDestroy(){
        begin=false;
        log.info("延迟任务停止执行！");

    }
    public void handleDelayTask() {
        while(begin){
            try {
                //1.获取到期的延迟任务
                DelayTask<RecordTaskData> task = queue.take();
                //2.查询Redis缓存
                RecordTaskData data = task.getData();
                LearningRecord record = readRecordCache(data.getLessonId(), data.getSectionId());
                if(record==null){
                    continue;
                }
                //3.比较数据moment
                if(!Objects.equals(record.getMoment(), data.getMoment())){
                    //不一致 说明用户还在持续提交播放进度，放弃旧数据
                    continue;
                }
                //一致，持久化播放进度数据到数据库
                log.debug("持久化播放进度数据到数据库");
                //4.1更新学习记录moment
                record.setFinished(null);
                recordMapper.updateById(record);
                //4.2更新课表最近学习信息
                LearningLesson lesson = new LearningLesson();
                lesson.setId(data.getLessonId());
                lesson.setLatestSectionId(data.getSectionId());
                lesson.setLatestLearnTime(LocalDateTime.now().plusSeconds(-20));
                lessonService.updateById(lesson);
            } catch (InterruptedException e) {
                log.error("延迟任务处理异常", e);
            }

        }
    }

    public void addLearningRecordTask(LearningRecord record){
        //1.添加数据到Redis数据库
        writeRecordCache(record);
        //2.提交延迟任务到延迟队列DelayQueue
        queue.add(new DelayTask<>(new RecordTaskData(record), Duration.ofSeconds(20)));
    }

    public void writeRecordCache(LearningRecord record) {
        log.debug("更新学习记录的缓存数据");
        try {
            //1.数据转化
            RecordCacheData recordCacheData = new RecordCacheData(record);
            String jsonStr = JsonUtils.toJsonStr(recordCacheData);
            //2.写入Redis
            String key= StringUtils.format(RECORD_KEY_TEMPLATE, record.getLessonId());
            redisTemplate.opsForHash().put(key,record.getSectionId().toString(),jsonStr);
            //3.设置过期时间
            redisTemplate.expire(key, Duration.ofMinutes(1));
        } catch (Exception e) {
            log.error("更新学习记录的缓存数据失败", e);
        }
    }
    public LearningRecord readRecordCache(Long lessonId,Long sectionId){
        //1.读取Redis数据
        try {
            String key= StringUtils.format(RECORD_KEY_TEMPLATE, lessonId);
            Object cacheData = redisTemplate.opsForHash().get(key, sectionId.toString());
            if(cacheData==null){
                return null;
            }
            //2.数据检查和转换
            LearningRecord record = JsonUtils.toBean(cacheData.toString(), LearningRecord.class);
            return record;
        } catch (Exception e) {
            log.error("缓存读取异常", e);
            return null;
        }
    }
    public void cleanRecordCache(Long lessonId,Long sectionId){
        String key= StringUtils.format(RECORD_KEY_TEMPLATE, lessonId);
        redisTemplate.opsForHash().delete(key, sectionId.toString());
        log.debug("删除学习记录的缓存数据");
    }



    @Data
    @NoArgsConstructor
    private static class RecordCacheData{
        private Long id;
        private Integer moment;
        private Boolean finished;

        public RecordCacheData(LearningRecord record) {
            this.id = record.getId();
            this.moment = record.getMoment();
            this.finished = record.getFinished();
        }
    }
    @Data
    @NoArgsConstructor
    private static class RecordTaskData{
        private Long lessonId;
        private Long sectionId;
        private Integer moment;

        public RecordTaskData(LearningRecord record) {
            this.lessonId = record.getLessonId();
            this.sectionId = record.getSectionId();
            this.moment = record.getMoment();
        }
    }

}
