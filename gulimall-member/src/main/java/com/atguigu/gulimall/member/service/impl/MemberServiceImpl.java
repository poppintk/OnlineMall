package com.atguigu.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.member.dao.MemberDao;
import com.atguigu.gulimall.member.dao.MemberLevelDao;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.entity.MemberLevelEntity;
import com.atguigu.gulimall.member.exception.PhoneExistException;
import com.atguigu.gulimall.member.exception.UsernameExistException;
import com.atguigu.gulimall.member.service.MemberService;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.MemberRegisterVo;
import com.atguigu.gulimall.member.vo.SocialUser;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void register(MemberRegisterVo vo) {
        MemberEntity entity = new MemberEntity();
        MemberDao memberDao = this.baseMapper;

        // 设置默认等级
        MemberLevelEntity levelEntity = memberLevelDao.getDefaultLevel();
        entity.setLevelId(levelEntity.getId());

        // 检查用户名和手机号是否唯一.为了让controller能感知异常，使用异常机制
        checkPhoneUnique(vo.getPhone());
        checkUsernameUnique(vo.getUserName());


        entity.setMobile(vo.getPhone());
        entity.setUsername(vo.getUserName());

        // 密码要进行加密
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(vo.getPassword());
        entity.setPassword(encode);

        // 其他的默认信息


        memberDao.insert(entity);
    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistException {
        MemberDao memberDao = this.baseMapper;
        Integer mobile = memberDao.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if (mobile > 0) {
            throw new PhoneExistException();
        }
    }

    @Override
    public void checkUsernameUnique(String userName) throws UsernameExistException {
        MemberDao memberDao = this.baseMapper;
        Integer count = memberDao.selectCount(new QueryWrapper<MemberEntity>().eq("username", userName));
        if (count > 0) {
            throw new UsernameExistException();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginAccount = vo.getLoginAccount();
        String password = vo.getPassword();
        // 1 去数据库查询 SELECT * FROM `ums_member` WHERE username=? OR mobile=?
        MemberDao memberDao = this.baseMapper;
        MemberEntity entity = memberDao.getMemberByUserName(loginAccount);
        if (entity == null) {
            // 登录失败
            return null;
        }

        String passwordDB = entity.getPassword();
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        // 密码匹配
        boolean matches = passwordEncoder.matches(password, passwordDB);
        if (matches) {
            return entity;
        }
        return null;
    }

    /**
     * 登录和注册合并逻辑
     * @param socialUser
     * @return
     */
    @Override
    public MemberEntity login(SocialUser socialUser) throws Exception {
        // 知道当前是那个社交用户
        // 1)当前用户如果是第一次进网站，自动注册进来（为当前社交用户生成一个会员信息账号，以后这个社交账号就对应指定的会员）
        // 判断这个社交用户是否 是第一次登录 用uid为唯一识别 => 注册这个社交用户
        String uid = socialUser.getUid();
        MemberDao memberDao = this.baseMapper;

        MemberEntity entity = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", socialUser.getUid()));
        if (entity != null) {
            System.out.println("User already exsit");
            MemberEntity update = new MemberEntity();
            update.setId(entity.getId());
            update.setAccessToken(socialUser.getAccess_token());
            update.setExpiresIn(String.valueOf(socialUser.getExpires_in()));

            memberDao.updateById(update);

            entity.setAccessToken(socialUser.getAccess_token());
            entity.setExpiresIn(String.valueOf(socialUser.getExpires_in()));

            return entity;
        }

        // 第一次登录 => 注册
        MemberEntity memberEntity = new MemberEntity();

        // https://www.googleapis.com/oauth2/v1/userinfo?alt=json&access_token=ya29.a0AfH6SMB-fSU6UdBOhOe_koiv3qWzO6Q4TADkImLY2inp7R75x5yx5D2xTUmF71l-CL640ZjLs3KBIPKp5sTBYgK-jdaNbrHmthbwtWLUyRJMvGGLF_ZhYH6jHj5hbYdp1vsifG0wKKfLX-LOFU2FC9lVd4zb35mylg

        Map<String, String> map = new HashMap<>();
        map.put("alt", "json");
        map.put("access_token", socialUser.getAccess_token());
        HttpResponse response = HttpUtils.doGet("https://www.googleapis.com", "/oauth2/v1/userinfo", "GET", new HashMap<>(), map);

        String json = EntityUtils.toString(response.getEntity());
        Map<String, String> data = JSON.parseObject(json, new TypeReference<HashMap<String,String>>(){});
        String email = data.get("email");
        memberEntity.setEmail(email);
        memberEntity.setUsername(socialUser.getUid());
        memberEntity.setSocialUid(uid);
        memberEntity.setAccessToken(socialUser.getAccess_token());
        memberEntity.setExpiresIn(String.valueOf(socialUser.getExpires_in()));

        this.baseMapper.insert(memberEntity);

        return memberEntity;
    }

}