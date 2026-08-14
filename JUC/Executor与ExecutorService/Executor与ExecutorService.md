# Executor 与 ExecutorService 超详细笔记

> 本章核心目标：
>
> 1. 理解为什么 Java 要设计 `Executor`
> 2. 理解 `Executor` 到底解决了什么问题
> 3. 理解 `ExecutorService` 为什么会在 `Executor` 基础上继续扩展
> 4. 掌握 `execute()`、`submit()`、`shutdown()`、`shutdownNow()`、`invokeAll()`、`invokeAny()` 等核心 API
> 5. 理清 `Executor`、`ExecutorService`、`Future`、`ThreadPoolExecutor` 之间的关系
> 6. 为后面学习真正的线程池 `ThreadPoolExecutor` 打基础

------

# 一、先看整个 Executor 体系的位置

在 Java 并发编程中，我们最开始学习线程时，通常会这样创建线程：

```java
Thread thread = new Thread(() -> {
    System.out.println("执行任务");
});

thread.start();
```

或者：

```java
Runnable task = () -> {
    System.out.println("执行任务");
};

new Thread(task).start();
```

这种方式本身没有问题。

但是如果项目中有大量任务：

```java
new Thread(task1).start();
new Thread(task2).start();
new Thread(task3).start();
new Thread(task4).start();
```

就会逐渐出现很多问题。

例如：

```text
任务越来越多
    ↓
不断创建 Thread
    ↓
线程创建、销毁成本越来越高
    ↓
线程数量难以控制
    ↓
可能创建成千上万个线程
    ↓
CPU、内存、线程调度压力暴涨
```

除此之外还有一个设计层面的问题。

以前我们写：

```java
new Thread(task).start();
```

实际上把两件事情绑死在一起了：

```text
任务是什么
+
任务怎么执行
```

例如：

```java
Runnable task = () -> {
    System.out.println("发送邮件");
};

new Thread(task).start();
```

这里：

```text
发送邮件
```

属于：

> 业务任务。

而：

```text
new Thread(...).start()
```

属于：

> 任务执行策略。

Java 希望把这两个东西拆开。

于是就出现了：

```text
Executor
```

------

# 二、Executor 是什么？

`Executor` 位于：

```java
java.util.concurrent
```

它是一个非常非常简单的接口。

源码核心就是：

```java
public interface Executor {

    void execute(Runnable command);

}
```

整个接口只有一个方法：

```java
void execute(Runnable command);
```

所以从 API 数量来看：

```text
Executor 非常简单
```

但是从设计思想来看：

```text
Executor 非常重要
```

因为它确立了 Java 并发框架中一个非常重要的思想：

> **任务提交者不应该关心任务具体是怎么执行的。**

------

# 三、Executor 最核心的设计思想：任务与执行机制解耦

以前：

```java
Runnable task = () -> {
    System.out.println("执行任务");
};

new Thread(task).start();
```

调用者明确指定：

```text
这个任务必须创建一个新线程执行
```

也就是说：

```text
任务
和
执行方式
```

耦合到了一起。

有了 `Executor` 之后：

```java
Executor executor = ...;

executor.execute(task);
```

调用者只负责：

```text
我这里有一个任务，请帮我执行。
```

至于：

```text
到底创建新线程？
使用线程池？
当前线程直接执行？
延迟执行？
排队等待？
```

调用者不用关心。

这就是 Executor 最大的意义。

------

# 四、一个非常重要的理解

看到：

```java
executor.execute(task);
```

千万不要自动理解成：

```text
创建一个新线程执行 task
```

这是错误的。

`Executor` 只规定：

> 执行这个任务。

但：

> **没有规定怎么执行。**

例如，我们完全可以自己写一个 Executor。

------

# 五、Executor 可以直接在当前线程执行任务

例如：

```java
Executor executor = command -> command.run();

executor.execute(() -> {
    System.out.println(Thread.currentThread().getName());
});
```

假设当前线程是：

```text
main
```

那么输出可能就是：

```text
main
```

为什么？

因为：

```java
command.run();
```

只是普通方法调用。

并没有：

```java
new Thread()
```

也没有：

```java
start()
```

所以：

```text
execute()
≠ 必然异步执行
execute()
≠ 必然创建线程
execute()
≠ 必然使用线程池
```

这一点非常重要。

------

# 六、也可以写一个每次创建新线程的 Executor

例如：

```java
Executor executor = command -> {
    new Thread(command).start();
};
```

调用：

```java
executor.execute(() -> {
    System.out.println(Thread.currentThread().getName());
});
```

这时候就是：

```text
execute()
    ↓
new Thread(command)
    ↓
start()
```

所以任务会在新线程中运行。

------

# 七、Executor 本质上是什么？

可以把 `Executor` 理解成：

> **任务执行能力的最顶层抽象。**

它只表达：

```text
我可以接收一个 Runnable
然后把它执行掉
```

至于怎么执行：

```text
Executor 不管。
```

因此：

```java
public interface Executor {

    void execute(Runnable command);

}
```

其实可以翻译成：

```text
给我一个任务，我负责执行。
```

------

# 八、Executor 为什么只接受 Runnable？

因为 `Executor` 是整个任务执行框架最底层、最基础的抽象。

它只关心：

```text
执行任务
```

所以使用：

```java
Runnable
```

即可。

而 `Runnable`：

```java
public interface Runnable {

    void run();

}
```

没有返回值。

因此：

```java
Executor
```

本身也没有提供：

```text
任务返回结果
```

相关能力。

如果你希望：

```text
提交 Callable
获取返回值
获取 Future
关闭线程池
等待任务结束
```

这些都已经超过 `Executor` 的职责范围。

于是就有了：

```java
ExecutorService
```

------

# 九、Executor 的局限性

Executor 只有：

```java
void execute(Runnable command);
```

所以它能表达的能力非常有限。

例如：

```java
Executor executor = ...;

executor.execute(task);
```

我们无法通过 `Executor` 接口本身知道：

```text
任务完成了吗？
```

也没有：

```text
任务返回值
```

更没有：

```text
关闭执行器
```

之类的 API。

因此 Executor 缺少很多实际开发需要的能力。

------

# 十、为什么需要 ExecutorService？

实际开发中我们不仅需要：

```text
执行 Runnable
```

通常还会需要：

```text
提交 Callable
```

例如：

```java
Callable<Integer> task = () -> {
    return 100;
};
```

我们还希望：

```text
拿到任务结果
```

例如：

```java
Future<Integer> future = ...
```

除此之外，线程池还有生命周期。

比如：

```text
什么时候关闭？
是否已经关闭？
任务是否全部执行结束？
要不要立即停止？
```

Executor 根本没有这些 API。

所以 Java 在 Executor 基础上又设计了：

```java
ExecutorService
```

------

# 十一、ExecutorService 是什么？

`ExecutorService` 是一个接口。

它继承：

```java
Executor
```

关系：

```text
Executor
    ↑
ExecutorService
```

