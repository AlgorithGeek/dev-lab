# ScheduledThreadPoolExecutor 详细笔记

## 一、ScheduledThreadPoolExecutor 是什么？

`ScheduledThreadPoolExecutor` 是 Java JUC 中专门用于：

- 延迟执行任务
- 周期性执行任务

的线程池。

完整类名：

```java
java.util.concurrent.ScheduledThreadPoolExecutor
```

继承关系：

```text
Executor
   ↑
ExecutorService
   ↑
AbstractExecutorService
   ↑
ThreadPoolExecutor
   ↑
ScheduledThreadPoolExecutor
```

所以本质上：

```text
ScheduledThreadPoolExecutor
=
ThreadPoolExecutor
+
定时调度能力
```

也就是说，它本身仍然是：

```text
线程池
```

只不过任务不是简单地：

```text
提交 → 排队 → 执行
```

而是：

```text
提交
 ↓
等待指定时间
 ↓
到达执行时间
 ↓
线程池线程执行
```

或者：

```text
提交
 ↓
等待
 ↓
执行
 ↓
再次等待
 ↓
再次执行
 ↓
……
```

------

# 二、为什么需要 ScheduledThreadPoolExecutor？

假设现在有一个需求：

```java
5 秒之后执行某个任务
```

最简单的方式可能想到：

```java
new Thread(() -> {
    try {
        Thread.sleep(5000);
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }

    System.out.println("执行任务");
}).start();
```

虽然能实现，但是问题很多。

例如：

```text
一个任务
→ 创建一个线程

1000 个定时任务
→ 创建 1000 个线程
```

这显然不好。

而且：

```java
Thread.sleep()
```

实际上是在占用一个线程等待。

ScheduledThreadPoolExecutor 则是：

```text
大量定时任务
       ↓
统一放进任务队列
       ↓
线程池统一调度
       ↓
到时间以后再执行
```

这样可以：

```text
复用线程
控制线程数量
统一管理任务
支持取消
支持周期任务
支持 Future
支持 shutdown
```

------

# 三、ScheduledThreadPoolExecutor 和 Timer

在以前的 Java 中，经常使用：

```java
Timer
TimerTask
```

例如：

```java
Timer timer = new Timer();

timer.schedule(new TimerTask() {
    @Override
    public void run() {
        System.out.println("执行");
    }
}, 3000);
```

但是现代 Java 并发开发中，一般更推荐：

```java
ScheduledThreadPoolExecutor
```

或者：

```java
ScheduledExecutorService
```

主要原因之一是：

```text
Timer 默认只有一个线程
```

假设：

```text
任务A执行 10 秒
任务B本来应该 3 秒后执行
```

Timer：

```text
Timer线程
   │
   ├── 执行A：10秒
   │
   └── B只能继续等
```

而 ScheduledThreadPoolExecutor：

```text
线程1 → A
线程2 → B
线程3 → C
```

只要线程池中有可用线程，就可以并发执行多个定时任务。

------

# 四、ScheduledExecutorService

实际开发时，你经常不会直接使用：

```java
ScheduledThreadPoolExecutor
```

作为变量类型。

而是：

```java
ScheduledExecutorService
```

例如：

```java
ScheduledExecutorService pool =
        Executors.newScheduledThreadPool(4);
```

原因和：

```java
ExecutorService pool
```

类似。

它是面向接口编程。

继承关系：

```text
Executor
   ↑
ExecutorService
   ↑
ScheduledExecutorService
```

接口定义大致为：

```java
public interface ScheduledExecutorService
        extends ExecutorService {

    public ScheduledFuture<?> schedule(
            Runnable command,
            long delay,
            TimeUnit unit);

    public <V> ScheduledFuture<V> schedule(
            Callable<V> callable,
            long delay,
            TimeUnit unit);

    public ScheduledFuture<?> scheduleAtFixedRate(
            Runnable command,
            long initialDelay,
            long period,
            TimeUnit unit);

    public ScheduledFuture<?> scheduleWithFixedDelay(
            Runnable command,
            long initialDelay,
            long delay,
            TimeUnit unit);
}
```

这里最核心的就是四个方法：

```text
schedule()
schedule()
scheduleAtFixedRate()
scheduleWithFixedDelay()
```

------

# 五、创建 ScheduledThreadPoolExecutor

## 1. 直接 new

```java
ScheduledThreadPoolExecutor pool =
        new ScheduledThreadPoolExecutor(4);
```

这里的：

```java
4
```

就是：

```text
corePoolSize
```

也就是核心线程数量。

------

## 2. Executors 工厂方法

```java
ScheduledExecutorService pool =
        Executors.newScheduledThreadPool(4);
```

内部实际上就是：

```java
new ScheduledThreadPoolExecutor(4);
```

简化理解：

```text
Executors.newScheduledThreadPool(4)

        ↓

ScheduledThreadPoolExecutor
```

------

# 六、schedule()：延迟执行一次

## Runnable 版本

```java
ScheduledFuture<?> future =
        pool.schedule(
                () -> {
                    System.out.println("执行任务");
                },
                3,
                TimeUnit.SECONDS
        );
```

意思：

```text
提交任务
 ↓
等待至少 3 秒
 ↓
执行一次
 ↓
结束
```

注意：

```text
不是每 3 秒执行一次
```

而是：

```text
3 秒以后执行一次
```

------

# 七、schedule() 的 Callable 版本

也可以提交：

```java
Callable
```

例如：

```java
ScheduledFuture<Integer> future =
        pool.schedule(
                () -> {
                    System.out.println("执行");
                    return 100;
                },
                3,
                TimeUnit.SECONDS
        );
```

之后：

```java
Integer result = future.get();
```

这里：

```java
future.get()
```

仍然可能阻塞。

例如：

```text
0秒：
提交任务

0~3秒：
任务还没到执行时间

3秒：
开始执行

3.1秒：
执行完成
```

如果你在：

```text
第1秒调用 future.get()
```

那么线程会一直等到：

```text
第3秒任务执行完成
```

之后才拿到结果。

