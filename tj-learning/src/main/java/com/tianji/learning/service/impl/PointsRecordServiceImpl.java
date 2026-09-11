package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.service.IPointsRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务实现类
 * </p>
 *
 * @author author
 */
@Service
@RequiredArgsConstructor
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements IPointsRecordService {
    private final StringRedisTemplate redisTemplate;
    @Override
    public void addPointsRecord(Long userId, int points, PointsRecordType type) {
        int maxPoints = type.getMaxPoints();
        int actualPoints=points;
        //1.判断当前方式有没有积分上限 没有 直接保存积分记录
        if(maxPoints >0){
        //2.有，则需要判断今日是否超过上限
            //今日已得
            LocalDateTime now=LocalDateTime.now();
            LocalDateTime begin=DateUtils.getDayStartTime(now);
            LocalDateTime end=DateUtils.getDayEndTime(now);
           int currPoints=queryUserPointsByTypeAndDate(userId, type, begin, end);
           int remainPoints=maxPoints-currPoints;
           if(remainPoints<=0){
               return;
           }
           actualPoints=Math.min(points,remainPoints);
        }
        //3.没超过 才保存积分记录
        PointsRecord pointsRecord = new PointsRecord();
        pointsRecord.setPoints(actualPoints);
        pointsRecord.setUserId(userId);
        pointsRecord.setType(type);
        save(pointsRecord);

        //4.累积积分数据到Redis的sortedset中
        String key=RedisConstants.POINTS_BOARD_KEY_PREFIX+LocalDate.now().format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        redisTemplate.opsForZSet().incrementScore(key,userId.toString(),actualPoints);
    }

    private int queryUserPointsByTypeAndDate(Long userId, PointsRecordType type, LocalDateTime begin, LocalDateTime end) {
        //1.查询条件
        QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(PointsRecord::getUserId, userId)
                .eq(type!=null,PointsRecord::getType,type)
                .between(begin!=null&&end!=null,PointsRecord::getCreateTime,begin,end);
        //2,查询结果
       Integer points= getBaseMapper().queryUserPointsByTypeAndDate(wrapper);
       return points==null?0:points;
    }

    @Override
    public List<PointsStatisticsVO> queryMyPointsToday() {
        Long userId = UserContext.getUser();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime begin = DateUtils.getDayStartTime(now);
        LocalDateTime end = DateUtils.getDayEndTime(now);

        QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(PointsRecord::getUserId,userId)
                .between(begin!=null&&end!=null,PointsRecord::getCreateTime,begin,end);
        List<PointsRecord> list= getBaseMapper().queryUserPointsByDate(wrapper);//获得其中两个字段 type和points
        if(CollUtils.isEmpty(list)){
            return CollUtils.emptyList();
        }

        List<PointsStatisticsVO> vos=new ArrayList<>(list.size());
        for (PointsRecord p : list) {
            PointsStatisticsVO vo = new PointsStatisticsVO();
            vo.setType(p.getType().getDesc());
            vo.setMaxPoints(p.getType().getMaxPoints());
            vo.setPoints(p.getPoints());
            vos.add(vo);
        }
        return vos;
    }
}