也就是说：

```java
ExecutorService extends Executor
```

因此：

> 所有 ExecutorService 都是 Executor。

所以 ExecutorService 天生拥有：

```java
execute(Runnable)
```

同时又增加了很多更强大的能力。

------

# 十二、Executor 和 ExecutorService 的关系

可以先粗略理解成：

```text
Executor
    │
    │ 最基础能力
    │
    └── 执行 Runnable
            ↓
ExecutorService
    │
    │ 增强能力
    │
    ├── submit()
    ├── Future
    ├── shutdown()
    ├── shutdownNow()
    ├── invokeAll()
    ├── invokeAny()
    └── 生命周期管理
```

所以：

```text
Executor
```

更像：

> 执行任务的最基础规范。

而：

```text
ExecutorService
```

则更像：

> 一个真正可以管理任务、管理生命周期的任务执行服务。

------

# 十三、ExecutorService 核心 API 总览

ExecutorService 中非常重要的方法主要可以分成三大类。

------

## 第一类：提交任务

```java
execute()
submit()
invokeAll()
invokeAny()
```

------

## 第二类：关闭 ExecutorService

```java
shutdown()
shutdownNow()
```

------

## 第三类：查询生命周期状态

```java
isShutdown()
isTerminated()
awaitTermination()
```

所以可以整理成：

```text
ExecutorService
│
├── 任务提交
│   ├── execute()
│   ├── submit()
│   ├── invokeAll()
│   └── invokeAny()
│
└── 生命周期管理
    ├── shutdown()
    ├── shutdownNow()
    ├── isShutdown()
    ├── isTerminated()
    └── awaitTermination()
```

------

# 十四、execute() 方法

因为：

```java
ExecutorService extends Executor
```

所以 ExecutorService 继承了：

```java
void execute(Runnable command);
```

使用方式：

```java
ExecutorService executorService = ...;

executorService.execute(() -> {
    System.out.println("执行任务");
});
```

注意：

```text
execute()
```

接受：

```java
Runnable
```

返回：

```java
void
```

所以：

```java
execute()
```

适合：

> 我只想让你执行这个任务，我不关心结果。

------

# 十五、submit() 方法

相比 `execute()`：

```java
submit()
```

是 ExecutorService 中非常重要的增强功能。

它可以提交：

```text
Runnable
```

也可以提交：

```text
Callable
```

并且：

> 会返回 `Future`。

------

# 十六、submit(Callable)

例如：

```java
ExecutorService executorService = ...;

Future<Integer> future =
        executorService.submit(() -> {
            return 100;
        });
```

这里：

```java
submit(...)
```

提交了一个：

```java
Callable<Integer>
```

任务。

Callable 返回：

```java
100
```

而 submit 返回：

```java
Future<Integer>
```

然后：

```java
Integer result = future.get();
```

最终获得：

```text
100
```

整个过程：

```text
Callable<Integer>
        ↓
ExecutorService.submit()
        ↓
Future<Integer>
        ↓
异步执行 Callable
        ↓
Future 保存任务执行状态/结果
        ↓
future.get()
        ↓
获得结果
```

------

# 十七、submit(Runnable)

`submit()` 同样可以提交 Runnable：

```java
Future<?> future =
        executorService.submit(() -> {
            System.out.println("执行任务");
        });
```

但是 Runnable 本身：

```java
void run()
```

没有返回值。

所以 Future 最终通常：

```java
future.get()
```

得到：

```text
null
```

例如：

```java
Object result = future.get();

System.out.println(result);
```

输出：

```text
null
```

------

# 十八、submit(Runnable, result)

ExecutorService 还有一种比较特殊的 submit：

```java
<T> Future<T> submit(
        Runnable task,
        T result
);
```

例如：

```java
Future<String> future =
        executorService.submit(
                () -> {
                    System.out.println("任务执行");
                },
                "SUCCESS"
        );
```

然后：

```java
String result = future.get();
```

结果：

```text
SUCCESS
```

注意：

```text
SUCCESS
```

不是 Runnable 自己返回的。

因为 Runnable 根本无法返回结果。

实际上表达的是：

```text
如果这个 Runnable 正常执行完成
那么 Future.get() 返回我预先指定的 SUCCESS
```

------

# 十九、execute() 和 submit() 的核心区别

这是 ExecutorService 学习中非常重要的知识点。

------

## execute()

```java
executor.execute(runnable);
```

特点：

```text
只能提交 Runnable
返回 void
无法通过返回值拿到 Future
```

------

## submit()

```java
Future<?> future =
        executor.submit(runnable);
```

或者：

```java
Future<Integer> future =
        executor.submit(callable);
```

特点：

```text
可以提交 Runnable
可以提交 Callable
返回 Future
可以获取任务状态
可以获取任务结果
可以取消任务
```

------

# 二十、execute 和 submit 对比表

| 对比         | execute                | submit              |
| ------------ | ---------------------- | ------------------- |
| 所属         | Executor               | ExecutorService     |
| Runnable     | 支持                   | 支持                |
| Callable     | 不支持                 | 支持                |
| 返回值       | void                   | Future              |
| 获取执行结果 | 不支持                 | 支持                |
| 取消任务     | 无法通过返回值取消     | 可以通过 Future     |
| 查询任务状态 | 不支持                 | 可以通过 Future     |
| 异常处理方式 | 通常直接由执行线程处理 | 通常封装进入 Future |

------

# 二十一、execute 和 submit 的异常差异

这是一个非常容易忽略的知识点。

假设任务：

```java
Runnable task = () -> {
    throw new RuntimeException("出错了");
};
```

------

## 使用 execute()

```java
executorService.execute(task);
```

如果任务内部抛出未捕获异常：

```text
RuntimeException
```

通常异常会从执行该任务的工作线程中抛出来。

也就是说：

```text
任务异常
    ↓
工作线程执行 run()
    ↓
异常没有被捕获
    ↓
走线程的未捕获异常处理机制
```

------

## 使用 submit()

```java
Future<?> future =
        executorService.submit(task);
```

这时候任务的异常通常会：

```text
被封装进 Future
```

你如果完全不调用：

```java
future.get();
```

可能会发现：

> 控制台甚至没有非常明显地看到异常。

但是调用：

```java
future.get();
```

就会抛：

```java
ExecutionException
```

真正的业务异常在：

```java
e.getCause()
```

里面。

例如：

```java
try {

    future.get();

} catch (ExecutionException e) {

    Throwable cause = e.getCause();

    System.out.println(cause);
}
```

所以需要注意：

> `submit()` 不是把异常吃掉了，而是把任务异常保存进了 Future。

------

# 二十二、为什么 submit 可以返回 Future？

因为 ExecutorService 相比 Executor 已经开始管理：

```text
任务本身的生命周期
```

任务不仅仅是：

```text
执行一下
```

还会拥有：

```text
未开始
运行中
正常完成
异常完成
被取消
```

等状态。

而：

```java
Future
```

