package com.atguigu.gulimall.member.dao;

import com.atguigu.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 会员
 * 
 * @author ryan
 * @email ryan.youdong@gmail.com
 * @date 2021-05-12 10:11:10
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {

    MemberEntity getMemberByUserName(@Param("loginAccount") String loginAccount);
}
