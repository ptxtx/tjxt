package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.query.CodeQuery;
import com.tianji.promotion.domain.vo.ExchangeCodeVO;
import com.tianji.promotion.mapper.ExchangeCodeMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 兑换码 服务实现类
 * </p>
 *
 * @author 虎哥
 */
@Service
public class ExchangeCodeServiceImpl extends ServiceImpl<ExchangeCodeMapper, ExchangeCode> implements IExchangeCodeService {

    @Override
    @Async("generateExchangeCodeExecutor")
    public void asyncGenerateCode(Coupon coupon) {
        // 发放数量
        // 1.获取Redis自增序列号
            // 2.生成兑换码
        // 3.保存数据库

        // 4.写入Redis缓存，member：couponId，score：兑换码的最大序列号
    }

    @Override
    public boolean updateExchangeMark(long serialNum, boolean mark) {
        return false;
    }

    @Override
    public PageDTO<ExchangeCodeVO> queryCodePage(CodeQuery query) {
        // 1.分页查询兑换码
        // 2.返回数据
        return null;
    }

    @Override
    public Long exchangeTargetId(long serialNum) {
        // 1.查询score值比当前序列号大的第一个优惠券
        // 2.数据转换
        return null;
    }
}
