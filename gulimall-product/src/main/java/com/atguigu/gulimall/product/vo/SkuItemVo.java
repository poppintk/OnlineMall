package com.atguigu.gulimall.product.vo;

import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;
import lombok.ToString;

import java.util.List;
@ToString
@Data
public class SkuItemVo {

    //1 sku 基本信息获取 pms_sku_info
    SkuInfoEntity info;

    // 2 sku的图片信息 pms_sku_images
    List<SkuImagesEntity> images;

    // 3 获取的是spu的销售属性组合
    List<SkuItemSaleAttrVo> saleAttr;

    // 4 获取spu的介绍
    SpuInfoDescEntity desp;

    // 5 获取spu的规格参数信息
    List<SpuItemAttrGroupVo> groupAttrs;




}
