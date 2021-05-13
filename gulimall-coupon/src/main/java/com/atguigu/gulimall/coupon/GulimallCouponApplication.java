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
 *
 *
 *  細節：
 *  1）命名空間
 *      默認：public(保留空間)： 默認新增的所有配置都子啊public空間
 *      1. 開發，測試， 生產 :利用命名空間來做環境隔離
 *        注意： 在bootstrap.properties: 配置上，需要使用哪個命名空間的配置
 *        spring.cloud.nacos.config.namespace=627bb290-7a78-49c2-845e-8fcd4edf0756
 *      2. 每一個微服務之間互相隔離配置， 每一個微服務都創建自己的命名空間，之加載自己的命名空間
 *  2） 配置集： 所有配置的集合
 *
 *  3）配置集ID： 配置文件名字
 *      Data ID :配置文件名
 *
 *  4) 配置分組：
 *      默認所有的配置集都屬於DEFAULT_GROUP：
 *      1111. 618, 1212
 *      spring.cloud.nacos.config.group=1111
 *
 * 每個微服務創建自己的命名空間， 使用配置分組來區分環境，dev, test, prod
 *
 * 同時加載多個命名空間
 * 1）微服務任何配置信息，任何配置文件都可以放在配置中心
 * 2）只需要在bootstrap.properties  中説明加載配置中心哪些配置文件即可
 *  spring.cloud.nacos.config.ext-config[0].adta-id=mybatis.yml
 *  spring.cloud.nacos.config.ext-config[0].group=dev
 *  spring.cloud.nacos.config.ext-config[0].refresh=true
 *
 */
@EnableDiscoveryClient
@MapperScan("com.atguigu.gulimall.coupon.dao")
@SpringBootApplication
public class GulimallCouponApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallCouponApplication.class, args);
    }

}
