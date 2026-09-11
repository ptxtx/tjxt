package com.tianji.learning.service.impl;

import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.vo.PointsBoardItemVO;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.query.PointsBoardQuery;
import com.tianji.learning.service.IPointsBoardService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 学霸天梯榜 服务实现类
 * </p>
 *
 * @author author
 */
@Service
@RequiredArgsConstructor
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {

    private final StringRedisTemplate redisTemplate;
    private final UserClient userClient;
    @Override
    public PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query) {
        //查询是否是当前赛季
        Long season = query.getSeason();
        boolean isCurrent=season==null||season==0;
        //1.查询我的积分和排名
        LocalDateTime now = LocalDateTime.now();
        String key= RedisConstants.POINTS_BOARD_KEY_PREFIX+ now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        PointsBoard myBoard;
        if(isCurrent){
            //查询当前赛季 （Redis）-得到JSON---统一po返回
            myBoard= queryMyCurrentBoard(key);
        }
        else{
            //查询指定赛季(Mysql) -得到PO
           myBoard= queryMyHistoryBoard(season);
        }
        //2.查询榜单列表
        List<PointsBoard> list=isCurrent?queryCurrentBoardList(key,query.getPageNo(),query.getPageSize()):queryHistoryBoardList(query);

        PointsBoardVO vo = new PointsBoardVO();
        //我的信息
        if(myBoard!=null){
            vo.setPoints(myBoard.getPoints());
            vo.setRank(myBoard.getRank());
        }
        if(CollUtils.isEmpty(list)){
            return vo;
        }
        Set<Long> userIds = list.stream().map(PointsBoard::getUserId).collect(Collectors.toSet());
        List<UserDTO> users = userClient.queryUserByIds(userIds);
        Map<Long, String> userMap=new HashMap<>(userIds.size());
        if(CollUtils.isNotEmpty(users)) {
            userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, UserDTO::getName));
        }
        List<PointsBoardItemVO> items=new ArrayList<>(list.size());
        for (PointsBoard p : list) {
            PointsBoardItemVO v = new PointsBoardItemVO();
            v.setPoints(p.getPoints());
            v.setRank(p.getRank());
            v.setName(userMap.get(p.getUserId()));
            items.add(v);
        }
        vo.setBoardList(items);
        return vo;
    }

    private List<PointsBoard> queryHistoryBoardList(PointsBoardQuery query) {
        return null;
    }

    private PointsBoard queryMyHistoryBoard(Long season) {
        return null;
    }

    @Override
    public List<PointsBoard> queryCurrentBoardList(String key, @Min(value = 1, message = "页码不能小于1") Integer pageNo, @Min(value = 1, message = "每页查询数量不能小于1") Integer pageSize) {
        int from = (pageNo - 1) * pageSize;
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, from, pageNo * pageSize - 1);
        //封装
        int rank=from+1;
        List<PointsBoard> list=new ArrayList<>(tuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String userId = tuple.getValue();
            Double points = tuple.getScore();
            PointsBoard p = new PointsBoard();
            p.setUserId(Long.valueOf(userId));
            p.setPoints(points==null?0:points.intValue());
            p.setRank(rank++);
            list.add(p);
        }
        return list;
    }

    private PointsBoard queryMyCurrentBoard(String key) {
        //绑定key，这样后面都不用再传key
        BoundZSetOperations<String, String> ops = redisTemplate.boundZSetOps(key);
        //查询积分
        String userId = UserContext.getUser().toString();
        Double score = ops.score(userId);
        //查询排名
        Long rank = ops.reverseRank(userId);
        //封装返回
        PointsBoard p = new PointsBoard();
        p.setPoints(score==null?0:score.intValue());
        p.setRank(rank==null?0:rank.intValue()+1);
        return p;
    }

    @Override
    public void createPointsBoardTableBySeason(Integer season) {
        getBaseMapper().createPointsBoardTable("points_board_"+season);
    }
}
