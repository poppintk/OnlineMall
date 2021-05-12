package com.atguigu.gulimall.product;

import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.service.BrandService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.atguigu.gulimall.product.entity.CommentReplayEntity;

import java.util.List;

@SpringBootTest
class GulimallProductApplicationTests {
    @Autowired
    BrandService brandService;

    @Test
    void contextLoads() {
        // 插入單個
        BrandEntity brandEntity = new BrandEntity();
        brandEntity.setDescript("");
        brandEntity.setName("hua wei ");
        brandService.save(brandEntity);

        //查詢
        //查多個, QueryWrapper 查詢條件, find all
        List<BrandEntity> list = brandService.list(new QueryWrapper<BrandEntity>().eq("name","hua wei "));

        //刪除
        list.forEach(item -> {
            System.out.println(item);
            brandService.removeById(item.getBrandId());
        });
    }

}
