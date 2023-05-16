package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.service.IPointsRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务实现类
 * </p>
 *
 * @author 虎哥
 */
@Service
@RequiredArgsConstructor
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements IPointsRecordService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void addPointsRecord(Long userId, int points, PointsRecordType type) {
        // 1.判断当前方式有没有积分上限
            // 2.有，则需要判断是否超过上限
            // 2.1.查询今日已得积分
            // 2.2.判断是否超过上限
                // 2.3.超过，直接结束
            // 2.4.没超过，保存积分记录
        // 3.没有，直接保存积分记录
        // 4.更新总积分到Redis
    }

    @Override
    public List<PointsStatisticsVO> queryMyPointsToday() {
        // 1.获取用户
        // 2.获取日期
        // 3.构建查询条件
        // 4.查询
        // 5.封装返回
        return null;
    }

    private int queryUserPointsByTypeAndDate(
            Long userId, PointsRecordType type, LocalDateTime begin, LocalDateTime end) {
        // 1.查询条件
        // 2.调用mapper，查询结果
        // 3.判断并返回
        return 0;
    }
}
