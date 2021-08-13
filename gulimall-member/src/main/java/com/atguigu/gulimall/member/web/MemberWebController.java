package com.atguigu.gulimall.member.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MemberWebController {

    @GetMapping("/memberOrder.html")
    public String memberOrderPage() {
        // get all current user's order list data

        return "orderList";
    }
}
