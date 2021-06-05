package com.atguigu.gulimall.product.web;


import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redissonClient;

    @ResponseBody
    @GetMapping("/hello")
    public String hello() {
        //1. 获取一把锁，只要锁的名字一样就是同一把锁
        RLock lock = redissonClient.getLock("testLock");

        // 2. 加锁
        lock.lock(10, TimeUnit.SECONDS); // 阻塞试等待， 默认ttl是30秒.
        // lock.lock(10, TimeUnit.SECONDS); 如果限制时间，就不会自动续锁，所以限制时间一定要大于 业务处理时间, 不然会有bug

        // 最佳实战
        //  1) lock.lock(10, TimeUnit.SECONDS); 省掉了整个续期操作，手动解锁

        try {
            System.out.println("加锁成功...." + Thread.currentThread().getId());
            Thread.sleep(30000);
        } catch (Exception e){

        } finally {
            // 3 解锁 问题：假设解锁代码没有运行，redisson会不会出现死锁？ 答案：不会有死锁问题，因为redisson用了watch dog机制 自动续ttl, 每十秒自动 续一次
            // 1) 锁的自动续期，如果业务超长，运行期间自动给锁续上新的30s.无需担心业务过长自动被删调
            // 2) 加锁的业务只要运行完成，就不会给当前锁续期，即使不手动解锁，锁默认再30s以后自动删除
            lock.unlock();
            System.out.println("释放锁...." + Thread.currentThread().getId());
        }

        return "Hello World";
    }


    @GetMapping({"/", "/index.html"})
    public String IndexPage(Model model) {

        // TODO 查出所有一级分类
        List<CategoryEntity> level1Categorys = categoryService.getLevel1Categorys();
        model.addAttribute("categories", level1Categorys);
        return "index";
    }

    //index/catalog.json
    @GetMapping("index/catalog.json")
    @ResponseBody
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        Map<String, List<Catelog2Vo>> catalogJson = categoryService.getCatalogJson();
        return catalogJson;
    }
}
