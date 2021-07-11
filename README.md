# gulimall

### System Architecture
![spanning tree](./img/system_architecture.png)


## Project Description

Gulimall is a Ecommerce project, including Shopping System and Content management system. it based on SpringCloud、SpringCloud Alibaba、MyBatis Plus Implementation.
Shopping system includes: User Login, User Register, Product Search, Product description, Shopping Cart, Order and Seckill module.
Content Management system includes: System Management, Product Management, Coupon Management, Stock Management, Order managment, User Management and Content Management.

## Project Demo

### Shopping System

### Content Management System


## Project Structure

```
gulimall
├── gulimall-common -- Served as general lib
├── renren-generator -- Code auto generator
├── gulimall-auth-server -- Social login OAuth2.0 
├── gulimall-cart -- Shopping cart service
├── gulimall-coupon --  Coupon service
├── gulimall-gateway --  API gateway service
├── gulimall-order --  Order service
├── gulimall-product --  Product service
├── gulimall-search --  Index seach service
├── gulimall-seckill --  Seckill service
├── gulimall-third-party -- Third party service（Object storage, text message）
├── gulimall-ware --  Stock service
└── gulimall-member --  Member service
```

## 技术选型

### 后端技术

|        技术        |           说明           |                      官网                       |
| :----------------: | :----------------------: | :---------------------------------------------: |
|     SpringBoot     |       容器+MVC框架       |     https://spring.io/projects/spring-boot      |
|    SpringCloud     |        微服务架构        |     https://spring.io/projects/spring-cloud     |
| SpringCloudAlibaba |        一系列组件        | https://spring.io/projects/spring-cloud-alibaba |
|    MyBatis-Plus    |         ORM框架          |             https://mp.baomidou.com             |
|  renren-generator  | 人人开源项目的代码生成器 |   https://gitee.com/renrenio/renren-generator   |
|   Elasticsearch    |         搜索引擎         |    https://github.com/elastic/elasticsearch     |
|      RabbitMQ      |         消息队列         |            https://www.rabbitmq.com             |
|   Springsession    |        分布式缓存        |    https://projects.spring.io/spring-session    |
|      Redisson      |         分布式锁         |      https://github.com/redisson/redisson       |
|       Docker       |       应用容器引擎       |             https://www.docker.com              |
|        OSS         |        对象云存储        |  https://github.com/aliyun/aliyun-oss-java-sdk  |

### 前端技术

|   技术    |    说明    |           官网            |
| :-------: | :--------: | :-----------------------: |
|    Vue    |  前端框架  |     https://vuejs.org     |
|  Element  | 前端UI框架 | https://element.eleme.io  |
| thymeleaf |  模板引擎  | https://www.thymeleaf.org |
|  node.js  | 服务端的js |   https://nodejs.org/en   |

## 架构图

### 系统架构图

![](https://i.loli.net/2021/02/18/zMrSWaAfbqYoF4t.png)

### 业务架构图

![](https://i.loli.net/2021/02/18/yBjlqvsCgpVkENc.png)

## 环境搭建

### 开发工具

|     工具      |        说明         |                      官网                       |
| :-----------: | :-----------------: | :---------------------------------------------: |
|     IDEA      |    开发Java程序     |     https://www.jetbrains.com/idea/download     |
| RedisDesktop  | redis客户端连接工具 |        https://redisdesktop.com/download        |
|  SwitchHosts  |    本地host管理     |       https://oldj.github.io/SwitchHosts        |
|    X-shell    |  Linux远程连接工具  | http://www.netsarang.com/download/software.html |
|    Navicat    |   数据库连接工具    |       http://www.formysql.com/xiazai.html       |
| PowerDesigner |   数据库设计工具    |             http://powerdesigner.de             |
|    Postman    |   API接口调试工具   |             https://www.postman.com             |
|    Jmeter     |    性能压测工具     |            https://jmeter.apache.org            |
|    Typora     |   Markdown编辑器    |                https://typora.io                |

### 开发环境

|     工具      | 版本号 |                             下载                             |
| :-----------: | :----: | :----------------------------------------------------------: |
|      JDK      |  1.8   | https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html |
|     Mysql     |  5.7   |                    https://www.mysql.com                     |
|     Redis     | Redis  |                  https://redis.io/download                   |
| Elasticsearch | 7.6.2  |               https://www.elastic.co/downloads               |
|    Kibana     | 7.6.2  |               https://www.elastic.co/cn/kibana               |
|   RabbitMQ    | 3.8.5  |            http://www.rabbitmq.com/download.html             |
|     Nginx     | 1.1.6  |              http://nginx.org/en/download.html               |

注意：以上的除了jdk都是采用docker方式进行安装，详细安装步骤可参考百度!!!

### 搭建步骤

> Windows环境部署

- 修改本机的host文件，映射域名端口至Nginx地址

```
192.168.56.102	gulimall.com
192.168.56.102	search.gulimall.com
192.168.56.102  item.gulimall.com
192.168.56.102  auth.gulimall.com
192.168.56.102  cart.gulimall.com
192.168.56.102  order.gulimall.com
192.168.56.102  member.gulimall.com
192.168.56.102  seckill.gulimall.com
以上ip换成自己Linux的ip地址
```

- 修改Linux中Nginx的配置文件

```shell
1、在nginx.conf中添加负载均衡的配置   
upstream gulimall{
	# 网关的地址
	server 192.168.56.1:88;
}    
2、在gulimall.conf中添加如下配置
server {
	# 监听以下域名地址的80端口
    listen       80;
    server_name  gulimall.com  *.gulimall.com hjl.mynatapp.cc;

    #charset koi8-r;
    #access_log  /var/log/nginx/log/host.access.log  main;

    #配置静态资源分离
    location /static/ {
        root   /usr/share/nginx/html;
    }

    #支付异步回调的一个配置
    location /payed/ {
        proxy_set_header Host order.gulimall.com;        #不让请求头丢失
        proxy_pass http://gulimall;
    }

    location / {
        #root   /usr/share/nginx/html;
        #index  index.html index.htm;
        proxy_set_header Host $host;        #不让请求头丢失
        proxy_pass http://gulimall;
    }
```

或者直接用项目nginx模块替换本机nginx配置目录文件

- 克隆前端项目 `renren-fast-vue` 以 `npm run dev` 方式去运行
- 克隆整个后端项目 `gulimall` ，并导入 IDEA 中完成编译




