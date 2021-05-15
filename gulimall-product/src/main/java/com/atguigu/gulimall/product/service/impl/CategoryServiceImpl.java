package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
//        IPage<CategoryEntity> page = this.page(
//                new Query<CategoryEntity>().getPage(params),
//                new QueryWrapper<CategoryEntity>()
//        );
//
//        return new PageUtils(page);
        return null;
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        // 查出所有分類
        List<CategoryEntity> entities = baseMapper.selectList(null);

        // 組裝父子的樹結構

        // 找到所有一級分類
        List<CategoryEntity> level1Menus = entities
            .stream()
            .filter(categoryEntity -> categoryEntity.getParentCid() == 0)
            .map(menu -> {
                menu.setChildren(getChildrens(menu, entities));
                return menu;
            })
            .sorted((menu1, menu2) -> (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort()))
            .collect(Collectors.toList());


        return level1Menus;
    }

    // recursion 找所有菜單的子菜單
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> categoryEntities = all.stream()
                .filter(categoryEntity -> categoryEntity.getParentCid() == root.getCatId())
                .map(categoryEntity -> {
                    categoryEntity.setChildren(getChildrens(categoryEntity, all));
                    return categoryEntity;
                })
                .sorted((menu1, menu2) -> (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort()))
                .collect(Collectors.toList());
        return categoryEntities;
    }

}