------

# 八、schedule() 的 delay 是什么含义？

例如：

```java
pool.schedule(task, 5, TimeUnit.SECONDS);
```

准确含义是：

```text
至少 5 秒以后，这个任务才有资格执行。
```

不是说：

```text
精确第 5.000000 秒一定开始执行。
```

假设线程池：

```java
corePoolSize = 1
```

此时：

```text
任务A：执行10秒
任务B：5秒后执行
```

可能出现：

```text
0秒：
线程开始执行A

5秒：
B已经到时间
但是没有线程

10秒：
A完成

10秒：
B才开始执行
```

所以：

```text
delay = 最早可以执行的时间
```

而不是：

```text
绝对保证执行时间
```

这一点非常重要。

------

# 九、ScheduledFuture 是什么？

`schedule()` 返回：

```java
ScheduledFuture
```

继承关系：

```text
Future
   ↑
ScheduledFuture
```

所以它首先就是一个：

```text
Future
```

意味着可以：

```java
get()
cancel()
isDone()
isCancelled()
```

除此之外：

```java
ScheduledFuture
```

还继承了：

```java
Delayed
```

所以：

```text
ScheduledFuture
=
Future能力
+
延迟任务能力
```

可以简单理解：

```text
Future
    ↓
管理异步任务结果

ScheduledFuture
    ↓
管理“延迟/周期异步任务”
```

------

# 十、scheduleAtFixedRate()

这是 ScheduledThreadPoolExecutor 中非常重要的方法。

方法：

```java
scheduleAtFixedRate(
    Runnable command,
    long initialDelay,
    long period,
    TimeUnit unit
)
```

例如：

```java
pool.scheduleAtFixedRate(
        () -> {
            System.out.println(System.currentTimeMillis());
        },
        2,
        3,
        TimeUnit.SECONDS
);
```

意思：

```text
2 秒之后第一次执行

之后：

每隔 3 秒按照固定频率执行
```

这里的核心关键词是：

```text
FixedRate
固定频率
```

------

# 十一、scheduleAtFixedRate 的时间计算

假设：

```java
initialDelay = 2 秒
period = 3 秒
```

理论执行时间：

```text
第 2 秒
第 5 秒
第 8 秒
第 11 秒
第 14 秒
……
```

它关注的是：

```text
任务理论上的开始时间
```

可以理解成：

```text
下一次计划时间
=
上一次计划时间
+
period
```

注意是：

```text
上一次“计划时间”
```

而不是：

```text
上一次执行结束时间
```

------

# 十二、如果任务执行时间小于 period

例如：

```text
period = 5 秒

任务执行需要 2 秒
```

那么：

```text
0秒：开始第一次
2秒：第一次结束

5秒：开始第二次
7秒：第二次结束

10秒：开始第三次
12秒：第三次结束
```

图：

```text
第一次
0 ├────2
       ↓等待

第二次
5 ├────7
       ↓等待

第三次
10├────12
```

它努力保持：

```text
0
5
10
15
20
```

这样的固定开始节奏。

------

# 十三、如果任务执行时间大于 period

这是最容易搞错的地方之一。

例如：

```text
period = 3秒
任务执行需要 5秒
```

理论时间：

```text
0
3
6
9
12
……
```

但是第一次执行：

```text
0~5 秒
```

到了：

```text
第3秒
```

第一次还没有结束。

那么怎么办？

它不会：

```text
同时启动第二个相同周期任务
```

也不会：

```text
多个相同任务重叠执行
```

而是：

```text
第一次结束
 ↓
发现第二次计划时间已经到了
 ↓
尽快执行第二次
```

可能变成：

```text
0秒：第一次开始
5秒：第一次结束

5秒：第二次立即开始
10秒：第二次结束

10秒：第三次立即开始
15秒：第三次结束
```

因此：

```text
scheduleAtFixedRate
```

并不意味着：

```text
无论如何每 3 秒创建一个并发任务
```

而是：

```text
同一个周期任务不会并发重叠执行。
```

------

# 十四、scheduleWithFixedDelay()

方法：

```java
scheduleWithFixedDelay(
    Runnable command,
    long initialDelay,
    long delay,
    TimeUnit unit
)
```

例如：

```java
pool.scheduleWithFixedDelay(
        () -> {
            System.out.println("执行");
        },
        2,
        3,
        TimeUnit.SECONDS
);
```

意思：

```text
2秒以后执行第一次

第一次执行结束
 ↓
等待3秒
 ↓
第二次

第二次执行结束
 ↓
等待3秒
 ↓
第三次
```

关键词：

```text
FixedDelay
固定延迟
```

------

# 十五、scheduleAtFixedRate 和 scheduleWithFixedDelay 的本质区别

这是本章最重要的知识之一。

## scheduleAtFixedRate

关注：

```text
开始时间 → 开始时间
```

例如：

```text
每 5 秒启动一次
```

理论：

```text
0
5
10
15
20
```

------

## scheduleWithFixedDelay

关注：

```text
结束时间 → 下一次开始时间
```

例如：

```text
任务执行完以后，再等 5 秒。
```

假设任务执行：

```text
2秒
```

那么：

```text
0秒开始
2秒结束

等待5秒

7秒开始
9秒结束

等待5秒

14秒开始
```

------

# 十六、用时间线彻底理解两个方法

假设：

```text
任务执行时间：2秒
间隔：5秒
```

## FixedRate

```text
0秒      5秒      10秒      15秒
│        │         │         │
执行     执行      执行      执行

任务：

0──2

5──7

10──12

15──17
```

关注：

```text
每隔5秒尝试开始
```

------

## FixedDelay

```text
0──2
   ↓
 等5秒
   ↓
   7──9
      ↓
    等5秒
      ↓
      14──16
```

实际开始时间：

```text
0
7
14
21
```

因为：

```text
任务执行2秒
+
delay 5秒
=
7秒
```

------

# 十七、一句话记住区别

可以直接记：

```text
scheduleAtFixedRate：

上一轮“计划开始时间”
        ↓
      + period
        ↓
下一轮计划开始时间
```

