package com.atguigu.gulimall.order.service.impl;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.order.dao.OrderItemDao;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;


@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }

    @RabbitListener(queues = {"hello-java-queue"})
    public void receiveMessage(Message message, OrderReturnReasonEntity content, Channel channel) {
        byte[] body = message.getBody(); //消息体
        MessageProperties properties = message.getMessageProperties(); // 消息头
        System.out.println("接受到的消息" + message + "===>内容：" + content);
        System.out.println(channel);
        //  开启手动ack 模式， 只要我们没有明确告诉MQ, 货物被签收。 那么消息就一直是unacked状态， 即使consumer宕机.消息也不会丢失
        long deliveryTag = message.getMessageProperties().getDeliveryTag();//chnnel内按顺序自增
        // 签收, 非批量模式
        try {
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Error: {}", e);
            // 第三个参数是，重新入queue的意思, false 不重新入队
            try {
                channel.basicNack(deliveryTag, false,true);
            } catch (IOException ioException) {
                log.error("Error: {}", ioException);
            }
        }
    }

}