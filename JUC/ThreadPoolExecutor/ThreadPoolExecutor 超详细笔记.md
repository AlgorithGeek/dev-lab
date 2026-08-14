# ThreadPoolExecutor

------

# 一、ThreadPoolExecutor 到底是什么？

`ThreadPoolExecutor` 是 Java 中最核心、最经典的线程池实现类之一。

完整类名：

```java
java.util.concurrent.ThreadPoolExecutor
```

它位于：

```java
java.util.concurrent
```

也就是我们常说的：

```text
JUC
Java Util Concurrent
```

------

## 1.1 它在整个线程池体系中的位置

先看继承体系：

```text
Executor
    │
    ▼
ExecutorService
    │
    ▼
AbstractExecutorService
    │
    ▼
ThreadPoolExecutor
```

也就是：

```java
public interface Executor {
    void execute(Runnable command);
}
```

然后：

```java
public interface ExecutorService extends Executor {
    ...
}
```

然后：

```java
public abstract class AbstractExecutorService
        implements ExecutorService {
    ...
}
```

最后：

```java
public class ThreadPoolExecutor
        extends AbstractExecutorService {
    ...
}
```

所以可以这样理解：

```text
Executor
    ↓
规定最基础的“执行任务”能力

ExecutorService
    ↓
在线程池语义上增加 submit、shutdown 等能力

AbstractExecutorService
    ↓
帮我们实现 submit 等公共逻辑

ThreadPoolExecutor
    ↓
真正负责：
线程怎么创建
任务怎么排队
线程最多多少
什么时候销毁
队列满了怎么办
线程池关闭怎么办
```

因此：

> `ThreadPoolExecutor` 是 Java 线程池机制真正的核心实现。

------

# 二、为什么需要 ThreadPoolExecutor？

假设没有线程池。

每来一个任务：

```java
new Thread(() -> {
    doSomething();
}).start();
```

如果有：

```text
100 个任务
```

那么：

```text
创建 100 个线程
```

如果突然来了：

```text
100000 个任务
```

那是不是：

```text
创建 100000 个线程？
```

显然不行。

因为线程本身是有成本的。

一个线程涉及：

```text
线程对象
线程栈
操作系统线程资源
CPU 调度
上下文切换
线程创建成本
线程销毁成本
```

所以线程不是越多越好。

于是产生线程池思想：

```text
任务
任务
任务
任务
任务
      ↓
┌──────────────────────┐
│       线程池          │
│                      │
│ Thread-1             │
│ Thread-2             │
│ Thread-3             │
│ Thread-4             │
└──────────────────────┘
      ↓
重复使用这些线程执行任务
```

线程和任务之间不再是：

```text
一个任务
    ↓
创建一个线程
    ↓
线程执行
    ↓
线程死亡
```

而变成：

```text
创建少量 Worker 线程

Worker-1：
    执行任务A
    ↓
    执行任务D
    ↓
    执行任务G
    ↓
    ...

Worker-2：
    执行任务B
    ↓
    执行任务E
    ↓
    ...
```

因此线程池的核心思想其实就是：

> **控制线程数量 + 线程复用 + 任务缓冲 + 过载保护。**

------

# 三、ThreadPoolExecutor 最重要的构造方法

最经典的构造方法：

```java
public ThreadPoolExecutor(
        int corePoolSize,						//(1)
        int maximumPoolSize,					//(2)
        long keepAliveTime,						//(3)
        TimeUnit unit,							//(4)
        BlockingQueue<Runnable> workQueue,		//(5)
        ThreadFactory threadFactory,			//(6)
        RejectedExecutionHandler handler		//(7)
)
```

这就是大家经常说的：

> 线程池 7 大参数。

分别是：

```text
1. corePoolSize
2. maximumPoolSize
3. keepAliveTime
4. unit
5. workQueue
6. threadFactory
7. handler
```

这 7 个参数必须真正理解。

------

# 四、先建立 ThreadPoolExecutor 的整体模型

在研究参数之前，先建立一个整体结构。

你可以把 `ThreadPoolExecutor` 想象成：

```text
                       提交任务
                          │
                          ▼
              ┌─────────────────────┐
              │ ThreadPoolExecutor  │
              └─────────────────────┘
                          │
          ┌───────────────┼────────────────┐
          │               │                │
          ▼               ▼                ▼
      核心线程          工作队列          非核心线程
     Core Thread         Queue          Extra Thread
          │               │                │
          │               │                │
          └───────────────┼────────────────┘
                          │
                          ▼
                     执行任务
```

如果任务太多：

```text
核心线程满
    ↓
队列满
    ↓
线程数量达到 maximumPoolSize
    ↓
拒绝策略
```

这就是整个线程池最核心的模型。

------

# 五、corePoolSize(1)：核心线程数

第一个参数：

```java
int corePoolSize
```

例如：

```java
corePoolSize = 4;
```

表示线程池的核心线程数量是：

```text
4
```

------

## 5.1 什么叫核心线程？

可以暂时理解为：

> 线程池希望长期维持的一批基础工作线程。

例如：

```text
corePoolSize = 3
```

运行过程中可能有：

```text
Worker-1
Worker-2
Worker-3
```

这三个就是核心线程范围内的 Worker。

------

## 5.2 核心线程默认并不是一创建线程池就创建

很多初学者容易误解：

```java
new ThreadPoolExecutor(
    3,
    ...
);
```

是不是立刻创建：

```text
Thread-1
Thread-2
Thread-3
```

答案：

```text
不是。
```

默认采用：

> **懒创建。**

也就是：

```text
创建 ThreadPoolExecutor
        ↓
当前线程数量 = 0
```

第一个任务：

```text
创建第一个线程
```

第二个任务：

```text
创建第二个线程
```

第三个任务：

```text
创建第三个线程
```

直到：

```text
workerCount == corePoolSize
```

------

## 5.3 可以提前启动核心线程

ThreadPoolExecutor 提供：

```java
prestartCoreThread();
```

提前创建：

```text
1 个核心线程
```

还有：

```java
prestartAllCoreThreads();
```

提前创建：

```text
所有核心线程
```

例如：

```java
ThreadPoolExecutor pool = ...;

pool.prestartAllCoreThreads();
```

那么即使当前一个任务都没有：

```text
核心线程也会提前准备好。
```

适合：

```text
任务一来就希望立即执行
不希望第一次请求承担线程创建成本
```

------

# 六、maximumPoolSize(2)：最大线程数

第二个参数：

```java
int maximumPoolSize
```

例如：

```java
corePoolSize = 4;
maximumPoolSize = 10;
```

表示：

```text
平时基础线程数量：
4

压力大时最多：
10
```

于是：

```text
核心线程：
4 个

额外最多创建：
6 个线程
```

结构：

```text
Thread-1    核心
Thread-2    核心
Thread-3    核心
Thread-4    核心

Thread-5    临时扩容
Thread-6    临时扩容
...
Thread-10   临时扩容
```

绝对不会正常扩展成：

```text
Thread-11
```

因为：

```text
maximumPoolSize = 10
```

------

# 七、最容易理解错的地方：什么时候创建非核心线程？

非常重要。

很多初学者会以为：

```text
核心线程忙了
    ↓
马上创建非核心线程
```

错。

ThreadPoolExecutor 默认执行逻辑是：

```text
核心线程没达到 corePoolSize
    ↓
创建核心线程

核心线程数量已经达到 corePoolSize
    ↓
先放工作队列

队列也满了
    ↓
才继续创建线程

线程达到 maximumPoolSize
    ↓
拒绝任务
```

注意：

> **先入队，后扩容。**

这句话特别重要。

------

# 八、workQueue(5)：任务等待队列

参数：

```java
BlockingQueue<Runnable> workQueue
```

你前面已经学过 `BlockingQueue`，这里终于看到它真正的大型应用场景了。

线程池的：

```text
任务缓冲区
```

实际上就是：

```java
BlockingQueue<Runnable>
```

例如：

```java
new ArrayBlockingQueue<>(100)
```

意味着最多允许：

```text
100 个任务
```

在队列中等待线程执行。

------

## 8.1 为什么类型是 BlockingQueue？

因为：

```java
Executor.execute()
```

本身就是：

```java
void execute(Runnable command);
```

所以线程池内部最终需要执行的是：

```java
Runnable
```

因此队列：

```java
BlockingQueue<Runnable>
```

保存的自然就是待执行的 `Runnable`。

------

# 九、完整任务提交流程

这是整个 `ThreadPoolExecutor` 最核心的知识。

假设：

```text
corePoolSize = 2
maximumPoolSize = 5
queueCapacity = 3
```

也就是：

```java
ThreadPoolExecutor pool =
        new ThreadPoolExecutor(
                2,
                5,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(3)
        );
```

假设每个任务执行很久，都暂时不会结束。

------

## 9.1 提交第 1 个任务

当前：

```text
线程数 = 0
核心线程数 = 2
```

因为：

```text
0 < 2
```

所以：

```text
创建 Worker-1
        ↓
Worker-1 执行任务1
```

------

## 9.2 提交第 2 个任务

当前：

```text
线程数 = 1
```

因为：

```text
1 < corePoolSize(2)
```

所以：

```text
创建 Worker-2
        ↓
Worker-2 执行任务2
```

现在：

```text
Worker-1 → Task1

Worker-2 → Task2
```

------

## 9.3 提交第 3 个任务

现在：

```text
workerCount == corePoolSize
```

线程池不会马上创建 Worker-3。

而是：

```text
Task3
  ↓
workQueue
```

于是：

```text
Worker-1 → Task1
Worker-2 → Task2

Queue：
Task3
```

------

## 9.4 提交第 4 个任务

```text
Queue：
Task3
Task4
```

------

## 9.5 提交第 5 个任务

```text
Queue：
Task3
Task4
Task5
```

队列满了。

------

## 9.6 提交第 6 个任务

现在：

```text
核心线程已经满
队列也满
当前线程数 2 < maximumPoolSize 5
```

于是：

```text
创建 Worker-3
```

注意：

```text
Task6
```

