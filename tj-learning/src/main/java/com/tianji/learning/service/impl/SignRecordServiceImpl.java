package com.tianji.learning.service.impl;

import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BooleanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.mq.message.SignInMessage;
import com.tianji.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SignRecordServiceImpl implements ISignRecordService {

    private final RedisTemplate redisTemplate;
    private final RabbitMqHelper mqHelper;
    @Override
    public SignResultVO addSignRecords() {
        Long userId = UserContext.getUser();
        //1.签到
        LocalDate now = LocalDate.now();
        String key= RedisConstants.SIGN_RECORD_KEY_PREFIX+userId+":"+ now.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER);//格式化器在common下提前定义
        int offset = now.getDayOfMonth() - 1;
        Boolean b = redisTemplate.opsForValue().setBit(key, offset, true);
        //还要防止重复签到！！！ 返回1则代表签过！！
        if(BooleanUtils.isTrue(b)){
            throw new BizIllegalException("不允许重复签到！");
        }
        //2.计算连续签到天数
        int signDays=countSignDays(key,now.getDayOfMonth());
        // 3.计算签到得分
        int rewardPoints=0;
        switch (signDays){
            case 7:
                rewardPoints=10;
                break;
            case 14:
                rewardPoints=20;
                break;
            case 28:
                rewardPoints=40;
                break;
        }
        //4.保存积分明细记录
        mqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.SIGN_IN,
               SignInMessage.of(userId, rewardPoints+1)
        );

        //5.封装返回
        SignResultVO vo = new SignResultVO();
        vo.setSignDays(signDays);
        vo.setRewardPoints(rewardPoints);
        return vo;
    }

    private int countSignDays(String key, int len) {
        //1.获取本月从第一天开始到今天为止的所有签到记录
        List<Long> result= redisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(len)).valueAt(0));
        if(CollUtils.isEmpty(result))return 0;
        int num=result.get(0).intValue();

        int count=0;
        while((num&1)==1){
            count++;
            num >>>=1;
        }
        return count;
    }

    @Override
    public Byte[] querySignRecords() {
        // 1.获取登录用户
        Long userId = UserContext.getUser();
        // 2.获取日期
        LocalDate now = LocalDate.now();
        int dayOfMonth = now.getDayOfMonth();
        // 3.拼接key
        String key = RedisConstants.SIGN_RECORD_KEY_PREFIX
                + userId+":"
                + now.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER);
        // 4.读取
        List<Long> result = redisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create().get(
                        BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));
        if (CollUtils.isEmpty(result)) {
            return new Byte[0];
        }
        int num = result.get(0).intValue();

        Byte[] arr = new Byte[dayOfMonth];
        int pos = dayOfMonth - 1;
        while (pos >= 0){
            arr[pos--] = (byte)(num & 1);
            // 把数字右移一位，抛弃最后一个bit位，继续下一个bit位
            num >>>= 1;
        }
        return arr;
    }
}
