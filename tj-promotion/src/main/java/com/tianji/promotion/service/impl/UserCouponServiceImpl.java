package com.tianji.promotion.service.impl;

import cn.hutool.core.bean.copier.CopyOptions;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.autoconfigure.redisson.annotations.Lock;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.enums.ExchangeCodeStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.utils.CodeUtil;
import com.tianji.promotion.utils.MyLock;
import com.tianji.promotion.utils.RedisLock;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类
 * </p>
 *
 * @author author
 */
@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {

    private final CouponMapper couponMapper;
    private final IExchangeCodeService codeService;
    private final StringRedisTemplate redisTemplate;

    private final RabbitMqHelper mqHelper;
    private final RedissonClient redissonClient;

    @Override
    @Lock(name = "lock:coupon:#{couponId}")
    public void receiveCoupon(Long couponId) {
        //查询优惠券
        //Coupon coupon = couponMapper.selectById(couponId);
        Coupon coupon=queryCouponByCache(couponId);
        if(coupon==null){
            throw new BadRequestException("优惠券不存在！");
        }
        //校验发放时间
        LocalDateTime now = LocalDateTime.now();
        if(now.isBefore(coupon.getIssueBeginTime())||now.isAfter(coupon.getIssueEndTime())){
            throw new BadRequestException("优惠券发放已结束或者尚未开始！");
        }
        //校验库存
        if(coupon.getTotalNum()<=0){
            throw new BadRequestException("优惠券库存不足！");
        }

        Long userId = UserContext.getUser();
        //校验每人限领数量
        //1.查询领取数量 再校验限领数量
        String key=PromotionConstants.USER_COUPON_CACHE_KEY_PREFIX+couponId;
        Long count = redisTemplate.opsForHash().increment(key, userId.toString(),1);//返回增加完之后的结果
        if(count >coupon.getUserLimit()){
            throw new BadRequestException("超出限领数量！");

        }
        //扣减优惠券库存
        redisTemplate.opsForHash().increment(PromotionConstants.COUPON_CACHE_KEY_PREFIX+couponId, "totalNum", -1);
        //发送mQ消息
        UserCouponDTO uc = new UserCouponDTO();
        uc.setCouponId(couponId);
        uc.setUserId(userId);
        mqHelper.send(MqConstants.Exchange.PROMOTION_EXCHANGE,MqConstants.Key.COUPON_RECEIVE,uc);

        //分布式锁
        //创建锁对象
       // String key="lock:coupon:uid"+userId;
        //RedisLock lock = new RedisLock(key, redisTemplate);
//        RLock lock = redissonClient.getLock(key);
//        boolean isLock = lock.tryLock();
//        if (!isLock) {
//            throw new BizIllegalException("请求太频繁！");
//        }
//        try {
//            IUserCouponService userCouponService = (IUserCouponService) AopContext.currentProxy();
//            userCouponService.checkAndCreateUserCoupon(userId, coupon);//校验并生成优惠券
//        }finally {
//            //加入
//            lock.unlock();
//        }


    }

    private Coupon queryCouponByCache(Long couponId) {
        String key= PromotionConstants.COUPON_CACHE_KEY_PREFIX+couponId;
        Map<Object, Object> objMap = redisTemplate.opsForHash().entries(key);
        if(objMap.isEmpty()){
            return null;
        }
        //数据反序列化
        return BeanUtils.mapToBean(objMap, Coupon.class,false, CopyOptions.create());
    }

    @Transactional
    @Override
    public void checkAndCreateUserCoupon(UserCouponDTO uc) {
        Coupon coupon = couponMapper.selectById(uc.getCouponId());
        if(coupon==null){
            throw new BizIllegalException("优惠券不存在！");
        }
//            Integer count = lambdaQuery()
//                    .eq(UserCoupon::getCouponId, coupon.getId())
//                    .eq(UserCoupon::getUserId, userId)
//                    .count();
//            if (count != null && count >= coupon.getUserLimit()) {
//                throw new BadRequestException("领取已达上限！");
//            }
            //更新优惠券已经发放数量+1
            int r = couponMapper.incrIssueNum(coupon.getId());//更新成功的行的数量
            if (r == 0) {
                throw new BizIllegalException("优惠券库存不足！");
            }
            //新增user-coupon
            saveUserCoupon(uc.getUserId(), coupon);

    }

    private void saveUserCoupon(Long userId, Coupon coupon) {
        //基本信息
        UserCoupon uc = new UserCoupon();
        uc.setCouponId(coupon.getId());
        uc.setUserId(userId);
        LocalDateTime termBeginTime = coupon.getTermBeginTime();
        LocalDateTime termEndTime = coupon.getTermEndTime();
        //有效期信息
        if(termBeginTime==null){
            //说明是按天来的
            termBeginTime=LocalDateTime.now();
            termEndTime=termBeginTime.plusDays(coupon.getTermDays());
        }
        uc.setTermBeginTime(termBeginTime);
        uc.setTermEndTime(termEndTime);
        save(uc);
    }

    @Override
    @Transactional
    public void exchangeCoupon(String code) {
        //1.校验并解析兑换码
        long serialNum = CodeUtil.parseCode(code);//也就是兑换码id
        //2.校验是否已经兑换（Redis的BitMap） GETBIT KEY OFFSET-->SETBIT
        boolean exchanged= codeService.updateExchangeMark(serialNum,true);
        if(exchanged){
            throw new BizIllegalException("兑换码已兑换过！");
        }
        try {
            //3.查询兑换码（数据库）
            ExchangeCode exchangeCode = codeService.getById(serialNum);
            if(exchangeCode==null){
                throw new BizIllegalException("兑换码不存在！");
            }
            //4.是否已经过期
            LocalDateTime now = LocalDateTime.now();
            if(now.isAfter(exchangeCode.getExpiredTime())){
                throw new BizIllegalException("兑换码已过期！");
            }
            //5.校验限领数量
            //6.更新优惠券已经发放的总数量
            Coupon coupon = couponMapper.selectById(exchangeCode.getExchangeTargetId());
            Long userId = UserContext.getUser();
           // checkAndCreateUserCoupon(userId,coupon);
            //7.新增一个用户券
            //8.更新兑换码状态（Redis 数据库双写）SETBIT KEY OFFSET VALUE--现在Redis直接在第一步就写了
            codeService.lambdaUpdate()
                    .set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
                    .set(ExchangeCode::getUserId,userId)
                    .eq(ExchangeCode::getId,serialNum)
                    .update();
        } catch (Exception e) {
            //重置兑换的标记 0
            codeService.updateExchangeMark(serialNum,false);
            throw e;//捕获了还要抛出
        }
    }
}
