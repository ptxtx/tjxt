package com.tianji.promotion.service.impl;

import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCouponDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.service.IDiscountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountServiceImpl implements IDiscountService {
    @Override
    public List<CouponDiscountDTO> findDiscountSolution(List<OrderCourseDTO> orderCourses) {
        // 1.查询我的所有可用优惠券
        // 2.初筛
        // 2.1.计算订单总价
        // 2.2.筛选可用券
        // 3.排列组合出所有方案
        // 3.1.细筛（找出每一个优惠券的可用的课程，判断课程总价是否达到优惠券的使用需求）
        // 3.2.排列组合
        // 3.3.添加单券的方案
        // 4.计算方案的优惠明细
        // 4.1.定义闭锁
            // 4.2.异步计算
                // 4.3.提交任务结果
        // 4.4.等待运算结束
        // 5.筛选最优解
        return null;
    }

    @Override
    public CouponDiscountDTO queryDiscountDetailByOrder(OrderCouponDTO orderCouponDTO) {
        // 1.查询用户优惠券
        // 2.查询优惠券对应课程
        // 3.查询优惠券规则
        return null;
    }

    private List<CouponDiscountDTO> findBestSolution(List<CouponDiscountDTO> list) {
        // 1.准备Map记录最优解
        // 2.遍历，筛选最优解
            // 2.1.计算当前方案的id组合
            // 2.2.比较用券相同时，优惠金额是否最大
            // 2.3.比较金额相同时，用券数量是否最少
            // 2.4.更新最优解
        // 3.求交集
        // 4.排序，按优惠金额降序
        return null;
    }

    private CouponDiscountDTO calculateSolutionDiscount(
            Map<Coupon, List<OrderCourseDTO>> couponMap, List<OrderCourseDTO> courses, List<Coupon> solution) {
        // 1.初始化DTO
        // 2.初始化折扣明细的映射
        // 3.计算折扣
            // 3.1.获取优惠券限定范围对应的课程
            // 3.2.计算课程总价(课程原价 - 折扣明细)
            // 3.3.判断是否可用
                // 券不可用，跳过
            // 3.4.计算优惠金额
            // 3.5.计算优惠明细
            // 3.6.更新DTO数据
        return null;
    }

    private void calculateDiscountDetails(Map<Long, Integer> detailMap, List<OrderCourseDTO> courses,
                                          int totalAmount, int discountAmount) {
            // 更新课程已计算数量
            // 判断是否是最后一个课程
                // 是最后一个课程，总折扣金额 - 之前所有商品的折扣金额之和
                // 计算折扣明细（课程价格在总价中占的比例，乘以总的折扣）
            // 更新折扣明细
    }

    private Map<Coupon, List<OrderCourseDTO>> findAvailableCoupon(
            List<Coupon> coupons, List<OrderCourseDTO> courses) {
            // 1.找出优惠券的可用的课程
                // 1.1.限定了范围，查询券的可用范围
                // 1.2.获取范围对应的分类id
                // 1.3.筛选课程
                // 没有任何可用课程，抛弃
            // 2.计算课程总价
            // 3.判断是否可用
        return null;
    }
}