直接交给：

```text
Worker-3
```

并不是把队列里的 Task3 拿出来给 Worker-3，然后 Task6 排队。

简化理解就是：

```text
新建线程直接执行当前提交任务。
```

现在：

```text
Worker-1 → Task1
Worker-2 → Task2
Worker-3 → Task6

Queue：
Task3
Task4
Task5
```

------

## 9.7 提交第 7 个任务

创建：

```text
Worker-4
```

执行：

```text
Task7
```

------

## 9.8 提交第 8 个任务

创建：

```text
Worker-5
```

执行：

```text
Task8
```

现在：

```text
Worker-1 → Task1
Worker-2 → Task2
Worker-3 → Task6
Worker-4 → Task7
Worker-5 → Task8

Queue：
Task3
Task4
Task5
```

已经达到：

```text
maximumPoolSize = 5
```

------

## 9.9 提交第 9 个任务

此时：

```text
核心线程满
+
队列满
+
线程数量达到 maximumPoolSize
```

已经没有任何地方可以接收任务。

于是：

```text
执行拒绝策略
```

所以完整口诀：

```text
先核心
再队列
队列满
再扩容
扩到最大
再拒绝
```

可以记成：

```text
core
 ↓
queue
 ↓
maximum
 ↓
reject
```

这是 ThreadPoolExecutor 最重要的一条主线。

------

# 十、ThreadPoolExecutor 的 execute() 核心逻辑

源代码虽然比较复杂，但是主逻辑其实可以简化成：

```java
public void execute(Runnable command) {

    if (当前线程数量 < corePoolSize) {

        创建线程执行任务;

    } else if (工作队列还能放任务) {

        workQueue.offer(command);

    } else if (当前线程数量 < maximumPoolSize) {

        创建非核心线程执行任务;

    } else {

        reject(command);
    }
}
```

这虽然不是源码，但已经非常接近设计思想了。

------

# 十一、真正源码级 execute() 流程

实际逻辑会更加严谨。

大概：

```text
execute(command)
        │
        ▼
command == null ?
        │
        ├── 是 → NullPointerException
        │
        └── 否
             ↓
workerCount < corePoolSize ?
        │
        ├── 是
        │     ↓
        │ addWorker(command, true)
        │     ↓
        │ 成功 → return
        │
        └── 否/失败
             ↓
线程池仍处于 RUNNING？
             +
workQueue.offer(command) 成功？
        │
        ├── 是
        │     ↓
        │ 再次检查线程池状态
        │     ↓
        │ 如果已经 shutdown
        │     ↓
        │ 尝试把任务从队列移除
        │     ↓
        │ reject
        │
        │ 如果 workerCount == 0
        │     ↓
        │ 创建一个 Worker
        │
        └── 否
             ↓
        addWorker(command, false)
             ↓
        创建失败
             ↓
        reject
```

这里有个很重要的细节：

> 任务进入队列后，ThreadPoolExecutor 还会再次检查线程池状态。

为什么？

因为可能出现并发情况：

```text
线程A：
准备提交任务

线程B：
调用 shutdown()

线程A：
刚刚把任务 offer 进队列
```

如果不重新检查：

```text
线程池都关闭了
结果新任务还混进来了
```

所以需要：

```text
recheck
```

这也是并发源码中非常常见的：

> **检查 → 操作 → 再检查。**

------

# 十二、keepAliveTime(3)：空闲线程存活时间

第三个参数：

```java
long keepAliveTime
```

第四个参数：

```java
TimeUnit unit
```

两者必须结合使用。

例如：

```java
60,
TimeUnit.SECONDS
```

表示：

```text
60 秒
```

------

## 12.1 为什么要 keepAliveTime？

假设：

```text
corePoolSize = 4
maximumPoolSize = 20
```

平时：

```text
4 个线程
```

突然高峰：

```text
扩容成 20 个线程
```

高峰结束后：

```text
只剩下一点任务
```

难道还长期养着：

```text
20 个线程？
```

没有必要。

所以：

```text
额外线程长期没任务
    ↓
等待 keepAliveTime
    ↓
销毁
```

最终：

```text
20
↓
18
↓
12
↓
8
↓
4
```

恢复：

```text
corePoolSize
```

------

# 十三、keepAliveTime 默认影响哪些线程？

默认：

```text
主要作用于超过 corePoolSize 的线程。
```

比如：

```text
corePoolSize = 4
maximumPoolSize = 10
```

线程：

```text
Worker1
Worker2
Worker3
Worker4
```

默认长期保留。

而：

```text
Worker5
Worker6
...
Worker10
```

如果空闲达到：

```text
keepAliveTime
```

就允许退出。

------

# 十四、核心线程能不能超时销毁？

可以。

调用：

```java
pool.allowCoreThreadTimeOut(true);
```

之后：

```text
核心线程
```

也允许在长时间没有任务后退出。

于是甚至可能：

```text
线程池对象还存在
但 Worker 数量 = 0
```

下次任务来了以后：

```text
重新创建 Worker。
```

注意：

如果：

```java
allowCoreThreadTimeOut(true);
```

通常要求：

```text
keepAliveTime > 0
```

否则没有意义。

------

# 十五、unit(4)：时间单位

第四个参数：

```java
TimeUnit unit
```

你前面应该也接触过。

例如：

```java
TimeUnit.SECONDS
```

还有：

```java
TimeUnit.MILLISECONDS
TimeUnit.MICROSECONDS
TimeUnit.NANOSECONDS
TimeUnit.MINUTES
TimeUnit.HOURS
TimeUnit.DAYS
```

所以：

```java
new ThreadPoolExecutor(
        4,
        10,
        60,
        TimeUnit.SECONDS,
        ...
);
```

表示：

```text
非核心线程空闲 60 秒后允许被回收。
```

------

# 十六、ThreadFactory(6)：线程工厂

第六个参数：

```java
ThreadFactory threadFactory
```

作用：

> 告诉线程池“线程应该怎么创建”。

接口非常简单：

```java
public interface ThreadFactory {

    Thread newThread(Runnable r);
}
```

注意：

`ThreadPoolExecutor` 自己负责：

```text
什么时候需要线程
```

而：

```text
ThreadFactory
```

负责：

```text
需要线程的时候，怎么 new Thread。
```

------

## 16.1 默认线程工厂

如果不传自定义 ThreadFactory，会使用：

```java
Executors.defaultThreadFactory()
```

创建出的线程名字一般类似：

```text
pool-1-thread-1
pool-1-thread-2
pool-1-thread-3
```

------

## 16.2 为什么生产环境经常自己定义 ThreadFactory？

最主要的原因：

> **线程名字非常重要。**

例如你日志里报：

```text
NullPointerException
at pool-3-thread-17
```

你很难知道：

```text
这个池是干嘛的？
```

如果名字是：

```text
order-payment-pool-1
order-payment-pool-2
```

马上知道：

```text
订单支付线程池。
```

例如：

```java
AtomicInteger index = new AtomicInteger(1);

ThreadFactory threadFactory = runnable -> {

    Thread thread = new Thread(runnable);

    thread.setName(
            "order-pool-" + index.getAndIncrement()
    );

    return thread;
};
```

使用：

```java
ThreadPoolExecutor pool =
        new ThreadPoolExecutor(
                4,
                8,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy()
        );
```

------

# 十七、RejectedExecutionHandler(7)：拒绝策略

最后一个参数：

```java
RejectedExecutionHandler handler
```

什么时候拒绝？

通常两个大场景。

------

## 17.1 场景一：线程池已经过载

```text
核心线程满
+
队列满
+
线程数达到 maximumPoolSize
```

于是：

```text
reject
```

------

## 17.2 场景二：线程池已经 shutdown

即使：

```text
线程不多
队列也有空间
```

但是线程池已经：

```java
shutdown();
```

再提交新任务：

```text
也要拒绝。
```

所以：

> 拒绝并不单纯等于“线程太多”。

还可能是：

```text
线程池生命周期已经不允许接任务。
```

------

# 十八、JDK 内置的四种拒绝策略

ThreadPoolExecutor 提供四种经典策略：

```text
AbortPolicy
CallerRunsPolicy
DiscardPolicy
DiscardOldestPolicy
```

------

# 十九、AbortPolicy

默认策略：

```java
new ThreadPoolExecutor.AbortPolicy()
```

拒绝时直接抛：

```java
RejectedExecutionException
```

例如：

```text
任务提交
   ↓
线程池满
   ↓
throw RejectedExecutionException
```

优点：

```text
问题不会被悄悄隐藏。
```

调用方能够发现：

```text
任务没有提交成功。
```

这往往也是比较安全的默认行为。

------

# 二十、CallerRunsPolicy

```java
new ThreadPoolExecutor.CallerRunsPolicy()
```

什么意思？

线程池不执行了。

由：

> **提交任务的线程自己执行。**

例如：

```java
httpThread：

pool.execute(task);
```

线程池满了。

那么可能变成：

```text
httpThread 自己执行 task.run()
```

------

## 20.1 CallerRunsPolicy 非常有意思

它不只是：

```text
换个人执行
```

它还会产生一种天然：

> **背压 Backpressure**

比如生产者：

```text
每秒提交 1000 个任务
```

线程池处理不过来。

开始让生产者自己执行：

```text
生产者：
提交
提交
提交
 ↓
线程池满
 ↓
生产者自己执行任务
 ↓
生产者没时间继续疯狂提交
```

于是：

```text
生产速度自动下降。
```

所以 `CallerRunsPolicy` 经常被看作一种简单的：

```text
削峰
限速
背压机制
```

不过也要小心。

如果调用线程是：

```text
Tomcat 请求线程
```

那么：

```text
请求线程会被任务占住
```

用户接口延迟就会增加。

所以它不是“万能最佳策略”。

------

# 二十一、DiscardPolicy

```java
new ThreadPoolExecutor.DiscardPolicy()
```

效果：

```text
任务直接丢掉。
```

并且：

```text
不报错。
```

相当于：

```java
// 什么都不做
```

这非常危险。

只有：

