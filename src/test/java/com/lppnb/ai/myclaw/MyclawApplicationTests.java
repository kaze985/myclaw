package com.lppnb.ai.myclaw;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.lppnb.ai.myclaw.agent.app.MyClaw;

@SpringBootTest
class MyclawApplicationTests {

    @Autowired
    private MyClaw myClaw;

    @Test
    void chatWithMyClaw() {
        String result = myClaw.run("""
               搜索魔女之旅伊蕾娜的图片并下载
               """);
        Assertions.assertNotNull(result);
    }

}
