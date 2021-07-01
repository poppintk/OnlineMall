package com.atguigu.gulimall.cart.controller;

import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.Cart;
import com.atguigu.gulimall.cart.vo.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CartController {

    @Autowired
    CartService cartService;


    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId) {
        cartService.deleteItem(skuId);

        return "redirect:http://cart.gulimall.com/cart.html";
    }

    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num) {
        cartService.changeItemCount(skuId, num);
        return "redirect:http://cart.gulimall.com/cart.html";
    }


    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId, @RequestParam("check") Integer check) {
        cartService.checkItem(skuId, check);
        return "redirect:http://cart.gulimall.com/cart.html";
    }

    /**
     * 浏览器有一个Cookie: user-key:标识用户身份，一个月后过期
     * 如果第一次使用购物车功能，都会给一个临时用户身份
     * 浏览器以后保存， 每次反问都会带上
     * @return
     * 登录： session 有
     * 没登录： 按照cookie里面带来user-key来做
     * 第一次： 如果没有临时用户， 帮忙创建一个临时用户
     */
    @GetMapping("/cart.html")
    public String cartListPage(Model model) {
        // 快速等到用户信息， id, user-key.用ThreadLocal-同一个线程共享数据
        // (one thread)拦截器 -> controller -> service -> dao
        Cart cart = cartService.getCart();


        model.addAttribute("cart", cart);
        return "cartList";
    }

    /**
     *  添加商品到购物车
     *
     *  RedirectAttributes ra
     *  ra.addFlashAttribute("skuId", skuId); // 将数据放在session里面可以在同页面取出，但是只能被取出一次
     *  ra.addAttribute("skuId", skuId); // 自动将数据存在url后面
     *
     * @param skuId
     * @param num
     * @param ra
     * @return
     */
    @GetMapping("/addCartItem")
    public String addToCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num, RedirectAttributes ra) {
        cartService.addToCart(skuId, num);
        ra.addAttribute("skuId", skuId);
        return "redirect:http://cart.gulimall.com/addToCartSuccess.html";
    }

    /**
     * 跳转到成功页
     * @param skuId
     * @param model
     * @return
     */
    @GetMapping("/addToCartSuccess.html")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId, Model model) {
        // 重定向到成功页面。 再次查询购物车数据即可
        CartItem item = cartService.getCartItem(skuId);
        model.addAttribute("cartItem", item);

        return "success";
    }
}
