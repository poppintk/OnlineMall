package com.atguigu.gulimall.auth.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.vo.SocialUser;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.atguigu.common.constant.AuthServerConstant.LOGIN_USER;

/**
 * 处理社交登录请求
 */
@Slf4j
@Controller
public class OAuth2Controller {

    @Autowired
    MemberFeignService memberFeignService;

    private final String GITHUB_CLIENT_SECRET = "3dfbe132f8b29be436abb995050f8da4f137f80d ";
    private final String GITHUB_CLIENT_ID= "df033e63739bd84aee6d";

    private final String GOOGLE_CLIENT_ID = "315609488815-eu15nq073oi4c670c3br73np2kdmb18b.apps.googleusercontent.com";
    private final String GOOGLE_CLIENT_SECRET = "HMt991-dJsTsGegUmDUP9yhy";

    @GetMapping("/oauth2.0/google/success")
    public String googleLogin(@RequestParam("code") String code, HttpSession session, HttpServletResponse servletResponse) throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("client_id", GOOGLE_CLIENT_ID);
        map.put("client_secret", GOOGLE_CLIENT_SECRET);
        map.put("grant_type", "authorization_code");
        map.put("code", code);
        map.put("redirect_uri", "http://auth.gulimall.com/oauth2.0/google/success");
        HttpResponse response = HttpUtils.doPost("https://oauth2.googleapis.com", "/token", "POST", null, null, map);

        if (response.getStatusLine().getStatusCode() == 200) {
            SocialUser userInfo = getUserInfo(response);
            R r = memberFeignService.oauthlogin(userInfo);
            if (r.getCode() == 0) {
                MemberRespVo data = r.getData("data", new TypeReference<MemberRespVo>() {});
                log.info("User login as : {}", data);
                // 第一次使用session: 命令浏览器保存卡号。 JSESSIONID这个cookie:
                // 以后浏览器范文哪个网站都会带上这个网站的cookie
                // 子域名之间： gulimall.com, auth.gullimall.com order.gulimall.com
                // 发卡的时候即使是子域系统发的卡， 也让父域直接使用
                // TODO 1 默认法的令牌， session=xxxx， 作用域：当前域名， （解决子域名共享问题）
                // TODO 2 使用json的序列化方式来序列化对象数据到redis中

                session.setAttribute(LOGIN_USER, data);
//                // 解决不同域名的sessin问题
//                new Cookie("JSESSIONID", "dadaa").setDomain("");
//                servletResponse.addCookie();


                return "redirect:http://gulimall.com";
            }
        }
        // 登录失败
        return "redirect:http://auth.gulimall.com/login.html";
    }


    @GetMapping("/oauth2.0/github/success")
    public String github(@RequestParam("code") String code) throws Exception {
        // 1 根据code换取accessToken
        HashMap<String, String> map = new HashMap<>();
        map.put("client_id", GITHUB_CLIENT_ID);
        map.put("client_secret", GITHUB_CLIENT_SECRET);
        map.put("code", code);
        HttpResponse response = HttpUtils.doPost("github.com", "/login/oauth/access_token", "post", null, null, map);

        // 2.处理 得到access token
        if (response.getStatusLine().getStatusCode() == 200) {
            String json = EntityUtils.toString(response.getEntity());
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);

            // 知道当前是那个社交用户
            // 1)当前用户如果是第一次进网站，自动注册进来（为当前社交用户生成一个会员信息账号，以后这个社交账号就对应指定的会员）
            // 判断这个社交用户是否 是第一次登录 用uid为唯一识别 => 注册这个社交用户



        } else {
            // 登录失败
            return "redirect:http://auth.gulimall.com/login.html";
        }


        // 2 登录成功就跳回首页
        return "redirect:http://gulimall.com";
    }


    private SocialUser getUserInfo(HttpResponse response) throws IOException, GeneralSecurityException {
        SocialUser socialUser = new SocialUser();
        String json = EntityUtils.toString(response.getEntity());
        Map<String, String> data = JSON.parseObject(json, new TypeReference<HashMap<String,String>>(){});
        String idTokenString = data.get("id_token");

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance())
                // Specify the CLIENT_ID of the app that accesses the backend:
                .setAudience(Collections.singletonList(GOOGLE_CLIENT_ID))
                // Or, if multiple clients access the backend:
                //.setAudience(Arrays.asList(CLIENT_ID_1, CLIENT_ID_2, CLIENT_ID_3))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken != null) {
            Payload payload = idToken.getPayload();
            // Print user identifier
            String userId = payload.getSubject();
            socialUser.setUid(userId);
            socialUser.setExpires_in(Long.parseLong(data.get("expires_in")));
            socialUser.setAccess_token(data.get("access_token"));
            socialUser.setIsRealName(payload.getEmail());
            return socialUser;

        }
        return null;
    }
}