就是调用方观察和控制这个任务的重要入口。

例如：

```java
future.isDone();
future.isCancelled();
future.cancel(true);
future.get();
```

所以可以理解成：

```text
ExecutorService
负责管理任务执行

Future
负责代表一次任务
```

------

# 二十三、ExecutorService 不等于线程池

这一点一定要注意。

很多初学者会直接认为：

```text
ExecutorService = 线程池
```

严格来说不对。

因为：

```java
ExecutorService
```

只是一个：

> 接口。

它定义了一组：

```text
任务提交 + 生命周期管理
```

的规范。

真正实现线程池功能的类之一是：

```java
ThreadPoolExecutor
```

关系：

```text
Executor
    ↑
ExecutorService
    ↑
AbstractExecutorService
    ↑
ThreadPoolExecutor
```

所以：

```text
ExecutorService
```

是规范。

而：

```text
ThreadPoolExecutor
```

才是真正具体实现。

------

# 二十四、完整的 Executor 体系结构

后面你会逐渐接触：

```text
Executor
│
└── ExecutorService
      │
      ├── AbstractExecutorService
      │       │
      │       └── ThreadPoolExecutor
      │               │
      │               └── ScheduledThreadPoolExecutor
      │
      └── ScheduledExecutorService
```

其中：

```text
Executor
```

负责定义：

```text
执行任务
```

------

```text
ExecutorService
```

负责增加：

```text
任务提交
Future
生命周期管理
批量任务
```

------

```text
AbstractExecutorService
```

负责：

```text
提供 ExecutorService 的一些公共实现
```

------

```text
ThreadPoolExecutor
```

是真正核心的：

```text
线程池实现
```

------

```text
ScheduledExecutorService
```

增加：

```text
延迟执行
周期执行
```

------

```text
ScheduledThreadPoolExecutor
```

则是：

```text
定时线程池的具体实现
```

------

# 二十五、ExecutorService 最大的第二个能力：生命周期管理

Executor 本身只有：

```java
execute()
```

所以它没有：

```text
关闭
```

这个概念。

但是实际线程池内部可能存在：

```text
核心线程
工作线程
任务队列
等待任务
正在执行任务
```

如果不关闭：

```text
某些工作线程可能一直存活
```

所以 ExecutorService 引入了一整套生命周期管理 API。

核心就是：

```java
shutdown();
```

和：

```java
shutdownNow();
```

------

# 二十六、shutdown() 是什么？

调用：

```java
executorService.shutdown();
```

表示：

> **优雅关闭 ExecutorService。**

注意：

```text
shutdown()
```

不是：

> 立即杀死所有线程。

而是：

```text
不再接受新任务
+
已经提交的任务继续执行
```

------

# 二十七、shutdown() 后发生了什么？

假设：

```text
任务 A：正在执行

任务 B：正在执行

任务 C：已经进入队列

任务 D：已经进入队列
```

现在：

```java
executorService.shutdown();
```

那么：

```text
A → 继续执行
B → 继续执行
C → 继续等待，之后执行
D → 继续等待，之后执行
```

但是新的任务：

```java
executorService.submit(taskE);
```

就不能再提交了。

通常会：

```java
RejectedExecutionException
```

------

# 二十八、shutdown() 可以理解成什么？

可以把线程池想象成一家餐厅。

调用：

```java
shutdown();
```

相当于：

```text
餐厅停止接新客人
```

但是：

```text
已经点单的人继续吃
厨房已经接到的订单继续做
```

等现有工作全部完成：

```text
餐厅彻底打烊
```

所以：

```text
shutdown
```

强调的是：

> **优雅关闭。**

------

# 二十九、shutdownNow() 是什么？

```java
executorService.shutdownNow();
```

和 shutdown 不一样。

它表达的是：

> **尝试尽快停止正在执行的任务，并停止等待中的任务。**

注意关键词：

```text
尝试
```

不是：

```text
强制杀死线程
```

------

# 三十、shutdownNow() 大致会干什么？

通常：

```text
1. 不再接受新任务

2. 尝试 interrupt 正在执行任务的线程

3. 把队列里还没有开始执行的任务取出来

4. 返回这些还没开始执行的任务
```

方法返回：

```java
List<Runnable>
```

例如：

```java
List<Runnable> tasks =
        executorService.shutdownNow();
```

这些：

```text
tasks
```

通常代表：

> 尚未开始执行的任务。

------

# 三十一、shutdownNow 为什么只是“尝试”停止？

因为 Java 的中断：

```java
interrupt()
```

不是：

> 强制把线程杀死。

它只是：

```text
发送中断请求
```

如果任务根本不响应中断：

```java
while (true) {

}
```

那么：

```text
shutdownNow()
```

也未必能马上停下来。

例如：

```java
executorService.submit(() -> {

    while (true) {

        // 完全不检查中断状态

    }

});
```

调用：

```java
shutdownNow();
```

虽然线程收到 interrupt：

```text
中断标记被设置
```

但任务完全不处理：

```java
Thread.currentThread().isInterrupted()
```

那么仍然可能继续执行。

所以：

```text
shutdownNow
≠ Thread.stop
≠ 强制终止
```

------

# 三十二、shutdown 和 shutdownNow 对比

| 对比       | shutdown | shutdownNow  |
| ---------- | -------- | ------------ |
| 接受新任务 | 不再接受 | 不再接受     |
| 已执行任务 | 继续执行 | 尝试中断     |
| 队列任务   | 继续执行 | 尝试移除     |
| 返回值     | void     | List         |
| 关闭风格   | 优雅关闭 | 尝试立即关闭 |

可以简单记：

```text
shutdown
=
别接新活了，把手上的活干完

shutdownNow
=
别接新活了，手上的也尽量停，没开始的先别干了
```

------

# 三十三、isShutdown()

```java
boolean isShutdown();
```

作用：

> 判断 ExecutorService 是否已经进入关闭状态。

例如：

```java
executorService.shutdown();

System.out.println(
        executorService.isShutdown()
);
```

输出：

```text
true
```

但是一定注意：

```text
isShutdown() == true
```

并不代表：

```text
所有任务都结束了
```

只代表：

> 已经发起关闭。

------

# 三十四、isTerminated()

```java
boolean isTerminated();
```

表示：

> ExecutorService 是否已经彻底终止。

也就是说：

```text
已经调用 shutdown/shutdownNow
+
所有任务都已经结束
+
所有工作线程已经退出
```

这时候：

```java
isTerminated()
```

才是：

```text
true
```

------

# 三十五、isShutdown 和 isTerminated 的区别

假设：

```text
线程池中还有一个任务正在执行。
```

现在：

```java
executorService.shutdown();
```

此时可能：

```java
isShutdown()   → true
isTerminated() → false
```

为什么？

因为：

```text
已经开始关闭
```

但是：

```text
任务还没彻底结束
```

等所有任务结束以后：

```java
isTerminated() → true
```

因此状态大概是：

