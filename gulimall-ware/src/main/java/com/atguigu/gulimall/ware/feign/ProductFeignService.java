package com.atguigu.gulimall.ware.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


@FeignClient("gulimall-product")
public interface ProductFeignService {

    /**
     *  /product/skuinfo/info/{skuId}    给gulimall-product 机器发请求
     *  /api/product/skuinfo/info/{skuId}  给gulimall-gateway 所在的机器发送请求
     * @param skuId
     * @return
     */
    @RequestMapping("/product/skuinfo/info/{skuId}")
    public R info(@PathVariable("skuId") Long skuId);
}
