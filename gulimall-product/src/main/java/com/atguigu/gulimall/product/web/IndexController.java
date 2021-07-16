package com.atguigu.gulimall.product.web;


import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller
@Slf4j
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @ResponseBody
    @GetMapping("/hello")
    public String hello() {
        //1. 获取一把锁，只要锁的名字一样就是同一把锁
        RLock lock = redissonClient.getLock("testLock");

        // 2. 加锁
        lock.lock(10, TimeUnit.SECONDS); // 阻塞试等待， 默认ttl是30秒.
        // lock.lock(10, TimeUnit.SECONDS); 如果手动给与限制时间，就不会自动续锁，所以限制时间一定要大于 业务处理时间, 不然会有bug

        // 最佳实战
        //  1) lock.lock(30, TimeUnit.SECONDS); 省掉了整个续期操作，手动解锁. 给与30秒是最佳实现，因为如果业务超过三十秒 说明业务完蛋就有各种问题

        try {
            System.out.println("加锁成功...." + Thread.currentThread().getId());
            Thread.sleep(30000);
        } catch (Exception e) {

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

    /**
     * 保证一定能督导最新数据，修改期间，写锁是一个排他锁（互斥锁，独享锁）。 读锁是一个共享锁
     * 写期间 读必须等待
     * 写 + 读： 读等待写锁释放
     * 写 + 写： 阻塞方式
     * 读 + 写： 写会等待读释放
     * 读 + 读： 没有任何锁
     *
     * @return
     */
    @GetMapping("/write")
    @ResponseBody
    public String writeValue() {
        RReadWriteLock lock = redissonClient.getReadWriteLock("rw-lock");
        String s = "";
        RLock rLock = lock.writeLock();
        try {
            rLock.lock();
            // 改数据的时候加写锁， 读数据加读锁
            s = UUID.randomUUID().toString();
            Thread.sleep(30000);
            stringRedisTemplate.opsForValue().set("writeValue", s);
        } catch (InterruptedException e) {
            log.error("{}", e);
        } finally {
            rLock.unlock();
        }
        return s;
    }

    @GetMapping("/read")
    @ResponseBody
    public String readValue() {
        String s = "";
        RReadWriteLock lock = redissonClient.getReadWriteLock("rw-lock");
        // 加读锁
        RLock rLock = lock.readLock();
        rLock.lock();
        try {
            s = stringRedisTemplate.opsForValue().get("writeValue");
        } catch (Exception e) {
            log.error("{}", e);
        } finally {
            rLock.unlock();
        }
        return s;
    }

    /**
     * 车库停车
     * 3车位
     */
    @ResponseBody
    @GetMapping("/park")
    public String park() throws InterruptedException {
        RSemaphore park = redissonClient.getSemaphore("park");
        boolean b = park.tryAcquire();//获取一个信号，或者获取一个值, 占一个车位. 如果获取不到信号量，就会阻塞
        // tryAcquire() – return true if a permit is available immediately and acquire it otherwise return false,
        // but acquire() acquires a permit and BLOCKING until one is available
        if (b) {
            // 执行业务
            return "获取到";
        } else {
            return "没有获取到";
        }

    }

    @ResponseBody
    @GetMapping("/go")
    public String go() throws InterruptedException {
        RSemaphore park = redissonClient.getSemaphore("park");
        park.release();//释放以个车位

        return "ok";
    }

    /**
     * 闭锁
     * 放假， 锁门
     * 1班每人了， 锁门
     * 5个班全部走完，我们可以锁大门
     */
    @GetMapping("/lockDoor")
    public String lockDoor() throws InterruptedException {
        RCountDownLatch door = redissonClient.getCountDownLatch("door");
        door.trySetCount(5); // 5个班的人
        door.await();// 等待闭锁完成

        return "放假了";
    }

    @GetMapping("/gogogo/{id}")
    public String gogogo(@PathVariable("id") Long id) {
        RCountDownLatch door = redissonClient.getCountDownLatch("door");
        door.countDown(); //走一个人，计数减一
        return id + "班的人都走了";
    }
}