```text
运行中
│
│ shutdown()
▼
关闭中
│
│ 所有任务完成
▼
彻底终止
```

对应：

```text
运行中：
isShutdown()   = false
isTerminated() = false

关闭中：
isShutdown()   = true
isTerminated() = false

彻底结束：
isShutdown()   = true
isTerminated() = true
```

------

# 三十六、awaitTermination()

方法：

```java
boolean awaitTermination(
        long timeout,
        TimeUnit unit
) throws InterruptedException;
```

意思：

> 等待 ExecutorService 在指定时间内彻底结束。

例如：

```java
executorService.shutdown();

boolean finished =
        executorService.awaitTermination(
                10,
                TimeUnit.SECONDS
        );
```

这里表示：

```text
调用 shutdown()
        ↓
等待最多 10 秒
        ↓
看看线程池能不能彻底结束
```

------

# 三十七、awaitTermination 会自动 shutdown 吗？

不会。

这是一个常见误区。

```java
awaitTermination()
```

本身只是：

```text
等待
```

通常要先：

```java
shutdown();
```

然后：

```java
awaitTermination(...);
```

例如：

```java
executorService.shutdown();

if (!executorService.awaitTermination(
        10,
        TimeUnit.SECONDS)) {

    executorService.shutdownNow();
}
```

意思：

```text
先优雅关闭
        ↓
最多等 10 秒
        ↓
还没结束？
        ↓
shutdownNow()
```

------

# 三十八、一个比较完整的关闭写法

例如：

```java
executorService.shutdown();

try {

    if (!executorService.awaitTermination(
            60,
            TimeUnit.SECONDS)) {

        executorService.shutdownNow();

        if (!executorService.awaitTermination(
                60,
                TimeUnit.SECONDS)) {

            System.err.println(
                    "线程池仍未终止"
            );
        }
    }

} catch (InterruptedException e) {

    executorService.shutdownNow();

    Thread.currentThread().interrupt();
}
```

这里涉及一个非常重要的并发习惯。

如果当前线程在：

```java
awaitTermination()
```

期间被 interrupt：

```java
InterruptedException
```

通常：

```java
catch (InterruptedException e)
```

以后应该：

```java
Thread.currentThread().interrupt();
```

恢复中断标记。

因为抛出 InterruptedException 时：

```text
线程的中断标记通常会被清除
```

而恢复中断标记可以让更上层代码：

```text
继续感知到这个中断请求
```

------

# 三十九、ExecutorService 的生命周期可以怎么理解？

可以理解成：

```text
RUNNING
   │
   │ shutdown()
   ▼
SHUTDOWN
   │
   │ 所有任务结束
   ▼
TERMINATED
```

如果：

```java
shutdownNow();
```

则倾向于：

```text
RUNNING
   │
   │ shutdownNow()
   ▼
STOP
   │
   │ 工作线程退出
   ▼
TERMINATED
```

不过：

```text
RUNNING
SHUTDOWN
STOP
TIDYING
TERMINATED
```

这些更加精确的内部状态属于后面的：

```text
ThreadPoolExecutor
```

章节。

学习 ExecutorService 时先建立：

```text
运行
→ 开始关闭
→ 完全终止
```

这个模型即可。

------

# 四十、invokeAll()

ExecutorService 还有批量提交 Callable 的能力。

例如：

```java
List<Callable<Integer>> tasks = List.of(

        () -> 10,

        () -> 20,

        () -> 30
);
```

可以：

```java
List<Future<Integer>> futures =
        executorService.invokeAll(tasks);
```

最终返回：

```java
List<Future<Integer>>
```

------

# 四十一、invokeAll 的作用

可以理解成：

> 把一批 Callable 全部提交出去，然后等待这批任务全部执行结束。

例如：

```java
List<Callable<Integer>> tasks = List.of(

        () -> {
            Thread.sleep(1000);
            return 10;
        },

        () -> {
            Thread.sleep(2000);
            return 20;
        },

        () -> {
            Thread.sleep(3000);
            return 30;
        }
);
```

然后：

```java
List<Future<Integer>> futures =
        executorService.invokeAll(tasks);
```

这里：

```text
invokeAll()
```

通常会：

```text
阻塞当前调用线程
```

直到：

```text
所有任务都执行完成
```

然后返回：

```text
Future 列表
```

------

# 四十二、invokeAll 和 submit 的区别

如果使用 submit：

```java
Future<Integer> f1 =
        executorService.submit(task1);

Future<Integer> f2 =
        executorService.submit(task2);

Future<Integer> f3 =
        executorService.submit(task3);
```

你需要：

```text
一个一个提交
```

而：

```java
invokeAll()
```

适合：

```text
一批 Callable
```

例如：

```java
List<Future<Integer>> futures =
        executorService.invokeAll(tasks);
```

------

# 四十三、invokeAll 的 Future 顺序

假设：

```java
tasks
```

是：

```text
taskA
taskB
taskC
```

那么返回：

```text
FutureA
FutureB
FutureC
```

通常和输入任务顺序对应。

需要注意：

> 并不是谁先执行完成，谁就一定排前面。

------

# 四十四、带超时时间的 invokeAll

还有一个版本：

```java
<T> List<Future<T>> invokeAll(
        Collection<? extends Callable<T>> tasks,
        long timeout,
        TimeUnit unit
)
```

例如：

```java
List<Future<Integer>> futures =
        executorService.invokeAll(
                tasks,
                5,
                TimeUnit.SECONDS
        );
```

表示：

```text
最多等待 5 秒
```

超时之后：

```text
没有执行完成的任务会被尝试取消
```

对应 Future 可以通过：

```java
future.isCancelled()
```

检查。

------

# 四十五、invokeAny()

`invokeAny()` 和 `invokeAll()` 的思路完全不同。

`invokeAll()`：

```text
我要所有结果
```

而：

```text
invokeAny()
```

表示：

> 我只需要任意一个成功完成的结果。

------

# 四十六、invokeAny 示例

例如：

```java
List<Callable<String>> tasks = List.of(

        () -> {
            Thread.sleep(3000);
            return "服务器 A";
        },

        () -> {
            Thread.sleep(1000);
            return "服务器 B";
        },

        () -> {
            Thread.sleep(2000);
            return "服务器 C";
        }
);
```

调用：

```java
String result =
        executorService.invokeAny(tasks);
```

假设：

```text
服务器 B
```

最快正常完成。

那么 result：

```text
服务器 B
```

------

# 四十七、invokeAny 的思想

它非常适合：

```text
多个任务都可以完成同一件事情
```

例如：

```text
同时请求多个镜像服务器
```

谁先成功：

```text
就用谁
```

可以理解成：

```text
taskA ──────── 3s
taskB ── 1s ← 第一个成功
taskC ───── 2s

          ↓

返回 taskB 的结果
```

剩余没有必要继续的任务：

```text
会被尝试取消
```

------

# 四十八、invokeAll 和 invokeAny 对比

