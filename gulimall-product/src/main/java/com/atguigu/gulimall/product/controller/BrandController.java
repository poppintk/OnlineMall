package com.atguigu.gulimall.product.controller;


import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;
import com.atguigu.common.valid.AddGroup;
import com.atguigu.common.valid.UpdateGroup;
import com.atguigu.common.valid.UpdateStatusGroup;
import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 品牌
 *
 * @author firenay
 * @email 1046762075@qq.com
 * @date 2020-05-31 17:06:04
 */
@RestController
@RequestMapping("product/brand")
public class BrandController {

	@Autowired
	private BrandService brandService;

	/**
	 * 列表
	 */
	@RequestMapping("/list")
	//@RequiresPermissions("product:brand:list")
	public R list(@RequestParam Map<String, Object> params) {
		PageUtils page = brandService.queryPage(params);
		return R.ok().put("page", page);
	}

	/**
	 * 信息
	 */
	@RequestMapping("/info/{brandId}")
	public R info(@PathVariable("brandId") Long brandId) {
		BrandEntity brand = brandService.getById(brandId);

		return R.ok().put("brand", brand);
	}

	@GetMapping("/infos")
	public R info(@RequestParam("brandIds") List<Long> brandIds) {
		List<BrandEntity> brand = brandService.getBrandByIds(brandIds);
		return R.ok().put("data", brand);
	}

	/**
	 * 保存 开启JSR303校验 规定这是新增分组 实现新增的规则
	 * POSTman：{"name":"aaa","logo":"abc","brandId":1}
	 * POSTman :{"name":"aaa","logo":"https://github.com/1046762075","sort":0,"firstLetter":"d","showStatus":0}
	 */
	@RequestMapping("/save")
	public R save(@Validated(AddGroup.class) @RequestBody BrandEntity brand) {

		brandService.save(brand);
		return R.ok();
	}

	/**
	 * 修改
	 * POSTman：{"name":"aaa","logo":"abc"}
	 */
	@RequestMapping("/update")
	public R update(@Validated(UpdateGroup.class) @RequestBody BrandEntity brand) {
		brandService.updateDetail(brand);
		return R.ok();
	}

	/**
	 * 修改状态
	 */
	@RequestMapping("/update/status")
	public R updateStatus(@Validated(UpdateStatusGroup.class) @RequestBody BrandEntity brand) {
		brandService.updateById(brand);
		return R.ok();
	}

	/**
	 * 删除
	 */
	@RequestMapping("/delete")
	//@RequiresPermissions("${moduleNamez}:brand:delete")
	public R delete(@RequestBody Long[] brandIds) {
		brandService.removeByIds(Arrays.asList(brandIds));

		return R.ok();
	}

}
