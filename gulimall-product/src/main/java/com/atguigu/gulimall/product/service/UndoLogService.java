package com.atguigu.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.UndoLogEntity;

import java.util.Map;

/**
 * 
 *
 * @author ryan
 * @email ryan.youdong@gmail.com
 * @date 2021-05-11 19:13:37
 */
public interface UndoLogService extends IService<UndoLogEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

