package com.atguigu.gulimall.thirdparty.component;

import lombok.Data;
import org.springframework.stereotype.Component;


/**
 * using this component for phone message like AWS SMS
 * Current logic is harded
 */
//@ConfigurationProperties(prefix = "spring.alicloud")
@Component
@Data
public class SmsComponent {

//    private String host;
//    private String path;
//    private String skin;
//    private String sign;

    public void sendSmsCode(String phone, String code) {
        // fake logic
        System.out.println("fake logic....");

    }
}