```text
任务允许丢失
```

时才适合。

比如某些：

```text
非关键统计
非关键日志
允许丢弃的刷新任务
```

而：

```text
支付
订单
资金
重要业务状态
```

肯定不能随便这么干。

------

# 二十二、DiscardOldestPolicy

```java
new ThreadPoolExecutor.DiscardOldestPolicy()
```

大概：

```text
队列满
    ↓
把队列里最老的任务扔掉
    ↓
重新尝试提交当前任务
```

例如：

```text
队列：

Task1
Task2
Task3
```

新来：

```text
Task4
```

那么：

```text
扔 Task1
```

然后尝试：

```text
加入 Task4
```

最终：

```text
Task2
Task3
Task4
```

注意：

这里的“oldest”实际上主要是：

```text
workQueue.poll()
```

所以具体哪个任务被认为是队头任务，也和：

```text
BlockingQueue 类型
```

有关。

------

# 二十三、自定义拒绝策略

因为：

```java
RejectedExecutionHandler
```

实际上就是接口：

```java
public interface RejectedExecutionHandler {

    void rejectedExecution(
            Runnable r,
            ThreadPoolExecutor executor
    );
}
```

所以可以：

```java
RejectedExecutionHandler handler =
        (task, executor) -> {

            System.out.println("任务被拒绝：" + task);

        };
```

甚至生产环境可以做：

```text
记录日志
上报告警
写数据库
写 MQ
计数监控
降级处理
```

例如：

```java
(task, executor) -> {

    log.error(
        "线程池任务拒绝，active={}, queue={}",
        executor.getActiveCount(),
        executor.getQueue().size()
    );

    // 其他补偿逻辑
};
```

------

# 二十四、ThreadPoolExecutor 和 BlockingQueue 的关系

这是你前面学 BlockingQueue 后特别应该理解的一点。

线程池本质上：

```text
生产者
    ↓
提交 Runnable
    ↓
BlockingQueue
    ↓
Worker
    ↓
取 Runnable
    ↓
执行
```

本质就是典型：

> **生产者—消费者模型。**

其中：

```text
业务线程
```

是生产者。

```text
Worker
```

是消费者。

```text
BlockingQueue<Runnable>
```

就是生产者和消费者之间的缓冲区。



注：

默认情况下，先创建核心线程直接执行任务；

当当前线程数达到 `corePoolSize` 后，后续任务才优先进入 `workQueue`。

只有 `workQueue` 满了，才继续创建线程，最多创建到 `maximumPoolSize`

也就是：

```
提交任务
   ↓
当前线程数 < corePoolSize？
   │
   ├── 是
   │    ↓
   │  创建新 Worker
   │    ↓
   │  这个 Worker 直接执行当前任务
   │
   └── 否
        ↓
     尝试放进 workQueue
        │
        ├── 放成功
        │     ↓
        │   等 Worker 来取
        │
        └── 队列满了
              ↓
      当前线程数 < maximumPoolSize？
              │
           ┌──┴──┐
          是     否
          ↓       ↓
      创建新线程  拒绝策略
      直接执行
```

比如：

```
corePoolSize = 2;
maximumPoolSize = 5;
workQueue = new ArrayBlockingQueue<>(3);
```

假设每个任务都一直没执行完。

提交 `Task1`：

```
线程数 0 < corePoolSize 2

→ 创建 Worker-1
→ Worker-1 直接执行 Task1
```

提交 `Task2`：

```
线程数 1 < corePoolSize 2

→ 创建 Worker-2
→ Worker-2 直接执行 Task2
```

现在已经达到：

```
workerCount = corePoolSize = 2
```

提交 `Task3`：

```
不创建 Worker-3

→ Task3 进入 workQueue
```

提交 Task4、Task5：

```
workQueue：

Task3
Task4
Task5
```

队列满了。

这时候再提交 `Task6`：

```
队列满
+
当前线程数 2 < maximumPoolSize 5

→ 创建 Worker-3
→ Worker-3 直接执行 Task6
```

所以顺序一定记成：

```
① 先扩到 corePoolSize

② 再塞 workQueue

③ workQueue 满了，再继续扩线程

④ 扩到 maximumPoolSize

⑤ 还来任务 → 拒绝
```

口诀就是：

```
核心 → 队列 → 最大 → 拒绝
```

### 还有一个很关键的细节

前 `corePoolSize` 个任务，**默认并不是：**

```
Task
 ↓
workQueue
 ↓
Worker取出来
 ↓
执行
```

而是：

```
Task
 ↓
创建 Worker
 ↓
Task 作为 Worker 的 firstTask
 ↓
直接执行
```

源码思想就是：

```
if (workerCount < corePoolSize) {
    addWorker(command, true);
}
```

这里的 `command` 就是你刚提交的任务。

所以新建 Worker 时，大致相当于：

```
Worker {
    Thread thread;
    Runnable firstTask = command;
}
```

然后：

```
Worker启动
 ↓
先执行 firstTask
 ↓
执行完
 ↓
再去 workQueue 中拿任务
```

### 你第三种理解要特别纠正

不是：

> “达到 `maximumPoolSize` 后才往 workQueue 放。”

正好反过来。

**必须是 workQueue 先满，才有机会从 `corePoolSize` 扩到 `maximumPoolSize`。**

所以例如：

```
core = 2
max = 100
queue = 无界队列
```

那么很可能永远只有：

```
2 个线程
```

因为：

```
2 个核心线程创建完
 ↓
后面的任务全进 queue
 ↓
queue 永远不满
 ↓
永远没有机会创建第 3 个线程
```

这就是为什么我们前面说：

> **使用无界队列时，`maximumPoolSize` 往往几乎失去意义。**

你现在可以直接把 `ThreadPoolExecutor` 的默认扩容模型刻在脑子里：

```
             Task
              ↓
     worker < core？
       ↓是       ↓否
   创建线程      进队列
   直接执行        ↓
               队列满？
             ↓否     ↓是
            排队   worker < max？
                   ↓是     ↓否
                创建线程    拒绝
                直接执行
```

这张图就是 `ThreadPoolExecutor.execute()` 最核心的骨架





------

# 二十五、线程池常用哪些 BlockingQueue？

非常重要。

常见：

```text
ArrayBlockingQueue
LinkedBlockingQueue
SynchronousQueue
PriorityBlockingQueue
```

------

# 二十六、ArrayBlockingQueue

例如：

```java
new ArrayBlockingQueue<>(100)
```

特点：

```text
数组结构
有界
容量固定
```

在线程池里通常是比较容易控制风险的选择。

例如：

```text
核心线程 = 8
最大线程 = 16
队列容量 = 500
```

系统最多大致承载：

```text
16 个正在执行
+
500 个队列等待
```

超过以后：

```text
进入拒绝策略。
```

这使资源上限非常明确。

------

# 二十七、LinkedBlockingQueue

可以：

```java
new LinkedBlockingQueue<>(1000)
```

这是：

```text
有界 LinkedBlockingQueue
```

但是如果：

```java
new LinkedBlockingQueue<>()
```

默认容量大到接近：

```java
Integer.MAX_VALUE
```

实际上通常被称为：

> 无界队列。

------

## 27.1 无界队列为什么危险？

假设：

```text
生产速度 = 每秒 10000
消费速度 = 每秒 100
```

剩余：

```text
9900 / 秒
```

持续进入队列。

那么：

```text
1 秒：9900
10 秒：99000
100 秒：990000
...
```

最后：

```text
大量 Runnable
FutureTask
业务参数对象
上下文对象
```

堆积在 JVM 堆中。

最终可能：

```text
OOM
```

------

# 二十八、极其重要：无界队列会让 maximumPoolSize 基本失效

这是 ThreadPoolExecutor 非常经典的面试题。

假设：

```text
corePoolSize = 4
maximumPoolSize = 100
```

使用：

```java
new LinkedBlockingQueue<>()
```

流程：

```text
前 4 个任务
    ↓
创建 4 个线程

后面的任务
    ↓
全部进入 LinkedBlockingQueue
```

因为队列几乎永远不会：

```text
offer 失败
```

于是：

```text
创建非核心线程
```

这一分支几乎永远走不到。

最终很可能：

```text
线程数量一直 = 4
```

虽然：

```text
maximumPoolSize = 100
```

但：

```text
100 基本没机会发挥作用。
```

所以：

> **maximumPoolSize 是否真正有意义，很大程度取决于 workQueue。**

------

# 二十九、SynchronousQueue 在线程池里的意义

你前面问过：

> `SynchronousQueue` 是不是就是占位用的？

现在 ThreadPoolExecutor 就是它最重要的应用场景之一。

`SynchronousQueue`：

```text
容量 = 0
```

它根本不保存任务。

相当于：

```text
生产者
    ↓
必须直接交给消费者
```

线程池里面：

```text
核心线程满
    ↓
尝试 offer 到 SynchronousQueue
    ↓
没有线程立即接收
    ↓
offer 失败
    ↓
创建新线程
```

于是它形成：

```text
任务来了
核心线程忙
    ↓
不排队
    ↓
直接扩线程
```

所以：

```java
new SynchronousQueue<>()
```

和：

```text
较大的 maximumPoolSize
```

组合后，会形成非常积极的线程扩容策略。

------

# 三十、newCachedThreadPool 为什么扩线程特别激进？

经典：

```java
Executors.newCachedThreadPool()
```

内部思想大概是：

```java
corePoolSize = 0

maximumPoolSize = Integer.MAX_VALUE

keepAliveTime = 60 秒

workQueue = SynchronousQueue
```

所以：

```text
Task1
 ↓
没有线程
 ↓
创建线程

Task2
 ↓
线程忙
 ↓
SynchronousQueue 放不进去
 ↓
继续创建线程

Task3
 ↓
继续创建
```

这就是为什么：

```text
CachedThreadPool
```

任务暴增时非常容易：

```text
疯狂创建线程。
```

------

# 三十一、PriorityBlockingQueue

还可以：

```java
PriorityBlockingQueue<Runnable>
```

让任务按照：

