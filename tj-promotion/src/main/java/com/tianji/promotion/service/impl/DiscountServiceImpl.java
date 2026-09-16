package com.tianji.promotion.service.impl;

import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.ICouponScopeService;
import com.tianji.promotion.service.IDiscountService;
import com.tianji.promotion.strategy.discount.Discount;
import com.tianji.promotion.strategy.discount.DiscountStrategy;
import com.tianji.promotion.utils.PermuteUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountServiceImpl implements IDiscountService {
    private final UserCouponMapper userCouponMapper;
    private final ICouponScopeService scopeService;
    private final Executor discountSolutionExecutor;
    @Override
    public List<CouponDiscountDTO> findDiscountSolution(List<OrderCourseDTO> orderCourses) {
        //查询我的所有可用优惠券
        List<Coupon> coupons= userCouponMapper.queryMyCoupon(UserContext.getUser());
        if(CollUtils.isEmpty(coupons)){
            return CollUtils.emptyList();
        }
        //初筛
        //2.1计算订单总价
        int totalAmount = orderCourses.stream().mapToInt(OrderCourseDTO::getPrice).sum();
        //2.2筛选出可用券
        List<Coupon> availableCoupons = coupons.stream().filter(c -> DiscountStrategy.getDiscount(c.getDiscountType()).canUse(totalAmount, c))
                .collect(Collectors.toList());
        if(CollUtils.isEmpty(availableCoupons)){
            return CollUtils.emptyList();
        }
        //排列组合出所有方案
        //3,1细筛（找出每一个优惠券可用的课程，判断课程总结是否达到优惠券的使用需求）
        Map<Coupon,List<OrderCourseDTO>> availableCouponMap=findAvailableCoupon(availableCoupons, orderCourses);
        if(CollUtils.isEmpty(availableCouponMap)){
            return CollUtils.emptyList();
        }
        //3.2 排列组合
        availableCoupons=new ArrayList<>(availableCouponMap.keySet());
        List<List<Coupon>> solutions = PermuteUtil.permute(availableCoupons);
        //3.3添加单券的方案
        for (Coupon c : availableCoupons) {
            solutions.add(List.of(c));
        }
        //计算方案的优惠明细
        List<CouponDiscountDTO> list=Collections.synchronizedList(new ArrayList<>(solutions.size()));
        //4.1 定义闭锁
        CountDownLatch latch = new CountDownLatch(solutions.size());
        for (List<Coupon> solution : solutions) {
            //4.2 异步计算
            CompletableFuture.supplyAsync(() -> calculateSolutionDiscount(availableCouponMap,solution,orderCourses), discountSolutionExecutor)
                    .thenAccept(dto->{
                        //4.3 添加结果
                        list.add(dto);
                        //4.4 闭锁减一
                        latch.countDown();
                    });
            //list.add(calculateSolutionDiscount(availableCouponMap,solution,orderCourses));
        }
        //4.5 等待所有任务完成
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("优惠方案计算中断,{}", e.getMessage());
        }

        //筛选最优解
        return findBestSolution(list);
    }

    private List<CouponDiscountDTO> findBestSolution(List<CouponDiscountDTO> list) {
        //准备map记录最优解
        Map<String,CouponDiscountDTO> moreDiscountMap=new HashMap<>();
        Map<Integer,CouponDiscountDTO> lessCouponMap=new HashMap<>();
        //遍历 筛选最优解
        for (CouponDiscountDTO solution : list) {
            //2.1 先去计算当前方案的id组合
            String ids = solution.getIds().stream().sorted(Long::compare).map(String::valueOf).collect(Collectors.joining(","));
            //2.2 比较用券想同时，优惠金额是否最大
            CouponDiscountDTO best = moreDiscountMap.get(ids);
            if(best!=null&&best.getDiscountAmount()>solution.getDiscountAmount()){
                //当前方案优惠少
                continue;
            }

            //2.3 比较金额相同时，用券是否最少
            best=lessCouponMap.get(solution.getDiscountAmount());
            if(solution.getIds().size()>1&& best!=null&&best.getIds().size()<solution.getIds().size()){
                //当前方案用券多
                continue;
            }
            moreDiscountMap.put(ids, solution);
            lessCouponMap.put(solution.getDiscountAmount(), solution);
        }
        //求交集
        Collection<CouponDiscountDTO> bestSolutions = CollUtils.intersection(moreDiscountMap.values(), lessCouponMap.values());
        //排序 按照优惠金额降序
        return bestSolutions.stream().sorted(Comparator.comparingInt(CouponDiscountDTO::getDiscountAmount).reversed()).collect(Collectors.toList());

    }

    private CouponDiscountDTO calculateSolutionDiscount(Map<Coupon, List<OrderCourseDTO>> couponMap, List<Coupon> solution, List<OrderCourseDTO> courses) {
        CouponDiscountDTO dto = new CouponDiscountDTO();
        //初始化折扣明细的映射
        Map<Long, Integer> detailMap = courses.stream().collect(Collectors.toMap(OrderCourseDTO::getId, oc -> 0));
        //计算折扣
        for (Coupon coupon : solution) {
            //1 获取优惠券限定范围的课程
            List<OrderCourseDTO> availableCourses = couponMap.get(coupon);
            //2。 计算课程总价(课程原价-折扣明细）
            int totalAmount = availableCourses.stream().mapToInt(oc -> oc.getPrice() - detailMap.get(oc.getId())).sum();
            //3.判断是否可用
            Discount discount = DiscountStrategy.getDiscount(coupon.getDiscountType());
            if(!discount.canUse(totalAmount, coupon)){
                continue;
            }
            //4.计算优惠金额
            int discountAmount = discount.calculateDiscount(totalAmount, coupon);
            //计算优惠明细
            calculateDiscountDetails(detailMap,availableCourses,totalAmount,discountAmount);
            //更新dto数据
            dto.getIds().add(coupon.getId());
            dto.getRules().add(discount.getRule(coupon));
            dto.setDiscountAmount(dto.getDiscountAmount() + discountAmount);//累加
            return dto;
        }

        return null;
    }

    private void calculateDiscountDetails(Map<Long, Integer> detailMap, List<OrderCourseDTO> courses, int totalAmount, int discountAmount) {
        int times=0;
        int remainDiscount=discountAmount;
        int discount=0;
        for (OrderCourseDTO course : courses) {
            times++;
            //计算优惠明细(课程价格在总价中所占比例，乘折扣）
            if(times==courses.size()-1){
                discount=remainDiscount;
            }else{
                discount = course.getPrice() * discountAmount / totalAmount;
                remainDiscount-=discount;
            }

            detailMap.put(course.getId(), discount+detailMap.get(course.getId()));

        }
    }

    private Map<Coupon, List<OrderCourseDTO>> findAvailableCoupon(List<Coupon> coupons, List<OrderCourseDTO> courses) {
        Map<Coupon, List<OrderCourseDTO>> map=new HashMap<>();
        for (Coupon coupon : coupons){
            //1.找出优惠券可用的课程
            List<OrderCourseDTO> availableCourses=courses;
            if(coupon.getSpecific()){
                //限定了范围
                List<CouponScope> scopes = scopeService.lambdaQuery()
                        .eq(CouponScope::getCouponId, coupon.getId())
                        .list();
                Set<Long> scopeIds = scopes.stream().map(CouponScope::getBizId).collect(Collectors.toSet());
                //筛选课程
                availableCourses= courses.stream().filter(c -> scopeIds.contains(c.getId())).collect(Collectors.toList());
            }
            if(CollUtils.isEmpty(availableCourses)){
                continue;
            }
            //2.计算课程总价
            int totalAmount=availableCourses.stream().mapToInt(OrderCourseDTO::getPrice).sum();
            //3.判断是否可用
            Discount discount = DiscountStrategy.getDiscount(coupon.getDiscountType());
            if(discount.canUse(totalAmount, coupon)){
                map.put(coupon,availableCourses);
            }
        }
        return map;
    }
}