而：

```text
scheduleWithFixedDelay：

上一轮“实际执行结束时间”
        ↓
      + delay
        ↓
下一轮开始时间
```

所以：

```text
FixedRate
更像钟表

FixedDelay
更像“做完 → 歇一会 → 再做”
```

------

# 十八、周期任务会不会并发执行自己？

一般不会。

例如：

```java
pool.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);
```

虽然：

```text
period = 1秒
```

但是：

```text
task每次执行10秒
```

不会出现：

```text
线程1执行 task 第一次

1秒以后
线程2执行 task 第二次

2秒以后
线程3执行 task 第三次
```

同一个周期任务不会这样叠加。

而是：

```text
第一次完成
 ↓
第二次
 ↓
第二次完成
 ↓
第三次
```

这是一个非常重要的安全特性。

否则如果任务执行速度：

```text
慢于任务产生速度
```

那任务可能疯狂堆积。

------

# 十九、不同周期任务可以并发执行

注意：

```text
同一个周期任务不会重叠
```

不代表：

```text
所有周期任务都不能并发
```

例如：

```java
pool.scheduleAtFixedRate(taskA, 0, 1, TimeUnit.SECONDS);

pool.scheduleAtFixedRate(taskB, 0, 1, TimeUnit.SECONDS);
```

如果线程池：

```java
new ScheduledThreadPoolExecutor(2);
```

那么完全可能：

```text
线程1 → taskA
线程2 → taskB
```

同时运行。

------

# 二十、周期任务只能使用 Runnable

注意 API：

```java
scheduleAtFixedRate(Runnable ...)
```

和：

```java
scheduleWithFixedDelay(Runnable ...)
```

都只接收：

```java
Runnable
```

没有：

```java
Callable
```

版本。

为什么？

因为普通一次性任务：

```java
执行
 ↓
返回结果
 ↓
Future.get()
```

非常清晰。

但是周期任务：

```text
第一次结果
第二次结果
第三次结果
第四次结果
……
```

那 Future 应该返回哪个？

语义会很复杂。

所以周期任务主要表达：

```text
重复执行某个动作
```

而不是：

```text
反复产生一个单独结果
```

------

# 二十一、周期任务的 ScheduledFuture.get()

例如：

```java
ScheduledFuture<?> future =
        pool.scheduleAtFixedRate(
                task,
                0,
                1,
                TimeUnit.SECONDS
        );
```

这时候：

```java
future.get()
```

很危险。

因为周期任务正常情况下：

```text
永远不会完成
```

所以：

```java
future.get()
```

通常会一直阻塞。

直到：

```text
任务被取消
```

或者：

```text
任务异常结束
```

等等。

因此周期任务通常主要使用：

```java
cancel()
```

而不是：

```java
get()
```

------

# 二十二、取消周期任务

例如：

```java
ScheduledFuture<?> future =
        pool.scheduleAtFixedRate(
                () -> System.out.println("执行"),
                0,
                1,
                TimeUnit.SECONDS
        );
```

之后：

```java
future.cancel(false);
```

表示：

```text
取消后续调度
```

如果任务当前正在执行：

```text
不主动中断当前执行
```

------

如果：

```java
future.cancel(true);
```

则表示：

```text
允许尝试 interrupt 当前正在执行任务的线程
```

注意：

```text
true
```

只是：

```text
允许中断
```

并不是：

```text
强制杀死线程
```

最终任务是否真正结束，还要看任务有没有：

```java
响应 interrupt
```

------

# 二十三、周期任务异常是一个大坑

假设：

```java
pool.scheduleAtFixedRate(() -> {

    System.out.println("执行");

    throw new RuntimeException("出错了");

}, 0, 1, TimeUnit.SECONDS);
```

很多初学者以为：

```text
这一次出错
 ↓
下一秒继续执行
```

实际上不是。

周期任务中：

```text
只要某一次执行抛出了未捕获异常
```

后续周期执行会：

```text
直接停止
```

也就是：

```text
第一次：正常
第二次：异常
第三次：不会执行
第四次：不会执行
……
```

因此：

```text
周期任务内部异常处理非常重要。
```

------

# 二十四、周期任务推荐自己捕获异常

例如：

```java
pool.scheduleAtFixedRate(() -> {

    try {

        doSomething();

    } catch (Exception e) {

        e.printStackTrace();

    }

}, 0, 10, TimeUnit.SECONDS);
```

这样：

```text
某一次执行异常
```

不会因为异常冒出去而直接终止整个周期调度。

业务代码里经常是：

```java
try {
    ...
} catch (Exception e) {
    log.error("定时任务执行失败", e);
}
```

------

# 二十五、为什么有时候异常日志都没看到？

这也是 ScheduledThreadPoolExecutor 很容易踩的坑。

线程池任务内部的异常：

```text
可能被 Future 封装
```

而不是像：

```java
new Thread(...)
```

那样直接明显地打印出来。

所以如果：

```text
周期任务莫名其妙不执行了
```

首先应该检查：

```text
上一次执行是不是抛异常了？
```

------

# 二十六、ScheduledThreadPoolExecutor 使用什么队列？

普通 ThreadPoolExecutor 常见：

```text
ArrayBlockingQueue
LinkedBlockingQueue
SynchronousQueue
```

而 ScheduledThreadPoolExecutor 默认使用的是：

```java
DelayedWorkQueue
```

完整理解：

```text
ScheduledThreadPoolExecutor
        │
        └── DelayedWorkQueue
```

它是一个：

```text
基于延迟时间排序的阻塞队列
```

------

# 二十七、DelayedWorkQueue 是什么？

可以把它理解成：

```text
专门存“未来任务”的队列
```

比如现在：

```text
任务A：10秒后执行
任务B：3秒后执行
任务C：7秒后执行
```

队列会按照：

```text
谁最早到期
```

排序。

最终：

```text
B
C
A
```

也就是类似：

```text
队头永远尽量放最早应该执行的任务
```

------

# 二十八、DelayedWorkQueue 和普通 FIFO 队列的区别