```text
优先级
```

执行。

不过使用复杂度较高。

任务本身需要支持比较：

```text
Comparable
```

或者提供：

```text
Comparator
```

而且默认也是：

```text
无界
```

所以同样存在任务堆积风险。

普通业务线程池一般不会随便使用。

------

# 三十二、线程池不是“永不丢数据”的存储系统

这一点和你前面问 BlockingQueue 的问题可以正式连起来。

你之前的想法类似：

> 数据爆炸了又不能丢，怎么办？

ThreadPoolExecutor 的答案不是：

```text
无限扩大队列。
```

因为内存是有限的。

如果任务真的：

```text
绝对不能丢
+
流量可能远远超过系统消费能力
```

那么不能单纯依赖 JVM 内存线程池。

应该引入：

```text
Kafka
RabbitMQ
RocketMQ
数据库
磁盘持久化
```

例如：

```text
请求
 ↓
MQ 持久化
 ↓
消费者
 ↓
线程池
 ↓
业务处理
```

于是：

```text
MQ
```

负责：

```text
可靠缓冲
持久化
削峰
```

而：

```text
ThreadPoolExecutor
```

负责：

```text
控制本机并发执行能力
```

所以必须建立一个思想：

> **线程池是执行器，不是可靠消息仓库。**

------

# 三十三、为什么“开发里到处都是权衡”？

线程池就是一个非常典型的例子。

你想：

```text
不丢任务
```

那可以把队列扩大。

但是：

```text
队列太大
→ 延迟越来越高
→ 内存越来越大
→ OOM 风险提高
```

你想：

```text
任务快速执行
```

可以扩大线程：

```text
maximumPoolSize = 1000
```

但：

```text
上下文切换
CPU 抢占
内存消耗
数据库连接打满
下游服务被打爆
```

又来了。

你想：

```text
立即拒绝
```

系统稳定了。

但：

```text
业务任务可能失败。
```

所以：

```text
容量
吞吐量
延迟
可靠性
资源
```

之间永远存在权衡。

线程池本身就是：

> **资源治理工具。**

而不是：

> “所有任务无论多少都神奇地完成。”

------

# 三十四、Worker 是什么？

ThreadPoolExecutor 内部并不是简单维护：

```text
List<Thread>
```

它内部存在一个重要结构：

```text
Worker
```

源码中大致：

```java
private final class Worker
        extends AbstractQueuedSynchronizer
        implements Runnable {
    ...
}
```

每一个 Worker：

```text
包装一个 Thread
```

你可以暂时理解：

```text
Worker
 ├── Thread
 ├── firstTask
 └── 一些运行状态
```

------

# 三十五、线程和任务不是一一对应关系

非常重要。

例如：

```text
Worker-1
```

内部 Thread：

```text
Thread-1
```

它可能：

```text
执行 Task1
↓
执行完成
↓
从 BlockingQueue 获取 Task5
↓
执行 Task5
↓
获取 Task8
↓
执行 Task8
↓
...
```

所以：

```text
1 个线程
```

可以执行：

```text
N 个任务。
```

这正是：

> **线程复用。**

------

# 三十六、Worker 怎么不断执行任务？

源码最核心思想：

```java
while (
        task != null
        ||
        (task = getTask()) != null
) {

    task.run();

}
```

简化成：

```java
while (线程池还允许工作) {

    Runnable task = 从队列获取任务;

    task.run();

}
```

所以 Worker 的生命：

```text
创建
 ↓
执行 firstTask
 ↓
从 queue 获取任务
 ↓
执行
 ↓
继续获取
 ↓
执行
 ↓
...
 ↓
满足退出条件
 ↓
结束
```

------

# 三十七、firstTask 是什么？

比如：

```text
当前线程数量 < corePoolSize
```

来了：

```text
Task1
```

线程池：

```java
addWorker(Task1, true);
```

Worker 创建时：

```text
firstTask = Task1
```

于是线程第一次启动：

```text
先执行 firstTask
```

执行完：

```text
再去 BlockingQueue 获取其他任务。
```

所以：

```text
新创建的 Worker
```

并不一定先跑去队列取任务。

它可以：

```text
直接带着当前任务出生。
```

------

# 三十八、getTask() 是什么？

Worker 当前任务执行完后：

```text
下一步干嘛？
```

调用：

```java
getTask()
```

去：

```text
workQueue
```

拿任务。

------

## 38.1 核心线程通常怎么取？

通常类似：

```java
workQueue.take();
```

`take()`：

```text
队列有任务
    ↓
拿任务

队列没任务
    ↓
阻塞等待
```

因此核心线程可以：

```text
一直等任务
```

而不是：

```text
没有任务就疯狂 while 空转。
```

------

## 38.2 非核心线程怎么实现超时？

类似：

```java
workQueue.poll(
    keepAliveTime,
    TimeUnit.NANOSECONDS
);
```

也就是：

```text
等 keepAliveTime
```

还没有任务：

```text
返回 null
```

Worker 便可能：

```text
退出。
```

所以 BlockingQueue 的：

```text
take()
poll(timeout)
```

在 ThreadPoolExecutor 中都真正派上用场了。

------

# 三十九、ThreadPoolExecutor 和 Runnable / Callable 的关系

你可能注意到：

```java
ThreadPoolExecutor
```

的队列类型：

```java
BlockingQueue<Runnable>
```

而你之前学过：

```java
Callable<V>
```

问题：

> Callable 怎么进线程池？

答案：

```text
Callable
 ↓
FutureTask
 ↓
Runnable
```

因为：

```java
FutureTask<V>
```

实现了：

```java
RunnableFuture<V>
```

而：

```java
RunnableFuture<V>
        extends Runnable, Future<V>
```

所以：

```text
Callable
```

最终被包装成：

```text
FutureTask
```

而 FutureTask 本身：

```text
就是 Runnable。
```

于是：

```text
BlockingQueue<Runnable>
```

完全可以存。

------

# 四十、submit() 到底发生了什么？

这一部分必须和你刚学过的 Future/FutureTask 连起来。

比如：

```java
Future<Integer> future =
        pool.submit(() -> {
            return 100;
        });
```

表面：

```text
Callable
 ↓
submit
 ↓
Future
```

内部大概：

```text
Callable
 ↓
newTaskFor()
 ↓
FutureTask
 ↓
execute(FutureTask)
 ↓
线程池
```

也就是：

```java
FutureTask<Integer> futureTask =
        new FutureTask<>(callable);

execute(futureTask);

return futureTask;
```

所以：

```text
submit()
```

真正提交给 ThreadPoolExecutor 的其实依然是：

```text
Runnable
```

只不过这个 Runnable 是：

```text
FutureTask。
```

------

# 四十一、为什么 ThreadPoolExecutor 只需要理解 Runnable 就够了？

因为最终可以统一成：

```text
Runnable
```

普通 Runnable：

```text
Runnable
 ↓
execute
```

Callable：

```text
Callable
 ↓
FutureTask
 ↓
Runnable
 ↓
execute
```

所以执行器底层其实不用关心：

```text
你原来到底是 Runnable 还是 Callable。
```

它只知道：

```java
task.run();
```

这是一种非常漂亮的统一设计。

------

# 四十二、execute() 和 submit() 的区别

------

## 42.1 execute()

来自：

```java
Executor
```

方法：

```java
void execute(Runnable command);
```

特点：

```text
只能直接提交 Runnable
没有 Future 返回值
```

例如：

```java
pool.execute(() -> {
    System.out.println("hello");
});
```

------

## 42.2 submit()

来自：

```java
ExecutorService
```

可以：

```java
submit(Runnable task);

submit(Runnable task, T result);

submit(Callable<T> task);
```

返回：

```java
Future
```

例如：

```java
Future<Integer> future =
        pool.submit(() -> 100);
```

------

# 四十三、submit() 是阻塞的吗？

不是。

这一点你之前问过。

```java
Future<Integer> future =
        pool.submit(task);
```

通常：

```text
submit
 ↓
把任务包装
 ↓
提交给线程池
 ↓
立即返回 Future
```

并不会等待：

```text
task 执行完成。
```

所以：

```text
submit 本身一般是异步提交。
```

真正可能阻塞：

```java
future.get();
```

例如：

```java
Future<Integer> future = pool.submit(task);

// 主线程继续干其他事情

Integer value = future.get();
```

到：

```java
get()
```

如果任务还没结束：

```text
当前线程阻塞等待。
```

------

# 四十四、同一个 Callable 提交多次会怎样？

例如：

```java
Callable<Integer> callable = () -> {
    System.out.println("执行");
    return 100;
};

Future<Integer> f1 = pool.submit(callable);
Future<Integer> f2 = pool.submit(callable);
Future<Integer> f3 = pool.submit(callable);
```

会执行：

```text
3 次。
```

原因不是：

```text
FutureTask 一次性原则失效了。
```

而是：

```text
submit(callable)
```

每调用一次：

```text
创建一个新的 FutureTask。
```

实际上：

```text
Callable
 ↓
FutureTask-1

Callable
 ↓
FutureTask-2

Callable
 ↓
FutureTask-3
```

每一个 FutureTask：

```text
只执行一次。
```

所以完全不冲突。

------

# 四十五、一个 FutureTask 自己提交两次呢？

例如：

```java
FutureTask<Integer> futureTask =
        new FutureTask<>(callable);

pool.execute(futureTask);
pool.execute(futureTask);
```

虽然：

```text
线程池接收了两次 Runnable
```

但：

```text
futureTask.run()
```

本身有一次性状态控制。

所以真正的 Callable：

```text
通常只会执行一次。
```

第二次：

```java
futureTask.run();
```

发现已经完成：

```text
直接返回。
```

所以：

> ThreadPoolExecutor 不会破坏 FutureTask 自己的一次性语义。

------

# 四十六、execute 和 submit 在异常处理上的一个重大区别

这个非常实用。

------

## 46.1 execute()

例如：

```java
pool.execute(() -> {

    throw new RuntimeException("出错了");

});
```

任务异常可能：

