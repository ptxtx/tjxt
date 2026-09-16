package com.tianji.promotion.controller;

import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.query.UserCouponQuery;
import com.tianji.promotion.service.IDiscountService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import com.tianji.promotion.service.IUserCouponService;
import com.tianji.promotion.domain.po.UserCoupon;
import lombok.RequiredArgsConstructor;

import java.util.List;

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
    private final IDiscountService discountService;

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

    @PostMapping("/available")
    @ApiOperation("查询我的优惠券可用方案")
    public List<CouponDiscountDTO> findDiscountSolution(@RequestBody List<OrderCourseDTO> orderCourses){
        return discountService.findDiscountSolution(orderCourses);
    }

    @ApiOperation("分页查询我的优惠券接口")
    @GetMapping("page")
    public PageDTO<CouponVO> queryMyCouponPage(UserCouponQuery query){
        return userCouponService.queryMyCouponPage(query);
    }

}
