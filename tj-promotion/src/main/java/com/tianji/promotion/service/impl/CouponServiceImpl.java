package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.cache.CategoryCache;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.*;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponScopeVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.enums.ObtainType;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.query.CouponQuery;
import com.tianji.promotion.service.ICouponScopeService;
import com.tianji.promotion.service.ICouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.pl.REGON;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tianji.promotion.enums.CouponStatus.*;

/**
 * <p>
 * 优惠券的规则信息 服务实现类
 * </p>
 *
 * @author author
 */
@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {

    private final StringRedisTemplate redisTemplate;
    private final CategoryCache categoryCache;
    private final ICouponScopeService scopeService;
    private final IExchangeCodeService codeService;
    private final IUserCouponService userCouponService;
    @Override
    @Transactional//有一张中间表存储信息 所以开启事务
    public void saveCoupon(CouponFormDTO dto) {
        //保存优惠券
        //1.1转po
        Coupon coupon = BeanUtils.copyBean(dto, Coupon.class);
        save(coupon);

        if(!dto.getSpecific()){
            //没有范围限定
            return;
        }
        //保存限定范围
        List<Long> scopes = dto.getScopes();
        if(CollUtils.isEmpty(scopes)){
            throw new BadRequestException("限定范围不能为空");
        }
        //转CouponScope的PO
        List<CouponScope> list = scopes.stream()
                .map(bizId -> new CouponScope().setBizId(bizId).setCouponId(coupon.getId()).setType(1))
                .collect(Collectors.toList());
        scopeService.saveBatch(list);
    }

    @Override
    public PageDTO<CouponPageVO> queryCouponByPage(CouponQuery query) {
        Integer type = query.getType();
        Integer status = query.getStatus();
        String name = query.getName();
        Page<Coupon> page = lambdaQuery()
                .eq(type != null, Coupon::getType, type)
                .eq(status != null, Coupon::getStatus, status)
                .eq(StringUtils.isNotBlank(name), Coupon::getName, name)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());

        //处理vo
        List<Coupon> records = page.getRecords();
        if(CollUtils.isEmpty(records)){
            return PageDTO.empty(page);
        }
        List<CouponPageVO> list = BeanUtils.copyList(records, CouponPageVO.class);
        //返回
        return PageDTO.of(page, list);
    }

    @Override
    public void deleteById(Long id) {
        // 1.查询
        Coupon coupon = getById(id);
        if (coupon == null || coupon.getStatus() != DRAFT) {
            throw new BadRequestException("优惠券不存在或者优惠券正在使用中");
        }
        // 2.删除优惠券
        boolean success = remove(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getId, id)
                .eq(Coupon::getStatus, DRAFT)
        );
        if (!success) {
            throw new BadRequestException("优惠券不存在或者优惠券正在使用中");
        }
        // 3.删除优惠券对应限定范围
        if(!coupon.getSpecific()){
            return;
        }
        scopeService.remove(new LambdaQueryWrapper<CouponScope>().eq(CouponScope::getCouponId, id));
    }

    @Override
    public CouponDetailVO queryCouponById(Long id) {
        // 1.查询优惠券
        Coupon coupon = getById(id);
        // 2.转换VO
        CouponDetailVO vo = BeanUtils.copyBean(coupon, CouponDetailVO.class);
        if (vo == null || !coupon.getSpecific()) {
            // 数据不存在，或者没有限定范围，直接结束
            return vo;
        }
        // 3.查询限定范围
        List<CouponScope> scopes = scopeService.lambdaQuery().eq(CouponScope::getCouponId, id).list();
        if (CollUtils.isEmpty(scopes)) {
            return vo;
        }
        List<CouponScopeVO> scopeVOS = scopes.stream()
                .map(CouponScope::getBizId)
                .map(cateId -> new CouponScopeVO(cateId, categoryCache.getNameByLv3Id(cateId)))
                .collect(Collectors.toList());
        vo.setScopes(scopeVOS);
        return vo;
    }

    @Override
    @Transactional
    public void beginIssue(CouponIssueFormDTO dto) {
        //判断优惠券状态 只有待发放和暂停中才能发放
        Coupon coupon = getById(dto.getId());
        if(coupon==null){
            throw  new BadRequestException("优惠券不存在");
        }
        if(coupon.getStatus()!=DRAFT&&coupon.getStatus()!=PAUSE){
            throw new BizIllegalException("优惠券状态不允许发放");
        }
        //判断是否是立刻发放
        LocalDateTime issueBeginTime = dto.getIssueBeginTime();
        LocalDateTime now = LocalDateTime.now();
        //如果开始发放的时间为null 或小于等于当前时间 则是立刻发放
        boolean isBegin = issueBeginTime == null || !issueBeginTime.isAfter(now);
        //更新优惠券信息
        //拷贝属性到po
        Coupon c = BeanUtils.copyBean(dto, Coupon.class);
        if(isBegin){
            c.setStatus(CouponStatus.ISSUING);
            c.setIssueBeginTime(now);
        }else{
            c.setStatus(CouponStatus.UN_ISSUE);
        }

        updateById(c);

        //添加缓存
        if(isBegin){
            //是立即发放
            coupon.setIssueBeginTime(c.getIssueBeginTime());
            coupon.setIssueEndTime(c.getIssueEndTime());
            cacheCouponInfo(coupon);
        }

        //判断是否需要生成兑换码，优惠券类型是兑换码，状态是待发放
        if(coupon.getObtainWay()== ObtainType.ISSUE&&coupon.getStatus()== DRAFT){
            coupon.setIssueEndTime(c.getIssueEndTime());
            codeService.asyncGenerateCode(coupon);

        }
    }

    private void cacheCouponInfo(Coupon coupon) {
        Map<String,String> map=new HashMap<>(4);
        map.put("issueBeginTime", String.valueOf(DateUtils.toEpochMilli(coupon.getIssueBeginTime())));
        map.put("issueEndTime", String.valueOf(DateUtils.toEpochMilli(coupon.getIssueEndTime())));
        map.put("totalNum", String.valueOf(coupon.getTotalNum()));
        map.put("userLimit", String.valueOf(coupon.getUserLimit()));
        redisTemplate.opsForHash().putAll(PromotionConstants.COUPON_CACHE_KEY_PREFIX+coupon.getId(),map);
    }

    @Override
    public List<CouponVO> queryIssuingCoupon() {
        //查询发放中优惠券列表（发放是手动发放）
        List<Coupon> coupons = lambdaQuery()
                .eq(Coupon::getStatus, ISSUING)
                .eq(Coupon::getObtainWay, ObtainType.PUBLIC)
                .list();
        if(CollUtils.isEmpty(coupons)){
            return CollUtils.emptyList();
        }
        //查询UserCoupon表 查询当前用户已经领取的优惠券的信息(是否可以领取、使用）
        //查询当前用户已经领取的优惠券信息（couponId userId）
        List<Long> couponIds = coupons.stream().map(Coupon::getId).collect(Collectors.toList());
        List<UserCoupon> userCoupons = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, UserContext.getUser())
                .in(UserCoupon::getCouponId, couponIds)
                .list();
        //统计当前用户对优惠券的已经领取的数量
        Map<Long, Long> issuedMap = userCoupons.stream().collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        //统计当前用户已经领取优惠券但没使用的数量
        Map<Long, Long> unusedMap = userCoupons.stream()
                .filter(u -> u.getStatus() == UserCouponStatus.UNUSED)
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));


        //封装VO结果
        List<CouponVO> vos =new ArrayList<>(coupons.size());
        for (Coupon c : coupons) {
            CouponVO vo = BeanUtils.copyBean(c, CouponVO.class);

            //是否可以领取：已被领取数量<优惠券数量、当前用户已经领取数量小于每人限领
            vo.setAvailable(
                    c.getIssueNum()<c.getTotalNum()
                    &&issuedMap.getOrDefault(c.getId(),0L)<c.getUserLimit()
            );

            //是否可以使用：已领并且未使用
            vo.setReceived(
                    unusedMap.getOrDefault(c.getId(), 0L) > 0
            );

            vos.add(vo);
        }
        return vos;
    }

    @Override
    @Transactional
    public void pauseIssue(Long id) {
        // 1.查询旧优惠券
        Coupon coupon = getById(id);
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在");
        }

        // 2.当前券状态必须是未开始或进行中
        CouponStatus status = coupon.getStatus();
        if (status != UN_ISSUE && status != ISSUING) {
            // 状态错误，直接结束
            return;
        }

        // 3.更新状态
        boolean success = lambdaUpdate()
                .set(Coupon::getStatus, PAUSE)
                .eq(Coupon::getId, id)
                .in(Coupon::getStatus, UN_ISSUE, ISSUING)
                .update();
        if (!success) {
            // 可能是重复更新，结束
            log.error("重复暂停优惠券");
        }

        //删除缓存
        redisTemplate.delete(PromotionConstants.COUPON_CACHE_KEY_PREFIX+id);

    }
}