```text
直接从 task.run() 抛出来
```

最终：

```text
Worker 线程异常结束
```

异常通常可能通过：

```text
UncaughtExceptionHandler
```

被看到。

线程池后续通常会根据需要：

```text
补一个 Worker。
```

------

## 46.2 submit()

如果：

```java
pool.submit(() -> {

    throw new RuntimeException("出错了");

});
```

真正执行的是：

```text
FutureTask.run()
```

FutureTask 会把异常：

```text
保存到自己的结果状态里面。
```

所以异常通常不会直接从：

```text
FutureTask.run()
```

抛到 Worker 外面。

你需要：

```java
future.get();
```

然后看到：

```java
ExecutionException
```

其：

```java
getCause()
```

才是原始异常。

例如：

```java
try {

    future.get();

} catch (ExecutionException e) {

    Throwable cause = e.getCause();

}
```

所以一个经典坑：

> `submit()` 提交的任务发生异常，如果你连 Future 都不检查，很可能感觉“异常消失了”。

实际上不是消失：

```text
异常被 FutureTask 保存起来了。
```

------

# 四十七、线程池中的线程为什么不会执行一个任务就死亡？

核心原因：

Worker 不只是：

```java
task.run();
```

然后结束。

而是：

```java
while (...) {

    Runnable task = getTask();

    task.run();

}
```

所以：

```text
Task1 执行完成
    ↓
线程没有退出
    ↓
继续 getTask
    ↓
Task2
    ↓
继续
```

这就是线程池所谓：

> **线程复用。**

------

# 四十八、线程池的生命周期状态

ThreadPoolExecutor 内部有几个重要状态：

```text
RUNNING
SHUTDOWN
STOP
TIDYING
TERMINATED
```

------

# 四十九、RUNNING

正常运行状态。

可以：

```text
接收新任务
+
处理队列已有任务
```

也就是：

```text
新任务：YES
队列任务：YES
```

------

# 五十、SHUTDOWN

调用：

```java
pool.shutdown();
```

之后进入：

```text
SHUTDOWN
```

特点：

```text
不接收新任务
但继续处理已经进入队列的任务
```

所以：

```text
新任务：NO
队列任务：YES
```

------

# 五十一、STOP

调用：

```java
pool.shutdownNow();
```

之后通常进入：

```text
STOP
```

特点：

```text
不接收新任务
不继续正常处理队列中的等待任务
尝试 interrupt 正在执行任务的 Worker
```

所以：

```text
新任务：NO
队列任务：NO
正在执行的任务：尝试中断
```

注意：

> `shutdownNow()` 并不保证真的能把任务强制杀死。

因为 Java 的：

```java
interrupt()
```

本身是：

```text
协作式中断。
```

如果任务完全无视中断：

```java
while (true) {

}
```

那么：

```text
shutdownNow()
```

也无法魔法般强杀。

------

# 五十二、TIDYING

线程池正在：

```text
做终止前的清理。
```

此时：

```text
workerCount = 0
```

并准备执行：

```java
terminated()
```

钩子方法。

------

# 五十三、TERMINATED

线程池已经：

```text
彻底终止。
```

整个生命周期：

```text
RUNNING
   │
   ├── shutdown()
   ▼
SHUTDOWN
   │
   ▼
TIDYING
   │
   ▼
TERMINATED
```

或者：

```text
RUNNING
   │
   ├── shutdownNow()
   ▼
STOP
   │
   ▼
TIDYING
   │
   ▼
TERMINATED
```

------

# 五十四、shutdown() 到底做什么？

```java
pool.shutdown();
```

不是：

```text
立刻把线程全部杀掉。
```

而是：

```text
从现在开始：
不接受新的任务

但是：
已经提交进去的任务继续完成
```

例如：

```text
Worker：
Task1 Task2 正在运行

Queue：
Task3 Task4 Task5
```

执行：

```java
shutdown();
```

之后：

```text
Task1
Task2
Task3
Task4
Task5
```

仍然会尽量全部执行完成。

但是：

```java
pool.execute(Task6);
```

会：

```text
拒绝。
```

------

# 五十五、shutdownNow()

```java
List<Runnable> tasks =
        pool.shutdownNow();
```

它会：

```text
拒绝新任务
+
尝试 interrupt 正在工作的线程
+
从 workQueue 中取出还没开始的任务
```

返回：

```text
尚未开始执行的任务列表。
```

例如：

```text
Worker：
Task1
Task2

Queue：
Task3
Task4
Task5
```

调用：

```java
shutdownNow()
```

可能：

```text
尝试 interrupt Task1 / Task2

返回：
Task3
Task4
Task5
```

------

# 五十六、isShutdown()

```java
pool.isShutdown()
```

用于判断：

```text
是否已经开始关闭。
```

一旦调用：

```text
shutdown
```

或者：

```text
shutdownNow
```

一般都会：

```text
true
```

------

# 五十七、isTerminated()

```java
pool.isTerminated()
```

意思不是：

```text
调用过 shutdown 了吗？
```

而是：

> 线程池是不是已经真正完全结束。

例如：

```java
pool.shutdown();
```

马上：

```java
pool.isShutdown();   // true
```

但如果还有任务：

```java
pool.isTerminated(); // false
```

所有任务和 Worker 都结束之后：

```java
pool.isTerminated(); // true
```

------

# 五十八、awaitTermination()

例如：

```java
pool.shutdown();

boolean finished =
        pool.awaitTermination(
                30,
                TimeUnit.SECONDS
        );
```

当前线程最多等待：

```text
30 秒
```

看看线程池是否彻底结束。

所以：

```text
awaitTermination()
```

是：

> **阻塞方法。**

它阻塞的是：

```text
调用 awaitTermination 的线程。
```

------

# 五十九、为什么线程池一定要 shutdown？

默认线程工厂创建的线程一般是：

```text
非守护线程。
```

所以如果程序中：

```text
线程池还活着
```

即使：

```java
main()
```

执行结束：

JVM 也可能：

```text
继续运行。
```

例如：

```java
public static void main(String[] args) {

    ExecutorService pool =
            Executors.newFixedThreadPool(4);

    pool.submit(() ->
            System.out.println("hello")
    );

}
```

如果不：

```java
pool.shutdown();
```

可能导致：

```text
JVM 不退出。
```

因为池中的 Worker：

```text
还活着等待新任务。
```

------

# 六十、一个相对正规的关闭模板

常见思想：

```java
pool.shutdown();

try {

    if (!pool.awaitTermination(
            30,
            TimeUnit.SECONDS)) {

        pool.shutdownNow();

    }

} catch (InterruptedException e) {

    pool.shutdownNow();

    Thread.currentThread().interrupt();
}
```

这里：

```java
Thread.currentThread().interrupt();
```

是为了：

> 恢复当前线程的中断状态。

因为捕获：

```java
InterruptedException
```

之后，中断状态通常已经被清除。

------

# 六十一、ThreadPoolExecutor 内部的 ctl

这一块属于源码进阶。

ThreadPoolExecutor 内部有个非常重要的字段：

```java
private final AtomicInteger ctl =
        new AtomicInteger(ctlOf(RUNNING, 0));
```

这个：

```text
ctl
```

非常厉害。

它一个整数同时保存：

```text
线程池运行状态
+
Worker 数量
```

------

# 六十二、为什么一个 int 能保存两个东西？

一个：

```text
32 位 int
```

被拆成：

```text
高 3 位：
线程池状态

低 29 位：
workerCount
```

大致：

```text
┌─────────┬──────────────────────────────┐
│ 高3位    │          低29位              │
├─────────┼──────────────────────────────┤
│ 状态     │       workerCount            │
└─────────┴──────────────────────────────┘
```

所以：

```text
一个 AtomicInteger
```

就可以 CAS 修改：

```text
状态 + 线程数量
```

减少一些额外同步成本。

------

# 六十三、线程池几个状态在 ctl 中的设计

源码类似：

```java
private static final int COUNT_BITS =
        Integer.SIZE - 3;

private static final int RUNNING =
        -1 << COUNT_BITS;

private static final int SHUTDOWN =
        0 << COUNT_BITS;

private static final int STOP =
        1 << COUNT_BITS;

private static final int TIDYING =
        2 << COUNT_BITS;

private static final int TERMINATED =
        3 << COUNT_BITS;
```

这一部分不要求刚开始就背。

但是要知道：

> ThreadPoolExecutor 并不是随便放几个 boolean 表示状态，而是用了非常精巧的位运算 + CAS。

------

# 六十四、addWorker() 是干嘛的？

ThreadPoolExecutor 中一个非常核心的方法：

```java
addWorker(
    Runnable firstTask,
    boolean core
)
```

其中：

```text
firstTask
```

代表：

```text
这个 Worker 出生后首先执行哪个任务。
```

而：

```text
core
```

代表使用哪个线程数上限判断。

如果：

```java
core == true
```

判断：

```text
corePoolSize
```

如果：

```java
core == false
```

判断：

```text
maximumPoolSize
```

所以：

```java
addWorker(command, true)
```

可以理解：

```text
按核心线程标准创建 Worker。
```

而：

```java
addWorker(command, false)
```

可以理解：

```text
允许扩张到 maximumPoolSize。
```

------

# 六十五、execute() 为什么第一次调用 addWorker(command, true)？

因为：

```text
workerCount < corePoolSize
```

时：

```text
优先创建核心 Worker。
```

所以：

```java
addWorker(command, true);
```

这里 true 就是在说：

```text
线程数量不能超过 corePoolSize。
```

------

# 六十六、队列满后为什么是 addWorker(command, false)？

因为这时候：

```text
核心线程数量已经够了
+
队列也没地方
```

于是允许：

```text
突破 corePoolSize
```

但不能超过：

```text
maximumPoolSize
```

所以：

```java
addWorker(command, false);
```

------

# 六十七、ThreadPoolExecutor 的完整核心流程图

把所有知识串起来：

