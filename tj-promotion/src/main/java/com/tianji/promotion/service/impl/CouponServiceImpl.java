package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.service.ICouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 优惠券的规则信息 服务实现类
 * </p>
 *
 * @author 虎哥
 */
@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {


    @Override
    @Transactional
    public void saveCoupon(CouponFormDTO dto) {
        // 1.保存优惠券
        // 1.1.转PO
        // 1.2.保存

        // 2.保存限定范围
        // 2.1.转换PO
        // 2.2.保存
    }

    @Override
    public PageDTO<CouponPageVO> queryCouponByPage(CouponQuery query) {
        // 1.分页查询
        // 2.处理VO
        // 3.返回
        return null;
    }

    @Transactional
    @Override
    public void beginIssue(CouponIssueFormDTO dto) {
        // 1.查询优惠券
        // 2.判断优惠券状态，是否是暂停或待发放
        // 3.判断是否是立刻发放
        // 4.更新优惠券
        // 4.1.拷贝属性到PO
        // 4.2.更新状态
        // 4.3.写入数据库

        // 5.添加缓存

        // 6.判断是否需要生成兑换码，优惠券类型必须是兑换码，优惠券状态必须是待发放
    }

    private void cacheCouponInfo(Coupon coupon) {
        // 1.组织数据
        // 2.写缓存
    }

    @Override
    public List<CouponVO> queryIssuingCoupons() {
        // 1.查询发放中的优惠券列表
        // 2.统计当前用户已经领取的优惠券的信息
        // 2.1.查询当前用户已经领取的优惠券的数据
        // 2.2.统计当前用户对优惠券的已经领取数量
        // 2.3.统计当前用户对优惠券的已经领取并且未使用的数量
        // 3.封装VO结果
            // 3.1.拷贝PO属性到VO
            // 3.2.是否可以领取：已经被领取的数量 < 优惠券总数量 && 当前用户已经领取的数量 < 每人限领数量
            // 3.3.是否可以使用：当前用户已经领取并且未使用的优惠券数量 > 0
        return null;
    }

    @Override
    @Transactional
    public void pauseIssue(Long id) {
        // 1.查询旧优惠券

        // 2.当前券状态必须是未开始或进行中

        // 3.更新状态

        // 4.删除缓存
    }

    @Override
    public void deleteById(Long id) {
        // 1.查询
        // 2.删除优惠券
        // 3.删除优惠券对应限定范围
    }

    @Override
    public CouponDetailVO queryCouponById(Long id) {
        // 1.查询优惠券
        // 2.转换VO
        // 3.查询限定范围
        return null;
    }

    @Override
    public void beginIssueBatch(List<Coupon> coupons) {
        // 1.更新券状态
        updateBatchById(coupons);
        // 2.批量缓存
                // 2.1.组织数据
                // 2.2.写缓存
    }
}
