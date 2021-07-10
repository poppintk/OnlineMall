package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.order.Interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.WmsFeignService;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.to.OrderCreateTo;
import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.atguigu.gulimall.order.constant.OrderConstant.USER_ORDER_TOKEN_PREFIX;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    private ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    WmsFeignService wmsFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ThreadPoolExecutor executor;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();


        CompletableFuture<Void> futureAddress = CompletableFuture.runAsync(() -> {
            // 远程查询所有的收货地址列表
            RequestContextHolder.setRequestAttributes(requestAttributes); // share threadLocal info in multi-thread
            List<MemberAddressVo> address = memberFeignService.getAddress(memberRespVo.getId());
            confirmVo.setMemberAddressVos(address);
        }, executor);


        CompletableFuture<Void> futureCart = CompletableFuture.runAsync(() -> {
            //远程查询购物车所有选中的购物项
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItems();
            confirmVo.setItems(items);
        }, executor).thenRunAsync(() -> {
            List<OrderItemVo> items = confirmVo.getItems();
            List<Long> collect = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            R hasStock = wmsFeignService.getSkusHasStock(collect);
            List<SkuStockVo> data = hasStock.getData(new TypeReference<List<SkuStockVo>>() {
            });
            if (data != null) {
                Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(map);
            }
        }, executor);


        // 查询用户积分
        Integer integration = memberRespVo.getIntegration();
        confirmVo.setIntegration(integration);


        //其他数据自动计算

        // TODO 幂等性
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(USER_ORDER_TOKEN_PREFIX + memberRespVo.getId(), token, 30, TimeUnit.MINUTES);
        confirmVo.setOrderToken(token);

        CompletableFuture<Void> allOf = CompletableFuture.allOf(futureAddress, futureCart);
        try {
            allOf.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error {}", e);
        }
        return confirmVo;
    }

    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        confirmVoThreadLocal.set(vo);
        SubmitOrderResponseVo response = new SubmitOrderResponseVo();
        // 验令牌，去创建订单，验价格， 锁定库存。。。
        String luaScript = "if redis.call(\"get\", KEYS[1]) == ARGV[1] then return redis.call(\"del\", KEYS[1]) else return 0 end";
        // 1 验证令牌 [令牌的对比和删除必须保证原子性]
        // 0 令牌失败 - 1 删除成功
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();


        String orderToken = vo.getOrderToken();
        Long result = redisTemplate.execute(
                new DefaultRedisScript<Long>(luaScript, Long.class),
                Arrays.asList(USER_ORDER_TOKEN_PREFIX + memberRespVo.getId()),
                orderToken
        );
        if (result == 0L) {
            // 令牌验证失败
            response.setCode(1);
            return response;
        }
        // 令牌验证成功 => 去创建订单，验价格， 锁定库存。。。

        OrderCreateTo order = createOrder(vo.getAddrId());


        return response;
    }

    private OrderCreateTo createOrder(Long addrId) {
        OrderCreateTo createTo = new OrderCreateTo();
        // 1 生成一个订单号
        String orderSn = IdWorker.getTimeId();
        // 创建订单号
        OrderEntity orderEntity = buildOrder(addrId, orderSn);

        // 2.获取到所有的订单项
        List<OrderItemEntity> itemEntities = buildOrderItems();

        // 3 验价

        return createTo;
    }

    private OrderEntity buildOrder(Long addrId, String orderSn) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderSn);

        //  获取收货地址信息
        R fare = wmsFeignService.getFare(addrId);
        FareVo fareResp = fare.getData(new TypeReference<FareVo>() {});

        entity.setFreightAmount(fareResp.getFare());
        entity.setReceiverCity(fareResp.getAddress().getCity());
        entity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
        entity.setReceiverName(fareResp.getAddress().getName());
        entity.setReceiverPhone(fareResp.getAddress().getPhone());
        entity.setReceiverPostCode(fareResp.getAddress().getPostCode());
        entity.setReceiverProvince(fareResp.getAddress().getProvince());
        entity.setReceiverRegion(fareResp.getAddress().getRegion());
        return entity;
    }

    private List<OrderItemEntity> buildOrderItems() {
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if (currentUserCartItems != null && currentUserCartItems.size() > 0) {
            List<OrderItemEntity> itemEntities = currentUserCartItems.stream().map(cartItem -> {
                OrderItemEntity itemEntity = mapOrderItem(cartItem);

                return itemEntity;
            }).collect(Collectors.toList());
        }

        return null;
    }

    private OrderItemEntity mapOrderItem(OrderItemVo cartItem) {

    }

}