```text
                     submit / execute
                           │
                           ▼
               当前 workerCount
              < corePoolSize ?
                  │           │
                YES           NO
                  │           │
                  ▼           ▼
           创建核心 Worker   workQueue.offer(task)
                  │           │
                  │       ┌───┴────┐
                  │      成功      失败
                  │       │         │
                  │       │         ▼
                  │       │ workerCount
                  │       │ < maximumPoolSize ?
                  │       │       │       │
                  │       │      YES      NO
                  │       │       │       │
                  │       │       ▼       ▼
                  │       │   创建Worker  reject
                  │       │
                  │       ▼
                  │    队列等待
                  │
                  └───────────────┐
                                  ▼
                            Worker.run()
                                  │
                                  ▼
                            runWorker()
                                  │
                                  ▼
                         firstTask / getTask()
                                  │
                                  ▼
                              task.run()
                                  │
                                  ▼
                             getTask()
                                  │
                                  ├── 有任务
                                  │      ↓
                                  │   继续执行
                                  │
                                  └── 满足退出条件
                                         ↓
                                      Worker结束
```

------

# 六十八、线程池到底应该配多大？

这是实际开发最重要但也最没有固定答案的问题。

不存在：

```text
所有项目都：
core = 10
max = 20
queue = 100
```

这种万能参数。

要看：

```text
CPU
任务类型
平均执行时间
下游资源
请求量
延迟要求
内存
```

------

# 六十九、CPU 密集型任务

例如：

```text
大量计算
压缩
加密
图片计算
复杂算法
```

CPU 本身是瓶颈。

假设：

```text
CPU = 8 核
```

开：

```text
500 个线程
```

不会让 CPU：

```text
突然变 500 核。
```

反而会产生：

```text
大量上下文切换。
```

经典经验值：

```text
线程数 ≈ CPU 核数
```

或者：

```text
CPU 核数 + 1
```

例如：

```java
int cpu =
    Runtime.getRuntime()
           .availableProcessors();
```

------

# 七十、I/O 密集型任务

例如：

```text
查数据库
调用 HTTP
读取文件
调用 Redis
调用第三方 API
```

线程大量时间可能在：

```text
等待 I/O。
```

例如：

```text
线程执行 100ms

其中：

10ms 真正在用 CPU
90ms 在等网络
```

这时候：

```text
CPU 大量时间是闲着的。
```

所以可以比 CPU 核数开更多线程。

常见理论估算：

```text
线程数
≈
CPU 核数 × (1 + 等待时间 / 计算时间)
```

例如：

```text
8 核 CPU

等待 = 90ms
计算 = 10ms
```

那么：

```text
8 × (1 + 90 / 10)

= 8 × 10

= 80
```

但是：

> 这只是估算模型，不是生产环境公式答案。

实际必须结合：

```text
压测
CPU
GC
数据库连接池
接口 RT
QPS
下游容量
```

调整。

------

# 七十一、一个极其重要的现实：线程数不能只看 CPU

比如：

```text
数据库连接池只有 20 个连接
```

结果线程池：

```text
200 个线程
```

200 个线程疯狂：

```text
查数据库
```

最后：

```text
180 个线程
```

可能只是：

```text
等数据库连接。
```

所以线程池容量一定要考虑：

```text
数据库连接池
Redis 连接池
HTTP 连接池
下游服务 QPS
限流阈值
```

线程池不能脱离整个系统单独看。

------

# 七十二、队列应该设置多大？

同样没有万能答案。

一个思考方式：

```text
队列越小：
    更容易扩容线程
    更快暴露过载
    更容易拒绝

队列越大：
    能吸收短暂突发
    但任务等待时间增加
    内存风险增加
```

所以：

```text
queueCapacity
```

实际上是在决定：

> 系统愿意容忍多少“还没开始执行的积压任务”。

------

# 七十三、队列很大一定好吗？

不一定。

例如：

```text
线程处理速度：
100 个 / 秒

队列积压：
100000 个
```

那么最后一个任务：

```text
1000 秒以后才开始。
```

约：

```text
16.7 分钟。
```

虽然：

```text
任务没有被拒绝。
```

但是对于一个 HTTP 请求：

```text
16 分钟后执行
```

有什么意义？

所以：

> **不拒绝 ≠ 系统健康。**

很多时候：

```text
快速失败
```

反而比：

```text
无限排队
```

更合理。

------

# 七十四、线程池“满了”实际上是什么意思？

线程池所谓：

```text
满
```

应该理解：

```text
当前系统已经达到你人为设定的并发承载边界。
```

即：

```text
最大 Worker 已满
+
等待队列已满
```

这个时候拒绝：

```text
并不是线程池坏了。
```

恰恰说明：

> 过载保护机制生效了。

就像一个饭店：

```text
20 张桌子
+
门口允许 30 人排队
```

现在：

```text
20 张桌子坐满
30 人队伍也满
```

再来第 51 个：

```text
不能再让他进去。
```

否则无限往店里塞人：

```text
最后消防通道都堵死。
```

线程池的边界就是：

> 系统的“消防安全线”。

------

# 七十五、为什么实际开发常推荐显式创建 ThreadPoolExecutor？

因为：

```java
Executors
```

虽然使用方便：

```java
Executors.newFixedThreadPool(...)
Executors.newCachedThreadPool(...)
Executors.newSingleThreadExecutor(...)
```

但很多工厂方法内部参数比较极端。

你看不到：

```text
queue
maximumPoolSize
rejectedHandler
```

很容易忽略风险。

所以实际项目经常：

```java
new ThreadPoolExecutor(...)
```

把：

```text
核心线程数
最大线程数
队列
拒绝策略
线程名称
```

全部明确写出来。

这样更：

```text
可控
可监控
可调优
```

------

# 七十六、newFixedThreadPool 的一个坑

例如：

```java
Executors.newFixedThreadPool(10);
```

内部主要特点：

```text
corePoolSize = 10
maximumPoolSize = 10
workQueue = LinkedBlockingQueue
```

而这个 LinkedBlockingQueue 默认容量非常大。

于是：

```text
任务堆积
```

可能造成：

```text
内存问题。
```

而且：

```text
maximum == core
```

本身就不存在临时扩容空间。

------

# 七十七、newSingleThreadExecutor

大致思想：

```text
1 个线程
+
LinkedBlockingQueue
```

保证：

```text
任务基本串行执行。
```

但是同样：

```text
无界队列风险。
```

------

# 七十八、newCachedThreadPool

前面讲过：

```text
core = 0
max ≈ Integer.MAX_VALUE
queue = SynchronousQueue
```

特点：

```text
几乎不排队
处理不过来就扩线程
```

所以突发大量慢任务时：

```text
线程数量可能迅速膨胀。
```

------

# 七十九、newScheduledThreadPool 呢？

它底层不是简单普通 ThreadPoolExecutor。

核心类：

```java
ScheduledThreadPoolExecutor
```

它本身：

```text
extends ThreadPoolExecutor
```

专门支持：

```text
延迟任务
周期任务
```

例如：

```java
schedule()
scheduleAtFixedRate()
scheduleWithFixedDelay()
```

这个可以等你后面单独学。

------

# 八十、CompletableFuture 和 ThreadPoolExecutor 的关系

你后面会学：

```java
CompletableFuture
```

例如：

```java
CompletableFuture.supplyAsync(() -> {
    return 100;
});
```

默认可能使用：

```text
ForkJoinPool.commonPool()
```

但是也可以自己传：

```java
Executor
```

例如：

```java
CompletableFuture<Integer> future =
        CompletableFuture.supplyAsync(
                () -> 100,
                pool
        );
```

其中：

```java
pool
```

完全可以是：

```java
ThreadPoolExecutor。
```

所以将来：

```text
CompletableFuture
```

解决：

```text
异步任务怎么编排
怎么组合
怎么依赖
怎么异常处理
```

而：

```text
ThreadPoolExecutor
```

解决：

```text
这些任务究竟由多少线程执行
怎么排队
怎么过载保护
```

二者是不同层级。

------

# 八十一、ThreadPoolExecutor 的重要监控方法

ThreadPoolExecutor 提供很多运行指标。

------

## 81.1 getCorePoolSize()

```java
pool.getCorePoolSize();
```

查看：

```text
核心线程数配置。
```

------

## 81.2 getMaximumPoolSize()

```java
pool.getMaximumPoolSize();
```

查看：

```text
最大线程数。
```

------

## 81.3 getPoolSize()

```java
pool.getPoolSize();
```

当前实际 Worker 数量。

注意不是：

```text
corePoolSize
```

而是：

```text
此时真正存在多少个 Worker。
```

------

## 81.4 getActiveCount()

```java
pool.getActiveCount();
```

大约有多少线程：

```text
正在执行任务。
```

它属于：

```text
近似监控值
```

不要拿来做特别严格的业务判断。

------

## 81.5 getLargestPoolSize()

```java
pool.getLargestPoolSize();
```

线程池生命周期中：

```text
曾经达到过的最大线程数量。
```

例如：

```text
core = 10
max = 50
```

历史高峰：

```text
37
```

那么：

```java
getLargestPoolSize()
```

可能：

```text
37
```

这对于调优很有帮助。

------

## 81.6 getTaskCount()

```java
pool.getTaskCount();
```

返回：

```text
大致计划执行过的任务总量。
```

同样属于：

```text
近似统计。
```

------

## 81.7 getCompletedTaskCount()

```java
pool.getCompletedTaskCount();
```

表示大致：

```text
已经执行完成的任务数。
```

------

## 81.8 getQueue()

```java
pool.getQueue();
```

获取：

```java
BlockingQueue<Runnable>
```

例如：

```java
int queueSize =
        pool.getQueue().size();
```

监控：

```text
当前积压任务数量。
```

还可以：

```java
pool.getQueue().remainingCapacity();
```

查看：

```text
剩余容量。
```

注意：

`getQueue()` 更多应该用于：

```text
监控
调试
有限的管理操作
```

不要随便从业务代码中：

```text
乱操作线程池内部队列。
```

------

# 八十二、线程池生产监控应该重点看什么？

通常至少关注：

