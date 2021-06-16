package com.atguigu.gulimall.search.thread;

import java.util.concurrent.*;

public class ThreadTest {

    public static ExecutorService executor = Executors.newFixedThreadPool(10);

    /**
     * 为什么仍需要引入CompletableFuture？
     * 对于简单的业务场景使用Future完全没有，但是想将多个异步任务的计算结果组合起来，
     * 后一个异步任务的计算结果需要前一个异步任务的值等等，使用Future提供的那点API就囊中羞涩
     * @param args
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main .... start");
        //  runAsync 无return
//        CompletableFuture.runAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果： " + i);
//        }, executor);

//        // supplyAsync 有return
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//
//            System.out.println("运行结果： " + i);
//            return i;
//        }, executor).whenCompleteAsync((res, exception) -> {
//            //when complete虽然可以得到异常信息，但是没法修改返回数据
//            System.out.println("异步任务成功完成了... 结果是： " + res + "  异常是 :" + exception);
//        }).exceptionally(throwable -> {
//            // 可以感知异常，并修改返回结果
//            return 10;
//        });


        // handle 方法执行完后的处理
        // 无论是成功还是失败处理
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果： " + i);
//            return i;
//        }, executor).handle((res, thr) -> {
//            if (res != null) {
//                return res * 2;
//            }
//            if (thr != null) {
//                return 0;
//            }
//            return 0;
//        });

        /**
         * 线程窜行化
         * 1) thenRunAsync 不能获取到上一步的执行结果
         *
         * 2） thenAcceptAsync能接受上一步结果，但是无返回值
         *
         * 3) thenApplyAsync 能接受上一步返回值已经可以return
         */
//        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果： " + i);
//            return i;
//        }, executor).thenApplyAsync((res) -> {
//            System.out.println(res);
//            System.out.println("任务2启动。。。");
//            return res;
//        }, executor);

//        /**
//         * 两个都完成
//         */
//        CompletableFuture<Object> future1 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("任务一线程：" + Thread.currentThread().getId());
//            int i = 10 / 4;
//            System.out.println("任务一运行结果： " + i);
//            return i;
//        }, executor);
//
//        CompletableFuture<Object> future2 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("任务二线程：" + Thread.currentThread().getId());
//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            System.out.println("任务二运行结果： ");
//            return "Hello";
//        }, executor);

//        // 合并 无返回， 无参数
//        future1.runAfterBothAsync(future2, () -> {
//            System.out.println("任务三开始。。。");
//        }, executor);

//        // 合并， 无返回，有参数
//        future1.thenAcceptBothAsync(future2, (f1, f2) -> {
//            System.out.println("任务三开始。。。之前的结果" + f1 + " " + f2);
//        }, executor);

//        // 合并， 有返回，有参数
//        CompletableFuture<String> future = future1.thenCombineAsync(future2, (f1, f2) -> {
//            System.out.println("任务三开始。。。之前的结果" + f1 + " " + f2);
//            return f1 + ": " + f2;
//        }, executor);

        /**
         * 两个任务，任意一个只要有一个完成，我们就执行任务3
         */

//        // 任意一个， 无返回， 无参数
//        future1.runAfterEitherAsync(future2, () -> {
//            System.out.println("任务三开始。。。");
//
//        }, executor);

//        // 任意一个， 无返回，有参数
//        future1.acceptEitherAsync(future2, (param) -> {
//            System.out.println("之前任务的参数:" + param);
//            System.out.println("任务三开始。。。");
//        }, executor);
//


//        // 任意一个， 有返回， 有参数
//        CompletableFuture<Object> finalResult = future1.applyToEitherAsync(future2, (param) -> {
//            System.out.println("之前任务的参数:" + param);
//            System.out.println("任务三开始。。。");
//            return param;
//        }, executor);
//
//        System.out.println(finalResult.get());

