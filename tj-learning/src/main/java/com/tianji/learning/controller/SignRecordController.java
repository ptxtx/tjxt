package com.tianji.learning.controller;

import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.service.ISignRecordService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("sign-records")
@Slf4j
@Api(tags = "签到记录相关接口")
@RequiredArgsConstructor
public class SignRecordController {
    private final ISignRecordService signRecordService;

    @PostMapping
    @ApiOperation("新增签到记录")
    public SignResultVO addSignRecords(){
        return signRecordService.addSignRecords();
    }

    @GetMapping
    @ApiOperation("查询签到记录")
    public Byte[] querySignRecords(){
        return signRecordService.querySignRecords();
    }

}
