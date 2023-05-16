package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.user.UserClient;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.service.IPointsBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 学霸天梯榜 服务实现类
 * </p>
 *
 * @author 虎哥
 */
@Service
@RequiredArgsConstructor
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {

    private final StringRedisTemplate redisTemplate;

    private final UserClient userClient;

    @Override
    public PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query) {
        // 1.判断是否是查询当前赛季
        // 2.获取Redis的Key
        // 2.查询我的积分和排名
        // 3.查询榜单列表
        // 4.封装VO
        // 4.1.处理我的信息
        // 4.2.查询用户信息
        // 4.3.转换VO
        return null;
    }

    @Override
    public void createPointsBoardTableBySeason(Integer season) {
    }

    private List<PointsBoard> queryHistoryBoardList(PointsBoardQuery query) {
        // 1.计算表名
        // 2.查询数据
        // 3.数据处理
        return null;
    }

    @Override
    public List<PointsBoard> queryCurrentBoardList(String key, Integer pageNo, Integer pageSize) {
        // 1.计算分页
        // 2.查询
        // 3.封装
        return null;
    }

    private PointsBoard queryMyHistoryBoard(Long season) {
        // 1.获取登录用户
        // 2.计算表名
        // 3.查询数据
        // 4.转换数据
        return null;
    }

    private PointsBoard queryMyCurrentBoard(String key) {
        // 1.绑定key
        // 2.获取当前用户信息
        // 3.查询积分
        // 4.查询排名
        // 5.封装返回
        return null;
    }
}