普通队列通常强调：

```text
先进先出
```

例如：

```text
A
B
C
```

那么：

```text
A → B → C
```

但是 ScheduledThreadPoolExecutor 不主要看：

```text
谁先提交
```

而是看：

```text
谁应该更早执行
```

例如：

```text
先提交A：30秒后执行

后提交B：2秒后执行
```

虽然：

```text
A先进入队列
```

但是：

```text
B会排到更前面
```

因为：

```text
B更早到期
```

------

# 二十九、DelayedWorkQueue 本质上类似优先级队列

底层思想可以理解成：

```text
最小堆
```

按照：

```text
任务下一次执行时间
```

排序。

大致：

```text
           最早执行
              A
            /   \
           B     C
          / \
         D   E
```

队头：

```text
永远是当前最早应该执行的任务
```

------

# 三十、ScheduledFutureTask

ScheduledThreadPoolExecutor 内部不会直接把你的：

```java
Runnable
```

原封不动放进队列。

而是会包装成类似：

```text
ScheduledFutureTask
```

它本质上同时具备：

```text
Runnable
Future
Delayed
ScheduledFuture
```

相关能力。

可以简化理解成：

```text
Runnable / Callable
        ↓
ScheduledFutureTask
        ↓
DelayedWorkQueue
```

这和普通线程池里的：

```text
Callable
   ↓
FutureTask
```

很像。

只不过 ScheduledThreadPoolExecutor 多了：

```text
什么时候执行
是否周期执行
周期是多少
```

这些信息。

------

# 三十一、普通 schedule 任务内部思路

例如：

```java
pool.schedule(task, 5, TimeUnit.SECONDS);
```

内部大致：

```text
Runnable
   ↓
包装成 ScheduledFutureTask
   ↓
记录执行时间：
now + 5 秒
   ↓
放进 DelayedWorkQueue
   ↓
线程池等待
   ↓
时间到达
   ↓
从队列取出
   ↓
执行
```

------

# 三十二、周期任务是如何实现重复执行的？

这里非常重要。

很多人可能会想：

```text
周期任务是不是一次性创建了无限多个任务？
```

不是。

比如：

```java
scheduleAtFixedRate(task, 0, 5, SECONDS)
```

不是：

```text
Task1
Task2
Task3
Task4
Task5
……
```

提前全部创建。

而更像：

```text
任务对象执行
 ↓
执行完成
 ↓
计算下一次时间
 ↓
重新把自己放回 DelayedWorkQueue
 ↓
等待
 ↓
再次执行
```

也就是说：

```text
执行 → 重新入队 → 等待 → 执行
```

循环。

------

# 三十三、周期任务和 FutureTask 一次性原则冲突吗？

不冲突。

普通：

```java
FutureTask
```

一般语义是：

```text
执行一次
```

但是 ScheduledThreadPoolExecutor 内部使用的：

```text
ScheduledFutureTask
```

对周期任务做了特殊处理。

普通一次性任务：

```text
run()
```

之后：

```text
完成
```

而周期任务会采用类似：

```text
runAndReset()
```

的机制。

也就是说：

```text
执行任务逻辑
 ↓
但是不永久进入“已完成”状态
 ↓
重置
 ↓
重新调度
```

因此：

```text
普通 FutureTask：
一次性

周期 ScheduledFutureTask：
可以执行 → reset → 再执行
```

------

# 三十四、scheduleAtFixedRate 的下一次时间计算

核心思想：

```text
下一次执行时间
=
当前计划执行时间
+
period
```

例如：

```text
第一次理论时间：0

period = 5秒
```

那么：

```text
第二次：5
第三次：10
第四次：15
第五次：20
```

所以它追求：

```text
固定频率
```

------

# 三十五、scheduleWithFixedDelay 的下一次时间计算

核心：

```text
下一次执行时间
=
当前执行结束时间
+
delay
```

例如：

```text
当前任务：

0秒开始
2秒结束

delay = 5秒
```

那么：

```text
下一次：
7秒
```

所以它追求：

```text
每次完成以后休息固定时间
```

------

# 三十六、ScheduledThreadPoolExecutor 的 corePoolSize 很重要

例如：

```java
new ScheduledThreadPoolExecutor(4);
```

表示：

```text
最多主要使用 4 个核心工作线程
```

对于 ScheduledThreadPoolExecutor 来说：

```text
corePoolSize
```

尤其重要。

因为它的工作队列：

```java
DelayedWorkQueue
```

本质上是一个：

```text
无界队列
```

因此普通 ThreadPoolExecutor 里的：

```text
corePoolSize
maximumPoolSize
workQueue
```

那套扩容逻辑，在这里表现得不太一样。

------

# 三十七、maximumPoolSize 在 ScheduledThreadPoolExecutor 中意义很弱

普通 ThreadPoolExecutor：

```text
核心线程满
 ↓
队列满
 ↓
创建非核心线程
 ↓
直到 maximumPoolSize
```

但是 ScheduledThreadPoolExecutor：

```text
DelayedWorkQueue 是无界队列
```

所以：

```text
队列几乎不会出现“满”
```

因此通常不会触发：

```text
队列满 → 扩容到 maximumPoolSize
```

所以官方使用 ScheduledThreadPoolExecutor 时，真正核心的是：

```text
corePoolSize
```

而：

```text
maximumPoolSize
```

基本没有普通 ThreadPoolExecutor 中那么重要。

------

# 三十八、不要把 corePoolSize 设置成 0

理论上可以：

```java
new ScheduledThreadPoolExecutor(0);
```

但通常不推荐。

因为 ScheduledThreadPoolExecutor 本质上依赖：

```text
核心线程处理延迟任务
```

实际业务中应该配置合理数量的：

```text
corePoolSize
```

------

# 三十九、ScheduledThreadPoolExecutor 与 ThreadPoolExecutor 的提交过程区别

普通 ThreadPoolExecutor：

```text
execute(task)

如果 worker < corePoolSize
    ↓
创建核心线程

否则
    ↓
workQueue.offer()

如果队列满
    ↓
尝试扩容到 maximumPoolSize
```