```text
corePoolSize

maximumPoolSize

poolSize

activeCount

largestPoolSize

queueSize

queueRemainingCapacity

completedTaskCount

rejectCount

taskExecutionTime
```

尤其这几个组合很有意义。

------

## 情况一

```text
activeCount 长期接近 maximum
+
queueSize 长期很大
```

说明：

```text
线程池持续过载。
```

------

## 情况二

```text
queueSize 持续增长
```

说明：

```text
生产速度 > 消费速度。
```

这是非常危险的信号。

------

## 情况三

```text
大量 rejection
```

说明：

```text
线程池容量已经经常被打满。
```

这时不一定简单粗暴：

```text
把 max 改大。
```

还要检查：

```text
任务是不是变慢了
数据库是不是慢了
第三方接口是不是慢了
线程是不是阻塞了
是不是流量异常增长
```

------

# 八十三、运行时可以修改线程池参数

ThreadPoolExecutor 很灵活。

例如：

```java
pool.setCorePoolSize(10);
```

动态修改核心线程数。

还有：

```java
pool.setMaximumPoolSize(20);
```

修改最大线程数。

```java
pool.setKeepAliveTime(
        30,
        TimeUnit.SECONDS
);
```

修改超时时间。

```java
pool.setRejectedExecutionHandler(...);
```

修改拒绝策略。

这也是一些：

```text
动态线程池框架
```

实现动态调参的基础。

------

# 八十四、beforeExecute()

ThreadPoolExecutor 提供扩展钩子：

```java
protected void beforeExecute(
        Thread t,
        Runnable r
)
```

在任务真正执行前调用。

可以用来：

```text
日志
埋点
设置上下文
计时
监控
```

例如：

```java
@Override
protected void beforeExecute(
        Thread t,
        Runnable r
) {

    super.beforeExecute(t, r);

    System.out.println(
            "准备执行：" + r
    );
}
```

------

# 八十五、afterExecute()

任务执行结束后：

```java
protected void afterExecute(
        Runnable r,
        Throwable t
)
```

可以：

```text
统计耗时
日志
异常处理
清理 ThreadLocal
```

------

## 85.1 一个特殊坑

如果任务通过：

```java
submit()
```

提交：

```text
Throwable t
```

可能：

```text
是 null
```

即使任务内部真的抛异常。

原因：

```text
FutureTask
```

已经把异常捕获并存进 Future。

所以如果想从 `afterExecute()` 检查 submit 异常，经常需要：

```java
if (t == null && r instanceof Future<?>) {

    try {

        ((Future<?>) r).get();

    } catch (ExecutionException e) {

        t = e.getCause();

    } catch (CancellationException e) {

        t = e;

    } catch (InterruptedException e) {

        Thread.currentThread().interrupt();

    }
}
```

这属于非常实战的高级知识。

------

# 八十六、terminated()

还有：

```java
protected void terminated()
```

在线程池：

```text
真正终止时
```

调用。

适合：

```text
资源清理
日志
监控
统计
```

------

# 八十七、ThreadLocal 在线程池中的巨大坑

这个和线程复用直接相关。

普通 Thread：

```text
线程结束
 ↓
ThreadLocal 数据跟着线程一起消失
```

但是线程池：

```text
Worker 不结束
```

它会：

```text
Task1
 ↓
Task2
 ↓
Task3
```

如果 Task1：

```java
threadLocal.set(userA);
```

但是忘了：

```java
threadLocal.remove();
```

Worker 执行 Task2 时：

```text
ThreadLocal 数据可能还存在。
```

于是可能：

```text
用户A的数据
污染用户B任务
```

这不仅是：

```text
内存泄漏问题
```

甚至可能是：

```text
业务数据串号
用户上下文污染
权限问题
```

所以线程池配合 ThreadLocal：

```java
try {

    threadLocal.set(value);

    // 业务

} finally {

    threadLocal.remove();

}
```

一定要养成习惯。

------

# 八十八、线程池里的任务最好不要无限阻塞

比如：

```java
pool.submit(() -> {

    while (true) {

    }

});
```

一个 Worker：

```text
永久没了。
```

如果：

```text
10 个线程
```

都被这种任务占住：

```text
整个线程池基本瘫痪。
```

还有：

```text
网络请求没有 timeout
数据库调用长时间卡死
lock 永久拿不到
Future.get() 无限等待
```

都会吃掉 Worker。

所以线程池调优不能只看：

```text
线程数量。
```

还必须保证：

```text
任务本身有合理超时。
```

------

# 八十九、线程池嵌套导致死锁

这是一个高级但很经典的问题。

假设线程池：

```text
corePoolSize = 2
maximumPoolSize = 2
```

TaskA：

```java
Future<?> future =
        pool.submit(taskB);

future.get();
```

如果两个 Worker 都正在执行：

```text
TaskA1
TaskA2
```

并且两者都：

```text
提交子任务
+
future.get()
```

那么：

```text
Worker1
等待 Child1

Worker2
等待 Child2
```

但是：

```text
Child1
Child2
```

都在队列里。

谁来执行？

```text
没有 Worker。
```

于是：

```text
Worker 等任务
任务等 Worker
```

死锁。

所以：

> **同一个有限线程池内部，“任务提交子任务然后同步等待子任务”需要特别小心。**

------

# 九十、不要把 maximumPoolSize 设置得无限大

比如：

```java
maximumPoolSize = Integer.MAX_VALUE;
```

看起来：

```text
绝不拒绝任务。
```

实际上可能：

```text
Task1 → Thread1
Task2 → Thread2
...
Task10000 → Thread10000
```

最后：

```text
内存爆炸
线程调度爆炸
CPU 上下文切换爆炸
系统无法响应
```

所以：

> 有界并发是一种保护，而不是限制。

------

# 九十一、核心线程是不是一定一直存在？

默认情况下：

```text
核心线程启动后
```

一般不会因为：

```text
空闲
```

自动超时退出。

但是存在几种情况可以消失：

```text
任务异常导致 Worker 线程终止

线程池关闭

允许 core thread timeout

其他生命周期变化
```

所以不要机械理解成：

```text
corePoolSize = 10
```

就永远物理存在：

```text
恰好 10 个 Thread。
```

更准确：

> `corePoolSize` 是线程池控制 Worker 数量时的一条核心容量边界。

------

# 九十二、“核心线程”和“非核心线程”是不是两种不同 Thread 类？

不是。

这是非常重要的概念纠正。

并不存在：

```java
CoreThread
```

和：

```java
NonCoreThread
```

两个类。

实际上都是：

```java
Thread
```

或者准确说：

```text
Worker 持有的 Thread。
```

所谓：

```text
核心线程
非核心线程
```

主要描述的是：

```text
线程数量所处范围
+
回收策略
```

不是线程对象本质不同。

------

# 九十三、核心线程一定优先执行队列任务吗？

别把核心线程和任务绑定起来。

不存在：

```text
核心任务给核心线程
临时任务给非核心线程
```

所有 Worker：

```text
都可以从同一个 workQueue 获取任务。
```

比如：

```text
Worker5
```

虽然它是在高峰期超过 corePoolSize 后创建的。

但 Task6 执行完以后：

```text
它完全可以继续从 queue 拿 Task3。
```

所以 Worker 之间本质：

```text
都是消费者。
```

------

# 九十四、为什么先排队再扩大 maximumPoolSize？

这是 Java ThreadPoolExecutor 的设计选择。

它更倾向：

```text
先控制线程数量
```

避免稍有并发：

```text
就疯狂创建线程。
```

所以：

```text
core 满
 ↓
先缓冲
 ↓
缓冲满
 ↓
说明压力真的很大
 ↓
再扩线程
```

这是一种：

```text
平稳扩容模型。
```

如果你希望：

```text
不怎么排队
尽量扩线程
```

就可以使用：

```java
SynchronousQueue
```

这也是为什么：

> workQueue 的选择，本质上是在改变线程池的扩容策略。

------

# 九十五、一个完整推荐示例

普通业务可以写成类似：

```java
AtomicInteger threadNumber =
        new AtomicInteger(1);

ThreadFactory threadFactory = task -> {

    Thread thread = new Thread(task);

    thread.setName(
            "order-worker-"
            + threadNumber.getAndIncrement()
    );

    return thread;
};

ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
                4,
                8,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
```

使用：

```java
executor.execute(() -> {

    System.out.println(
            Thread.currentThread().getName()
            + " 正在处理订单"
    );

});
```

关闭：

```java
executor.shutdown();
```

------

# 九十六、这个线程池参数应该怎么读？

```java
new ThreadPoolExecutor(
        4,
        8,
        60,
        TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(200),
        threadFactory,
        new ThreadPoolExecutor.CallerRunsPolicy()
);
```

翻译成人话：

```text
平常最多先维持 4 个核心 Worker。

4 个 Worker 都已经创建之后，
新任务优先进入最多容纳 200 个任务的等待队列。

如果 200 个位置全部排满，
继续扩容 Worker。

Worker 最多扩到 8 个。

超过 4 个的额外 Worker，
如果空闲 60 秒，
允许被销毁。

线程由 threadFactory 创建。

如果：
8 个 Worker 都顶满
+
200 个等待位也满
那么：
由任务提交者自己执行任务。
```

只要你能把一个 ThreadPoolExecutor 配置：

> **完整翻译成人话**

你基本就真的懂 7 大参数了。

------

# 九十七、经典面试题：线程池 7 大参数是什么？

```text
corePoolSize
    核心线程数

maximumPoolSize
    最大线程数

keepAliveTime
    空闲额外线程最大存活时间

unit
    keepAliveTime 的时间单位

workQueue
    等待执行任务的阻塞队列

threadFactory
    创建 Worker Thread 的工厂

handler
    无法接收任务时的拒绝策略
```

------

# 九十八、经典面试题：任务进入线程池后的执行流程？

标准回答：

```text
1. workerCount < corePoolSize
   创建 Worker 执行任务。

2. 否则尝试把任务加入 workQueue。

3. 如果 workQueue 已满，
   且 workerCount < maximumPoolSize，
   创建额外 Worker 执行任务。

4. 如果：
   workQueue 满
   +
   workerCount 达到 maximumPoolSize

   执行拒绝策略。
```

