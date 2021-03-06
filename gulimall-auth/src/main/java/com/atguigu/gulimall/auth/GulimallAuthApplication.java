package com.atguigu.gulimall.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

// 整合redis作为session存储
@EnableRedisHttpSession
@EnableFeignClients(basePackages = "com.atguigu.gulimall.auth.feign")
@EnableDiscoveryClient
@SpringBootApplication
@EnableSwagger2
public class GulimallAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallAuthApplication.class, args);
    }

}