而 ScheduledThreadPoolExecutor 更像：

```text
schedule(task)
    ↓
包装 ScheduledFutureTask
    ↓
直接加入 DelayedWorkQueue
    ↓
保证有核心线程负责等待/执行
```

因为：

```text
DelayedWorkQueue
```

本身负责：

```text
按照延迟时间管理任务
```

------

# 四十、普通 execute() 还能不能用？

可以。

因为：

```java
ScheduledThreadPoolExecutor
extends ThreadPoolExecutor
```

所以：

```java
pool.execute(task);
```

当然可以。

还有：

```java
pool.submit(task);
```

也可以。

实际上可以理解：

```text
execute(task)

≈

schedule(task, 0, NANOSECONDS)
```

也就是说：

```text
立即执行任务
```

可以看成：

```text
延迟时间 = 0
```

的特殊定时任务。

------

# 四十一、submit() 也相当于 0 延迟

例如：

```java
Future<Integer> future =
        pool.submit(() -> 100);
```

也完全可以。

ScheduledThreadPoolExecutor 并不是只能执行：

```text
延时任务
```

它一样可以当普通线程池使用。

不过如果你只是需要普通线程池：

```java
ThreadPoolExecutor
```

通常更加语义明确。

------

# 四十二、拒绝策略什么时候发生？

ScheduledThreadPoolExecutor 同样有：

```java
RejectedExecutionHandler
```

例如：

```java
new ScheduledThreadPoolExecutor(
        4,
        new ThreadPoolExecutor.AbortPolicy()
);
```

但是因为：

```text
DelayedWorkQueue 是无界的
```

通常不会因为：

```text
队列满
```

触发拒绝。

最常见触发拒绝的情况是：

```text
线程池已经 shutdown
```

然后继续提交任务。

例如：

```java
pool.shutdown();

pool.schedule(task, 1, TimeUnit.SECONDS);
```

可能抛：

```java
RejectedExecutionException
```

------

# 四十三、shutdown() 之后会发生什么？

```java
pool.shutdown();
```

意思仍然是：

```text
不再接收新任务
```

但是已经提交的任务怎么处理，要进一步区分。

尤其 ScheduledThreadPoolExecutor 有：

```text
延迟任务
周期任务
```

所以它提供了一些额外策略。

------

# 四十四、shutdown 之后是否执行已有延迟任务？

默认情况下：

```text
已经提交的“一次性延迟任务”
```

通常仍然会执行。

例如：

```java
pool.schedule(task, 10, TimeUnit.SECONDS);

pool.shutdown();
```

默认情况下：

```text
task依然可以在10秒后执行
```

这个行为可以配置。

方法：

```java
setExecuteExistingDelayedTasksAfterShutdownPolicy(boolean value)
```

默认：

```java
true
```

也就是：

```text
shutdown 后仍执行已经存在的一次性延迟任务
```

如果设置：

```java
pool.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
```

那么 shutdown 后：

```text
未到期的延迟任务可以被取消
```

------

# 四十五、shutdown 之后周期任务怎么办？

方法：

```java
setContinueExistingPeriodicTasksAfterShutdownPolicy(boolean value)
```

默认值：

```java
false
```

意味着：

```text
调用 shutdown()
以后

周期任务默认不会继续无限运行
```

这很合理。

否则：

```java
shutdown()
```

可能永远无法真正终止线程池。

如果你显式设置：

```java
pool.setContinueExistingPeriodicTasksAfterShutdownPolicy(true);
```

那么 shutdown 后：

```text
已有周期任务仍然可以继续执行
```

------

# 四十六、两个 shutdown 策略一定要区分

一次性延迟任务：

```java
setExecuteExistingDelayedTasksAfterShutdownPolicy(...)
```

默认：

```text
true
```

周期任务：

```java
setContinueExistingPeriodicTasksAfterShutdownPolicy(...)
```

默认：

```text
false
```

可以记：

```text
一次性延迟任务：
都已经答应人家了
默认继续做

周期任务：
没完没了
shutdown 后默认停止
```

------

# 四十七、shutdownNow()

```java
pool.shutdownNow();
```

语义仍然比：

```java
shutdown()
```

更强。

主要行为：

```text
尝试 interrupt 正在执行任务

取消/移除等待中的任务

不再接受新任务
```

但是：

```text
interrupt 仍然不是强制杀死线程
```

任务必须配合处理中断。

------

# 四十八、取消任务以后会不会立刻从队列删除？

这是一个比较实用的问题。

例如：

```java
ScheduledFuture<?> future =
        pool.schedule(task, 1, TimeUnit.HOURS);

future.cancel(false);
```

任务虽然已经：

```text
cancelled
```

但是默认情况下：

```text
这个任务对象可能暂时仍然存在 DelayedWorkQueue 中
```

直到：

```text
它本来应该到期
```

或者：

```text
线程池做其他清理
```

这可能导致：

```text
大量已取消长延迟任务
仍然占内存
```

------

# 四十九、setRemoveOnCancelPolicy

可以开启：

```java
pool.setRemoveOnCancelPolicy(true);
```

意思：

```text
任务一旦 cancel
 ↓
尽快从 DelayedWorkQueue 删除
```

对于：

```text
大量动态创建/取消定时任务
```

的系统，这个设置很有价值。

例如：

```java
ScheduledThreadPoolExecutor pool =
        new ScheduledThreadPoolExecutor(4);

pool.setRemoveOnCancelPolicy(true);
```

------

# 五十、实际开发推荐的初始化

例如：

```java
ScheduledThreadPoolExecutor scheduler =
        new ScheduledThreadPoolExecutor(
                4,
                new ThreadFactory() {

                    private final AtomicInteger index =
                            new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {

                        Thread thread =
                                new Thread(
                                        r,
                                        "scheduler-" +
                                        index.getAndIncrement()
                                );

                        return thread;
                    }
                }
        );

scheduler.setRemoveOnCancelPolicy(true);
```

这样：

