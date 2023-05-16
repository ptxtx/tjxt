package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.UserCouponQuery;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.IUserCouponService;
import com.tianji.promotion.utils.CodeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类
 * </p>
 *
 * @author 虎哥
 */
@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {

    @Override
    public void receiveCoupon(Long couponId) {
        // 1.查询优惠券
        // 2.校验发放时间
        // 3.校验库存
        // 4.校验每人限领数量
        // 4.1.查询领取数量
        // 4.2.校验限领数量
        // 5.扣减优惠券库存

    }

    private Coupon queryCouponByCache(Long couponId) {
        // 1.准备KEY
        // 2.查询
        // 3.数据反序列化
        return null;
    }

    @Transactional
    @Override
    public void checkAndCreateUserCoupon(UserCouponDTO uc) {
        // 1.查询优惠券
        // 2.更新优惠券的已经发放的数量 + 1
        // 3.新增一个用户券

        // 4.更新兑换码状态
    }

    @Override
    public void exchangeCoupon(String code) {
        // 1.校验并解析兑换码
        long serialNum = CodeUtil.parseCode(code);

        // 2.校验是否已经兑换 SETBIT KEY 4 1
            // 3.查询兑换码对应的优惠券id
            // 4.是否过期

            // 5.校验每人限领数量
            // 5.1.查询领取数量
            // 5.2.校验限领数量

            // 6.发送MQ消息通知
    }

    @Override
    public PageDTO<CouponVO> queryMyCouponPage(UserCouponQuery query) {
        // 1.获取当前用户
        // 2.分页查询用户券

        // 3.获取优惠券详细信息
        // 3.1.获取用户券关联的优惠券id
        // 3.2.查询

        // 4.封装VO
        return null;
    }

    @Override
    @Transactional
    public void writeOffCoupon(List<Long> userCouponIds) {
        // 1.查询优惠券
        // 2.处理数据


        // 4.核销，修改优惠券状态
        // 5.更新已使用数量
    }

    @Override
    @Transactional
    public void refundCoupon(List<Long> userCouponIds) {
        // 1.查询优惠券
        // 2.处理优惠券数据
                    // 3.判断有效期，是否已经过期，如果过期，则状态为 已过期，否则状态为 未使用

        // 4.修改优惠券状态
        // 5.更新已使用数量
    }

    @Override
    public List<String> queryDiscountRules(List<Long> userCouponIds) {
        // 1.查询优惠券信息
        // 2.转换规则
        return null;
    }

    private void saveUserCoupon(Coupon coupon, Long userId) {
        // 1.基本信息
        // 2.有效期信息
        // 3.保存
    }
}
