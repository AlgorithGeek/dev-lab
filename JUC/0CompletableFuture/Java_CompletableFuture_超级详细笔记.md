# CompletableFuture

> 适用版本：**JDK 17**
>
> 学习定位：已经学过 `Runnable / Callable`、`Executor / ExecutorService`、`Future / FutureTask`、`ThreadPoolExecutor` 后，再学习本章效果最好。
>
> 一句话定位：
>
> **`CompletableFuture` = 一个既能表示“未来结果”，又能描述“异步任务之间依赖、组合、转换、异常处理”的工具。**
>
> 它不只是“更高级的 Future”，更准确地说，它是一套**异步任务编排模型**。

---

# 目录

- [一、为什么需要 CompletableFuture](#一为什么需要-completablefuture)
- [二、CompletableFuture 到底是什么](#二completablefuture-到底是什么)
- [三、先建立最重要的整体模型](#三先建立最重要的整体模型)
- [四、CompletableFuture 的创建方式](#四completablefuture-的创建方式)
- [五、runAsync 与 supplyAsync](#五runasync-与-supplyasync)
- [六、同步方法与 Async 方法到底有什么区别](#六同步方法与-async-方法到底有什么区别)
- [七、结果转换：thenApply](#七结果转换thenapply)
- [八、消费结果：thenAccept](#八消费结果thenaccept)
- [九、只关心任务完成：thenRun](#九只关心任务完成thenrun)
- [十、串行依赖：thenCompose](#十串行依赖thencompose)
- [十一、并行任务合并：thenCombine](#十一并行任务合并thencombine)
- [十二、两个任务只等一个：applyToEither 等](#十二两个任务只等一个applytoeither-等)
- [十三、多个任务：allOf 与 anyOf](#十三多个任务allof-与-anyof)
- [十四、获取结果：get 与 join](#十四获取结果get-与-join)
- [十五、异常处理：exceptionally / handle / whenComplete](#十五异常处理exceptionally--handle--whencomplete)
- [十六、超时处理](#十六超时处理)
- [十七、手动完成 CompletableFuture](#十七手动完成-completablefuture)
- [十八、取消任务](#十八取消任务)
- [十九、CompletableFuture 与线程池](#十九completablefuture-与线程池)
- [二十、ForkJoinPool.commonPool 到底是什么](#二十forkjoinpoolcommonpool-到底是什么)
- [二十一、CompletableFuture 与 Future / FutureTask 的关系](#二十一completablefuture-与-future--futuretask-的关系)
- [二十二、CompletableFuture 与 Stream API 为什么很像](#二十二completablefuture-与-stream-api-为什么很像)
- [二十三、业务开发最常见的使用场景](#二十三业务开发最常见的使用场景)
- [二十四、完整业务案例：并行查询再聚合](#二十四完整业务案例并行查询再聚合)
- [二十五、完整业务案例：有依赖的异步调用](#二十五完整业务案例有依赖的异步调用)
- [二十六、完整业务案例：批量并发处理](#二十六完整业务案例批量并发处理)
- [二十七、常见坑与生产环境注意事项](#二十七常见坑与生产环境注意事项)
- [二十八、和 Spring 项目结合时要注意什么](#二十八和-spring-项目结合时要注意什么)
- [二十九、常用 API 速查表](#二十九常用-api-速查表)
- [三十、最容易混淆的 API 对比](#三十最容易混淆的-api-对比)
- [三十一、面试常问问题](#三十一面试常问问题)
- [三十二、学习优先级：哪些必须会，哪些先了解](#三十二学习优先级哪些必须会哪些先了解)
- [三十三、最终知识地图](#三十三最终知识地图)

---

# 一、为什么需要 CompletableFuture

在学习 `CompletableFuture` 之前，你应该已经知道：

```java
Future<Integer> future = executorService.submit(() -> {
    return 100;
});

Integer result = future.get();
```

`Future` 能做什么？

```text
提交异步任务
    ↓
拿到 Future
    ↓
以后通过 get() 获取结果
```

它解决了一个非常核心的问题：

> **异步任务的结果以后怎么拿？**

但是 `Future` 的能力非常有限。

例如业务中有下面一个需求：

```text
查询用户信息
查询用户订单
查询用户优惠券
查询用户积分
```

这四个任务互相没有依赖。

如果全部同步执行：

```text
用户信息：300ms
订单：500ms
优惠券：400ms
积分：200ms
```

总耗时大约：

```text
300 + 500 + 400 + 200
= 1400ms
```

但实际上它们完全可以同时执行：

```text
        ┌── 用户信息 300ms
        ├── 订单     500ms
请求 ───┼── 优惠券   400ms
        └── 积分     200ms
```

理论总耗时接近：

```text
max(300, 500, 400, 200)
≈ 500ms
```

如果只用普通 `Future`，当然也能写：

```java
Future<User> userFuture = pool.submit(...);
Future<List<Order>> orderFuture = pool.submit(...);
Future<List<Coupon>> couponFuture = pool.submit(...);
Future<Integer> scoreFuture = pool.submit(...);

User user = userFuture.get();
List<Order> orders = orderFuture.get();
List<Coupon> coupons = couponFuture.get();
Integer score = scoreFuture.get();
```

但问题来了。

---

## 1. Future 很难表达任务依赖

假设：

```text
任务 A：查询用户
任务 B：根据 A 返回的 userId 查询订单
任务 C：根据 B 的结果计算金额
```

关系是：

```text
A
↓
B
↓
C
```

普通 `Future` 最终往往写成：

```java
Future<User> f1 = pool.submit(...);

User user = f1.get();

Future<List<Order>> f2 = pool.submit(() -> {
    return queryOrders(user.getId());
});

List<Order> orders = f2.get();
```

你会发现：

```text
异步
↓
get()
↓
阻塞
↓
再异步
↓
再 get()
```

代码很快就会退化成“伪异步”。

---

## 2. Future 很难进行任务组合

现实业务经常有：

```text
A 和 B 并行执行
↓
两个都完成
↓
执行 C
```

或者：

```text
A
↓
根据 A 的结果执行 B
↓
根据 B 的结果执行 C
```

或者：

```text
A / B / C / D 全部并行
↓
等待全部完成
↓
统一组装返回值
```

普通 `Future` 没有直接提供这种“任务关系”的表达方式。

---

## 3. Future 的异常处理不优雅

普通 Future：

```java
try {
    Integer result = future.get();
} catch (InterruptedException e) {
    ...
} catch (ExecutionException e) {
    ...
}
```

如果有十几个异步任务，异常处理很快就会非常难看。

---

## 4. Future 不方便声明式编排

我们希望能够写出：

```java
CompletableFuture
        .supplyAsync(...)
        .thenApply(...)
        .thenCompose(...)
        .thenCombine(...)
        .exceptionally(...);
```

也就是说：

```text
我要描述：

任务完成之后
    ↓
做什么

结果出来之后
    ↓
怎么转换

另一个任务完成之后
    ↓
怎么合并

出现异常之后
    ↓
怎么兜底
```

这就是 `CompletableFuture` 的核心价值。

---

# 二、CompletableFuture 到底是什么

类定义：

```java
public class CompletableFuture<T>
        implements Future<T>, CompletionStage<T>
```

注意它实现了两个非常重要的接口：

```text
CompletableFuture<T>
      │
      ├── Future<T>
      │
      └── CompletionStage<T>
```

这两个接口代表两种完全不同的能力。

---

## 1. Future：表示一个未来结果

`Future<T>` 让它拥有：

```java
get()
get(timeout, unit)
cancel()
isDone()
isCancelled()
```

所以：

```java
CompletableFuture<String> future
```

本身仍然可以理解成：

> **未来某个时间会产生一个 String。**

---

## 2. CompletionStage：描述异步阶段之间的关系

`CompletionStage<T>` 才是 CompletableFuture 真正强大的地方。

它提供：

```text
thenApply
thenAccept
thenRun
thenCompose
thenCombine
handle
whenComplete
...
```

因此：

```text
Future
=
“未来会有结果”
```

而：

```text
CompletionStage
=
“这个阶段完成后，下一阶段应该怎么执行”
```

所以：

> **CompletableFuture = Future 的结果能力 + CompletionStage 的异步编排能力。**

---

# 三、先建立最重要的整体模型

学习 CompletableFuture 最重要的不是背 API。

而是先形成一个模型：

```text
CompletableFuture
不是简单的一次任务。

它更像：

“一个异步计算阶段（Stage）”
```

例如：

```java
CompletableFuture<String> future =
        CompletableFuture
                .supplyAsync(() -> "100")
                .thenApply(Integer::parseInt)
                .thenApply(num -> num * 2)
                .thenApply(String::valueOf);
```

你可以把它理解成：

```text
Stage 1
异步产生 "100"
      ↓
Stage 2
"100" -> 100
      ↓
Stage 3
100 -> 200
      ↓
Stage 4
200 -> "200"
```

也就是：

```text
CompletableFuture
本质上可以形成一条“计算流水线”
```

甚至不只是流水线。

它还可以形成：

```text
               A
             ↙   ↘
            B     C
             ↘   ↙
               D
```

所以更准确地说：

> **CompletableFuture 可以形成一张异步任务依赖图。**

---

## 1. 每一个 thenXXX 都是在注册后续动作

例如：

```java
CompletableFuture<Integer> future =
        CompletableFuture.supplyAsync(() -> 100);

CompletableFuture<Integer> future2 =
        future.thenApply(x -> x * 2);
```

这里不是：

```text
future 直接“变成了” future2
```

而是：

```text
future
  │
  │ 完成后
  ↓
执行 x -> x * 2
  │
  ↓
future2
```

---

## 2. 一个 CompletableFuture 可以有多个下游

例如：

```java
CompletableFuture<Integer> root =
        CompletableFuture.supplyAsync(() -> 100);

CompletableFuture<Integer> a =
        root.thenApply(x -> x + 1);

CompletableFuture<Integer> b =
        root.thenApply(x -> x * 10);
```

结构：

```text
           root = 100
           /       \
          /         \
         ↓           ↓
      +1任务       ×10任务
         ↓           ↓
        101         1000
```

所以：

> CompletableFuture 并不是只能形成一条链。

---

# 四、CompletableFuture 的创建方式

最常见有以下几种。

---

## 1. runAsync

没有返回值：

```java
CompletableFuture<Void> future =
        CompletableFuture.runAsync(() -> {
            System.out.println("执行任务");
        });
```

适用于：

```text
发送日志
发送通知
异步记录
刷新缓存
执行某个不需要返回值的动作
```

---

## 2. supplyAsync

有返回值：

```java
CompletableFuture<Integer> future =
        CompletableFuture.supplyAsync(() -> {
            return 100;
        });
```

更常用。

---

## 3. completedFuture

直接创建一个已经成功完成的 Future：

```java
CompletableFuture<String> future =
        CompletableFuture.completedFuture("hello");
```

此时：

```java
future.isDone()
```

立即为：

```text
true
```

常用于：

```text
测试
默认值
某些条件下无需异步执行
快速返回一个已经存在的结果
```

---

## 4. failedFuture

JDK 9+：

```java
CompletableFuture<String> future =
        CompletableFuture.failedFuture(
                new RuntimeException("失败")
        );
```

直接得到一个：

```text
异常完成的 CompletableFuture
```

---

## 5. new CompletableFuture()

也可以手动创建：

```java
CompletableFuture<String> future =
        new CompletableFuture<>();
```

此时它：

```text
还没有完成
```

以后你可以：

```java
future.complete("成功");
```

或者：

```java
future.completeExceptionally(
        new RuntimeException("失败")
);
```

这个能力是普通 `FutureTask` 不具备的典型特征之一。

---

# 五、runAsync 与 supplyAsync

这是最基础的一组。

---

## 1. runAsync

签名：

```java
public static CompletableFuture<Void> runAsync(Runnable runnable)
```

因为 `Runnable`：

```java
void run();
```

没有结果。

例如：

```java
CompletableFuture<Void> future =
        CompletableFuture.runAsync(() -> {
            System.out.println("保存日志");
        });
```

最终类型：

```java
CompletableFuture<Void>
```

---

## 2. supplyAsync

签名：

```java
public static <U> CompletableFuture<U> supplyAsync(
        Supplier<U> supplier
)
```

`Supplier<T>`：

```java
T get();
```

所以：

```java
CompletableFuture<String> future =
        CompletableFuture.supplyAsync(() -> {
            return "hello";
        });
```

---

## 3. 二者最简单的记忆方式

```text
runAsync
=
Runnable
=
无返回值
```

```text
supplyAsync
=
Supplier
=
有返回值
```

---

## 4. 指定线程池

两个方法都有 Executor 版本：

```java
CompletableFuture.supplyAsync(
        () -> queryUser(),
        executor
);
```

例如：

```java
ExecutorService executor =
        Executors.newFixedThreadPool(8);

CompletableFuture<User> future =
        CompletableFuture.supplyAsync(
                () -> queryUser(),
                executor
        );
```

在生产业务中：

> **涉及数据库、HTTP、RPC、文件等阻塞型任务时，通常应该认真考虑使用业务自定义线程池，而不是所有任务都丢到默认 commonPool。**

---

# 六、同步方法与 Async 方法到底有什么区别

CompletableFuture 中会看到大量方法：

```text
thenApply
thenApplyAsync

thenAccept
thenAcceptAsync

thenRun
thenRunAsync

thenCompose
thenComposeAsync

thenCombine
thenCombineAsync
```

这个 `Async` 非常重要。

---

## 1. 非 Async 版本

例如：

```java
future.thenApply(...)
```

它的意思不是：

```text
一定同步
```

也不是：

```text
一定在主线程执行
```

正确理解是：

> **这个后续阶段不会强制重新提交到一个异步线程池。**

它有可能由：

```text
完成上一个阶段的线程
```

直接继续执行。

如果上游在注册动作之前已经完成，某些情况下也可能由：

```text
当前调用 thenApply 的线程
```

执行。

因此不要死记：

```text
thenApply = 某个固定线程
```

线程归属取决于完成时机和执行过程。

---

## 2. Async 版本

例如：

```java
future.thenApplyAsync(...)
```

意味着：

```text
后续动作以异步任务方式调度
```

没有指定线程池：

```java
future.thenApplyAsync(x -> x * 2);
```

一般使用：

```text
CompletableFuture 默认异步执行器
```

通常就是：

```text
ForkJoinPool.commonPool()
```

---

## 3. Async + 自定义 Executor

```java
future.thenApplyAsync(
        x -> x * 2,
        executor
);
```

表示：

```text
明确把这个阶段交给 executor
```

这是控制线程资源最清晰的方法。

---

## 4. 一个非常重要的原则

如果后续操作只是很轻的计算：

```java
future.thenApply(user -> user.getName());
```

通常没必要强行：

```java
thenApplyAsync(...)
```

因为：

```text
重新调度任务
本身也有成本
```

但如果后续操作：

```text
很耗时
会阻塞
希望隔离线程池
```

那么可以选择 Async + 指定 Executor。

---

# 七、结果转换：thenApply

`thenApply` 是最常用的方法之一。

它的作用：

```text
拿到上一个阶段的结果
↓
转换
↓
产生一个新的结果
```

可以类比 Stream：

```java
stream.map(...)
```

---

## 1. 基本例子

```java
CompletableFuture<Integer> future =
        CompletableFuture
                .supplyAsync(() -> "100")
                .thenApply(str -> Integer.parseInt(str))
                .thenApply(num -> num * 2);
```

过程：

```text
"100"
  ↓
100
  ↓
200
```

最终：

```java
Integer result = future.join();
```

得到：

```text
200
```

---

## 2. 类型可以发生变化

```java
CompletableFuture<User> userFuture = ...;

CompletableFuture<String> nameFuture =
        userFuture.thenApply(User::getName);
```

所以：

```text
CompletableFuture<User>
        ↓
thenApply(User -> String)
        ↓
CompletableFuture<String>
```

---

## 3. thenApply 需要上游正常完成

如果上游异常：

```java
CompletableFuture
        .supplyAsync(() -> {
            throw new RuntimeException();
        })
        .thenApply(x -> {
            // 通常不会执行到
            return x;
        });
```

异常会沿着依赖链继续传播。

---

# 八、消费结果：thenAccept

如果：

```text
你需要上一个阶段的结果
但是自己不再产生新结果
```

使用：

```java
thenAccept
```

---

## 1. 示例

```java
CompletableFuture<Void> future =
        CompletableFuture
                .supplyAsync(() -> "hello")
                .thenAccept(result -> {
                    System.out.println(result);
                });
```

关系：

```text
supplyAsync
产生 String
     ↓
thenAccept
消费 String
     ↓
Void
```

---

## 2. 和 thenApply 对比

```java
thenApply:
T -> U
```

```java
thenAccept:
T -> void
```

记忆：

```text
Apply
=
加工、转换
```

```text
Accept
=
消费
```

---

# 九、只关心任务完成：thenRun

如果：

```text
不需要上一个任务的结果
只要它完成后执行某个动作
```

使用：

```java
thenRun
```

---

## 示例

```java
CompletableFuture<Void> future =
        CompletableFuture
                .supplyAsync(() -> queryUser())
                .thenRun(() -> {
                    System.out.println("前面的任务完成了");
                });
```

注意：

```text
thenRun 拿不到上一步的结果
```

因为它接收的是：

```java
Runnable
```

---

## 三兄弟必须记住

```text
thenApply
T -> U
有入参，有返回值
```

```text
thenAccept
T -> void
有入参，无返回值
```

```text
thenRun
() -> void
无入参，无返回值
```

这三个是 CompletableFuture 最基础的一组。

---

# 十、串行依赖：thenCompose

`thenCompose` 是 CompletableFuture 最重要的方法之一。

它用于：

> **第二个异步任务依赖第一个异步任务的结果。**

例如：

```text
先查询用户
↓
拿 userId
↓
再异步查询订单
```

---

## 1. 错误/别扭写法：thenApply 返回 Future

假设：

```java
CompletableFuture<User> userFuture =
        CompletableFuture.supplyAsync(
                () -> queryUser()
        );
// <User> --> <String>
```

现在根据用户查询订单：

```java
CompletableFuture<CompletableFuture<List<Order>>> result =
        userFuture.thenApply(user -> {
            return CompletableFuture.supplyAsync(
                    () -> queryOrders(user.getId())
            );
        });
```

你会得到：

```text
CompletableFuture<
    CompletableFuture<List<Order>>
>
```

出现了嵌套：

```text
Future 套 Future
```

非常难用。

---

## 2. thenCompose 解决嵌套

```java
CompletableFuture<List<Order>> result =
        userFuture.thenCompose(user -> {
            return CompletableFuture.supplyAsync(
                    () -> queryOrders(user.getId())
            );
        });
```

结构：

```text
User
 ↓
返回 CompletableFuture<List<Order>>
 ↓
thenCompose 自动压平
 ↓
CompletableFuture<List<Order>>
```

---

## 3. thenApply vs thenCompose

这是 CompletableFuture 最重要的区别之一。

### thenApply

```java
T -> U
```

例如：

```java
User -> String
```

---

### thenCompose

```java
T -> CompletionStage<U>
```

例如：

```java
User
  ->
CompletableFuture<List<Order>>
```

然后把：

```text
CompletableFuture<
    CompletableFuture<List<Order>>
>
```

压平成：

```text
CompletableFuture<List<Order>>
```

---

## 4. 类比 Optional / Stream 的 flatMap

你可以把：

```text
thenApply
```

类比：

```text
map
```

而：

```text
thenCompose
```

类比：

```text
flatMap
```

---

## 5. 什么时候必须想到 thenCompose

只要你看到：

```text
任务 B 是异步任务
并且任务 B 的参数依赖任务 A 的结果
```

优先想到：

```java
thenCompose
```

---

# 十一、并行任务合并：thenCombine

假设两个任务：

```text
A：查询用户信息
B：查询用户订单
```

互不依赖。

它们可以并行执行：

```java
CompletableFuture<User> userFuture =
        CompletableFuture.supplyAsync(
                () -> queryUser()
        );

CompletableFuture<List<Order>> orderFuture =
        CompletableFuture.supplyAsync(
                () -> queryOrders()
        );
```

两个都完成后组装：

```java
CompletableFuture<UserDetail> resultFuture =
        userFuture.thenCombine(
                orderFuture,
                (user, orders) -> {
                    return new UserDetail(user, orders);
                }
        );
```

结构：

```text
    userFuture --------┐
                       ├── thenCombine
    orderFuture -------┘
                              ↓
                        UserDetail
```

---

## 1. thenCombine 特别适合

```text
多个独立数据源
↓
并行查询
↓
聚合结果
```

例如：

```text
用户
+
积分
+
优惠券
+
订单
```

---

## 2. 和 thenCompose 的核心区别

### thenCompose

```text
B 依赖 A
```

关系：

```text
A
↓
B
```

---

### thenCombine

```text
A 和 B 独立
```

关系：

```text
A ──┐
    ├── C
B ──┘
```

---

# 十二、两个任务只等一个：applyToEither 等

CompletableFuture 还支持：

```text
两个任务
谁先完成
就用谁
```

---

## 1. applyToEither

```java
CompletableFuture<String> a =
        CompletableFuture.supplyAsync(
                () -> queryFromServerA()
        );

CompletableFuture<String> b =
        CompletableFuture.supplyAsync(
                () -> queryFromServerB()
        );

CompletableFuture<String> result =
        a.applyToEither(
                b,
                value -> value.toUpperCase()
        );
```

大致意思：

```text
A ──┐
    ├── 谁先正常完成就处理谁
B ──┘
```

---

## 2. acceptEither

只消费结果：

```java
a.acceptEither(
        b,
        result -> System.out.println(result)
);
```

---

## 3. runAfterEither

不关心结果：

```java
a.runAfterEither(
        b,
        () -> System.out.println("至少一个完成")
);
```

---

## 4. runAfterBoth

两个都完成后执行：

```java
a.runAfterBoth(
        b,
        () -> System.out.println("两个都完成")
);
```

---

## 5. thenAcceptBoth

两个都完成，并消费两个结果：

```java
a.thenAcceptBoth(
        b,
        (x, y) -> {
            System.out.println(x);
            System.out.println(y);
        }
);
```

---

# 十三、多个任务：allOf 与 anyOf

如果有：

```text
5 个
10 个
100 个
```

CompletableFuture，就不适合一直：

```java
a.thenCombine(b, ...)
 .thenCombine(c, ...)
 .thenCombine(d, ...);
```

这时可以使用：

```java
allOf
```

或者：

```java
anyOf
```

---

## 1. allOf

```java
CompletableFuture<Void> all =
        CompletableFuture.allOf(
                future1,
                future2,
                future3
        );
```

含义：

```text
等待所有 CompletableFuture 完成
```

注意返回值：

```java
CompletableFuture<Void>
```

它不会自动帮你把：

```text
future1 的结果
future2 的结果
future3 的结果
```

塞进 List。

---

## 2. allOf 后获取所有结果

例如：

```java
List<CompletableFuture<User>> futures = ...;

CompletableFuture<Void> all =
        CompletableFuture.allOf(
                futures.toArray(
                        new CompletableFuture[0]
                )
        );

CompletableFuture<List<User>> resultFuture =
        all.thenApply(v ->
                futures.stream()
                        .map(CompletableFuture::join)
                        .toList()
        );
```

这里为什么 `join()` 通常不会再真正等待很久？

因为：

```text
all 已经完成
=
所有 futures 都已经完成
```

因此后面逐个 `join()` 只是读取结果。

---

## 3. 批量并发经典模板

```java
List<CompletableFuture<Result>> futures =
        ids.stream()
                .map(id ->
                        CompletableFuture.supplyAsync(
                                () -> process(id),
                                executor
                        )
                )
                .toList();

CompletableFuture<List<Result>> allResult =
        CompletableFuture
                .allOf(
                        futures.toArray(
                                new CompletableFuture[0]
                        )
                )
                .thenApply(v ->
                        futures.stream()
                                .map(CompletableFuture::join)
                                .toList()
                );
```

这个模板在业务中非常实用。

---

## 4. anyOf

```java
CompletableFuture<Object> any =
        CompletableFuture.anyOf(
                future1,
                future2,
                future3
        );
```

含义：

```text
任意一个先完成
↓
整个 anyOf 完成
```

注意它返回：

```java
CompletableFuture<Object>
```

所以类型安全比较差。

---

# 十四、获取结果：get 与 join

CompletableFuture 继承 Future，所以可以：

```java
future.get();
```

同时它还提供：

```java
future.join();
```

两者非常常见。

---

## 1. get()

```java
String result = future.get();
```

会阻塞等待。

它会抛检查异常：

```java
InterruptedException
ExecutionException
```

所以通常：

```java
try {
    String result = future.get();
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
} catch (ExecutionException e) {
    ...
}
```

---

## 2. join()

```java
String result = future.join();
```

同样：

```text
如果未完成
=
阻塞等待
```

但是异常包装为：

```java
CompletionException
```

它是：

```text
RuntimeException
```

所以不强制写 checked exception 的 catch。

---

## 3. get 和 join 的核心区别

| 方法 | 是否阻塞 | 异常类型 |
|---|---:|---|
| `get()` | 是 | `InterruptedException` / `ExecutionException` |
| `join()` | 是 | `CompletionException` |
| `get(timeout, unit)` | 是 | 还可能 `TimeoutException` |

---

## 4. 业务里为什么 join 很常见

尤其是在：

```java
allOf(...).thenApply(v -> ...)
```

内部：

```java
future.join()
```

写起来很干净。

但不要产生误解：

> **join 不是非阻塞。**

它只是：

```text
异常处理方式比 get 更适合函数式链式代码
```

---

## 5. getNow

```java
String result =
        future.getNow("默认值");
```

如果 Future 已完成：

```text
返回真实结果
```

如果还没完成：

```text
立刻返回默认值
```

不会等待。

---

# 十五、异常处理：exceptionally / handle / whenComplete

这是 CompletableFuture 非常重要的一部分。

假设：

```java
CompletableFuture<Integer> future =
        CompletableFuture.supplyAsync(() -> {
            return 10 / 0;
        });
```

任务会：

```text
异常完成
```

---

## 1. exceptionally

最像：

```text
catch + 默认值
```

例如：

```java
CompletableFuture<Integer> future =
        CompletableFuture
                .supplyAsync(() -> 10 / 0)
                .exceptionally(ex -> {
                    System.out.println("失败：" + ex);
                    return 0;
                });
```

流程：

```text
正常
↓
直接传递正常结果

异常
↓
exceptionally
↓
返回兜底值
```

---

## 2. handle

`handle` 无论成功失败都会执行：

```java
future.handle((result, ex) -> {
    if (ex != null) {
        return 0;
    }

    return result * 2;
});
```

它同时拿到：

```text
result
exception
```

通常：

```text
成功：
result != null
ex == null

失败：
ex != null
```

---

## 3. whenComplete

```java
future.whenComplete((result, ex) -> {
    System.out.println("result = " + result);
    System.out.println("ex = " + ex);
});
```

它也：

```text
成功失败都会执行
```

但它主要用于：

```text
观察
记录日志
埋点
清理动作
```

而不是专门用于把结果转换成另一个结果。

---

## 4. 三者区别

### exceptionally

```text
只重点处理异常
异常 -> 恢复为正常结果
```

类似：

```text
catch
```

---

### handle

```text
成功失败都处理
并且可以返回一个新值
```

类似：

```text
try/catch 后统一转换
```

---

### whenComplete

```text
成功失败都能看到
主要做旁路动作
通常保持原结果/异常继续往后传
```

类似：

```text
观察最终状态
```

---

## 5. 一个推荐的思考方式

```text
我要异常兜底
→ exceptionally
```

```text
我要成功失败都统一转换
→ handle
```

```text
我要打印日志 / 监控 / 清理
→ whenComplete
```

---

## 6. 异常会沿链向后传播

```java
CompletableFuture<Integer> future =
        CompletableFuture
                .supplyAsync(() -> {
                    throw new RuntimeException("A失败");
                })
                .thenApply(x -> {
                    System.out.println("B");
                    return x + 1;
                })
                .thenApply(x -> {
                    System.out.println("C");
                    return x + 1;
                });
```

如果 A 异常：

```text
A：异常
↓
B：跳过
↓
C：跳过
↓
future：异常完成
```

直到遇到能够处理异常的阶段：

```java
.exceptionally(...)
```

或者：

```java
.handle(...)
```

---

# 十六、超时处理

JDK 9 之后 CompletableFuture 自带很实用的超时能力。

---

## 1. orTimeout

```java
future.orTimeout(
        2,
        TimeUnit.SECONDS
);
```

如果 2 秒内没完成：

```text
future 以 TimeoutException 异常完成
```

---

## 2. completeOnTimeout

```java
future.completeOnTimeout(
        "默认结果",
        2,
        TimeUnit.SECONDS
);
```

如果超时：

```text
直接用默认值完成
```

区别：

```text
orTimeout
=
超时 -> 异常
```

```text
completeOnTimeout
=
超时 -> 默认值
```

---

## 3. 业务场景

例如：

```text
调用推荐服务
最大允许 300ms

300ms 内有结果
→ 使用推荐结果

300ms 没结果
→ 返回默认推荐
```

可以：

```java
CompletableFuture<List<Item>> future =
        CompletableFuture
                .supplyAsync(
                        this::queryRecommend,
                        executor
                )
                .completeOnTimeout(
                        List.of(),
                        300,
                        TimeUnit.MILLISECONDS
                );
```

---

# 十七、手动完成 CompletableFuture

这是名字中：

```text
Completable
```

的一个重要来源。

你可以：

```java
CompletableFuture<String> future =
        new CompletableFuture<>();
```

此时还未完成：

```java
future.isDone(); // false
```

然后手动：

```java
future.complete("hello");
```

---

## 1. complete

```java
boolean success =
        future.complete("hello");
```

如果成功完成：

```text
true
```

如果它之前已经完成：

```text
false
```

CompletableFuture 一旦进入最终完成状态，普通 `complete` 不会再次改变它。

---

## 2. completeExceptionally

```java
future.completeExceptionally(
        new RuntimeException("失败")
);
```

表示：

```text
以异常状态完成
```

---

## 3. 典型使用场景

这种能力常用于：

```text
把回调式 API
转换成 CompletableFuture
```

例如某个第三方 API：

```java
client.request(
        new Callback() {
            @Override
            public void onSuccess(String data) {
                ...
            }

            @Override
            public void onFailure(Throwable e) {
                ...
            }
        }
);
```

可以包装：

```java
CompletableFuture<String> future =
        new CompletableFuture<>();

client.request(
        new Callback() {

            @Override
            public void onSuccess(String data) {
                future.complete(data);
            }

            @Override
            public void onFailure(Throwable e) {
                future.completeExceptionally(e);
            }
        }
);
```

于是传统回调 API：

```text
Callback
```

被转换成：

```text
CompletableFuture
```

后面就可以继续：

```java
future
        .thenApply(...)
        .thenCompose(...)
        .exceptionally(...);
```

---

# 十八、取消任务

因为 CompletableFuture 实现 Future，所以有：

```java
future.cancel(true);
```

但这里有一个非常容易误解的地方。

---

## 1. cancel 的结果

取消成功后：

```java
future.isCancelled(); // true
future.isDone();      // true
```

并且这个 CompletableFuture 会以：

```text
CancellationException
```

的方式完成。

---

## 2. mayInterruptIfRunning 对 CompletableFuture 不代表一定中断底层线程

例如：

```java
future.cancel(true);
```

很多人会以为：

```text
true
=
一定 interrupt 正在执行任务的线程
```

但对于 `CompletableFuture`：

> `mayInterruptIfRunning` 参数本身并不能像很多人想象的那样可靠地“杀掉”正在执行 supplyAsync 的底层线程。

`CompletableFuture` 的取消更应该理解成：

```text
把这个 Future 标记成取消/异常完成
```

而不是：

```text
强制终止底层计算
```

如果业务真的需要“可中断取消”，需要专门设计：

```text
线程中断
取消标记
底层 HTTP 客户端取消
任务协作式退出
```

等机制。

---

# 十九、CompletableFuture 与线程池

这是生产环境必须理解的一部分。

---

## 1. 不传 Executor

例如：

```java
CompletableFuture.supplyAsync(
        () -> query()
);
```

默认异步执行通常使用：

```java
ForkJoinPool.commonPool()
```

---

## 2. 指定 Executor

```java
CompletableFuture.supplyAsync(
        () -> query(),
        executor
);
```

生产业务一般更可控。

例如：

```java
ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
                8,
                16,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
```

然后：

```java
CompletableFuture<User> future =
        CompletableFuture.supplyAsync(
                this::queryUser,
                executor
        );
```

---

## 3. 为什么不建议所有业务都无脑 commonPool

因为 `commonPool`：

```text
是 JVM 级共享资源
```

你的代码用了它：

```text
别的代码也可能在用
```

如果你往里面塞大量：

```text
数据库查询
HTTP 调用
RPC
文件 I/O
长时间阻塞任务
```

就可能出现：

```text
线程被阻塞
↓
commonPool 资源被占满
↓
其他 CompletableFuture / parallelStream 等任务也被影响
```

这就出现：

```text
线程池资源相互污染
```

---

## 4. 推荐做线程池隔离

例如：

```text
订单服务调用
→ orderExecutor

推荐服务调用
→ recommendExecutor

批量数据处理
→ batchExecutor
```

至少对于：

```text
重要的高并发业务
阻塞型 I/O
不同 SLA 的任务
```

线程池隔离非常重要。

---

# 二十、ForkJoinPool.commonPool 到底是什么

你不用为了学习 CompletableFuture 立刻把 ForkJoinPool 全部学透。

你现在先知道：

```text
CompletableFuture 默认异步任务
通常会进入 ForkJoinPool.commonPool()
```

就够了。

---

## 1. commonPool 是全局共享线程池

可以：

```java
ForkJoinPool.commonPool();
```

获取。

它和普通 ThreadPoolExecutor 的设计理念并不完全一样。

ForkJoinPool 更擅长：

```text
可拆分的小计算任务
递归任务
CPU 型任务
```

例如：

```text
大任务
↓
拆成小任务
↓
多个线程并行处理
↓
再合并结果
```

---

## 2. CompletableFuture 为什么用它

因为：

```text
CompletableFuture
天生就是为了并发异步阶段编排
```

而 ForkJoinPool 提供了：

```text
低成本任务调度
工作窃取
并行执行
```

所以默认选择它。

---

## 3. 但业务阻塞任务不是它最理想的使用方式

例如：

```text
JDBC 查询 2 秒
HTTP 调用 3 秒
```

线程大量时间都在：

```text
等待外部 I/O
```

这和 ForkJoinPool 偏 CPU 计算的设计思想不完全一致。

因此：

> **业务后端里，CompletableFuture + 自定义 ThreadPoolExecutor 是非常常见的组合。**

---

# 二十一、CompletableFuture 与 Future / FutureTask 的关系

你之前已经学过：

```text
Future
FutureTask
```

现在需要把位置摆正。

---

## 1. Future

Future 是：

```text
结果凭证
```

典型：

```java
Future<Integer> future =
        pool.submit(callable);
```

能力主要是：

```text
get
cancel
isDone
```

---

## 2. FutureTask

FutureTask：

```text
Runnable + Future
```

它既：

```text
是任务
```

又：

```text
持有任务结果
```

可以：

```java
FutureTask<Integer> task =
        new FutureTask<>(callable);

new Thread(task).start();

Integer value = task.get();
```

---

## 3. CompletableFuture

CompletableFuture 更偏：

```text
异步计算结果
+
阶段编排
```

它并不是简单地：

```text
FutureTask 的升级版任务容器
```

你更应该把它理解成：

```text
一个可完成的“异步阶段节点”
```

---

## 4. 三者定位

```text
Future
=
未来结果接口
```

```text
FutureTask
=
可运行的一次性 Future 任务实现
```

```text
CompletableFuture
=
可以手动完成、可以链式组合、可以描述异步依赖的 Future
```

---

# 二十二、CompletableFuture 与 Stream API 为什么很像

你之前可能会感觉：

> CompletableFuture 很像 Stream API。

这个感觉是对的。

因为两者都有：

```text
声明式
链式
函数式
```

---

## 1. Stream

```java
users.stream()
        .filter(...)
        .map(...)
        .sorted(...)
        .toList();
```

表示：

```text
数据经过一连串转换
```

---

## 2. CompletableFuture

```java
CompletableFuture
        .supplyAsync(...)
        .thenApply(...)
        .thenCompose(...)
        .exceptionally(...);
```

表示：

```text
异步结果经过一连串阶段
```

---

## 3. 对比

```text
Stream
处理的是：
数据流
```

```text
CompletableFuture
处理的是：
异步结果流 / 异步阶段
```

---

## 4. thenApply 很像 map

```java
future.thenApply(x -> ...)
```

类似：

```java
stream.map(x -> ...)
```

---

## 5. thenCompose 很像 flatMap

```java
future.thenCompose(x ->
        CompletableFuture.supplyAsync(...)
);
```

类似：

```text
flatMap
```

解决嵌套。

---

# 二十三、业务开发最常见的使用场景

如果你以后是 Java 后端业务开发，最常遇到的 CompletableFuture 场景大概有下面这些。

---

## 场景 1：多个远程接口并行查询

例如一个页面要展示：

```text
用户信息
订单
优惠券
积分
推荐内容
```

这些接口互不依赖：

```text
并发执行
↓
allOf / thenCombine
↓
统一组装
```

---

## 场景 2：任务有前后依赖

例如：

```text
先查账号
↓
根据账号查权限
↓
根据权限加载菜单
```

使用：

```java
thenCompose
```

---

## 场景 3：批量处理

例如：

```text
100 个用户
↓
并发调用第三方接口
↓
收集 100 个结果
```

通常：

```text
List<CompletableFuture<T>>
+
allOf
```

---

## 场景 4：异步降级

例如：

```text
推荐服务最多等 300ms
```

超时：

```text
返回默认推荐
```

使用：

```java
completeOnTimeout
```

或：

```java
orTimeout
+ exceptionally
```

---

## 场景 5：主流程与非关键流程分离

例如：

```text
创建订单
↓
订单成功后

主流程：
返回订单

异步：
发通知
记录日志
更新画像
```

不过这里要注意：

```text
是否真的适合 CompletableFuture
```

如果要求：

```text
可靠投递
失败重试
服务重启不能丢
```

通常消息队列：

```text
RabbitMQ / Kafka
```

比进程内 CompletableFuture 更合适。

---

# 二十四、完整业务案例：并行查询再聚合

需求：

```text
GET /user/detail
```

需要查询：

```text
用户基本信息
订单
积分
优惠券
```

这四个接口彼此独立。

---

## 1. 定义线程池

```java
ExecutorService bizExecutor =
        new ThreadPoolExecutor(
                8,
                16,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
```

---

## 2. 并行创建任务

```java
CompletableFuture<User> userFuture =
        CompletableFuture.supplyAsync(
                () -> userService.getUser(userId),
                bizExecutor
        );

CompletableFuture<List<Order>> orderFuture =
        CompletableFuture.supplyAsync(
                () -> orderService.listOrders(userId),
                bizExecutor
        );

CompletableFuture<Integer> scoreFuture =
        CompletableFuture.supplyAsync(
                () -> scoreService.getScore(userId),
                bizExecutor
        );

CompletableFuture<List<Coupon>> couponFuture =
        CompletableFuture.supplyAsync(
                () -> couponService.listCoupons(userId),
                bizExecutor
        );
```

注意：

```text
这四个 supplyAsync 调用之后
四个任务都有机会并行执行
```

---

## 3. 等待全部完成

```java
CompletableFuture<Void> all =
        CompletableFuture.allOf(
                userFuture,
                orderFuture,
                scoreFuture,
                couponFuture
        );
```

---

## 4. 组装结果

```java
CompletableFuture<UserDetailVO> resultFuture =
        all.thenApply(v -> {

            UserDetailVO vo =
                    new UserDetailVO();

            vo.setUser(userFuture.join());
            vo.setOrders(orderFuture.join());
            vo.setScore(scoreFuture.join());
            vo.setCoupons(couponFuture.join());

            return vo;
        });
```

最后：

```java
return resultFuture.join();
```

---

## 5. 时间模型

如果：

```text
user      200ms
order     500ms
score     150ms
coupon    300ms
```

同步：

```text
1150ms
```

并发后理论接近：

```text
500ms + 少量调度开销
```

---

# 二十五、完整业务案例：有依赖的异步调用

需求：

```text
先查用户
↓
拿用户所属学校 schoolId
↓
再查学校
↓
组装 DTO
```

---

## 1. 查询用户

```java
CompletableFuture<User> userFuture =
        CompletableFuture.supplyAsync(
                () -> userService.getById(userId),
                executor
        );
```

---

## 2. 根据用户结果再异步查学校

```java
CompletableFuture<School> schoolFuture =
        userFuture.thenCompose(user ->
                CompletableFuture.supplyAsync(
                        () ->
                                schoolService.getById(
                                        user.getSchoolId()
                                ),
                        executor
                )
        );
```

这里重点：

```text
查询学校必须等用户结果
```

所以：

```text
不能和查询用户同时启动
```

因此使用：

```java
thenCompose
```

---

## 3. 合并

如果还需要用户本身：

```java
CompletableFuture<UserSchoolVO> result =
        userFuture.thenCombine(
                schoolFuture,
                (user, school) -> {
                    UserSchoolVO vo =
                            new UserSchoolVO();

                    vo.setUser(user);
                    vo.setSchool(school);

                    return vo;
                }
        );
```

---

# 二十六、完整业务案例：批量并发处理

需求：

```text
有 100 个 accountId
需要调用第三方 API 查询数据
```

最自然的写法：

```java
List<CompletableFuture<Data>> futures =
        accountIds.stream()
                .map(accountId ->
                        CompletableFuture.supplyAsync(
                                () ->
                                        thirdApi.query(
                                                accountId
                                        ),
                                executor
                        )
                )
                .toList();
```

等待全部：

```java
CompletableFuture<Void> all =
        CompletableFuture.allOf(
                futures.toArray(
                        new CompletableFuture[0]
                )
        );
```

收集：

```java
List<Data> result =
        all.thenApply(v ->
                futures.stream()
                        .map(CompletableFuture::join)
                        .toList()
        ).join();
```

---

## 但是这里有一个生产环境重点

不要看到：

```text
100 个
1000 个
10000 个
```

就无脑全部：

```java
supplyAsync
```

你真正的并发度取决于：

```text
线程池大小
队列
第三方接口限流
数据库连接池
下游服务容量
机器资源
```

例如：

```text
10000 条数据
```

你一次创建 10000 个异步任务：

```text
并不等于 10000 个线程
```

但是：

```text
可能把 10000 个任务全部塞进线程池队列
```

仍然可能造成：

```text
内存压力
排队时间过长
下游压力
超时
```

因此批量场景要考虑：

```text
分批
限并发
信号量
合理线程池
下游限流
```

---

# 二十七、常见坑与生产环境注意事项

这一章非常重要。

---

## 坑 1：以为 supplyAsync 就等于“性能一定提升”

不是。

如果原来一个任务只需要：

```text
2ms
```

你把它异步化之后：

```text
线程切换
任务提交
队列调度
Future 创建
```

反而可能更慢。

CompletableFuture 适合：

```text
可以并行
并且单个任务有一定耗时
```

的场景。

---

## 坑 2：异步之后立刻 join

例如：

```java
String a =
        CompletableFuture
                .supplyAsync(this::queryA)
                .join();

String b =
        CompletableFuture
                .supplyAsync(this::queryB)
                .join();
```

这实际上：

```text
启动 A
↓
立刻等 A
↓
A 完
↓
再启动 B
↓
再等 B
```

没有真正实现 A/B 并发。

正确：

```java
CompletableFuture<String> a =
        CompletableFuture.supplyAsync(
                this::queryA
        );

CompletableFuture<String> b =
        CompletableFuture.supplyAsync(
                this::queryB
        );

String av = a.join();
String bv = b.join();
```

先：

```text
把任务都发出去
```

然后再等结果。

---

## 坑 3：无脑使用 commonPool

业务系统最好考虑：

```text
自定义线程池
容量
隔离
线程命名
监控
拒绝策略
```

尤其是：

```text
HTTP
RPC
数据库
文件
```

这种阻塞型任务。

---

## 坑 4：thenApply 不是“异步版 map”

`thenApply`：

```text
不保证切线程
```

如果你明确希望新阶段异步调度：

```java
thenApplyAsync(...)
```

---

## 坑 5：Async 也不要无脑用

例如：

```java
.thenApplyAsync(User::getName)
```

只是：

```text
getName()
```

这样非常轻的操作，额外切线程不一定有意义。

---

## 坑 6：忘记处理异常

例如：

```java
CompletableFuture<User> future =
        CompletableFuture.supplyAsync(
                this::queryUser
        );
```

如果异常：

```text
Future 会异常完成
```

直到：

```java
join()
```

才抛：

```text
CompletionException
```

建议关键链路明确设计：

```text
异常传播
兜底
日志
超时
```

---

## 坑 7：把 CompletableFuture 当可靠消息系统

例如：

```java
CompletableFuture.runAsync(
        () -> sendImportantPaymentMessage()
);
```

然后 HTTP 请求直接返回。

如果此时：

```text
JVM 崩溃
应用重启
机器宕机
```

任务可能直接丢。

所以对于：

```text
必须成功
必须重试
必须持久化
```

的任务：

```text
MQ / 任务调度系统
```

通常更合适。

---

## 坑 8：ThreadLocal 上下文不会天然传播

假设请求线程里：

```java
ThreadLocal<User> currentUser
```

然后：

```java
CompletableFuture.supplyAsync(...)
```

异步线程是：

```text
线程池里的另一个线程
```

它默认不会自动得到请求线程的普通 ThreadLocal 值。

这会影响：

```text
登录上下文
traceId
MDC
租户信息
语言环境
事务上下文
```

需要专门处理上下文传递。

---

## 坑 9：事务不会自动跨异步线程

例如：

```java
@Transactional
public void method() {

    updateA();

    CompletableFuture.runAsync(
            () -> updateB()
    );
}
```

不要简单认为：

```text
updateA 和 updateB
处于同一个 Spring 事务
```

Spring 常规事务上下文通常绑定在线程上。

异步线程：

```text
是另一个线程
```

因此事务边界通常已经不同。

---

## 坑 10：线程池过小 + 内部阻塞等待，可能出现饥饿甚至死锁

假设：

```text
线程池只有 1 个线程
```

任务 A 在这个线程中执行：

```text
提交任务 B 到同一个线程池
↓
A 又 join B
```

但：

```text
唯一线程正在执行 A
```

B 永远没线程执行。

于是：

```text
A 等 B
B 等线程
线程被 A 占着
```

形成死锁/线程饥饿。

核心原则：

> 不要在受限线程池中随意让任务同步等待同一个池里的后续任务。

---

## 坑 11：并发不是越大越好

假设数据库连接池：

```text
20
```

你异步线程池：

```text
200
```

同时 200 个 JDBC 查询。

最终：

```text
180 个线程
可能只是在等数据库连接
```

这并不会神奇提升性能。

系统吞吐量受：

```text
最弱资源
```

限制。

---

# 二十八、和 Spring 项目结合时要注意什么

CompletableFuture 在 Spring Boot 业务里非常常见。

---

## 1. 推荐把线程池交给 Spring 管理

例如：

```java
@Bean("bizExecutor")
public Executor bizExecutor() {

    ThreadPoolTaskExecutor executor =
            new ThreadPoolTaskExecutor();

    executor.setCorePoolSize(8);
    executor.setMaxPoolSize(16);
    executor.setQueueCapacity(200);
    executor.setThreadNamePrefix(
            "biz-async-"
    );

    executor.initialize();

    return executor;
}
```

使用：

```java
@Resource(name = "bizExecutor")
private Executor bizExecutor;
```

然后：

```java
CompletableFuture.supplyAsync(
        () -> service.query(),
        bizExecutor
);
```

这样更方便：

```text
统一配置
统一监控
线程命名
依赖注入
```

---

## 2. CompletableFuture 和 @Async 不是一回事

`@Async`：

```text
Spring 提供的异步方法机制
```

例如：

```java
@Async
public CompletableFuture<User> queryUser() {
    ...
}
```

而 CompletableFuture：

```text
JDK 自带异步编排工具
```

两者可以结合。

但不要混淆：

```text
@Async
解决：
“这个 Spring 方法异步执行”
```

```text
CompletableFuture
解决：
“异步结果怎么组合、依赖、转换、处理异常”
```

---

## 3. 自调用问题

Spring `@Async` 和 `@Transactional` 一样涉及代理。

例如：

```java
this.asyncMethod();
```

可能绕过代理，导致：

```text
@Async 不生效
```

这个问题属于 Spring AOP 范畴，不是 CompletableFuture 自身的问题。

---

# 二十九、常用 API 速查表

## 1. 创建

| API | 作用 |
|---|---|
| `runAsync(Runnable)` | 异步执行，无返回值 |
| `supplyAsync(Supplier<T>)` | 异步执行，有返回值 |
| `completedFuture(value)` | 创建已正常完成的 Future |
| `failedFuture(ex)` | 创建已异常完成的 Future |
| `new CompletableFuture<>()` | 创建未完成 Future |

---

## 2. 单个阶段转换

| API | 函数模型 | 作用 |
|---|---|---|
| `thenApply` | `T -> U` | 转换结果 |
| `thenAccept` | `T -> void` | 消费结果 |
| `thenRun` | `() -> void` | 完成后执行动作 |

---

## 3. 串行异步依赖

| API | 函数模型 | 作用 |
|---|---|---|
| `thenCompose` | `T -> CompletionStage<U>` | 下一个异步任务依赖当前结果，并压平 Future |

---

## 4. 两个任务组合

| API | 作用 |
|---|---|
| `thenCombine` | 两个都完成后合并结果 |
| `thenAcceptBoth` | 两个都完成后消费两个结果 |
| `runAfterBoth` | 两个都完成后执行动作 |
| `applyToEither` | 任意一个完成后转换其结果 |
| `acceptEither` | 任意一个完成后消费结果 |
| `runAfterEither` | 任意一个完成后执行动作 |

---

## 5. 多任务

| API | 作用 |
|---|---|
| `allOf` | 所有任务完成 |
| `anyOf` | 任意一个任务完成 |

---

## 6. 异常

| API | 作用 |
|---|---|
| `exceptionally` | 失败兜底 |
| `handle` | 成功失败都处理并转换 |
| `whenComplete` | 成功失败都观察 |

---

## 7. 等待结果

| API | 作用 |
|---|---|
| `get()` | 阻塞获取，checked exception |
| `join()` | 阻塞获取，CompletionException |
| `getNow(default)` | 不阻塞，未完成则返回默认值 |

---

## 8. 超时

| API | 作用 |
|---|---|
| `orTimeout` | 超时后异常完成 |
| `completeOnTimeout` | 超时后用默认值完成 |

---

## 9. 手动完成

| API | 作用 |
|---|---|
| `complete(value)` | 正常完成 |
| `completeExceptionally(ex)` | 异常完成 |

---

# 三十、最容易混淆的 API 对比

---

## 1. thenApply vs thenCompose

### thenApply

```java
future.thenApply(x -> process(x));
```

其中：

```java
process(x)
```

直接返回：

```text
普通值 U
```

---

### thenCompose

```java
future.thenCompose(
        x -> asyncProcess(x)
);
```

其中：

```java
asyncProcess(x)
```

返回：

```text
CompletableFuture<U>
```

---

## 最简单记忆

```text
返回普通值
→ thenApply
```

```text
返回 Future
→ thenCompose
```

---

## 2. thenCompose vs thenCombine

```text
thenCompose：
有依赖
A → B
```

```text
thenCombine：
无依赖
A ─┐
   ├→ C
B ─┘
```

---

## 3. thenApply vs thenAccept vs thenRun

```text
thenApply
有参数
有返回
```

```text
thenAccept
有参数
无返回
```

```text
thenRun
无参数
无返回
```

---

## 4. exceptionally vs handle vs whenComplete

```text
exceptionally
重点处理失败
可以恢复
```

```text
handle
成功失败都处理
可以产生新结果
```

```text
whenComplete
成功失败都观察
主要用于日志/清理/监控
```

---

## 5. get vs join

```text
get
checked exception
```

```text
join
unchecked CompletionException
```

两者都可能：

```text
阻塞
```

---

## 6. 非 Async vs Async

```text
thenApply
=
不强制异步重新调度
```

```text
thenApplyAsync
=
异步调度后续阶段
```

不要把：

```text
非 Async
```

错误理解成：

```text
一定主线程
```

---

# 三十一、面试常问问题

---

## 1. CompletableFuture 和 Future 有什么区别？

回答重点：

```text
Future：
主要用于表示异步任务结果
只能 get / cancel / 判断状态
组合能力弱
```

```text
CompletableFuture：
实现 Future + CompletionStage
支持任务链式编排
结果转换
多个任务组合
异常处理
超时
手动完成
```

---

## 2. thenApply 和 thenCompose 有什么区别？

```text
thenApply：
T -> U
```

```text
thenCompose：
T -> CompletionStage<U>
```

thenCompose 用于：

```text
异步任务依赖
并解决 Future 嵌套
```

---

## 3. thenCompose 和 thenCombine 有什么区别？

```text
thenCompose：
任务 B 依赖任务 A
```

```text
thenCombine：
两个任务彼此独立
完成后合并
```

---

## 4. CompletableFuture 默认线程池是什么？

通常：

```java
ForkJoinPool.commonPool()
```

但生产环境阻塞业务经常建议：

```text
传入自定义 Executor
```

进行线程池隔离和容量控制。

---

## 5. thenApply 和 thenApplyAsync 区别？

```text
thenApply：
不强制切到异步执行器
可能由完成上游的线程继续执行
```

```text
thenApplyAsync：
会异步调度
不指定 Executor 时通常使用默认异步线程池
```

---

## 6. get 和 join 区别？

```text
两者都会等待
```

但：

```text
get：
ExecutionException
InterruptedException
```

```text
join：
CompletionException
```

---

## 7. allOf 为什么返回 Void？

因为：

```text
allOf 只负责表达
“所有任务都完成”
```

而各任务结果本身仍然保存在：

```text
各自的 CompletableFuture
```

所以一般：

```java
allOf(...)
        .thenApply(v ->
                futures.stream()
                        .map(
                            CompletableFuture::join
                        )
                        .toList()
        );
```

---

## 8. CompletableFuture cancel(true) 会中断线程吗？

不能简单认为：

```text
一定会。
```

对于 CompletableFuture：

```text
mayInterruptIfRunning
并不像 FutureTask 那样可以理解为可靠的线程中断控制
```

取消更主要是：

```text
让 CompletableFuture 进入取消状态
```

真正终止底层动作需要：

```text
协作式取消
中断机制
底层客户端取消
```

等设计。

---

## 9. CompletableFuture 是否一定能提高性能？

不能。

它只有在：

```text
多个任务可以并行
且任务耗时足以覆盖异步调度成本
```

时更可能提升整体响应时间。

---

## 10. CompletableFuture 有哪些风险？

常见：

```text
线程池耗尽
commonPool 污染
任务过量
上下文丢失
事务跨线程失效
异常漏处理
异步任务丢失
并发冲击下游
线程饥饿/死锁
```

---

# 三十二、学习优先级：哪些必须会，哪些先了解

如果你的目标是：

```text
尽快进入 Java 后端业务开发
```

CompletableFuture 不需要第一次就把所有 API 背下来。

---

## 第一优先级：必须掌握

这些你应该真正会写：

```text
CompletableFuture.supplyAsync
CompletableFuture.runAsync

thenApply
thenAccept
thenRun

thenCompose
thenCombine

allOf

get
join

exceptionally
handle
whenComplete

自定义 Executor
```

如果这些会了：

> **已经足够覆盖绝大多数日常业务 CompletableFuture 场景。**

---

## 第二优先级：很值得会

```text
orTimeout
completeOnTimeout

anyOf

applyToEither

complete
completeExceptionally

getNow
```

---

## 第三优先级：先知道存在即可

CompletionStage 还有很多更边缘的 API。

例如：

```text
runAfterBoth
runAfterEither
thenAcceptBoth
acceptEither
delayedExecutor
obtrudeValue
obtrudeException
minimalCompletionStage
copy
```

这些：

```text
第一次学习不用死磕
```

真正工作遇到时：

```text
再查 API
```

完全没问题。

---

# 三十三、最终知识地图

最后把整个 CompletableFuture 压缩成一张图。

```text
                          CompletableFuture
                                  │
                 ┌────────────────┴────────────────┐
                 │                                 │
              Future                        CompletionStage
                 │                                 │
          获取未来结果                        编排异步阶段
                 │                                 │
       ┌─────────┼─────────┐            ┌──────────┼───────────┐
       │         │         │            │          │           │
      get       join      cancel      转换        组合         异常
                                       │          │           │
                           ┌───────────┼───┐      │        ┌──┼─────────┐
                           │           │   │      │        │  │         │
                       thenApply   thenAccept thenRun   exceptionally handle whenComplete
                                                   │
                    ┌──────────────────────────────┼──────────────────────────────┐
                    │                              │                              │
                 串行依赖                        并行合并                       多任务
                    │                              │                              │
               thenCompose                   thenCombine                       allOf
                                                                             anyOf
```

再用一句非常重要的话结束：

```text
Future 的思维：

“我提交了一个任务，
以后我要怎么拿它的结果？”
```

而 CompletableFuture 的思维应该变成：

```text
“我的业务由多个计算阶段组成。

哪些可以并行？
哪些有依赖？
哪些结果需要转换？
哪些任务完成后需要合并？
异常怎么传播？
超时怎么处理？
这些阶段应该跑在哪个线程池？”
```

这才是学习 CompletableFuture 真正要建立的能力。

---

# 附录 A：最推荐背下来的业务模板

## 模板 1：单个异步任务

```java
CompletableFuture<User> future =
        CompletableFuture.supplyAsync(
                () -> userService.getById(userId),
                executor
        );
```

---

## 模板 2：两个独立任务并行

```java
CompletableFuture<User> userFuture =
        CompletableFuture.supplyAsync(
                this::queryUser,
                executor
        );

CompletableFuture<List<Order>> orderFuture =
        CompletableFuture.supplyAsync(
                this::queryOrders,
                executor
        );

UserDetailVO result =
        userFuture
                .thenCombine(
                        orderFuture,
                        UserDetailVO::new
                )
                .join();
```

---

## 模板 3：第二个异步任务依赖第一个

```java
CompletableFuture<List<Order>> result =
        CompletableFuture
                .supplyAsync(
                        this::queryUser,
                        executor
                )
                .thenCompose(user ->
                        CompletableFuture.supplyAsync(
                                () -> queryOrders(
                                        user.getId()
                                ),
                                executor
                        )
                );
```

---

## 模板 4：批量并发 + allOf

```java
List<CompletableFuture<Result>> futures =
        ids.stream()
                .map(id ->
                        CompletableFuture.supplyAsync(
                                () -> process(id),
                                executor
                        )
                )
                .toList();

List<Result> result =
        CompletableFuture
                .allOf(
                        futures.toArray(
                                new CompletableFuture[0]
                        )
                )
                .thenApply(v ->
                        futures.stream()
                                .map(
                                    CompletableFuture::join
                                )
                                .toList()
                )
                .join();
```

---

## 模板 5：异常兜底

```java
CompletableFuture<Result> future =
        CompletableFuture
                .supplyAsync(
                        this::query,
                        executor
                )
                .exceptionally(ex -> {
                    log.error(
                            "异步查询失败",
                            ex
                    );

                    return Result.empty();
                });
```

---

## 模板 6：超时降级

```java
CompletableFuture<Result> future =
        CompletableFuture
                .supplyAsync(
                        this::query,
                        executor
                )
                .completeOnTimeout(
                        Result.empty(),
                        500,
                        TimeUnit.MILLISECONDS
                );
```

---

# 附录 B：学习时建议亲手敲的 8 个练习

建议不要一下做几十道题。

真正把下面 8 个敲通就很有价值：

```text
1. supplyAsync 返回一个数字，join 获取。

2. supplyAsync
   → thenApply
   → thenApply
   做三段结果转换。

3. supplyAsync
   → thenAccept
   看最终为什么是 CompletableFuture<Void>。

4. 用户查询
   → thenCompose
   → 根据 userId 查询订单。

5. 用户查询 + 订单查询并行
   → thenCombine
   → 组装 DTO。

6. 创建 10 个 CompletableFuture
   → allOf
   → 收集 List。

7. 某一个任务主动抛异常
   → exceptionally / handle / whenComplete
   分别观察效果。

8. 创建一个只有 2 个线程的自定义线程池，
   给每个任务打印线程名，
   对比 thenApply 和 thenApplyAsync。
```

如果这 8 个你能独立写出来：

> 你的 CompletableFuture 已经不是“看懂 API”，而是真正开始具备业务使用能力了。

---

# 附录 C：你现阶段最应该记住的 10 句话

```text
1. CompletableFuture 不只是 Future，它更重要的是 CompletionStage。

2. supplyAsync 有结果，runAsync 无结果。

3. thenApply = T -> U。

4. thenAccept = T -> void。

5. thenRun = () -> void。

6. thenCompose 用于“下一个异步任务依赖当前结果”。

7. thenCombine 用于“两个独立任务并行完成后合并”。

8. allOf 用于“多个任务全部完成”，但本身返回 Void。

9. join 也会阻塞，只是异常是 CompletionException。

10. 生产环境真正重要的不只是 API，而是线程池、超时、异常、并发度和下游承载能力。
```

---

# 结束

如果把 Java 并发学习路径放在一起，你现在可以这样理解：

```text
Runnable / Callable
        ↓
描述任务

Executor / ExecutorService
        ↓
提交任务

ThreadPoolExecutor
        ↓
控制任务怎么在线程池里执行

Future / FutureTask
        ↓
拿异步结果

CompletableFuture
        ↓
把多个异步任务真正组织成业务流程
```

所以 CompletableFuture 确实是 Java 多线程业务开发中非常重要、非常实用的一块。

但它“强大”的真正原因，不是 API 多，而是：

> **它让你能够直接用代码表达异步业务之间的依赖关系。**