```text
线程有名字
取消任务及时清理
线程数量明确
```

会比直接：

```java
Executors.newScheduledThreadPool(4)
```

更加可控。

------

# 五十一、业务示例：延迟通知

例如：

```java
scheduler.schedule(() -> {

    sendMessage();

}, 10, TimeUnit.SECONDS);
```

可以用于：

```text
10 秒后发送通知
```

------

# 五十二、业务示例：定时检查

例如：

```java
scheduler.scheduleWithFixedDelay(() -> {

    try {

        checkSomething();

    } catch (Exception e) {

        log.error("检查任务失败", e);

    }

}, 0, 30, TimeUnit.SECONDS);
```

表示：

```text
执行检查
 ↓
检查完成
 ↓
等30秒
 ↓
再次检查
```

这种场景特别适合：

```text
轮询
健康检查
资源扫描
缓存刷新
状态检查
```

------

# 五十三、业务示例：固定频率采样

比如：

```java
scheduler.scheduleAtFixedRate(() -> {

    collectMetrics();

}, 0, 10, TimeUnit.SECONDS);
```

适合：

```text
每10秒采集一次指标
```

因为它更强调：

```text
时间节奏
```

------

# 五十四、什么时候用 FixedRate？

适合：

```text
希望任务尽量按照固定时间频率发生
```

例如：

```text
每10秒采集一次指标

每1分钟更新一次统计值

固定频率心跳
```

它强调：

```text
时间点
```

------

# 五十五、什么时候用 FixedDelay？

适合：

```text
必须保证上一次做完以后
再休息一段时间
然后做下一次
```

例如：

```text
轮询远程接口

扫描数据库

检查队列

后台同步数据
```

假设远程接口执行时间不确定：

```text
2秒
10秒
30秒
```

如果用：

```text
FixedDelay
```

则可以保证：

```text
请求完成
 ↓
休息10秒
 ↓
下一次请求
```

这种通常更加稳妥。

------

# 五十六、为什么业务轮询经常更适合 FixedDelay？

比如你写：

```text
每10秒查询一次第三方接口
```

如果使用：

```java
scheduleAtFixedRate(...)
```

但是某一次请求：

```text
花了30秒
```

线程会产生一种：

```text
任务已经严重落后
```

的状态。

执行完以后可能马上继续追赶下一轮。

而 FixedDelay：

```text
这一次完成
 ↓
再等10秒
 ↓
下一轮
```

更容易控制：

```text
对数据库
对第三方接口
对资源系统
```

的压力。

------

# 五十七、ScheduledThreadPoolExecutor 不适合什么场景？

需要特别注意：

```text
它是 JVM 内存级定时器
```

意味着任务信息主要存在：

```text
当前 Java 进程内存中
```

如果 JVM：

```text
崩溃
重启
服务器宕机
重新发布
```

内存里的任务：

```text
就没有了
```

因此：

```text
非常重要、必须可靠执行的业务定时任务
```

不能仅靠 ScheduledThreadPoolExecutor。

------

# 五十八、例如哪些任务不应该只靠它？

比如：

```text
订单30分钟自动取消

付款超时关闭订单

优惠券到期

贷款扣款

必须执行的财务任务

跨服务器可靠调度
```

如果 JVM 重启以后任务直接丢了：

```text
业务不能接受
```

这种场景通常要结合：

```text
数据库
Redis
消息队列
延迟消息
Quartz
XXL-JOB
分布式调度平台
```

等机制。

------

# 五十九、它适合 JVM 内部短生命周期任务

比如：

```text
连接超时检查

缓存延迟刷新

本地状态扫描

心跳

指标采集

临时重试

后台轮询

内存任务维护
```

这些场景通常很适合：

```java
ScheduledThreadPoolExecutor
```

------

# 六十、它和 @Scheduled 是什么关系？

Spring 中经常见：

```java
@Scheduled(fixedRate = 5000)
public void task() {

}
```

这是：

```text
Spring提供的定时任务抽象
```

而：

```java
ScheduledThreadPoolExecutor
```

是：

```text
JDK 原生并发工具
```

两者不是同一个东西。

但 Spring 的调度底层也会使用：

```text
TaskScheduler
ScheduledExecutorService
```

这一类机制。

可以粗略理解：

```text
ScheduledThreadPoolExecutor
        ↓
JDK底层线程调度能力

@Scheduled
        ↓
Spring给你的更方便的声明式定时任务
```

------

# 六十一、ScheduledThreadPoolExecutor 和 CompletableFuture

两者方向不同。

ScheduledThreadPoolExecutor：

```text
重点：
什么时候执行？
多久执行一次？
```

CompletableFuture：

```text
重点：
异步任务之间怎么组合？
A完成以后做B
A+B完成以后做C
异常怎么处理
多个异步任务怎么编排
```

所以：

```text
ScheduledThreadPoolExecutor
=
时间调度

CompletableFuture
=
异步任务编排
```

两者不是上下级关系。

------

# 六十二、ScheduledThreadPoolExecutor 和 BlockingQueue

你学过 BlockingQueue 以后会很好理解这一部分。

普通 ThreadPoolExecutor：

```text
生产者
 ↓
提交任务
 ↓
BlockingQueue
 ↓
消费者Worker线程
```

ScheduledThreadPoolExecutor：

```text
生产者
 ↓
提交定时任务
 ↓
DelayedWorkQueue
 ↓
等待任务到期
 ↓
Worker线程
 ↓
执行任务
```

区别就是：

```text
普通 BlockingQueue：
有任务就可以拿

DelayedWorkQueue：
即使有任务
没到时间也不能拿出来执行
```

------

# 六十三、DelayedWorkQueue.take() 的直觉

假设：

```text
任务A：10秒后
任务B：20秒后
```

线程调用：

```java
queue.take();
```

不会马上拿到 A。

而是：

```text
看看队头A

距离A执行：
还有10秒

线程等待

10秒到了

取出A
```

如果期间突然来了：

```text
任务C：2秒后
```

那么：

```text
C成为新的队头
```

等待策略也会相应变化。

