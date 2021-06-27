package com.atguigu.gulimall.cart.webcontroller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @GetMapping("/")
    public String skuItem(){

        return "success";
    }
}
