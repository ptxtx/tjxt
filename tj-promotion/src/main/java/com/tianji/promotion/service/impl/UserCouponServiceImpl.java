package com.tianji.promotion.service.impl;

import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.UserContext;
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
import lombok.RequiredArgsConstructor;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
    @Override
    public void receiveCoupon(Long couponId) {
        //查询优惠券
        Coupon coupon = couponMapper.selectById(couponId);
        if(coupon==null){
            throw new BadRequestException("优惠券不存在！");
        }
        //校验发放时间
        LocalDateTime now = LocalDateTime.now();
        if(now.isBefore(coupon.getIssueBeginTime())||now.isAfter(coupon.getIssueEndTime())){
            throw new BadRequestException("优惠券发放已结束或者尚未开始！");
        }
        //校验库存
        if(coupon.getIssueNum()>=coupon.getTotalNum()){
            throw new BadRequestException("优惠券库存不足！");
        }
        //校验每人限领数量
        Long userId = UserContext.getUser();
        synchronized (userId.toString().intern()) {
            IUserCouponService userCouponService = (IUserCouponService) AopContext.currentProxy();
            userCouponService.checkAndCreateUserCoupon(userId, coupon);//校验并生成优惠券
        }
    }
    @Transactional
    @Override
    public void checkAndCreateUserCoupon(Long userId, Coupon coupon) {
            Integer count = lambdaQuery()
                    .eq(UserCoupon::getCouponId, coupon.getId())
                    .eq(UserCoupon::getUserId, userId)
                    .count();
            if (count != null && count >= coupon.getUserLimit()) {
                throw new BadRequestException("领取已达上限！");
            }
            //更新优惠券已经发放数量+1
            int r = couponMapper.incrIssueNum(coupon.getId());//更新成功的行的数量
            if (r == 0) {
                throw new BizIllegalException("优惠券库存不足！");
            }
            //新增user-coupon
            saveUserCoupon(userId, coupon);

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
            checkAndCreateUserCoupon(userId,coupon);
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
