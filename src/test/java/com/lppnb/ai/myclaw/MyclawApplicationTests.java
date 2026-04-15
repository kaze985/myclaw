package com.lppnb.ai.myclaw;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.lppnb.ai.myclaw.agent.app.MyClaw;
import com.lppnb.ai.myclaw.tool.search.WallpaperSearchTool;

@SpringBootTest
class MyclawApplicationTests {

    @Autowired
    private MyClaw myClaw;

//    @Test
//    void chatWithMyClaw() {
//        String result = myClaw.run("""
//               你好
//               """);
//        Assertions.assertNotNull(result);
//    }

    @Test
    void testWallpaperSearchTool() {
        String result = new WallpaperSearchTool().wallpaperSearch("明日方舟", 0);
        System.out.println(result);
        Assertions.assertNotNull(result);
    }

}