------

# 六十四、Leader-Follower 优化思想

DelayedWorkQueue 内部有一个比较有意思的优化：

```text
Leader-Follower
```

简单来说：

如果有很多线程都在等：

```text
队头任务什么时候到期
```

没必要所有线程都进行：

```text
定时等待
```

否则会有很多线程：

```text
同时超时醒来
同时抢锁
```

造成资源浪费。

因此：

```text
选一个 leader 线程
```

专门：

```text
等待队头任务到期
```

其他线程：

```text
普通等待
```

当 leader 完成以后：

```text
再唤醒其他线程
```

这属于 ScheduledThreadPoolExecutor 比较深入的实现细节。

日常业务：

```text
知道思想即可
```

------

# 六十五、DelayedWorkQueue 为什么使用时间差而不是绝对时间？

定时任务内部通常不是简单依赖：

```java
System.currentTimeMillis()
```

去做延迟计算。

JDK 更适合使用：

```java
System.nanoTime()
```

去计算：

```text
时间间隔
```

原因是：

```text
System.currentTimeMillis()
```

表示：

```text
现实世界时间
```

可能受到：

```text
系统校时
NTP
人工修改时间
```

影响。

而：

```java
System.nanoTime()
```

主要适合计算：

```text
经过了多久
```

因此更适合：

```text
延迟调度
```

------

# 六十六、定时任务 ≠ 精确定时器

一定要建立这个认知：

```text
ScheduledThreadPoolExecutor
不是实时操作系统级精确定时器
```

例如：

```java
schedule(task, 1, MILLISECONDS)
```

并不保证：

```text
精确1毫秒以后执行
```

会受到：

```text
CPU调度
GC
线程池是否繁忙
操作系统线程调度
当前任务执行时间
JVM暂停
```

等因素影响。

所以它保证的更接近：

```text
不到时间不会提前执行

到时间以后尽快执行
```

------

# 六十七、周期任务不要执行特别慢的阻塞逻辑

例如：

```java
scheduleAtFixedRate(() -> {

    调第三方接口，可能卡2分钟

}, 0, 5, SECONDS);
```

这是危险设计。

因为：

```text
period = 5秒
```

但：

```text
执行时间 = 120秒
```

调度完全失去意义。

这种情况应该考虑：

```text
设置调用超时

使用 FixedDelay

拆分任务

使用异步执行

合理增加线程

做并发控制
```

------

# 六十八、线程池大小怎么设置？

例如：

```text
只有几个非常轻量的任务
```

可能：

```java
corePoolSize = 1 ~ 2
```

就够了。

如果：

```text
很多任务会进行IO
HTTP
数据库查询
```

可能需要更多线程。

但是不能机械地说：

```text
任务100个 → 线程100个
```

仍然要考虑：

```text
CPU
IO等待
数据库连接池
HTTP连接池
下游承载能力
任务频率
任务执行时间
```

------

# 六十九、定时任务线程一定要命名

不要让线程都叫：

```text
pool-1-thread-1
pool-1-thread-2
```

建议：

```text
order-timeout-scheduler-1
metrics-scheduler-1
cache-refresh-scheduler-1
```

这样排查日志时非常有帮助。

例如：

```java
ThreadFactory factory = r -> {

    Thread t =
            new Thread(r);

    t.setName("cache-refresh-scheduler");

    return t;
};
```

实际可以使用：

```text
AtomicInteger
```

给线程编号。

------

# 七十、不要在任务中吞掉异常

错误：

```java
try {

    doSomething();

} catch (Exception e) {

}
```

这样虽然周期任务不会停止，但是：

```text
错误完全看不到
```

正确：

```java
try {

    doSomething();

} catch (Exception e) {

    log.error("定时任务执行失败", e);

}
```

------

# 七十一、周期任务的异常处理策略

业务里通常推荐：

```java
scheduler.scheduleWithFixedDelay(() -> {

    try {

        process();

    } catch (Exception e) {

        log.error("任务执行失败", e);

    }

}, 0, 30, TimeUnit.SECONDS);
```

整个任务入口：

```text
最好有总兜底异常处理
```

尤其是周期任务。

------

# 七十二、重复提交和周期执行是两回事

假设：

```java
Runnable task = () ->
        System.out.println("执行");
```

你写：

```java
scheduler.schedule(task, 1, SECONDS);

scheduler.schedule(task, 1, SECONDS);

scheduler.schedule(task, 1, SECONDS);
```

这是：

```text
提交了3个独立的一次性任务
```

所以会执行：

```text
3次
```

而：

```java
scheduler.scheduleAtFixedRate(
        task,
        1,
        1,
        SECONDS
);
```

是：

```text
提交1个周期任务
```

这个任务：

```text
反复调度自己
```

两者不要混淆。

------

# 七十三、和 FutureTask 的理解连起来

普通：

```java
submit(callable)
```

大致：

```text
Callable
 ↓
FutureTask
 ↓
线程池
```

ScheduledThreadPoolExecutor：

```java
schedule(callable)
```

大致：

```text
Callable
 ↓
ScheduledFutureTask
 ↓
DelayedWorkQueue
 ↓
线程池
```

因此整体思想仍然没变：

```text
用户任务
 ↓
包装成线程池内部任务对象
 ↓
排队
 ↓
Worker执行
```

只不过现在队列多了一层：

```text
时间调度
```

------

# 七十四、一个完整 Demo