| 方法         | 含义             |
| ------------ | ---------------- |
| invokeAll    | 所有任务都要     |
| invokeAny    | 任意一个成功即可 |
| 返回         | List<Future<T>>  |
| 是否等待全部 | 是               |
| 典型用途     | 批量计算         |

可以记：

```text
All
=
全都要

Any
=
有一个成功就够
```

------

# 四十九、为什么 invokeAny 返回 T，而不是 Future？

因为：

```java
invokeAny()
```

本身：

```text
会等待某一个任务成功完成
```

所以当方法返回的时候：

```text
结果已经出来了
```

因此直接返回：

```java
T
```

而不是：

```java
Future<T>
```

------

# 五十、ExecutorService 的方法整体整理

可以把 ExecutorService 看成：

```text
ExecutorService
│
├── execute()
│
│     ↓
│   只负责执行 Runnable
│
├── submit()
│
│     ↓
│   Runnable / Callable
│   返回 Future
│
├── invokeAll()
│
│     ↓
│   批量执行
│   所有结果都要
│
├── invokeAny()
│
│     ↓
│   批量竞争
│   一个成功即可
│
├── shutdown()
│
│     ↓
│   优雅关闭
│
├── shutdownNow()
│
│     ↓
│   尝试立即关闭
│
├── isShutdown()
│
│     ↓
│   是否已经发起关闭
│
├── isTerminated()
│
│     ↓
│   是否彻底终止
│
└── awaitTermination()
      ↓
    等待彻底终止
```

------

# 五十一、Executor、ExecutorService、Future 的关系

这是当前阶段非常重要的一张图：

```text
Runnable / Callable
        │
        │ 表示任务
        ▼
ExecutorService
        │
        │ submit()
        ▼
Future
        │
        │
        ├── get()
        ├── cancel()
        ├── isDone()
        └── isCancelled()
```

也就是说：

```text
Runnable / Callable
=
任务是什么

ExecutorService
=
任务怎么组织和执行

Future
=
这个任务现在怎么样
```

------

# 五十二、为什么 ExecutorService 要设计成接口？

因为调用者最好只依赖：

```text
能力
```

而不是：

```text
具体实现
```

例如：

```java
ExecutorService executorService =
        new ThreadPoolExecutor(...);
```

左边：

```java
ExecutorService
```

表达：

```text
我只需要：
提交任务
获取 Future
关闭线程池
```

至于底层到底：

```text
ThreadPoolExecutor
ScheduledThreadPoolExecutor
某个自定义实现
```

调用方不一定需要关心。

这就是：

> 面向接口编程。

------

# 五十三、为什么还要保留 Executor 这么简单的接口？

有人可能会问：

```text
既然 ExecutorService 这么强，
为什么不直接只设计 ExecutorService？
```

因为：

```text
接口应该尽量表达最小能力。
```

有些场景调用方只需要：

```text
执行 Runnable
```

根本不需要：

```text
shutdown
Future
Callable
批量执行
```

这时候：

```java
Executor
```

就是最精确的抽象。

例如：

```java
public void executeTask(
        Executor executor,
        Runnable task
) {

    executor.execute(task);
}
```

这个方法只需要：

```text
执行能力
```

所以没必要要求调用者一定传：

```java
ExecutorService
```

------

# 五十四、这体现了接口隔离思想

如果一个组件只需要：

```text
execute()
```

那么参数写：

```java
Executor
```

比：

```java
ExecutorService
```

更加合理。

因为：

```text
依赖最小必要能力
```

而不是：

```text
依赖一个拥有大量自己根本不用的方法的接口
```

所以：

```text
Executor API 少
```

不代表：

```text
Executor 没意义
```

恰恰相反：

> Executor 的价值主要体现在抽象设计上。

------

# 五十五、Executor 最值得记住的不是 API

Executor 的 API：

```java
void execute(Runnable command);
```

5 秒钟就能记住。

真正重要的是：

> **Executor 把“任务”与“任务执行机制”解耦了。**

以前：

```java
new Thread(task).start();
```

相当于：

```text
任务
+
线程创建策略
```

绑在一起。

现在：

```java
executor.execute(task);
```

变成：

```text
任务提交者
    ↓
Executor
    ↓
具体执行策略
```

------

# 五十六、一个经典例子：更换执行策略

业务代码：

```java
public void doSomething(
        Executor executor
) {

    executor.execute(() -> {

        System.out.println(
                "执行核心业务"
        );

    });
}
```

现在可以传不同 Executor。

------

## 当前线程执行

```java
Executor executor =
        Runnable::run;
```

------

## 每个任务创建新线程

```java
Executor executor =
        task -> new Thread(task).start();
```

------

## 线程池执行

```java
ExecutorService executor =
        Executors.newFixedThreadPool(10);
```

业务代码：

```java
doSomething(executor);
```

完全不用修改。

这就是：

> 执行策略和业务任务解耦。

------

# 五十七、ExecutorService 和线程池对象是什么关系？

实际开发中你可能看到：

```java
ExecutorService executorService =
        Executors.newFixedThreadPool(10);
```

但是后面我们会学习：

```java
Executors
```

其实只是一个：

```text
线程池工厂工具类
```

真正返回的底层对象通常是：

```java
ThreadPoolExecutor
```

例如逻辑上：

```text
Executors.newFixedThreadPool()
        ↓
创建 ThreadPoolExecutor
        ↓
以 ExecutorService 类型返回
```

所以：

```java
ExecutorService executorService =
        Executors.newFixedThreadPool(10);
```

本质可以理解成：

```text
ExecutorService
        ↑
ThreadPoolExecutor 对象
```

------

# 五十八、多态关系

例如：

```java
ExecutorService service =
        new ThreadPoolExecutor(...);
```

左边：

```text
ExecutorService
```

是：

```text
引用类型
```

右边：

```text
ThreadPoolExecutor
```

是：

```text
实际对象类型
```

所以：

```text
service
```

只能直接使用 ExecutorService 接口暴露出来的方法。

例如：

```java
submit()
shutdown()
invokeAll()
```

如果想使用：

```text
ThreadPoolExecutor 特有 API
```

通常需要：

```java
ThreadPoolExecutor executor =
        new ThreadPoolExecutor(...);
```

------

# 五十九、一个非常重要的层次划分

学线程池时不要把所有东西混在一起。

可以分成：

```text
第一层：任务
Runnable
Callable


第二层：任务执行抽象
Executor


第三层：任务执行服务
ExecutorService


第四层：结果
Future


第五层：Future 的具体实现
FutureTask


第六层：线程池具体实现
ThreadPoolExecutor


第七层：定时任务
ScheduledExecutorService
ScheduledThreadPoolExecutor
```

这样整个体系就很清楚。

------

# 六十、Runnable、Callable 和 Executor 的关系

Runnable：

```java
Runnable task = () -> {
    System.out.println("执行");
};
```

表达：

```text
我要做什么
```

Executor：

```java
executor.execute(task);
```

表达：

```text
谁负责把这个任务执行掉
```

