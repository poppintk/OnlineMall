package com.atguigu.gulimall.order;


import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

@SpringBootTest
@RunWith(SpringRunner.class)
public class GulimallOrderApplicationTests {

    @Autowired
    AmqpAdmin amqpAdmin;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Test
    public void sendMessageTest() {
        // 发送消息, 如果发送的消息是一个对象， 我们会使用序列化机制，将对象写出去。对象必须实现 Serializable
        OrderReturnReasonEntity reasonEntity = new OrderReturnReasonEntity();
        reasonEntity.setId(1L);
        reasonEntity.setCreateTime(new Date());
        reasonEntity.setName("退货");
        reasonEntity.setStatus(0);

        String msg = "Hello world";
        // 发送的对象类型的信息， 可以是一个json
        rabbitTemplate.convertAndSend("hello.java.exchange", "hello.java", reasonEntity);
        System.out.println("消息发送完成");
    }






    /**
     * 如何创建Exchange, Queue, Binding
     * 如何收发消息
     */
    @Test
    public void createExchange() {
        Exchange directExchange = new DirectExchange("hello.java.exchange", true, false);
        amqpAdmin.declareExchange(directExchange);
        System.out.println("创建exchange成功");
    }

    @Test
    public void createQueue() {
        Queue queue = new Queue("hello-java-queue", true, false, true);
        amqpAdmin.declareQueue(queue);
        System.out.println("队列创建成功");
    }

    @Test
    public void createBinding() {
        Binding binding = new Binding(
                "hello-java-queue",
                Binding.DestinationType.QUEUE,
                "hello.java.exchange",
                "hello.java", null);
        amqpAdmin.declareBinding(binding);
        System.out.println("hello-java-bingding 创建成功");
    }


}
