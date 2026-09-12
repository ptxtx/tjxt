package com.tianji.promotion.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeUtilTest {

    @Test
    void testCodeUtil(){
        for (int i = 5; i < 15; i++) {
            String code = CodeUtil.generateCode(i,1000);
            System.out.println("code="+code);

            long num = CodeUtil.parseCode(code);
            System.out.println("num="+num);
        }

    }
}