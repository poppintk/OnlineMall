package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.dao.BrandDao;
import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.service.BrandService;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;


@Service("brandService")
public class BrandServiceImpl extends ServiceImpl<BrandDao, BrandEntity> implements BrandService {

	@Resource
	private CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
//		QueryWrapper<BrandEntity> wrapper = new QueryWrapper<>();
//		String key = (String) params.get("key");
//		if(!StringUtils.isEmpty(key)){
//			wrapper.eq("brand_id", key).or().like("name", key);
//		}
//		IPage<BrandEntity> page = this.page(
//                new Query<BrandEntity>().getPage(params),
//				wrapper
//        );
        return null;
    }

	/**
	 * 当品牌进行更新的时候 保证关联表的数据也需要进行更新
	 */
	@Transactional
	@Override
	public void updateDetail(BrandEntity brand) {
		// 保证冗余字段的数据一致

	}

	@Override
	public List<BrandEntity> getBrandByIds(List<Long> brandIds) {
		return baseMapper.selectList(new QueryWrapper<BrandEntity>().in("brand_id",brandIds));
	}
}