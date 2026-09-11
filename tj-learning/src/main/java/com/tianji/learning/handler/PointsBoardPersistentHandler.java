package com.tianji.learning.handler;

import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsBoardService;
import com.tianji.learning.utils.TableInfoContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PointsBoardPersistentHandler {
    private final IPointsBoardSeasonService seasonService;
    private final IPointsBoardService pointsBoardService;
    private  final StringRedisTemplate redisTemplate;
    //@Scheduled(cron = "0 0 3 1 * ?")
    @XxlJob("createTableJob")
    public void createPointsBoardTableOfLastSeason(){
        //1.获取上月时间
        LocalDateTime time = LocalDateTime.now().minusMonths(1);
        //2，查询赛季id
        Integer season=seasonService.querySeasonByTime(time);
        if(season==null){
            //赛季不存在
            XxlJobHelper.log("未查询到对应赛季，时间：{}", time);
            throw new IllegalStateException("上月赛季不存在");
        }
        //3.创建表
        pointsBoardService.createPointsBoardTableBySeason(season);
    }

    @XxlJob("savePoints2DB")
    public void savePoints2DB() {
        //1.获取上月时间
        LocalDateTime time = LocalDateTime.now().minusMonths(1);
        //3.计算动态表名
        Integer season = seasonService.querySeasonByTime(time);
        TableInfoContext.setInfo("points_board_" + season);
        //2.查询榜单数据
        //2.1 拼接redis的key
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + time.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        //2.2查询数据
        int index = XxlJobHelper.getShardIndex();
        int total = XxlJobHelper.getShardTotal();
        int pageNo = index+1;
        int pageSize = 1000;
        while (true) {
            List<PointsBoard> boardList = pointsBoardService.queryCurrentBoardList(key, pageNo, pageSize);
            if (CollUtils.isEmpty(boardList)) {
                break;
            }
            //4.持久化到数据库
            //把排名信息写到id
            boardList.forEach(board -> {
                board.setId(board.getRank().longValue());
                board.setRank(null);
            });
            pointsBoardService.saveBatch(boardList);

            //5.分页
            pageNo+=total;
        }
        TableInfoContext.removeInfo();
    }

    @XxlJob("cleanPointsBoardFromRedis")
    public void cleanPointsBoardFromRedis(){
        //1.获取上月时间
        LocalDateTime time = LocalDateTime.now().minusMonths(1);
        String key= RedisConstants.POINTS_BOARD_KEY_PREFIX+time.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        redisTemplate.unlink(key);//异步删除！！！
    }
}
