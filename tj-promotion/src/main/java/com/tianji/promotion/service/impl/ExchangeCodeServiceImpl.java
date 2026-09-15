package com.tianji.promotion.service.impl;

import com.tianji.common.utils.CollUtils;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.mapper.ExchangeCodeMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.utils.CodeUtil;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.tianji.promotion.constants.PromotionConstants.*;


/**
 * <p>
 * 兑换码 服务实现类
 * </p>
 *
 * @author author
 */
@Service
public class ExchangeCodeServiceImpl extends ServiceImpl<ExchangeCodeMapper, ExchangeCode> implements IExchangeCodeService {

    private final StringRedisTemplate redisTemplate;
    private  BoundValueOperations<String, String> serialOps;//好处 从此以后 不用再指定key
    public ExchangeCodeServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate=redisTemplate;
        //Redis自增序列号 key都是同一个 所以这里用bound 直接绑定
       serialOps = redisTemplate.boundValueOps(COUPON_CODE_SERIAL_KEY);
    }

    @Override
    @Async("generateExchangeCodeExecutor")
    public void asyncGenerateCode(Coupon coupon) {
        Integer totalNum = coupon.getTotalNum();
        //1，获取Redis自增序列号
        Long maxSerialNum = serialOps.increment(totalNum);
        if(maxSerialNum == null){
            return;
        }
        //2.生成兑换码
        //(fresh用coupon的id）
        List<ExchangeCode> list=new ArrayList<>(totalNum);
        for (int serialNum = (int)(maxSerialNum-totalNum+1); serialNum <=maxSerialNum ; serialNum++) {
            String code = CodeUtil.generateCode(serialNum, coupon.getId());
            ExchangeCode e = new ExchangeCode();
            e.setCode(code);
            e.setId(serialNum);
            e.setExchangeTargetId(coupon.getId());
            e.setExpiredTime(coupon.getIssueEndTime());
            list.add(e);
        }
        //3.保存到数据库
        saveBatch(list);

        redisTemplate.opsForZSet().add(COUPON_RANGE_KEY,coupon.getId().toString(),maxSerialNum);
    }

    @Override
    public boolean updateExchangeMark(long serialNum, boolean mark) {
        Boolean b = redisTemplate.opsForValue().setBit(COUPON_CODE_MAP_KEY, serialNum, mark);
        return (b !=null) && b;
    }

    @Override
    public Long exchangeTargetId(long serialNum) {
        //查询score值比当前序列号大的第一个优惠券
        Set<String> results = redisTemplate.opsForZSet().rangeByScore(
                COUPON_RANGE_KEY, serialNum, serialNum + 5000, 0L, 1L
        );
        if(CollUtils.isEmpty(results)){
            return null;
        }
        String next = results.iterator().next();
        return Long.parseLong(next);
    }
}
