package com.atguigu.gulimall.order.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

//订单确认页需要的信息
public class OrderConfirmVo {
    // address info
    @Setter @Getter
    private List<MemberAddressVo> memberAddressVos;

    //product info
    @Setter @Getter
    private List<OrderItemVo> items;

    @Getter @Setter
    Map<Long,Boolean> stocks;

    // receipt info

    //coupon info
    @Setter @Getter
    private Integer integration;


    //防重令牌-》 幂等性
    String orderToken;

    // order total count
    public Integer getCount() {
        Integer i = 0;
        if (items != null) {
            for (OrderItemVo item : items) {
                i += item.getCount();
            }
        }
        return i;
    }

    // order total
    public BigDecimal getTotal() {
        BigDecimal sum = new BigDecimal("0");
        if (items != null) {
            for (OrderItemVo item : items) {
                BigDecimal multiply = item.getPrice().multiply(new BigDecimal(item.getCount().toString()));
                sum = sum.add(multiply);
            }
        }
        return sum;
    }

    // final pay price
    public BigDecimal getPayPrice() {
        return getTotal();
    }


}
