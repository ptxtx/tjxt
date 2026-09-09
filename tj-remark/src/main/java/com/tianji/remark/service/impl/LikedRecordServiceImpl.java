package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.api.dto.remark.LikeTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE;

/**
 * <p>
 * 点赞记录表 服务实现类
 * </p>
 *
 * @author author
 */
//@Service
@RequiredArgsConstructor
public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {

    //private final AmqpTemplate amqpTemplate;
    private final RabbitMqHelper mqHelper;
    @Override
    public void addLikeRecord(LikeRecordFormDTO dto) {
        //1.根据前端的参数，判断是执行点赞还是取消点赞
        boolean success= dto.getLiked()?like(dto):unlike(dto);
        //2.判断是否执行成功，如果失败，则直接结束
        if(!success){
            return;
        }
        //3.如果执行成功，统计点赞总数
        Integer likeTimes = lambdaQuery()
                .eq(LikedRecord::getBizId, dto.getBizId())
                .count();
        //4.发送MQ通知
        //业务类型作为routingKey模版中的变量
        mqHelper.send(
                LIKE_RECORD_EXCHANGE,
                StringUtils.format(LIKED_TIMES_KEY_TEMPLATE, dto.getBizType()),
                LikeTimesDTO.of(dto.getBizId(), likeTimes)
                );
    }

    private boolean unlike(LikeRecordFormDTO dto) {
        //可以上来就删 根据删除的结果判断点赞记录是否存在
        boolean success = remove(new QueryWrapper<LikedRecord>().lambda()
                .eq(LikedRecord::getUserId, UserContext.getUser())
                .eq(LikedRecord::getBizId, dto.getBizId()));
        return success;
    }

    private boolean like(LikeRecordFormDTO dto) {
        //1.查询点赞记录
        Integer count = lambdaQuery()
                .eq(LikedRecord::getUserId, UserContext.getUser())
                .eq(LikedRecord::getBizId, dto.getBizId())
                .count();

        //2.判断是否存在
        if(count>0){
            return false;//已经点赞过 算作失败
        }
        //3.插入点赞记录
        LikedRecord r=new LikedRecord();
        r=BeanUtils.copyBean(dto, LikedRecord.class);
        r.setUserId(UserContext.getUser());
        save(r);
        return true;
    }

    @Override
    public Set<Long> isBizLiked(List<Long> bizIds) {
        Long userId = UserContext.getUser();
        List<LikedRecord> list = lambdaQuery()
                .in(LikedRecord::getBizId, bizIds)
                .eq(LikedRecord::getUserId, userId)
                .list();
        return list.stream().map(LikedRecord::getBizId).collect(Collectors.toSet());
    }

    @Override
    public void readLikedTimesAndSendMessage(String bizType, Integer maxBizSize) {
    }
}
