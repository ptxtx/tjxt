package com.tianji.promotion.controller;

import com.tianji.promotion.domain.dto.CouponFormDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import com.tianji.promotion.service.ICouponService;
import com.tianji.promotion.domain.po.Coupon;
import lombok.RequiredArgsConstructor;

import javax.validation.Valid;

/**
 * <p>
 * 优惠券的规则信息 控制器
 * </p>
 *
 * @author author
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/coupons")
@Api(tags = "优惠券相关接口")
public class CouponController {

    private final ICouponService couponService;

    @PostMapping
    @ApiOperation("新增优惠券")
    public void saveCoupon(@RequestBody @Valid CouponFormDTO dto){
        couponService.saveCoupon(dto);
    }


}
