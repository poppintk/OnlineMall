package com.atguigu.gulimall.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.atguigu.gulimall.product.service.CommentReplayService;
import com.atguigu.gulimall.product.entity.CommentReplayEntity;

import java.util.List;

@SpringBootTest
class GulimallProductApplicationTests {
    @Autowired
    CommentReplayService commentReplayService;
    @Test
    void contextLoads() {
        List<CommentReplayEntity> list = commentReplayService.list();
        System.out.println(list);
    }

}
