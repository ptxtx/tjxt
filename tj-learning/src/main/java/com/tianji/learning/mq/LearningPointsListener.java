package com.tianji.learning.mq;

import com.tianji.learning.service.IPointsRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LearningPointsListener {

    private final IPointsRecordService recordService;


}
