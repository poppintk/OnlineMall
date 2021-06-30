package com.atguigu.gulimall.cart.service;

import com.atguigu.gulimall.cart.vo.CartItem;

public interface CartService {
    CartItem addToCart(Long skuId, Integer num);

    CartItem getCartItem(Long skuId);
}