所以：

```text
Runnable
=
任务描述

Executor
=
任务执行者
```

------

# 六十一、Callable 和 ExecutorService 的关系

Callable：

```java
Callable<Integer> task = () -> {
    return 100;
};
```

但是 Executor：

```java
execute(Runnable)
```

不能直接提交 Callable。

所以需要：

```java
ExecutorService
```

例如：

```java
Future<Integer> future =
        executorService.submit(task);
```

于是：

```text
Callable
       ↓
ExecutorService
       ↓
Future
       ↓
结果
```

------

# 六十二、为什么 execute 只接受 Runnable，而 submit 支持 Callable？

因为：

```java
Executor
```

是一个：

```text
极简任务执行抽象
```

而：

```text
ExecutorService
```

提供：

```text
任务结果管理
```

Callable 的核心特点是：

```java
V call() throws Exception;
```

它：

```text
有返回值
+
允许抛受检异常
```

所以想支持 Callable：

```text
必须有某种机制保存结果
```

这就是：

```java
Future
```

ExecutorService 正好提供了这套机制。

------

# 六十三、submit 内部为什么能把 Runnable 变成 Future？

这一点后面学习：

```text
FutureTask
```

时会详细讲。

现在先建立一个大致模型：

```text
Runnable / Callable
        ↓
submit()
        ↓
包装成一个“既能执行，又能保存结果”的任务对象
        ↓
交给 execute()
        ↓
工作线程执行
        ↓
执行结果保存起来
        ↓
Future.get() 获取
```

这个：

```text
既能执行，又能保存结果
```

的重要实现之一就是：

```java
FutureTask
```

所以后面你会发现：

```text
ExecutorService
Future
FutureTask
```

实际上联系非常紧密。

------

# 六十四、一个非常值得理解的源码思想

`AbstractExecutorService` 中：

```text
submit()
```

大致思想可以理解成：

```java
public Future<?> submit(Runnable task) {

    FutureTask<?> futureTask =
            new FutureTask<>(task, null);

    execute(futureTask);

    return futureTask;
}
```

注意：

这是为了理解思想的简化版本。

真正源码细节以后可以再看。

核心逻辑就是：

```text
submit(task)
        ↓
把任务包装成 FutureTask
        ↓
execute(FutureTask)
        ↓
返回 FutureTask
```

由于：

```java
FutureTask
```

既是：

```java
Runnable
```

又是：

```java
Future
```

所以非常巧妙。

------

# 六十五、因此 execute 和 submit 并不是完全两套独立体系

从思想上可以理解：

```text
submit()
    ↓
把任务包装成 Future 类型任务
    ↓
最终还是交给 execute()
```

所以：

```text
execute
```

其实仍然是非常底层的任务执行入口。

而：

```text
submit
```

是在 execute 上面增加了：

```text
任务状态
结果
异常
取消
```

这些能力。

------

# 六十六、execute 与 submit 的关系图

```text
execute(Runnable)
        │
        └── 直接执行 Runnable


submit(Runnable / Callable)
        │
        ▼
    包装任务
        │
        ▼
   FutureTask
        │
        ▼
    execute(...)
        │
        ▼
     执行任务
        │
        ▼
   保存结果/异常
        │
        ▼
      Future
```

这张图非常重要。

------

# 六十七、ExecutorService 是不是只能是线程池？

理论上：

```text
不是。
```

因为 ExecutorService 是接口。

任何类只要实现：

```java
ExecutorService
```

都可以成为 ExecutorService。

但是 Java 标准库中最典型、最重要的实现就是：

```java
ThreadPoolExecutor
```

所以实际开发里：

```text
ExecutorService
```

经常和线程池绑定出现。

因此很多人日常口语中：

```text
ExecutorService
```

基本就是在谈：

```text
线程池服务
```

但是学习时最好知道：

```text
接口 ≠ 实现
```

------

# 六十八、ExecutorService 的典型使用模式

最简单：

```java
ExecutorService executorService =
        Executors.newFixedThreadPool(4);

try {

    Future<Integer> future =
            executorService.submit(() -> {
                return 100;
            });

    Integer result = future.get();

    System.out.println(result);

} finally {

    executorService.shutdown();
}
```

整体逻辑：

```text
创建 ExecutorService
        ↓
提交任务
        ↓
执行任务
        ↓
获得 Future
        ↓
获取结果
        ↓
关闭 ExecutorService
```

------

# 六十九、为什么 ExecutorService 要关闭？

因为线程池内部的线程：

```text
通常并不会执行完一个任务就退出
```

而是：

```text
执行任务
    ↓
继续等待下一个任务
    ↓
继续工作
```

例如：

```text
Worker-1
Worker-2
Worker-3
Worker-4
```

这些工作线程可能一直存活。

如果你创建：

```java
ExecutorService executorService =
        Executors.newFixedThreadPool(4);
```

任务执行完以后完全不关闭：

```text
JVM 可能因为这些非守护线程仍然存活而无法正常退出
```

所以：

> ExecutorService 用完之后应该考虑正确关闭。

------

# 七十、错误示例：创建线程池但不关闭

```java
public static void main(String[] args) {

    ExecutorService executorService =
            Executors.newFixedThreadPool(2);

    executorService.submit(() -> {
        System.out.println("任务完成");
    });

}
```

你可能发现：

```text
任务已经打印完成
```

但：

```text
程序迟迟没有退出
```

原因：

```text
线程池中的工作线程仍然活着
```

它们还在：

```text
等待未来任务
```

------

# 七十一、ExecutorService 的责任范围

ExecutorService 主要关心：

```text
我要怎样提交任务？
任务是否有结果？
任务执行完了吗？
任务能不能取消？
什么时候停止接收新任务？
什么时候关闭？
什么时候彻底结束？
```

但是它不直接规定：

```text
线程池有几个核心线程？
最大线程数是多少？
队列是什么？
拒绝策略是什么？
空闲线程活多久？
```

这些属于：

```java
ThreadPoolExecutor
```

的职责。

------

# 七十二、为什么现在不要把 ThreadPoolExecutor 混进来？

因为你现在学习的是：

```text
任务执行框架的抽象层
```

Executor 和 ExecutorService 解决的是：

```text
有什么能力
```

而 ThreadPoolExecutor 学的是：

```text
这些能力内部具体怎么实现
```

最好顺序：

```text
Executor
    ↓
ExecutorService
    ↓
Future
    ↓
FutureTask
    ↓
ThreadPoolExecutor
```

这样后面会舒服很多。

------

# 七十三、Executor 的学习重点

Executor API 本身几乎不用背。

你只需要真正理解下面几点。

------

## 1. Executor 是顶层任务执行抽象

```java
public interface Executor {

    void execute(Runnable command);

}
```

------

## 2. Executor 实现任务与执行机制解耦

```text
Runnable
只描述任务

Executor
决定如何执行
```

------

## 3. execute 不代表创建新线程

完全由具体实现决定。

------

## 4. Executor 不管理任务结果

