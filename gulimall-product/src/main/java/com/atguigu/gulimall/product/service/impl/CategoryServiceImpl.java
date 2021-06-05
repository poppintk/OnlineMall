package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Resource
    private CategoryBrandRelationService categoryBrandRelationService;


    @Autowired
    StringRedisTemplate stringRedisTemplate;

    private final String CATALOG_JSON = "catalogJSON";


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        // 1. 查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        // 2 组装成父子的树形结构


        // 找到所有一级分类
        List<CategoryEntity> level1Menus = entities
                .stream()
                .filter((categoryEntity -> categoryEntity.getParentCid() == 0))
                .map(menu -> {
                    menu.setChildren(getChildrens(menu, entities));
                    return menu;
                })
                .sorted((menu1, menu2) -> {
                    return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
                })
                .collect(Collectors.toList());


        return level1Menus;
    }

    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> children = all
                .stream()
                .filter(categoryEntity -> categoryEntity.getParentCid() == root.getCatId())
                .map(categoryEntity -> {
                    categoryEntity.setChildren(getChildrens(categoryEntity, all));
                    return categoryEntity;
                })
                .sorted((menu1, menu2) -> {
                    return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
                })
                .collect(Collectors.toList());
        return children;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        // TODO 检查当前节点是否被别的地方引用
        baseMapper.deleteBatchIds(asList);
    }



    /**
     * @Cacheable: 当前方法的结果需要缓存 并指定缓存名字
     *  缓存的value值 默认使用jdk序列化
     *  默认ttl时间 -1
     *	key: 里面默认会解析表达式 字符串用 ''
     *
     *  自定义:
     *  	1.指定生成缓存使用的key
     *  	2.指定缓存数据存活时间	[配置文件中修改]
     *  	3.将数据保存为json格式
     *
     *  sync = true: --- 开启同步锁
     *
     */
    @Cacheable(value = {"category"}, key = "#root.method.name", sync = true)
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("cat_level", 1));
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> parentPaths = new ArrayList<>();

        this.findParentPath(catelogId, parentPaths);

        Collections.reverse(parentPaths);
        return (Long[]) parentPaths.toArray(new Long[parentPaths.size()]);
    }

    /**
     * 级联更新所有关联的数据
     * @param category
     */
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    /**
     * TODO 产生对外内存溢出， OutOfDirectMemoryError
     *  1) springboot 2.0 以后默认使用lettuce作为操作redis的客户端。 它使用netty进行网络通信
     *  2) lettuce的bug导致netty堆外内存溢出 -xmx300m netty如果没有指定堆外内存，默认使用-Xmx300m
     *   可以通过-Dio.netty.maxDirectMemory进行设置
     *
     * 解决方案: 步能使用 -Dio.netty.maxDirectMemory 支取调大堆外内存
     * 1) 升级Lettuce客户端
     * 2） 接换使用jedis客户端
     * 我们用 2）
     *  redisTemplate:
     *      Lettuce, jedis 操作 redis的底层客户端。 spring 挨次封装redisTemplate
     * @return
     */
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        /**
         * 1. 空结果缓存， 解决缓存穿透
         * 2. 设置过期时间（加随机值）： 解决缓存雪崩
         * 3. 枷锁， 解决缓存击穿问题
         */


        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        // 加入缓存逻辑
        String catalogJSON = ops.get(CATALOG_JSON);
        if (StringUtils.isEmpty(catalogJSON)) {
            System.out.println("缓存不命中。。。将要查询数据库。。。");
            return getCatalogJsonWithReidsLock();
        }

        System.out.println("缓存命中。。。直接返回。。。");
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>(){});
        return result;
    }

    public Map<String, List<Catelog2Vo>> getCatalogJsonWithReidsLock() {
        // 占分布式锁， 去redis 占坑
        // 设置过期时间,必须和加锁是同步的, 加ttl的原因是防止 中间服务器异常退出，导致死锁
        // 缺点：业务超市情况，如果当前的thread 执行30秒后，就会释放锁，那么后面的thread 就会进来。
        String uuid = UUID.randomUUID().toString();
        // 加锁原子性
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if (lock) {
            System.out.println("获取分布式成功...");
            Map<String, List<Catelog2Vo>> dataFromDb = null;
            // 加锁成功，执行业务
            try {
                dataFromDb = getDataFromDb();
            } finally {
                String luaScript = "if redis.call(\"get\", KEYS[1]) == ARGV[1] then return redis.call(\"del\", KEYS[1]) else return 0 end";
                // 原子删除锁，还需要考虑锁的自动续期(如果不想做续期，把锁的时间放长一点也可以)
                Long lock1 = stringRedisTemplate.execute(new DefaultRedisScript<Long>(luaScript, Long.class), Arrays.asList("lock"), uuid);
            }

            // 获取值对比 + 对比成功删除=原子操作 lua脚本解锁
//            String lockValue = stringRedisTemplate.opsForValue().get("lock");
//            if (uuid.equals(lockValue)) {
//                //删除我自己的锁
//                stringRedisTemplate.delete("lock");
//            }
            return dataFromDb;
        } else {
            System.out.println("获取分布式失败... 等待重试");
            // 加锁失败，重试(recursion)， 用休眠来降低重试频率
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                log.error("{}", e);
            }
            return getCatalogJsonWithReidsLock();
        }
    }

    private Map<String, List<Catelog2Vo>> getDataFromDb() {
        //得到锁后，应该去再去缓存中确定一次，如果没有才需要继续查询
        String catalogJSON = stringRedisTemplate.opsForValue().get(CATALOG_JSON);
        if (!StringUtils.isEmpty(catalogJSON)) {
            //缓存部位null直接返回
            return JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
        }
        System.out.println("查询数据库。。。。");
        // 将数据库的多次查询变成为一次
        List<CategoryEntity> selectList = baseMapper.selectList(null);

        //查出说有一级分类
        List<CategoryEntity> level1Categorys = getParentCid(selectList, 0L);

        //封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys
                .stream()
                .collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                    List<CategoryEntity> categoryEntities = getParentCid(selectList, v.getCatId());
                    List<Catelog2Vo> catelog2Vos = null;
                    if (categoryEntities != null) {
                        catelog2Vos = categoryEntities
                                .stream()
                                .map(l2 -> {
                                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                                    // 当前2级分类找三级分类
                                    List<CategoryEntity> level3Catelog = getParentCid(selectList, l2.getCatId());
                                    if (level3Catelog != null) {
                                        // 封装指定格式
                                        List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map(l3 -> {
                                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                            return catelog3Vo;
                                        }).collect(Collectors.toList());
                                        catelog2Vo.setCatelog3List(collect);
                                    }
                                    return catelog2Vo;
                                })
                                .collect(Collectors.toList());
                    }
                    return catelog2Vos;
                }));

        stringRedisTemplate.opsForValue().set(CATALOG_JSON, JSON.toJSONString(parent_cid), 1, TimeUnit.DAYS);
        return parent_cid;
    }


    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDB() {
        // 只要是同一把锁， 就能锁住，需要这个锁的所有线程
        // synchronized(this) , this指这个class 锁住这个class, SpringBoot所有的组件在容器中国都是单例的
        // TODO 本地锁， synchronized, JUC(lock) 再分布式情况下， 想要锁住所有，必须使用分布式锁
        synchronized (this) {
            //得到锁后，应该去再去缓存中确定一次，如果没有才需要继续查询
            return getDataFromDb();
        }
    }

    private List<CategoryEntity> getParentCid(List<CategoryEntity> selectList, Long parentId) {
        List<CategoryEntity> collect = selectList
                .stream()
                .filter(item -> item.getParentCid() == parentId)
                .collect(Collectors.toList());
        return collect;
    }

    private void findParentPath(Long catelogId, List<Long> paths) {
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0) {
            this.findParentPath(byId.getParentCid(), paths);
        }
    }

    /**
     * 第一次查询的所有 CategoryEntity 然后根据 parent_cid去这里找
     */
    private List<CategoryEntity> getCategoryEntities(List<CategoryEntity> entityList, Long parent_cid) {

        return entityList.stream().filter(item -> item.getParentCid() == parent_cid).collect(Collectors.toList());
    }

}