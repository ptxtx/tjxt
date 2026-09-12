package com.tianji.promotion.service;

import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.validation.Valid;

/**
 * <p>
 * 优惠券的规则信息 服务类
 * </p>
 *
 * @author author
 */
public interface ICouponService extends IService<Coupon> {

    void saveCoupon(@Valid CouponFormDTO dto);
}
