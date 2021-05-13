package com.atguigu.gulimall.coupon;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;


/**
 * 如何使用配置中心？
 * 1） 引入nacos config maven 依賴
 * 2） 創建一個bootstrap.protperties
 *      spring.application.name=gulimall-coupon
 *      spring.cloud.nacos.config.server-addr=127.0.0.1:8848
 * 3) 需要給配置中心默認添加一個 數據集(gulimall-coupon.properties， 應用名 + .properties)
 *  給這個xxx.properties 添加任何配置
 *  如何動態獲取配置？ 用2個注解 1） @RefreshScope 2) @Value
 *  如果配置中心和當前應用的配置都配置了相同的項，有限使用配置中心的項
 */
@EnableDiscoveryClient
@MapperScan("com.atguigu.gulimall.coupon.dao")
@SpringBootApplication
public class GulimallCouponApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallCouponApplication.class, args);
    }

}
