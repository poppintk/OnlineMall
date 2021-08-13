package com.atguigu.gulimall.member;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;


/**
 * 1 想要遠程調用
 * 1）引入open-feign
 * 2)編寫一個接口， 告訴springcloud這個接口需要調用遠程接口
 *      1.申明接口的每一個方法都是調用那個遠程服務的那個請求
 * 3） 開啓遠程調用功能
 */

@EnableRedisHttpSession
@EnableFeignClients(basePackages = "com.atguigu.gulimall.member.feign")
@EnableDiscoveryClient
@MapperScan("com.atguigu.gulimall.member.dao")
@SpringBootApplication
public class GulimallMemberApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallMemberApplication.class, args);
    }

}
