package com.atguigu.gulimall.ware.dao;

import com.atguigu.gulimall.ware.entity.PurchaseEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采购信息
 * 
 * @author ryan
 * @email ryan.youdong@gmail.com
 * @date 2021-05-29 14:03:27
 */
@Mapper
public interface PurchaseDao extends BaseMapper<PurchaseEntity> {
	
}
