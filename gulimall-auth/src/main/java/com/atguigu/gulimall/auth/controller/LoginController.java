package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.feign.ThirdPartyFeignService;
import com.atguigu.gulimall.auth.vo.UserLoginVo;
import com.atguigu.gulimall.auth.vo.UserRegisterVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * 发送一个请求直接跳转到一个页面
 * SpringMVC viewcontroller: 将请求和页面映射
 */
@Slf4j
@Controller
public class LoginController {

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    ThirdPartyFeignService thirdPartyFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @ResponseBody
    @GetMapping("/sms/sendCode")
    public R sendCode(@RequestParam("phone") String phone) {
        //TODO 1 接口防刷

        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (!StringUtils.isEmpty(redisCode)) {
            long l = Long.parseLong(redisCode.split("_")[1]);
            if (System.currentTimeMillis() - l < 600000) {
                // 60 秒内不能再发
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
            }
        }


        // 2 验证码的再次效验。 redis 存key:phone value:code
        String code = UUID.randomUUID().toString().substring(0, 5);
        String substring = code + "_" + System.currentTimeMillis();

        // redis 缓存验证码， 使用缓存时间为code的方式，防止同一个phone在60秒再次发送验证码
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, substring,10L, TimeUnit.MINUTES);

        thirdPartyFeignService.sendCode(phone, code);


        return R.ok();
    }

    /**
     * // TODO, 重定向携带数据， 利用session 原理。 将数据放在session中。只要跳到下一个页面
     * 取出这个数据以后，session 里面的数据就会被删掉
     *
     *  // TODO, 分布式下的 session问题
     * @return
     */
    @PostMapping("/register")
    public String register(@Valid UserRegisterVo vo, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            Map<String, String> errors = result.getFieldErrors()
                    .stream()
                    .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));

            redirectAttributes.addFlashAttribute("errors", errors);
            // if there is error on the form
            // redirection will session.set(ZXXX,XXX)
            // 在分布式情况下，redirect会出问题
            return "redirect:http://auth.gulimall.com/reg.html";
        }
        // register, need to RPC call;
        // 1. 校验验证码
        String code = vo.getCode();
        String phone = vo.getPhone();
        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if(!StringUtils.isEmpty(redisCode)) {
            String s = redisCode.split("_")[0];
            if (code.equals(s)) {
                // 删除验证码, 令牌机制
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
                // 验证码通过，进行注册。调用远程服务进行注册
                R r = memberFeignService.register(vo);
                if (r.getCode() != 0) {
                    // 成功
                    Map<String, String> errors = new HashMap<>();
                    errors.put("msg", r.getData("msg", new TypeReference<String>() {}));
                    redirectAttributes.addFlashAttribute("errors", errors);
                    return "redirect:http://auth.gulimall.com/reg.html";
                }
                return "redirect:http://auth.gulimall.com/login.html";
            } else {
                Map<String, String> errors = new HashMap<>();
                errors.put("code", "验证码错误");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        } else {
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "验证码错误");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }
    }

    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes) {
        //远程登录
        R login = memberFeignService.login(vo);
        if (login.getCode() == 0){
          return "redirect:http://gulimall.com";
        }
        Map<String,String> errors = new HashMap<>();
        errors.put("msg", login.getData("msg", new TypeReference<String>() {}));
        redirectAttributes.addFlashAttribute("errors",errors);
        return "redirect:http://auth.gulimall.com/login.html";
    }
}
