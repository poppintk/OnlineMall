package com.atguigu.gulimall.order.vo;

import lombok.Data;
import java.util.List;
import java.math.BigDecimal;

@Data
public class OrderItemVo {
    private Long skuId;

    private String title;

    private String image;

    private List<String> skuAttr;

    private BigDecimal price;

    private Integer count;

    private BigDecimal totalPrice;
}
