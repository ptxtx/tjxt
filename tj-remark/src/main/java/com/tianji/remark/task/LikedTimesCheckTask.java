package com.tianji.remark.task;

import com.tianji.remark.config.LikeTimesProperties;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LikedTimesCheckTask {

    private final LikeTimesProperties properties;
    private final ILikedRecordService likedRecordService;
    @Scheduled(fixedDelay = 20000)
    public void checkLikedTImes(){
        List<String> bizTypes = properties.getBizTypes();
        for (String bizType : bizTypes) {
            likedRecordService.readLikedTimesAndSendMessage(bizType, properties.getMaxBizSize());
        }

    }
}
