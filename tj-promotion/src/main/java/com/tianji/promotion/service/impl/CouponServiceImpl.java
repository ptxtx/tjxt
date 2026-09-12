package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.cache.CategoryCache;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponScopeVO;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.enums.ObtainType;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.query.CouponQuery;
import com.tianji.promotion.service.ICouponScopeService;
import com.tianji.promotion.service.ICouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.service.IExchangeCodeService;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.pl.REGON;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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

    private final CategoryCache categoryCache;
    private final ICouponScopeService scopeService;
    private final IExchangeCodeService codeService;
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

        //判断是否需要生成兑换码，优惠券类型是兑换码，状态是待发放
        if(coupon.getObtainWay()== ObtainType.ISSUE&&coupon.getStatus()== DRAFT){
            coupon.setIssueEndTime(c.getIssueEndTime());
            codeService.asyncGenerateCode(coupon);
        }
    }
}