再补一句：

```text
线程池 shutdown 后提交新任务同样会拒绝。
```

会更加完整。

------

# 九十九、经典面试题：线程池什么时候扩容？

不是：

```text
核心线程忙了就扩。
```

而是：

```text
当前 Worker 已经达到 corePoolSize
+
workQueue 无法继续接收任务
+
当前 Worker 数量还小于 maximumPoolSize
```

才扩。

------

# 一百、经典面试题：最大线程数为什么可能永远没用？

因为使用：

```text
无界 BlockingQueue
```

以后：

```text
任务可以一直进入队列。
```

队列：

```text
offer 很难失败。
```

所以：

```text
创建超过 corePoolSize 的 Worker
```

这一逻辑几乎不会触发。

------

# 一百零一、经典面试题：线程池四大拒绝策略？

```text
AbortPolicy
    抛 RejectedExecutionException

CallerRunsPolicy
    调用者线程自己执行

DiscardPolicy
    直接丢弃，不抛异常

DiscardOldestPolicy
    丢弃队列中最前面的任务，再尝试提交当前任务
```

------

# 一百零二、经典面试题：shutdown 和 shutdownNow 区别？

### shutdown()

```text
不接收新任务
继续完成已提交任务
不会粗暴停止正在执行任务
```

### shutdownNow()

```text
不接收新任务
尝试 interrupt 正在执行的 Worker
队列中的未执行任务被取出并返回
```

但：

```text
shutdownNow 也不能保证任务一定停止。
```

因为：

```text
interrupt 是协作式中断。
```

------

# 一百零三、经典面试题：execute 和 submit 区别？

### execute

```text
Executor 定义

只接收 Runnable

无返回值
```

### submit

```text
ExecutorService 定义

支持：
Runnable
Callable

返回 Future
```

内部：

```text
Callable / Runnable
 ↓
FutureTask
 ↓
execute()
```

另外异常处理方式也不同。

------

# 一百零四、经典面试题：为什么线程池能复用线程？

因为 Worker 不是：

```text
执行一个 task.run()
然后退出
```

而是：

```text
while (...) {

    Runnable task = getTask();

    task.run();

}
```

不断从：

```text
workQueue
```

获取任务。

------

# 一百零五、经典面试题：线程池为什么使用 BlockingQueue？

因为它天然适合：

```text
生产者—消费者模型。
```

生产者：

```text
业务线程提交任务。
```

消费者：

```text
Worker。
```

中间：

```text
BlockingQueue<Runnable>。
```

队列为空：

```text
消费者可以阻塞等待。
```

队列有任务：

```text
消费者被唤醒执行。
```

------

# 一百零六、经典面试题：ArrayBlockingQueue 和 LinkedBlockingQueue 在线程池中怎么选？

### ArrayBlockingQueue

```text
有界
内存可控
更容易进行过载保护
```

### LinkedBlockingQueue

如果：

```java
new LinkedBlockingQueue<>(capacity)
```

也可以有界使用。

但是：

```java
new LinkedBlockingQueue<>()
```

默认容量极大：

```text
容易造成任务积压
maximumPoolSize 很难发挥作用
严重时可能 OOM
```

所以生产环境要：

> 明确知道自己到底想不想要“几乎无界队列”。

------

# 一百零七、经典面试题：SynchronousQueue 为什么容量是 0 还能用于线程池？

因为：

```text
它不存任务。
```

而是：

```text
生产线程
   ↓
直接把任务交给消费线程
```

没有消费者接：

```text
offer 失败。
```

ThreadPoolExecutor 于是：

```text
尝试创建 Worker。
```

所以：

```text
SynchronousQueue
```

会让 ThreadPoolExecutor 更积极：

```text
扩线程
```

而不是：

```text
堆积任务。
```

------

# 一百零八、ThreadPoolExecutor 最重要的几个思想

如果最后只记核心思想，可以记下面几个。

------

## 思想一：线程和任务分离

不是：

```text
一个任务一个线程。
```

而是：

```text
固定/受控的一批 Worker
重复执行大量任务。
```

------

## 思想二：有界资源

线程：

```text
有上限。
```

队列：

```text
最好也有合理上限。
```

线程池的本质不是：

```text
让无限任务都执行。
```

而是：

> 控制有限机器资源如何处理任务。

------

## 思想三：生产者—消费者

```text
业务线程
    ↓
BlockingQueue
    ↓
Worker
```

ThreadPoolExecutor 本质上就是一个非常成熟的：

```text
生产者—消费者实现。
```

------

## 思想四：先核心，后队列，再扩容

牢记：

```text
core
 ↓
queue
 ↓
maximum
 ↓
reject
```

------

## 思想五：maximumPoolSize 和 queue 强关联

不能单独看：

```text
maximumPoolSize = 100
```

还必须看：

```text
workQueue 是什么。
```

如果：

```text
无界队列
```

那么 maximum 可能形同虚设。

------

## 思想六：拒绝策略是保护机制

拒绝不是：

```text
线程池失败。
```

而是：

```text
系统已经超过设计承载能力。
```

如果不拒绝：

```text
无限排队
无限线程
```

最后可能：

```text
整个 JVM 一起死。
```

------

## 思想七：ThreadPoolExecutor 不是可靠任务仓库

如果任务：

```text
绝对不能丢
```

并且可能大量积压：

```text
不要企图用 JVM 内存无限缓存。
```

应该：

```text
MQ / DB / 磁盘持久化
+
ThreadPoolExecutor
```

各司其职。

------

# 一百零九、把你目前学过的并发知识彻底串起来

你现在其实已经可以画出一条很漂亮的链路：

```text
Runnable
Callable
    │
    │ 描述“任务是什么”
    ▼

FutureTask
    │
    │ Runnable + Future
    │ 把 Callable 包装成可执行、可获取结果的任务
    ▼

Executor
    │
    │ execute(Runnable)
    │ 定义“执行任务”能力
    ▼

ExecutorService
    │
    │ submit / shutdown / Future
    │ 定义完整线程池服务能力
    ▼

AbstractExecutorService
    │
    │ 实现 submit
    │ Callable → FutureTask → execute
    ▼

ThreadPoolExecutor
    │
    │ 真正控制线程
    │
    ├── corePoolSize
    ├── maximumPoolSize
    ├── keepAliveTime
    ├── ThreadFactory
    ├── RejectedExecutionHandler
    │
    ▼

BlockingQueue<Runnable>
    │
    │ 保存暂时执行不了的任务
    ▼

Worker
    │
    │ 一个 Worker 持有一个 Thread
    ▼

Thread
    │
    │ 不断：
    │ getTask()
    │ task.run()
    ▼

真正执行 Runnable / FutureTask
```

再把 Future 接上：

```text
Callable
    ↓
FutureTask
    ↓
ThreadPoolExecutor
    ↓
Worker
    ↓
FutureTask.run()
    ↓
Callable.call()
    ↓
结果保存到 FutureTask
    ↓
future.get()
    ↓
调用线程取得结果
```

这样：

```text
Runnable
Callable
Future
FutureTask
Executor
ExecutorService
BlockingQueue
ThreadPoolExecutor
Thread
```

这些原来看起来零零散散的类，就全部连成了一套完整体系。

------

# 一百一十、最终脑图

```text
                    ThreadPoolExecutor
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
      线程管理            任务管理           生命周期
        │                  │                  │
        │                  │                  ├─ RUNNING
        │                  │                  ├─ SHUTDOWN
        │                  │                  ├─ STOP
        │                  │                  ├─ TIDYING
        │                  │                  └─ TERMINATED
        │                  │
        │                  ├─ Runnable
        │                  ├─ FutureTask
        │                  └─ BlockingQueue
        │
        ├─ corePoolSize
        ├─ maximumPoolSize
        ├─ keepAliveTime
        ├─ allowCoreThreadTimeOut
        ├─ ThreadFactory
        └─ Worker

任务提交：

execute / submit
       │
       ▼
workerCount < core ?
       │
    YES│NO
       │
       ▼
   创建Worker      workQueue.offer
                       │
                    ┌──┴──┐
                   成功   失败
                    │      │
                    │      ▼
                    │ workerCount < max ?
                    │      │
                    │   ┌──┴──┐
                    │  YES    NO
                    │   │      │
                    │   ▼      ▼
                    │ 创建   Reject
                    │ Worker
                    ▼
                  排队

Worker：

firstTask
    ↓
task.run()
    ↓
getTask()
    ↓
BlockingQueue
    ↓
task.run()
    ↓
getTask()
    ↓
...
```

------

# 一百一十一、最后必须背下来的核心口诀

### 线程池接收任务

```text
核心没满 → 创建线程

核心满了 → 进入队列

队列满了 → 扩线程

线程到最大 → 拒绝
```

即：

```text
core
→ queue
→ maximum
→ reject
```

### 线程回收

```text
超过 corePoolSize 的空闲 Worker

等待 keepAliveTime

仍没有任务

退出
```

### submit

```text
Callable / Runnable
        ↓
FutureTask
        ↓
execute
        ↓
ThreadPoolExecutor
        ↓
Worker
        ↓
run
        ↓
Future.get()
```

### shutdown

```text
不再接新任务
但继续处理已经提交的任务
```

### shutdownNow

```text
不接新任务
尝试 interrupt 正在执行任务
返回队列中尚未开始的任务
```

------

# 一百一十二、一句话理解 ThreadPoolExecutor

如果让我把整个 `ThreadPoolExecutor` 压缩成一句话：

> **ThreadPoolExecutor 就是通过“有限数量的可复用 Worker + BlockingQueue + 扩容规则 + 回收规则 + 拒绝策略”，把无限可能到来的任务约束在有限机器资源能够承受的范围之内。**

所以它真正解决的，不只是：

```text
“怎么开几个线程”
```

而是：

> **在并发环境中，如何管理任务、线程、内存、吞吐量、延迟和系统过载之间的关系。**