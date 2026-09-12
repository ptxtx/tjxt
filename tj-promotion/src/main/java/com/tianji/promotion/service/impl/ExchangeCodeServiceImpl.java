package com.tianji.promotion.service.impl;

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

import static com.tianji.promotion.constants.PromotionConstants.COUPON_CODE_SERIAL_KEY;

/**
 * <p>
 * 兑换码 服务实现类
 * </p>
 *
 * @author author
 */
@Service
public class ExchangeCodeServiceImpl extends ServiceImpl<ExchangeCodeMapper, ExchangeCode> implements IExchangeCodeService {

    private  BoundValueOperations<String, String> serialOps;//好处 从此以后 不用再指定key
    public ExchangeCodeServiceImpl(StringRedisTemplate redisTemplate) {
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
    }
}
