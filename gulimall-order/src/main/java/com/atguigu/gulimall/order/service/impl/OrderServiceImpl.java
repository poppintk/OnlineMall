package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.order.Interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.enume.OrderStatusEnum;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.ProductFeignService;
import com.atguigu.gulimall.order.feign.WmsFeignService;
import com.atguigu.gulimall.order.service.OrderItemService;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.atguigu.gulimall.order.constant.OrderConstant.USER_ORDER_TOKEN_PREFIX;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

//    private ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();

    @Autowired
    OrderItemService orderItemService;

    @Autowired
    ProductFeignService productFeignService;

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

    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        SubmitOrderResponseVo response = new SubmitOrderResponseVo();
        // 验令牌，去创建订单，验价格，保存订单(insert DB), (RPC -> ware service)锁定库存
        String luaScript = "if redis.call(\"get\", KEYS[1]) == ARGV[1] then return redis.call(\"del\", KEYS[1]) else return 0 end";
        // 1 验证令牌 [令牌的对比和删除必须保证原子性]
        // 0 令牌失败 - 1 删除成功
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        response.setCode(0);


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
        // 令牌验证成功 => 去创建订单，验价格，保存订单(insert DB), (RPC -> ware service)锁定库存
        // 1. 创建订单
        OrderCreateTo order = createOrder(vo.getAddrId());
        // 2 验价格
        BigDecimal payAmount = order.getOrder().getPayAmount();
        BigDecimal payPrice = vo.getPayPrice();
        if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
            //金额对比
            // 3. 保存订单
            saveOrder(order);
            // 4. 库存锁定 只要有异常回滚订单数据
            //  为了保证高并发，库存服务自己回滚。可以发消息给库存服务
            // 库存服务本身也可以使用自动解锁模式， 消息队列
            // 订单号， 所有订单项 (skuId,skuNum, num)
            WareSkuLockVo lockVo = new WareSkuLockVo();
            lockVo.setOrderSn(order.getOrder().getOrderSn());
            List<OrderItemVo> locks = order.getOrderItems().stream().map(item -> {
                OrderItemVo itemVo = new OrderItemVo();
                itemVo.setSkuId(item.getSkuId());
                itemVo.setCount(item.getSkuQuantity());
                itemVo.setTitle(item.getSkuName());
                return itemVo;
            }).collect(Collectors.toList());
            lockVo.setLocks(locks);
            // RPC 远程锁库存
            R r = wmsFeignService.orderLockStock(lockVo);
            if (r.getCode() == 0) {
                // lock successfully
                response.setOrder(order.getOrder());
                int i = 10/0;
                return response;
            } else {
                String msg = (String) r.get("msg");
                throw new NoStockException(msg);
            }

        } else {
            response.setCode(2);
            return response;
        }
    }

    /**
     * SAVE ORDER DATA
     * @param order
     */
    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);

        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }

    private OrderCreateTo createOrder(Long addrId) {
        OrderCreateTo createTo = new OrderCreateTo();
        // 1 生成一个订单号
        String orderSn = IdWorker.getTimeId();
        // 创建订单号
        OrderEntity orderEntity = buildOrder(addrId, orderSn);

        // 2.获取到所有的订单项
        List<OrderItemEntity> itemEntities = buildOrderItems(orderSn);

        // 3 计算价格和积分等信息
        computePrice(orderEntity, itemEntities);
        createTo.setOrder(orderEntity);
        createTo.setOrderItems(itemEntities);

        return createTo;
    }

    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> itemEntities) {
        BigDecimal total = new BigDecimal("0.0");
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal integration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");

        BigDecimal gift = new BigDecimal("0.0");
        BigDecimal growth = new BigDecimal("0.0");
        // 订单的总额， 叠加每一个订单项的总额信息
        for (OrderItemEntity entity : itemEntities) {
            coupon = coupon.add(entity.getCouponAmount());
            integration = integration.add(entity.getIntegrationAmount());
            promotion = promotion.add(entity.getPromotionAmount());
            total = total.add(entity.getRealAmount());
            gift = gift.add(new BigDecimal(entity.getGiftIntegration().toString()));
            growth = growth.add(new BigDecimal(entity.getGiftGrowth().toString()));
        }
        // 1 订单价格相关
        orderEntity.setTotalAmount(total);
        // 应付
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setIntegrationAmount(integration);
        orderEntity.setCouponAmount(coupon);
        // 设置积分等信息
        orderEntity.setIntegration(gift.intValue());
        orderEntity.setGrowth(growth.intValue());
        orderEntity.setDeleteStatus(0); // 未删除

    }

    private OrderEntity buildOrder(Long addrId, String orderSn) {
        MemberRespVo respVo = LoginUserInterceptor.loginUser.get();
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderSn);
        entity.setMemberId(respVo.getId());

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

        // 实则只订单的相关状态信息
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        entity.setAutoConfirmDay(7);


        return entity;
    }

    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        // 最后确定每个购物项的价格
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if (currentUserCartItems != null && currentUserCartItems.size() > 0) {
            List<OrderItemEntity> itemEntities = currentUserCartItems.stream().map(cartItem -> {
                OrderItemEntity itemEntity = mapOrderItem(cartItem);
                itemEntity.setOrderSn(orderSn);
                return itemEntity;
            }).collect(Collectors.toList());
            return itemEntities;
        }
        return null;
    }

    private OrderItemEntity mapOrderItem(OrderItemVo cartItem) {
        OrderItemEntity itemEntity = new OrderItemEntity();
        // 1订单信息： 订单号
        // 2商品的SPU信息
        Long skuId = cartItem.getSkuId();
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo data = r.getData(new TypeReference<SpuInfoVo>() {
        });
        itemEntity.setSpuId(data.getId());
        itemEntity.setSpuBrand(data.getBrandId().toString());
        itemEntity.setSpuName(data.getSpuName());
        itemEntity.setCategoryId(data.getCatalogId());


        // 3商品的sku 信息
        itemEntity.setSkuId(cartItem.getSkuId());
        itemEntity.setSkuName(cartItem.getTitle());
        itemEntity.setSkuPic(cartItem.getImage());
        itemEntity.setSkuPrice(cartItem.getPrice());
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        itemEntity.setSkuAttrsVals(skuAttr);
        itemEntity.setSkuQuantity(cartItem.getCount());

        // 4优惠信息 [不做]
        // 5积分信息
        itemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        itemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());


        // 订单项的价格信息
        itemEntity.setPromotionAmount(new BigDecimal("0"));
        itemEntity.setCouponAmount(new BigDecimal("0"));
        itemEntity.setIntegrationAmount(new BigDecimal("0"));
        //当前订单项的实际金额 总额-各种优惠
        BigDecimal orgin = itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity().toString()));
        BigDecimal subtract = orgin.subtract(itemEntity.getCouponAmount())
                .subtract(itemEntity.getPromotionAmount())
                .subtract(itemEntity.getIntegrationAmount());
        itemEntity.setRealAmount(subtract);
        return itemEntity;
    }

}