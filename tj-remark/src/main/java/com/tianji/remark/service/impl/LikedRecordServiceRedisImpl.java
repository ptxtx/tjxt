package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.remark.LikeTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.config.LikeTimesProperties;
import com.tianji.remark.constants.RedisConstants;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE;
import org.springframework.data.redis.serializer.RedisSerializer;
/**
 * <p>
 * 点赞记录表 服务实现类
 * </p>
 *
 * @author author
 */
@Service
@RequiredArgsConstructor
@Builder
public class LikedRecordServiceRedisImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {

    //private final AmqpTemplate amqpTemplate;
    private final RabbitMqHelper mqHelper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void addLikeRecord(LikeRecordFormDTO dto) {
        //1.根据前端的参数，判断是执行点赞还是取消点赞
        boolean success= dto.getLiked()?like(dto):unlike(dto);
        //2.判断是否执行成功，如果失败，则直接结束
        if(!success){
            return;
        }
        //3.如果执行成功，统计点赞总数
        String key= RedisConstants.LIKES_BIZ_KEY_PREFIX+dto.getBizId();
        Long likedTimes = redisTemplate.opsForSet().size(key);
        //4.缓存点赞总数到Redis
        if(likedTimes==null){
            return;
        }
        redisTemplate.opsForZSet().add(
                RedisConstants.LIKES_TIMES_KEY_PREFIX+dto.getBizType(),
                dto.getBizId().toString(),
                likedTimes
        );


    }

    private boolean unlike(LikeRecordFormDTO dto) {
        Long userId = UserContext.getUser();
        //直接执行SADD命令
        String key= RedisConstants.LIKES_BIZ_KEY_PREFIX+dto.getBizId();
        Long result = redisTemplate.opsForSet().remove(key, userId.toString());//成功返回1 失败返回0
        return result!=null&&result>0;
    }

    private boolean like(LikeRecordFormDTO dto) {
        Long userId = UserContext.getUser();
        //直接执行SADD命令
        String key= RedisConstants.LIKES_BIZ_KEY_PREFIX+dto.getBizId();
        Long result = redisTemplate.opsForSet().add(key, userId.toString());//返回新增元素个数
        return result!=null&&result>0;
    }

    @Override
    public Set<Long> isBizLiked(List<Long> bizIds) {
        Long userId = UserContext.getUser();
        List<Object> objects = redisTemplate.executePipelined(
                (RedisCallback<Object>) connection -> {
                    RedisSerializer<String> serializer =
                            redisTemplate.getStringSerializer();

                    byte[] userBytes =
                            serializer.serialize(userId.toString());

                    for (Long bizId : bizIds) {
                        String key =
                                RedisConstants.LIKES_BIZ_KEY_PREFIX + bizId;

                        connection.sIsMember(
                                serializer.serialize(key),
                                userBytes
                        );
                    }
                    return null;
                }
        );
        Set<Long> set=new HashSet<>();
        for (int i = 0; i < objects.size(); i++) {
            Boolean o = (Boolean) objects.get(i);//isMember的结果 返回boolean值
            if(o){
                Long l = bizIds.get(i);
                set.add(l);
            }
        }
//        IntStream.range(0, objects.size())
//                .filter(i->(Boolean) objects.get(i))
//                .mapToObj(i->bizIds.get(i))
//                .collect(Collectors.toSet());
        return set;
    }

    @Override
    public void readLikedTimesAndSendMessage(String bizType, Integer maxBizSize) {
        //读取并移除Redis中的缓存总数
        String key=RedisConstants.LIKES_TIMES_KEY_PREFIX+bizType;
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().popMin(key, maxBizSize);
        if(CollUtils.isEmpty(tuples)){
            return;
        }
        //2.数据转换
        List<LikeTimesDTO> list=new ArrayList<>(tuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String bizId = tuple.getValue();
            Double likedTime = tuple.getScore();
            list.add(LikeTimesDTO.builder()
                    .bizId(Long.valueOf(bizId))
                    .likeTimes(likedTime.intValue())
                    .build());
            if(bizId==null||likedTime==null){
                continue;
            }
        }
        //3.MQ
        mqHelper.send(LIKE_RECORD_EXCHANGE, StringUtils.format(LIKED_TIMES_KEY_TEMPLATE, bizType), list);
    }
}