        CompletableFuture<String> futureImg = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的图片信息");
            return "hello.jpg";
        }, executor);

        CompletableFuture<String> futureAttr = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的属性");
            return "black + 256G";
        }, executor);

        CompletableFuture<String> futureDesc = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("查询商品信息");
            return "huawei";
        }, executor);

        //CompletableFuture<Void> allOf = CompletableFuture.allOf(futureImg, futureAttr, futureDesc);
        //allOf.get();//等待所有结果完成
        CompletableFuture<Object> anyOf = CompletableFuture.anyOf(futureImg, futureAttr, futureDesc);
        anyOf.get(); //任意一个完成


        System.out.println("main .... end");
        executor.shutdown();
    }

    public static void thread(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main .... start");
        /**
         * 有三种方法可以结束线程：
         * 1.设置退出标志，使线程正常退出，也就是当run()方法完成后线程终止
         * 2.使用interrupt()方法中断线程
         * 3.使用stop方法强行终止线程（不推荐使用，Thread.stop, Thread.suspend, Thread.resume 和Runtime.runFinalizersOnExit 这些终止线程运行的方法已经被废弃，使用它们是极端不安全的！）
         */
        /**
         * 线程的4种创建方式
         * 1)继承Thread
         * Thread01 thread01 = new Thread01();
         * thread01.start();//启动线程
         * 2) 实现Runnable接口
         * //Runnalbe01 runnalbe01 = new Runnalbe01();
         * //new Thread(runnalbe01).start();
         * // OR
         * new Thread(() -> {
         *   System.out.println("当前线程：" + Thread.currentThread().getId());
         *   int i = 10 / 2;
         *   System.out.println("运行结果： " + i);
         * }).start();
         * 3) 现实Callable接口 + Future Task(可以拿到返回结果， 可以处理异常)
         * FutureTask futureTask = new FutureTask<>(new Callable01());
         * new Thread(futureTask).start();
         *
         * //如何取得返回值？
         * Object ans = futureTask.get();
         * System.out.println(ans);
         *
         *
         * 4) 线程池
         *      给线程池提交任务，为什么要用线程池？ 减小开销，因为如果每次创建新的线程开销大
         *
         *      Future可以获取到异步结果（类似JS的promise）
         */
        // 我们以后在业务代码里面，以上三种启动线程的方式都用，将所有的多线程异步任务交给线程池处理
        // 保证当前系统中池只有一到两个，每个异步任务，提交给线程池让他自己去执行就行
        Future submit = executor.submit(new Callable01());// 用execute 只能用Runnalbe, 用submit 可以用Callable和Runnable
        Object i = submit.get();
        System.out.println(i);


        /**
         * 线程池 7大参数
         * corePollSize： 核心线程数， 一直存在，线程池，创建好了以后就准备就绪的线程数量，就等待来接受异步任务去执行
         * maximumPoolSize: 最大线程数量， 控制资源并发
         * keepAliveTime: 存活时间，如果当前正在运行的线程数量大于core数量
         *          释放空闲的线程(maximumPoolSize - corePoolSize)。只要线程空闲大于指定的keepAliveTime
         * unit: 时间单位
         * BlokingQueue<Runnalbe> workQueue: 阻塞队列，如果任务有很多， 就会将目前多的任务放在队列里面
         *      只要有线程空闲了，就会去队列里面取出新的任务继续执行
         *
         * treadFactory: 线程的创建工程
         * RejectedExecutionHandler handler: 如果队列满了， 按照我们指定的拒绝策略拒绝执行任务
         *
         * 工作顺序：
         * 1）线程池创建，准备好core数量的核心线程，准备接受任务
         * 2） 新的任务进来，用core准备好的空闲线程执行
         * （a）如果core满了，就将再进来的任务放到阻塞队列里面。空闲的core就会自己去阻塞队列获取任务执行
         *  (b)阻塞队列满了，就直接开新线程执行，最大只能开到max指定的数量
         *  (c)max都执行完成了。 Max-core 数量空闲的线程会再keepAlivetime 指定的时间后自动销毁。最终保持到core大小
         *  (d)如果线程数开到了max的数量，还有新任务进来，就会使用reject指定的拒绝策略进行处理
         *
         * 3) 所有的现场创建都是由指定factory创建的
         *
         *
         * 面试：
         * 一个线程池 core7, max 20, queue 50, 100并发进来组面分配
         * 先有7个能直接得到执行， 接下来50个进入队列排队， 再多开13个继续执行。现在70个被安排上了。剩下30个默认决绝策略
         *
         * 如果不想抛弃还要执行， CallerRunsPolicy;
         */

        ThreadPoolExecutor executor = new ThreadPoolExecutor(5,
                200,
                10,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(1000),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());

        // 4种常见的线程池
//        Executors.newCachedThreadPool(); // core是0， 所有都可回收
//        Executors.newFixedThreadPool(); // 固定大小的线程池
//        Executors.newScheduledThreadPool() // 定时任务的线程池
//        Executors.newSingleThreadExecutor() // 单线程的线程池，后台从队列里面获取任务， 挨个执行


        System.out.println("main .... end");
    }

    public static class Callable01 implements Callable {

        @Override
        public Integer call() throws Exception {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("运行结果： " + i);
            return i;
        }
    }

    public static class Runnalbe01 implements Runnable {

        @Override
        public void run() {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("运行结果： " + i);
        }
    }


    public static class Thread01 extends Thread {

        @Override
        public void run() {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("运行结果： " + i);
        }
    }
}
