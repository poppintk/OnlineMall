package com.atguigu.gulimall.thirdparty;

import com.aliyun.oss.OSS;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@SpringBootTest
class GulimallThirdPartyApplicationTests {

    @Autowired
    OSS ossClient;

    @Test
    void contextLoads() throws FileNotFoundException {


        InputStream inputStream = new FileInputStream("C:\\Users\\ryou\\Pictures\\08.png");
        ossClient.putObject("gulimall-youdong", "api.jpg", inputStream);
        ossClient.shutdown();
        System.out.println("上传完成。。。");
    }

}
