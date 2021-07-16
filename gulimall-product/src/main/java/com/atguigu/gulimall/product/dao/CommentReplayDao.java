package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.CommentReplayEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品评价回复关系
 *
 * @author ryan
 * @email ryan.youdong@gmail.com
 * @date 2021-05-12 00:48:47
 */
@Mapper
public interface CommentReplayDao extends BaseMapper<CommentReplayEntity> {

}
