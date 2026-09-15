package com.tianji.promotion.domain.dto;

import lombok.Data;

@Data
public class UserCouponDTO {
    private Long userId;
    private Long couponId;
    private Integer serialNum;
}