```java
import java.util.concurrent.*;

public class ScheduledPoolDemo {

    public static void main(String[] args)
            throws InterruptedException {

        ScheduledThreadPoolExecutor scheduler =
                new ScheduledThreadPoolExecutor(2);

        scheduler.setRemoveOnCancelPolicy(true);

        // 3秒后执行一次
        scheduler.schedule(() -> {

            System.out.println(
                    "一次性任务：" +
                    Thread.currentThread().getName()
            );

        }, 3, TimeUnit.SECONDS);


        // 2秒后开始，每5秒按照固定频率执行
        ScheduledFuture<?> fixedRateFuture =
                scheduler.scheduleAtFixedRate(() -> {

                    try {

                        System.out.println(
                                "FixedRate：" +
                                Thread.currentThread().getName()
                        );

                    } catch (Exception e) {

                        e.printStackTrace();

                    }

                }, 2, 5, TimeUnit.SECONDS);


        // 2秒后开始，每次完成以后等待5秒
        ScheduledFuture<?> fixedDelayFuture =
                scheduler.scheduleWithFixedDelay(() -> {

                    try {

                        System.out.println(
                                "FixedDelay：" +
                                Thread.currentThread().getName()
                        );

                    } catch (Exception e) {

                        e.printStackTrace();

                    }

                }, 2, 5, TimeUnit.SECONDS);


        Thread.sleep(20000);

        fixedRateFuture.cancel(false);
        fixedDelayFuture.cancel(false);

        scheduler.shutdown();
    }
}
```

------

# 七十五、四个核心 API 总结

## schedule(Runnable)

```java
schedule(
    Runnable command,
    long delay,
    TimeUnit unit
)
```

作用：

```text
延迟一段时间
执行一次
```

------

## schedule(Callable)

```java
schedule(
    Callable<V> callable,
    long delay,
    TimeUnit unit
)
```

作用：

```text
延迟一段时间
执行一次
返回结果
```

------

## scheduleAtFixedRate

```java
scheduleAtFixedRate(
    Runnable command,
    long initialDelay,
    long period,
    TimeUnit unit
)
```

作用：

```text
按照固定频率重复执行
```

关注：

```text
计划开始时间
```

------

## scheduleWithFixedDelay

```java
scheduleWithFixedDelay(
    Runnable command,
    long initialDelay,
    long delay,
    TimeUnit unit
)
```

作用：

```text
任务执行完成以后
等待固定时间
再执行下一次
```

关注：

```text
任务结束时间
```

------

# 七十六、最终脑图

```text
ScheduledThreadPoolExecutor
│
├── 本质
│      │
│      └── ThreadPoolExecutor
│             +
│          定时调度
│
├── 接口
│      │
│      └── ScheduledExecutorService
│
├── 创建
│      │
│      ├── new ScheduledThreadPoolExecutor(n)
│      │
│      └── Executors.newScheduledThreadPool(n)
│
├── 一次性任务
│      │
│      ├── schedule(Runnable)
│      │
│      └── schedule(Callable)
│
├── 周期任务
│      │
│      ├── scheduleAtFixedRate
│      │      │
│      │      └── 固定频率
│      │
│      └── scheduleWithFixedDelay
│             │
│             └── 完成后固定等待
│
├── 返回值
│      │
│      └── ScheduledFuture
│             │
│             ├── get
│             ├── cancel
│             ├── isDone
│             └── isCancelled
│
├── 工作队列
│      │
│      └── DelayedWorkQueue
│             │
│             ├── 延迟队列
│             ├── 按到期时间排序
│             └── 本质类似最小堆
│
├── 内部任务
│      │
│      └── ScheduledFutureTask
│             │
│             ├── 一次性任务
│             └── 周期任务
│                    │
│                    └── 执行后重新入队
│
├── 线程参数
│      │
│      ├── corePoolSize 很重要
│      │
│      └── maximumPoolSize 意义较弱
│
├── 异常
│      │
│      └── 周期任务出现未捕获异常
│             ↓
│          后续执行停止
│
├── 取消
│      │
│      ├── Future.cancel()
│      │
│      └── setRemoveOnCancelPolicy(true)
│
└── shutdown
       │
       ├── 一次性延迟任务
       │     默认继续
       │
       └── 周期任务
             默认停止
```

------

# 七十七、必须掌握的核心结论

如果只记住这一章最重要的内容，至少记住下面这些：

```text
1.
ScheduledThreadPoolExecutor
=
ThreadPoolExecutor + 定时调度能力
2.
schedule()
=
延迟执行一次
3.
scheduleAtFixedRate()
=
按照固定频率调度

关注：
计划开始时间
4.
scheduleWithFixedDelay()
=
上一次执行结束以后
等待固定时间
再执行

关注：
实际结束时间
5.
同一个周期任务不会自己并发重叠执行。
6.
周期任务只要某一次抛出未捕获异常：

后续周期执行会直接停止。
```

所以业务代码通常：

```java
try {
    ...
} catch (Exception e) {
    log.error(...);
}
7.
ScheduledThreadPoolExecutor 使用：

DelayedWorkQueue
```

不是普通：

```text
FIFO BlockingQueue
```

而是：

```text
按照任务到期时间排序
8.
周期任务不是提前创建无限多个任务。

而是：

执行
 ↓
计算下一次时间
 ↓
重新入队
 ↓
再次执行
9.
DelayedWorkQueue 是无界队列，

所以 ScheduledThreadPoolExecutor 中：

corePoolSize 很重要

maximumPoolSize 基本没有普通
ThreadPoolExecutor 中那么大的意义。
10.
ScheduledThreadPoolExecutor 是 JVM 内存级调度器。

JVM 重启后：

任务不会天然持久化。
```

所以：

```text
重要业务任务
不能只依赖它保证可靠性。
```

------

# 七十八、学习到什么程度就够了？

日常 Java 后端开发中，对 ScheduledThreadPoolExecutor 建议掌握到：

```text
第一层：必须掌握

schedule
scheduleAtFixedRate
scheduleWithFixedDelay
ScheduledFuture
cancel
shutdown
```

然后：

```text
第二层：必须理解

FixedRate 和 FixedDelay 的时间区别

周期任务不会自身并发

异常会终止周期任务

DelayedWorkQueue
```

再然后：

```text
第三层：了解源码思想

ScheduledFutureTask
周期任务重新入队
runAndReset
nanoTime
Leader-Follower
```

至于：

```text
DelayedWorkQueue 具体堆操作源码

ScheduledFutureTask 每个字段

Leader线程完整源码

decorateTask
```

这些暂时：

```text
不需要死磕
```

对于普通 Java 业务开发来说，理解原理即可。