没有：

```text
Future
```

------

## 5. Executor 不提供生命周期管理

没有：

```text
shutdown
```

------

# 七十四、ExecutorService 的学习重点

ExecutorService 就要认真掌握。

核心有：

```text
1. 继承 Executor

2. submit()

3. Future

4. shutdown()

5. shutdownNow()

6. isShutdown()

7. isTerminated()

8. awaitTermination()

9. invokeAll()

10. invokeAny()
```

------

# 七十五、一个总对比

## Executor

核心问题：

```text
“怎么把任务交给某个执行器？”
```

核心 API：

```java
execute(Runnable)
```

特点：

```text
简单
最顶层
抽象
只负责执行
```

------

## ExecutorService

核心问题：

```text
“怎么提交、管理、等待、关闭一套任务执行服务？”
```

核心能力：

```text
execute
submit
Future
shutdown
批量任务
生命周期
```

------

# 七十六、可以把它们类比成什么？

可以把：

```text
Executor
```

理解成：

> 一个“能干活的人”。

接口只要求：

```text
给你一个活，你能干。
```

而：

```text
ExecutorService
```

像：

> 一个完整的工作团队管理服务。

除了：

```text
能干活
```

还负责：

```text
接任务
登记任务
告诉你任务结果
批量处理任务
停止接单
下班
等待所有员工下班
```

所以：

```text
Executor
```

是最基础能力。

```text
ExecutorService
```

是真正完整的服务抽象。

------

# 七十七、常见误区一：Executor 就是线程池

错误。

Executor 只是：

```text
任务执行接口
```

可以：

```text
当前线程执行
新线程执行
线程池执行
其他方式执行
```

所以：

```text
Executor ≠ ThreadPoolExecutor
```

------

# 七十八、常见误区二：execute 一定异步

错误。

例如：

```java
Executor executor = Runnable::run;
```

执行：

```java
executor.execute(task);
```

任务就是：

```text
当前线程同步执行
```

------

# 七十九、常见误区三：ExecutorService 就是 ThreadPoolExecutor

错误。

关系：

```text
ExecutorService
=
接口

ThreadPoolExecutor
=
实现类
```

------

# 八十、常见误区四：shutdown 会立刻停止所有任务

错误。

```java
shutdown();
```

是：

```text
停止接收新任务
+
已有任务继续执行
```

------

# 八十一、常见误区五：shutdownNow 一定能立刻杀死线程

错误。

它主要依赖：

```text
interrupt
```

因此：

```text
任务是否响应中断
```

非常关键。

------

# 八十二、常见误区六：isShutdown=true 就代表结束了

错误。

```text
isShutdown
```

表示：

```text
已经开始关闭
```

而：

```text
isTerminated
```

才表示：

```text
彻底结束
```

------

# 八十三、常见误区七：submit 只用于 Callable

错误。

submit 可以提交：

```text
Runnable
Callable
```

只不过：

```text
Callable
```

天然有返回结果。

------

# 八十四、常见误区八：submit 后异常消失了

错误。

异常通常进入：

```text
Future
```

调用：

```java
future.get();
```

时通过：

```java
ExecutionException
```

暴露。

------

# 八十五、execute 与 submit 应该怎么选？

如果只是：

```text
执行一个简单任务
不关心返回结果
也不需要 Future 控制
```

可以：

```java
execute()
```

例如：

```java
executorService.execute(() -> {
    cleanCache();
});
```

如果需要：

```text
结果
状态
取消
异常获取
```

则：

```java
submit()
```

例如：

```java
Future<User> future =
        executorService.submit(() -> {
            return loadUser();
        });
```

------

# 八十六、实际项目中 submit 一定比 execute 好吗？

不是。

它们只是语义不同。

如果完全不关心结果：

```text
execute
```

语义反而更直接。

如果用了：

```java
submit()
```

但是：

```java
Future<?> future
```

完全不保存：

```java
executorService.submit(task);
```

这时候还有一个隐患：

```text
任务抛异常时，
异常被放进 Future，
但你根本没有 get()
```

可能导致异常没有被及时发现。

所以：

> 不是所有任务都应该无脑 submit。

------

# 八十七、ExecutorService 中 Runnable 与 Callable 的选择

如果任务：

```text
没有业务返回值
```

使用：

```java
Runnable
```

例如：

```java
executorService.execute(() -> {
    sendMessage();
});
```

------

如果任务：

```text
需要返回结果
```

使用：

```java
Callable<T>
```

例如：

```java
Future<User> future =
        executorService.submit(() -> {
            return queryUser();
        });
```

------

# 八十八、ExecutorService 与异步的关系

ExecutorService 经常用来：

```text
异步执行任务
```

例如：

```java
Future<Integer> future =
        executorService.submit(() -> {

            Thread.sleep(3000);

            return 100;
        });

System.out.println("主线程继续干活");
```

此时：

```text
main
│
├── submit(task)
│
├── 继续执行其他代码
│
└── ...
    
worker
│
└── 执行 task
```

这就是常见的：

```text
异步执行
```

------

# 八十九、但 future.get() 又可能把异步变回等待

例如：

```java
Future<Integer> future =
        executorService.submit(task);

Integer result =
        future.get();
```

如果任务还没完成：

```text
当前线程会阻塞等待
```

于是：

```text
submit 本身很快返回
```

但是：

```text
get()
```

可能等待。

这也是为什么后面：

```java
CompletableFuture
```

非常重要。

因为 CompletableFuture 更适合：

```text
异步任务编排
```

而不是到处：

```java
future.get()
```

阻塞等待。

------

# 九十、ExecutorService 与 CompletableFuture 的关系

现在不用深入。

先知道：

```text
ExecutorService
```

解决：

```text
谁来执行异步任务
```

而：

```text
CompletableFuture
```

进一步解决：

```text
异步任务之间怎么组合
```

例如：

```text
任务 A
    ↓
拿 A 结果
    ↓
任务 B
    ↓
同时执行 C
    ↓
汇总
```

这类复杂异步流程：

```text
CompletableFuture
```

比裸 Future 更方便。

------

# 九十一、整个 Java 并发任务体系可以这样记

```text
Runnable
Callable
    │
    │ 描述任务
    ▼
Executor
    │
    │ 定义最基本的执行能力
    ▼
ExecutorService
    │
    │ 提交、管理任务
    ├───────────────┐
    ▼               ▼
Future          生命周期管理
    │             shutdown
    ▼             shutdownNow
FutureTask      awaitTermination
    │
    │
    ▼
ThreadPoolExecutor
    │
    ▼
真正的线程池
```

------

# 九十二、学习 Executor 时到底需要学到什么程度？

Executor 不需要像：

```text
ReentrantLock
ThreadPoolExecutor
CompletableFuture
```

一样深挖大量 API。

你只需要做到：

```text
看到 Executor
```

马上想到：

> Java 对“任务执行能力”的最顶层抽象。

看到：

```java
execute(Runnable)
```

