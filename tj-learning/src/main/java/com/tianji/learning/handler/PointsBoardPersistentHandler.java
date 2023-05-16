package com.tianji.learning.handler;

import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsBoardService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointsBoardPersistentHandler {

    private final IPointsBoardSeasonService seasonService;

    private final IPointsBoardService pointsBoardService;

    private final StringRedisTemplate redisTemplate;

    @XxlJob("createTableJob")
    public void createPointsBoardTableOfLastSeason(){
        // 1.获取上月时间
        // 2.查询赛季id
            // 赛季不存在
        // 3.创建表
    }

    @XxlJob("savePointsBoard2DB")
    public void savePointsBoard2DB(){
        // 1.获取上月时间

        // 2.计算动态表名
        // 2.1.查询赛季信息
        // 2.2.存入ThreadLocal

        // 3.查询榜单数据
        // 3.1.拼接KEY
        // 3.2.查询数据
            // 4.持久化到数据库
            // 4.1.把排名信息写入id
            // 4.2.持久化
            // 5.翻页

    }

    @XxlJob("clearPointsBoardFromRedis")
    public void clearPointsBoardFromRedis(){
        // 1.获取上月时间
        // 2.计算key
        // 3.删除
    }
}
