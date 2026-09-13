package com.tianji.promotion.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import com.tianji.promotion.service.IUserCouponService;
import com.tianji.promotion.domain.po.UserCoupon;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 控制器
 * </p>
 *
 * @author author
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/user-coupons")
@Api("优惠券相关接口")
public class UserCouponController {

    private final IUserCouponService userCouponService;

    @PostMapping("/{couponId}/receive")
    @ApiOperation("领取优惠券接口")
    public void receiveCoupon(@PathVariable("couponId") Long couponId){
        userCouponService.receiveCoupon(couponId);
    }

    @PostMapping("/{code}/exchange")
    @ApiOperation("兑换码兑换优惠券")
    public void exchangeCoupon(@PathVariable("code") String code){
        userCouponService.exchangeCoupon(code);
    }

}
