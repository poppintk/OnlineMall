package com.atguigu.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

//2级分类vo
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Catelog2Vo {
    private String catelogId; //一级父分类
    private List<Catelog3Vo> catelog3List; //三级子分类
    private String id;
    private String name;

    //三级分类vo
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class Catelog3Vo {
        private String catalog2Id; // 2级分类id
        private String id;
        private String name;
    }
}
