package com.atguigu.gulimall.product;

import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.dao.SkuSaleAttrValueDao;
import com.atguigu.gulimall.product.service.SkuSaleAttrValueService;
import com.atguigu.gulimall.product.vo.SkuItemSaleAttrVo;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.UUID;

@SpringBootTest
@RunWith(SpringRunner.class)
public class GulimallProductApplicationTest extends TestCase {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    AttrGroupDao attrGroupDao;


    @Autowired
    SkuSaleAttrValueDao skuSaleAttrValueDao;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Test
    public void test() {

        List<SkuItemSaleAttrVo> saleAttrsBySpuId = skuSaleAttrValueDao.getSaleAttrsBySpuId(8L);
        System.out.println(saleAttrsBySpuId);

    }

    @Test
    public void testRedissonClient() {

        System.out.println(redissonClient);

    }


    @Test
    public void testStringRedisTemplate() {
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        ops.set("hello", "word" + UUID.randomUUID().toString());

        // 查询
        String key = ops.get("hello");
        System.out.println(key);
    }
}