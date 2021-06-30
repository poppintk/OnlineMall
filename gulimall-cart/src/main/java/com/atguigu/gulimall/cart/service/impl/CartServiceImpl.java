package com.atguigu.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.cart.feign.ProductFeignService;
import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.CartItem;
import com.atguigu.gulimall.cart.vo.SkuInfoVo;
import com.atguigu.gulimall.cart.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
public class CartServiceImpl implements CartService {
    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    private final String CART_PREFIX = "gulimall:cart:";

    @Override
    public CartItem addToCart(Long skuId, Integer num) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        String res = (String) cartOps.get(skuId.toString());
        if (!StringUtils.isEmpty(res)) {
            CartItem cartItem = new CartItem();
            // 购物车有此商品，修改数量
            cartItem = JSON.parseObject(res, CartItem.class);
            cartItem.setCount(cartItem.getCount() + num);
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));

            return cartItem;
        }
        CartItem cartItem = new CartItem();
        // 1远程查询当前药添加的商品的信息
        R info = productFeignService.getSkuInfo(skuId);
        if (info.getCode() == 0) {
            SkuInfoVo data = info.getData("skuInfo", new TypeReference<SkuInfoVo>() {});
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                cartItem.setCheck(true);
                cartItem.setCount(1);
                cartItem.setImage(data.getSkuDefaultImg());
                cartItem.setTitle(data.getSkuTitle());
                cartItem.setSkuId(skuId);
                cartItem.setPrice(data.getPrice());
            }, executor);

            CompletableFuture<Void> getSkuSaleAttrValues = CompletableFuture.runAsync(() -> {
                List<String> values = productFeignService.getSkuSaleAttrValues(skuId);
                cartItem.setSkuAttr(values);
            }, executor);

            CompletableFuture<Void> allOf = CompletableFuture.allOf(getSkuInfoTask, getSkuSaleAttrValues);
            try {
                allOf.get();
            } catch (InterruptedException | ExecutionException e) {
                log.info("Error : {}", e);
            }

            String s = JSON.toJSONString(cartItem);
            cartOps.put(skuId.toString(), s);
        }
        return cartItem;
    }

    @Override
    public CartItem getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        String s = (String) cartOps.get(skuId.toString());
        CartItem cartItem = JSON.parseObject(s, CartItem.class);
        return cartItem;
    }

    /**
     * 获取到我们药操作的购物车
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey = "";
        if (userInfoTo.getUserId() != null) { // 用户登录了
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        } else { // 用户没有登录
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }

        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);
        return operations;
    }
}