马上想到：

> 调用者只提交任务，具体怎么执行交给 Executor 实现决定。

这就够了。

------

# 九十三、ExecutorService 要学到什么程度？

ExecutorService 至少要做到：

看到：

```java
execute()
```

知道：

```text
执行 Runnable，不返回 Future
```

看到：

```java
submit()
```

知道：

```text
支持 Runnable / Callable
返回 Future
```

看到：

```java
shutdown()
```

知道：

```text
停止接新任务，已有任务继续
```

看到：

```java
shutdownNow()
```

知道：

```text
尝试中断正在执行任务，返回未执行任务
```

看到：

```java
isShutdown()
```

知道：

```text
是否开始关闭
```

看到：

```java
isTerminated()
```

知道：

```text
是否彻底结束
```

看到：

```java
awaitTermination()
```

知道：

```text
等待线程池终止
```

看到：

```java
invokeAll()
```

知道：

```text
批量任务，全都要
```

看到：

```java
invokeAny()
```

知道：

```text
批量任务，一个成功即可
```

------

# 九十四、面试高频问题：Executor 和 ExecutorService 有什么区别？

可以这样回答：

> `Executor` 是 Java 并发任务执行框架中最顶层的接口，只定义了 `execute(Runnable)` 方法，核心作用是把任务的提交与任务的具体执行方式解耦。
>
> `ExecutorService` 继承了 `Executor`，在执行任务的基础上增加了任务结果管理、Callable 支持、Future、批量任务执行以及线程池生命周期管理等能力，例如 `submit()`、`invokeAll()`、`shutdown()`、`shutdownNow()` 等。
>
> 因此 Executor 更偏向最基础的执行能力抽象，而 ExecutorService 则是完整的任务执行服务抽象。

------

# 九十五、面试高频问题：execute 和 submit 有什么区别？

可以回答：

> `execute()` 来自 Executor，只能提交 Runnable，并且没有返回值。
>
> `submit()` 来自 ExecutorService，可以提交 Runnable 或 Callable，并返回 Future，因此可以获取任务执行状态、结果、异常以及进行取消。
>
> 另外任务抛出异常时，execute 提交的任务通常通过线程未捕获异常机制暴露，而 submit 提交的任务异常通常会保存到 Future 中，在调用 `Future.get()` 时通过 `ExecutionException` 暴露。

------

# 九十六、面试高频问题：shutdown 和 shutdownNow 区别？

可以回答：

> `shutdown()` 是优雅关闭，停止接受新任务，但已经提交的任务仍然会继续执行。
>
> `shutdownNow()` 会尝试中断正在执行的任务，同时把任务队列中尚未开始执行的任务移除并返回。
>
> 但是 shutdownNow 并不能保证任务一定立即停止，因为 Java 的线程中断是协作式的，任务本身必须正确响应中断。

------

# 九十七、面试高频问题：isShutdown 和 isTerminated 区别？

```text
isShutdown
=
是否已经发起关闭

isTerminated
=
是否已经彻底结束
```

因此：

```text
isShutdown = true
```

的时候：

```text
isTerminated
```

完全可能：

```text
false
```

------

# 九十八、面试高频问题：ExecutorService 是线程池吗？

比较标准的回答：

> ExecutorService 本身是接口，不是具体线程池。它定义了任务提交、任务结果管理以及生命周期管理等能力。Java 中典型的线程池实现 `ThreadPoolExecutor` 实现了 ExecutorService，所以实际开发中 ExecutorService 经常作为线程池的引用类型使用。

------

# 九十九、Executor 与 ExecutorService 的最终定位

最终你可以这样记：

```text
Executor
=
“执行任务”这件事情的最小抽象
```

核心思想：

```text
任务
和
执行机制
解耦
```

------

```text
ExecutorService
=
完整的任务执行服务
```

核心能力：

```text
执行
提交
结果
取消
批量任务
关闭
生命周期管理
```

------

# 一百、整个章节最重要的一张图

```text
                    Runnable
                       │
                       │
                       ▼
                  Executor
                       │
                       │ execute()
                       │
                       ▼
            ┌────────────────────┐
            │  ExecutorService   │
            └────────────────────┘
                │      │      │
                │      │      │
           submit()   生命周期   批量任务
                │      │      │
                ▼      │      │
              Future   │      │
                       │      │
                shutdown()    │
                shutdownNow() │
                isShutdown()  │
                isTerminated()│
                await...      │
                              │
                         invokeAll()
                         invokeAny()
                              
                    ExecutorService
                           │
                           ▼
              AbstractExecutorService
                           │
                           ▼
                 ThreadPoolExecutor
                           │
                           ▼
                    真正线程池实现
```

------

# 一百零一、本章最重要的十句话

如果整章最后只记十句话，就记下面这些：

1. **Executor 是 Java 对“任务执行能力”的最顶层抽象。**
2. **Executor 只有一个核心方法：`execute(Runnable)`。**
3. **Executor 最大的意义是把“任务是什么”和“任务怎么执行”解耦。**
4. **调用 `execute()` 不代表一定创建新线程，也不代表一定异步执行。**
5. **ExecutorService 继承 Executor，并在此基础上增加任务管理能力。**
6. **`submit()` 可以提交 Runnable 或 Callable，并返回 Future。**
7. **`shutdown()` 是停止接收新任务，但已经提交的任务继续执行。**
8. **`shutdownNow()` 只是尝试通过中断等方式尽快停止任务，并不保证一定立即停止。**
9. **ExecutorService 是接口，ThreadPoolExecutor 才是最核心的具体线程池实现之一。**
10. **Executor 重要在设计思想，ExecutorService 重要在实际使用，ThreadPoolExecutor 则是线程池学习真正的核心。**

------

# 一百零二、最终学习路线

这一章学完以后，不建议立刻跳到：

```text
CompletableFuture
```

更顺畅的路线是：

```text
Runnable / Callable
        ↓
Executor
        ↓
ExecutorService
        ↓
Future
        ↓
FutureTask
        ↓
ThreadPoolExecutor
        ↓
Executors
        ↓
ScheduledExecutorService
        ↓
ScheduledThreadPoolExecutor
        ↓
CompletableFuture
```

其中当前这一步：

```text
Executor + ExecutorService
```

主要完成的是：

> 从“自己创建线程”过渡到“把任务交给执行框架管理”。

真正到了：

```java
ThreadPoolExecutor
```

以后，才会正式进入：

```text
核心线程数
最大线程数
任务队列
线程创建
线程回收
拒绝策略
线程池执行流程
线程池状态
Worker
ctl
```

这些真正的线程池内部机制。

------

# 一百零三、一句话收尾

可以把这一整章浓缩成：

> **Runnable / Callable 定义“要干什么”，Executor 定义“有人负责执行”，ExecutorService 进一步解决“任务怎么提交、怎么拿结果、怎么管理、怎么关闭”，而 ThreadPoolExecutor 最终负责真正把这些事情用线程池实现出来。**