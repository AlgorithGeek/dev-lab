# Java 多线程：Future 与 FutureTask

------

# 一、先用一句话认识 Future 和 FutureTask

学习 `Future` 和 `FutureTask` 时，最重要的是先建立下面这个认识：

```text
Future
    ↓
描述“一个异步任务未来的执行结果”

FutureTask
    ↓
是一个真正可以被执行的任务，
同时又可以保存并获取未来的执行结果
```

也就是说：

```text
Future = 结果凭证 / 查询接口

FutureTask = 任务本身 + Future 结果凭证
```

可以粗略理解成：

```text
Future
像一张“取餐小票”

FutureTask
像一个“既能自己去厨房做饭，
又会保存最终结果的任务对象”
```

------

# 二、为什么会出现 Future？

在普通单线程代码中，我们调用一个方法：

```java
int result = calculate();
```

调用者必须等：

```text
calculate()
    ↓
执行完成
    ↓
返回结果
    ↓
result 拿到值
```

这是典型的：

```text
同步调用
```

例如：

```java
public static int calculate() {
    try {
        Thread.sleep(3000);
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }

    return 100;
}
```

执行：

```java
int result = calculate();

System.out.println(result);
```

当前线程会在：

```java
calculate()
```

里面等待三秒。

------

但是多线程的意义之一就是：

```text
耗时任务交给别的线程执行

当前线程继续干其他事情
```

例如：

```text
主线程
    │
    ├── 把任务交给线程池
    │
    ├── 继续执行其他代码
    │
    └── 等真正需要结果的时候
            ↓
         获取任务结果
```

问题来了：

> 任务已经交给另外一个线程执行了，我现在没有结果，那以后怎么拿到这个结果？

于是就需要一个东西代表：

```text
“这个任务未来会产生的结果”
```

这就是：

```java
Future
```

------

# 三、Future 到底是什么？

`Future` 是一个接口：

```java
public interface Future<V> {

    boolean cancel(boolean mayInterruptIfRunning);

    boolean isCancelled();

    boolean isDone();

    V get()
        throws InterruptedException, ExecutionException;

    V get(long timeout, TimeUnit unit)
        throws InterruptedException,
               ExecutionException,
               TimeoutException;
}
```

注意：

```text
Future 自己不负责执行任务。
```

它主要负责：

```text
1. 查询任务有没有完成

2. 查询任务有没有被取消

3. 取消任务

4. 获取任务结果

5. 等待任务结果
```

所以：

```text
Future 关注的是：

“任务现在怎么样了？”
“任务结果是什么？”
```

而不是：

```text
“任务应该怎么执行？”
```

------

# 四、Future 最重要的设计思想

Future 可以理解为：

```text
未来结果的占位符
```

比如：

```java
Future<Integer> future;
```

此时：

```text
future
```

里面并不一定已经有：

```text
Integer
```

而是：

```text
未来某个时间点
任务执行完成后
可以通过 future 拿到 Integer
```

所以叫：

```text
Future
```

也就是：

```text
未来
```

------

# 五、Callable 为什么经常和 Future 一起出现？

我们之前学过：

```java
Runnable
```

特点：

```java
public interface Runnable {

    void run();
}
```

没有返回值。

------

而：

```java
Callable<V>
```

是：

```java
public interface Callable<V> {

    V call() throws Exception;
}
```

它可以：

```text
① 返回结果

② 抛出异常
```

例如：

```java
Callable<Integer> task = () -> {

    Thread.sleep(3000);

    return 100;
};
```

问题是：

```text
call() 最终返回 100

但是这个任务如果由另外一个线程执行，
当前线程怎么拿到这个 100？
```

于是：

```text
Callable
    ↓
执行产生结果
    ↓
Future
    ↓
保存 / 获取未来结果
```

它们天然非常搭。

------

# 六、最常见的 Future 使用方式

实际开发中，你最常看到的通常不是：

```java
new Future(...)
```

因为：

```text
Future 是接口
```

而是：

```java
ExecutorService.submit(...)
```

例如：

```java
ExecutorService executorService =
        Executors.newFixedThreadPool(2);

Future<Integer> future =
        executorService.submit(() -> {

            Thread.sleep(3000);

            return 100;
        });

System.out.println("主线程继续运行");

Integer result = future.get();

System.out.println("结果：" + result);

executorService.shutdown();
```

执行流程：

```text
主线程
  │
  ├─ submit(task)
  │      │
  │      └─ 把任务交给线程池
  │
  ├─ 立即拿到 Future
  │
  ├─ 打印：
  │      主线程继续运行
  │
  └─ future.get()
          │
          ├─ 如果任务没完成
          │      ↓
          │    阻塞等待
          │
          └─ 任务完成
                 ↓
               返回 100
```

------

# 七、submit() 是阻塞的吗？

这是非常重要的一点。

一般来说：

```java
submit()
```

本身：

```text
不会等待任务执行完成。
```

也就是说：

```java
Future<Integer> future =
        executorService.submit(task);
```

执行到这里时：

```text
任务可能还根本没有开始执行
```

也可能：

```text
正在执行
```

也可能：

```text
刚好已经执行完成
```

但是：

```java
submit()
```

不会因为任务需要 10 秒：

```text
就跟着等 10 秒。
```

------

例如：

```java
Future<Integer> future =
        executorService.submit(() -> {

            Thread.sleep(5000);

            return 100;
        });

System.out.println("submit 后面的代码");
```

通常：

```text
submit 后面的代码
```

会马上执行。

------

所以：

```text
submit()
    ↓
提交任务
    ↓
返回 Future
```

而不是：

```text
submit()
    ↓
等待任务完成
    ↓
返回 Future
```

------

真正可能发生阻塞的，是：

```java
future.get();
```

------

# 八、Future.get() 为什么会阻塞？

假设：

```java
Future<Integer> future =
        executorService.submit(task);
```

任务需要：

```text
5 秒
```

但是主线程：

```text
1 秒以后
```

执行：

```java
future.get();
```

问题是：

```text
任务还没有完成
```

那 Future 不可能凭空返回结果。

所以：

```text
当前线程只能等待
```

于是：

```java
future.get();
```

发生阻塞。

------

执行过程：

```text
Worker Thread
     │
     │ 执行任务
     │
     │ 需要 5 秒
     │
     ▼

Main Thread
     │
     ├─ submit()
     │
     ├─ 做其他事情
     │
     └─ get()
          │
          │ 结果还没出来
          │
          ▼
        WAITING
          │
          │
Worker 执行完成
          │
          ▼
       唤醒主线程
          │
          ▼
       返回结果
```

------

# 九、如果任务已经执行完成，get() 还会阻塞吗？

不会。

例如：

```java
Future<Integer> future =
        executorService.submit(() -> {

            Thread.sleep(1000);

            return 100;
        });

Thread.sleep(3000);

Integer result = future.get();
```

因为主线程已经睡了：

```text
3 秒
```

而任务：

```text
1 秒就完成
```

所以执行：

```java
future.get();
```

时：

```text
结果早就准备好了
```

于是：

```java
get()
```

直接返回。

因此要准确地说：

```text
Future.get()
不是“一定阻塞”

而是：

如果任务没有完成，就阻塞；
如果任务已经完成，就立即返回。
```

------

# 十、Future 的核心方法

------

## 10.1 get()

```java
V get()
```

作用：

```text
获取任务执行结果
```

如果任务：

```text
已经完成
```

直接返回。

如果任务：

```text
还没有完成
```

当前线程等待。

------

例如：

```java
Integer result = future.get();
```

------

## 10.2 get(timeout, unit)

```java
V get(long timeout, TimeUnit unit)
```

例如：

```java
Integer result =
        future.get(3, TimeUnit.SECONDS);
```

意思：

```text
我最多等 3 秒。
```

如果：

```text
3 秒内任务完成
```

返回结果。

如果：

```text
超过 3 秒还没完成
```

抛出：

```java
TimeoutException
```

------

这通常比：

```java
get()
```

无限等待更加安全。

------

例如：

```java
try {

    Integer result =
            future.get(3, TimeUnit.SECONDS);

} catch (TimeoutException e) {

    System.out.println("任务执行超时");
}
```

------

# 十一、Future.get() 可能抛出的异常

这是 Future 非常重要的一部分。

------

## 11.1 InterruptedException

```java
InterruptedException
```

表示：

```text
当前调用 get() 正在等待结果的线程
被别人 interrupt() 了
```

注意：

不是说：

```text
任务线程一定被中断了
```

而是：

```text
等待 Future.get() 的这个线程
被中断
```

例如：

```java
Thread mainThread = Thread.currentThread();

Future<Integer> future =
        executorService.submit(() -> {

            Thread.sleep(10000);

            return 100;
        });
```

如果主线程执行：

```java
future.get();
```

等待期间被其他线程：

```java
mainThread.interrupt();
```

那么：

```java
get()
```

会抛出：

```java
InterruptedException
```

------

## 11.2 ExecutionException

这个非常重要。

假设任务内部：

```java
throw new RuntimeException("任务出错了");
```

例如：

```java
Future<Integer> future =
        executorService.submit(() -> {

            int x = 1 / 0;

            return x;
        });
```

任务线程内部发生：

```text
ArithmeticException
```

但是这个异常并不会直接在：

```text
submit()
```

那里抛给主线程。

因为：

```text
执行任务的是线程池线程
```

而不是主线程。

------

Future 会保存：

```text
任务执行失败的信息
```

等你：

```java
future.get();
```

的时候：

```java
get()
```

抛出：

```java
ExecutionException
```

真正的异常可以通过：

```java
e.getCause();
```

获取。

例如：

```java
try {

    Integer result = future.get();

} catch (ExecutionException e) {

    Throwable cause = e.getCause();

    System.out.println(cause);
}
```

可能输出：

```text
java.lang.ArithmeticException: / by zero
```

------

所以：

```text
任务异常
      ↓
Future 保存
      ↓
get()
      ↓
ExecutionException
      ↓
getCause()
      ↓
真正的任务异常
```

------

# 十二、isDone()

```java
boolean isDone()
```

作用：

```text
判断任务是不是已经结束
```

例如：

```java
if (future.isDone()) {

    System.out.println("任务结束了");
}
```

但是有个很重要的细节：

```text
isDone() == true
```

不一定意味着：

```text
任务正常成功
```

它可能是：

```text
正常完成

异常完成

被取消
```

都属于：

```text
Done
```

也就是：

```text
任务已经没有继续执行了
```

------

# 十三、isCancelled()

```java
boolean isCancelled()
```

判断：

```text
任务是否被取消
```

例如：

```java
if (future.isCancelled()) {

    System.out.println("任务已被取消");
}
```

------

# 十四、cancel()

Future 还可以尝试取消任务：

```java
boolean cancel(boolean mayInterruptIfRunning);
```

这里的参数非常重要：

```java
mayInterruptIfRunning
```

意思：

```text
如果任务已经正在运行，
是否允许通过 interrupt()
尝试中断任务线程
```

------

例如：

```java
future.cancel(true);
```

表示：

```text
如果任务还没开始
    ↓
取消它

如果任务已经运行
    ↓
尝试 interrupt 执行任务的线程
```

------

而：

```java
future.cancel(false);
```

表示：

```text
如果任务已经开始运行
不要主动 interrupt 它
```

------

# 十五、cancel(true) 不等于强制停止线程

这是非常重要的知识点。

```java
future.cancel(true);
```

不是：

```text
杀死线程
```

而是：

```text
尝试通过 interrupt()
通知任务停止
```

所以任务代码是否真的停止：

```text
取决于任务是否响应中断
```

------

例如：

```java
Future<?> future =
        executorService.submit(() -> {

            while (!Thread.currentThread().isInterrupted()) {

                System.out.println("执行任务");

            }
        });
```

调用：

```java
future.cancel(true);
```

线程发现：

```java
isInterrupted() == true
```

退出循环。

这种就是：

```text
良好的中断响应
```

------

但是如果任务：

```java
while (true) {

    int x = 1 + 1;
}
```

完全不检查：

```java
isInterrupted()
```

也没有执行：

```text
sleep()
wait()
join()
BlockingQueue.take()
```

等响应中断的方法。

那么：

```java
cancel(true)
```

也不一定让它真正停下来。

所以：

```text
interrupt 是协作式中断
```

不是强制杀死线程。

------

# 十六、Future 被取消后再 get() 会怎么样？

例如：

```java
future.cancel(true);
```

之后：

```java
future.get();
```

会抛出：

```java
CancellationException
```

注意：

```text
CancellationException
```

是：

```java
RuntimeException
```

所以它不是 `get()` 方法声明中的 checked exception。

------

# 十七、Future 的核心问题

虽然 Future 很有用，但是它有一个非常明显的问题：

```text
获取结果的方式比较“被动”
```

例如：

```java
future.get();
```

如果结果没出来：

```text
只能等
```

或者：

```java
if (future.isDone()) {
    ...
}
```

自己不断查询。

例如：

```java
while (!future.isDone()) {

    // 不断检查

}
```

这种：

```text
轮询
```

通常也不好。

------

所以 Future 比较像：

```text
“以后我主动回来问你结果”
```

而不是：

```text
“任务完成以后自动继续做什么”
```

这也是为什么后来 Java 8 出现：

```java
CompletableFuture
```

它支持：

```java
thenApply()
thenAccept()
thenCompose()
thenCombine()
exceptionally()
```

等异步编排能力。

------

但学习顺序依然应该是：

```text
Future
    ↓
FutureTask
    ↓
CompletableFuture
```

因为 CompletableFuture 的很多思想依然建立在：

```text
异步结果
```

这个基础上。

------

# 十八、现在正式认识 FutureTask

FutureTask 是一个类：

```java
public class FutureTask<V>
        implements RunnableFuture<V>
```

继续看：

```java
public interface RunnableFuture<V>
        extends Runnable, Future<V> {

    void run();
}
```

所以完整关系：

```text
                Runnable
                   ↑
                   │
              RunnableFuture<V>
                   │
                   ↓
              FutureTask<V>
                   ↑
                   │
                Future<V>
```

更准确地画：

```text
Runnable                Future<V>
    ↑                       ↑
    │                       │
    └──── RunnableFuture<V> ┘
                ↑
                │
          FutureTask<V>
```

也就是说：

```text
FutureTask 同时拥有两种身份：
```

第一种：

```text
Runnable
```

所以：

```text
它可以被线程执行
```

第二种：

```text
Future
```

所以：

```text
它可以保存和获取执行结果
```

这正是 FutureTask 最核心的地方。

------

# 十九、FutureTask 和 Future 最大区别

可以这样理解：

```text
Future：
只关心“结果”。

FutureTask：
既是“任务”，又是“结果容器”。
```

所以：

```java
Future<Integer> future;
```

不能：

```java
new Thread(future);
```

因为：

```text
Future 没有实现 Runnable
```

------

但是：

```java
FutureTask<Integer> futureTask;
```

可以：

```java
new Thread(futureTask);
```

因为：

```text
FutureTask implements Runnable
```

------

# 二十、Future 能不能直接放进 Thread？

不能。

例如：

```java
Future<Integer> future = ...;
```

下面代码：

```java
new Thread(future);
```

编译失败。

因为 Thread 构造方法需要的是：

```java
Runnable
```

例如：

```java
public Thread(Runnable target)
```

而：

```text
Future
```

没有继承：

```java
Runnable
```

------

Future 只表示：

```text
结果
```

它不是：

```text
任务
```

------

# 二十一、FutureTask 为什么可以放进 Thread？

因为：

```java
FutureTask<V>
```

实现了：

```java
Runnable
```

所以可以：

```java
Callable<Integer> callable = () -> {

    Thread.sleep(2000);

    return 100;
};

FutureTask<Integer> futureTask =
        new FutureTask<>(callable);

Thread thread =
        new Thread(futureTask);

thread.start();

Integer result =
        futureTask.get();

System.out.println(result);
```

流程：

```text
Callable
    ↓
FutureTask 包装
    ↓
FutureTask 同时是 Runnable
    ↓
交给 Thread
    ↓
Thread.start()
    ↓
FutureTask.run()
    ↓
内部调用 Callable.call()
    ↓
得到结果
    ↓
结果保存到 FutureTask
    ↓
futureTask.get()
    ↓
拿到结果
```

------

# 二十二、为什么 Thread 不能直接执行 Callable？

因为：

```java
Thread
```

主要接收：

```java
Runnable
```

例如：

```java
new Thread(Runnable target)
```

但是：

```text
Callable
```

不是 Runnable。

所以不能：

```java
Callable<Integer> callable = () -> 100;

new Thread(callable);
```

编译失败。

------

于是 FutureTask 就可以充当：

```text
Callable → Runnable
```

的适配器。

流程：

```text
Callable
   │
   │ FutureTask 包装
   ▼
FutureTask
   │
   │ 实现 Runnable
   ▼
Thread
```

与此同时：

```text
FutureTask 还保存 Callable 的返回值
```

所以它不仅仅是适配器。

------

# 二十三、FutureTask 最经典的使用场景

如果你：

```text
不用线程池

直接使用 Thread

但是任务又需要返回结果
```

那么：

```java
Callable + FutureTask + Thread
```

就是经典组合。

完整示例：

```java
public class FutureTaskDemo {

    public static void main(String[] args)
            throws Exception {

        Callable<Integer> callable = () -> {

            System.out.println(
                    Thread.currentThread().getName()
                            + " 开始执行");

            Thread.sleep(2000);

            return 100;
        };

        FutureTask<Integer> futureTask =
                new FutureTask<>(callable);

        Thread thread =
                new Thread(futureTask);

        thread.start();

        System.out.println("主线程继续运行");

        Integer result =
                futureTask.get();

        System.out.println(
                "任务结果：" + result);
    }
}
```

可能输出：

```text
主线程继续运行

Thread-0 开始执行

任务结果：100
```

------

# 二十四、FutureTask 的两个构造方法

FutureTask 最常见有两个构造方式。

------

## 24.1 包装 Callable

```java
public FutureTask(Callable<V> callable)
```

例如：

```java
FutureTask<Integer> task =
        new FutureTask<>(() -> {

            return 100;
        });
```

最终：

```java
task.get();
```

返回：

```text
100
```

------

## 24.2 包装 Runnable

FutureTask 还可以包装：

```java
Runnable
```

构造：

```java
public FutureTask(
        Runnable runnable,
        V result)
```

例如：

```java
FutureTask<String> task =
        new FutureTask<>(() -> {

            System.out.println("执行任务");

        }, "success");
```

执行完成以后：

```java
String result = task.get();
```

得到：

```text
success
```

------

为什么 Runnable 明明没有返回值还能这样？

因为：

```text
FutureTask 提前帮你指定一个固定结果
```

也就是：

```text
Runnable 执行成功
        ↓
FutureTask 返回你提前指定的 result
```

例如：

```java
new FutureTask<>(
        runnable,
        "success"
);
```

并不是 Runnable 自己：

```text
return "success"
```

而是：

```text
FutureTask 帮它附加了一个结果。
```

------

# 二十五、Runnable + FutureTask 的实际含义

比如：

```java
Runnable runnable = () -> {

    System.out.println("写入数据库");

};

FutureTask<Boolean> task =
        new FutureTask<>(
                runnable,
                true
        );
```

如果 Runnable：

```text
正常执行完成
```

那么：

```java
task.get();
```

得到：

```java
true
```

这表示的不是：

```text
Runnable 算出来了 true
```

而是你提前规定：

```text
正常结束后就返回 true
```

------

# 二十六、FutureTask 的完整执行流程

假设：

```java
Callable<Integer> callable =
        () -> {

            Thread.sleep(2000);

            return 100;
        };

FutureTask<Integer> futureTask =
        new FutureTask<>(callable);

new Thread(futureTask).start();

Integer result =
        futureTask.get();
```

内部大致可以理解成：

```text
① 创建 Callable

Callable
   ↓

② FutureTask 保存 Callable

FutureTask
┌─────────────────────┐
│ Callable             │
│ state                │
│ result               │
│ exception            │
│ waiting threads      │
└─────────────────────┘

③ Thread 执行 FutureTask

Thread.start()
      ↓
FutureTask.run()
      ↓
Callable.call()

④ call() 返回 100

FutureTask
      ↓
保存结果 100
      ↓
状态改成完成

⑤ 唤醒等待 get() 的线程

⑥ get() 返回 100
```

------

# 二十七、FutureTask 为什么叫 Task？

因为它自己就是：

```text
一个任务
```

它拥有：

```java
run()
```

所以：

```text
Task
```

------

为什么叫 Future？

因为它还提供：

```java
get()
isDone()
cancel()
isCancelled()
```

所以又是：

```text
Future
```

合起来：

```text
Future + Task

= FutureTask
```

这个类名其实非常准确。

------

# 二十八、FutureTask 的状态

FutureTask 内部会维护任务状态。

从源码层面，它有类似下面这些状态：

```text
NEW
COMPLETING
NORMAL
EXCEPTIONAL
CANCELLED
INTERRUPTING
INTERRUPTED
```

现在学习阶段不需要死记数字值，理解含义就行。

------

大致可以看成：

```text
              NEW
               │
       ┌───────┼─────────┐
       │       │         │
       ▼       ▼         ▼
    NORMAL  EXCEPTIONAL CANCELLED
```

另外取消并中断时还有：

```text
INTERRUPTING
    ↓
INTERRUPTED
```

------

## NEW

表示：

```text
任务还没有最终完成
```

可能：

```text
还没执行

或者正在执行
```

------

## NORMAL

表示：

```text
任务正常执行完成
```

------

## EXCEPTIONAL

表示：

```text
任务执行过程中抛异常
```

------

## CANCELLED

表示：

```text
任务被取消
```

------

## INTERRUPTED

表示：

```text
任务取消过程中要求中断执行线程
```

------

# 二十九、FutureTask.get() 内部为什么能阻塞？

这是 FutureTask 比较重要的底层思想。

假设主线程：

```java
futureTask.get();
```

但是：

```text
FutureTask 还没完成
```

那么 FutureTask 会让：

```text
调用 get() 的线程
```

进入等待状态。

大致：

```text
Main Thread
    │
    │ futureTask.get()
    ▼
检查 state
    │
    │ 未完成
    ▼
加入等待线程结构
    │
    ▼
park()
```

底层会使用类似：

```java
LockSupport.park()
```

让等待线程挂起。

------

之后任务线程执行完成：

```text
Worker Thread
      │
      ▼
FutureTask 完成
      │
      ▼
保存结果
      │
      ▼
唤醒等待线程
```

内部类似：

```java
LockSupport.unpark(waiterThread);
```

于是：

```text
Main Thread
```

从：

```text
get()
```

的等待中恢复。

------

所以你前面学的：

```java
LockSupport.park()
LockSupport.unpark()
```

实际上就是很多 JUC 高级工具底层的重要基础设施。

------

# 三十、FutureTask 可以被执行几次吗？

这是一个非常有意思的点。

例如：

```java
FutureTask<Integer> task =
        new FutureTask<>(() -> {

            System.out.println("执行任务");

            return 100;
        });

new Thread(task).start();

new Thread(task).start();
```

你可能以为：

```text
会执行两遍 Callable
```

实际上：

```text
FutureTask 默认只会真正执行一次任务。
```

因为 FutureTask 会维护状态。

第一次执行：

```text
NEW
 ↓
执行
 ↓
NORMAL
```

第二个线程再执行：

```java
task.run();
```

看到：

```text
任务已经完成
```

就不会重复执行 Callable。

------

所以：

```text
FutureTask 天然具备“一次性任务”语义。
```

------

# 三十一、两个线程同时执行同一个 FutureTask 会怎样？

例如：

```java
FutureTask<Integer> task =
        new FutureTask<>(() -> {

            System.out.println(
                    Thread.currentThread().getName()
                    + " 真正执行");

            return 100;
        });

new Thread(task, "A").start();

new Thread(task, "B").start();
```

最终：

```text
只有一个线程
```

真正执行：

```java
Callable.call()
```

另一个不会重复执行任务。

这是 FutureTask 内部：

```text
状态控制 + CAS
```

实现的。

------

这说明 FutureTask 本身也是：

```text
线程安全的任务容器。
```

------

# 三十二、ExecutorService.submit() 和 FutureTask 的关系

这是特别重要的一部分。

我们平时写：

```java
Future<Integer> future =
        executorService.submit(() -> 100);
```

表面看：

```text
只有 Callable 和 Future
```

但很多标准 ExecutorService 实现内部：

```text
其实会把任务包装成 RunnableFuture
```

而常见的：

```text
RunnableFuture 实现
```

就是：

```java
FutureTask
```

例如 `AbstractExecutorService` 的思想大致是：

```text
submit(Callable)
      ↓
newTaskFor(callable)
      ↓
创建 RunnableFuture
      ↓
execute(task)
      ↓
返回 task
```

其中默认 `newTaskFor()` 会创建类似：

```java
new FutureTask<>(callable)
```

------

所以：

```java
Future<Integer> future =
        executorService.submit(callable);
```

可以大致理解成线程池内部做了：

```java
FutureTask<Integer> task =
        new FutureTask<>(callable);

execute(task);

return task;
```

当然：

```text
这是典型标准实现的内部思路，
接口契约只保证 submit 返回 Future，
不要在业务代码里依赖它一定就是 FutureTask。
```

------

# 三十三、为什么 submit() 能返回 Future，而 execute() 不行？

先看：

```java
Executor
```

只有：

```java
void execute(Runnable command);
```

返回：

```text
void
```

所以：

```java
executor.execute(task);
```

只是：

```text
把任务交出去执行
```

没有：

```text
返回结果凭证
```

------

而：

```java
ExecutorService.submit(...)
```

返回：

```java
Future
```

例如：

```java
Future<Integer> future =
        executorService.submit(callable);
```

因此你可以：

```java
future.get();
future.cancel(true);
future.isDone();
```

------

可以这样记：

```text
execute()
    ↓
只负责“提交执行”

submit()
    ↓
提交执行
+
给你 Future
```

------

# 三十四、execute() 和 submit() 的重要区别

## execute()

```java
executor.execute(runnable);
```

特点：

```text
参数：
Runnable

返回值：
void

主要用途：
只管执行，不关心结果
```

------

## submit()

可以提交：

```java
Runnable

Callable
```

并且返回：

```java
Future
```

------

例如：

```java
Future<?> future =
        executorService.submit(runnable);
```

Runnable 没有结果时：

```java
future.get();
```

正常完成会返回：

```text
null
```

------

还可以：

```java
Future<String> future =
        executorService.submit(
                runnable,
                "success"
        );
```

Runnable 执行成功以后：

```java
future.get();
```

返回：

```text
success
```

------

Callable：

```java
Future<Integer> future =
        executorService.submit(
                () -> 100
        );
```

返回：

```text
100
```

------

# 三十五、submit() 的三个常见重载

ExecutorService 中常见：

```java
<T> Future<T> submit(Callable<T> task);
Future<?> submit(Runnable task);
<T> Future<T> submit(
        Runnable task,
        T result);
```

------

## Callable

```java
Future<Integer> future =
        pool.submit(() -> 100);
```

结果：

```text
100
```

------

## Runnable

```java
Future<?> future =
        pool.submit(() -> {

            System.out.println("执行");

        });
```

正常结束：

```java
future.get();
```

返回：

```text
null
```

------

## Runnable + result

```java
Future<String> future =
        pool.submit(
                () -> {

                    System.out.println("执行");

                },
                "success"
        );
```

执行成功：

```java
future.get();
```

返回：

```text
success
```

------

# 三十六、FutureTask 和线程池的关系

FutureTask 不只能：

```java
new Thread(futureTask)
```

它同样可以交给线程池：

```java
FutureTask<Integer> task =
        new FutureTask<>(() -> {

            return 100;
        });

executorService.execute(task);

Integer result =
        task.get();
```

为什么？

因为：

```text
execute()
```

需要：

```java
Runnable
```

FutureTask：

```text
implements Runnable
```

所以完全可以。

------

整个过程：

```text
Callable
   ↓
FutureTask
   ↓
executor.execute()
   ↓
线程池线程执行
   ↓
FutureTask 保存结果
   ↓
futureTask.get()
```

------

# 三十七、既然有 submit()，还需要自己 FutureTask 吗？

大多数普通线程池业务代码中：

```java
executorService.submit(...)
```

已经非常方便。

所以确实经常不需要自己：

```java
new FutureTask(...)
```

例如：

```java
Future<Integer> future =
        pool.submit(() -> 100);
```

比：

```java
FutureTask<Integer> task =
        new FutureTask<>(() -> 100);

pool.execute(task);
```

更加简单。

------

但是 FutureTask 依然非常重要。

因为它有几个经典用途。

------

# 三十八、FutureTask 的主要使用场景

------

## 场景一：Thread + Callable

这是学习 FutureTask 最经典的场景。

因为：

```text
Thread 不能直接接收 Callable
```

于是：

```text
Callable
   ↓
FutureTask
   ↓
Thread
```

例如：

```java
FutureTask<Integer> task =
        new FutureTask<>(() -> 100);

new Thread(task).start();

Integer result = task.get();
```

------

## 场景二：把“执行能力”和“结果能力”放进同一个对象

FutureTask 同时：

```text
Runnable
+
Future
```

所以有时候需要一个对象：

```text
既可以进入任务队列

又可以在外部查询执行状态
```

FutureTask 就非常合适。

------

## 场景三：自定义线程池框架

例如你自己：

```text
实现 Executor

实现任务队列

实现调度逻辑
```

就可能手动使用：

```java
FutureTask
```

------

## 场景四：JDK 并发框架内部

FutureTask 本身也是：

```text
JUC 中非常重要的基础实现
```

很多：

```text
ExecutorService.submit()
```

相关逻辑本质上都会涉及：

```text
RunnableFuture
```

以及 FutureTask 这种设计。

------

## 场景五：一次性初始化 / 缓存计算

FutureTask 还有一种很经典的设计：

```text
某个昂贵计算只希望执行一次，
其他线程等待这个结果。
```

例如：

```text
多个线程同时需要初始化某个大型资源
```

可以共享：

```java
FutureTask<Resource>
```

第一个线程执行计算：

```text
其他线程 futureTask.get()
```

等待结果。

这样可以避免：

```text
同一个昂贵计算执行很多遍
```

------

# 三十九、Future 和 FutureTask 到底是什么关系？

这是这一章最核心的问题。

正确关系：

```text
Future
    ↓
接口

FutureTask
    ↓
Future 的一个实现类
```

但还不够。

因为 FutureTask 不仅实现：

```java
Future
```

还实现：

```java
Runnable
```

准确来说：

```java
FutureTask
        ↓
implements RunnableFuture
        ↓
RunnableFuture extends Runnable, Future
```

因此：

```text
FutureTask
=
Future 能力
+
Runnable 能力
+
任务状态管理
+
结果保存
+
异常保存
+
等待/唤醒机制
+
取消机制
```

------

# 四十、不要把 Future / FutureTask 类比成 Executor / ExecutorService

这个区别特别值得记下来。

你之前容易产生这种感觉：

```text
Executor
    ↓
ExecutorService

Future
    ↓
FutureTask
```

因为：

```text
名字长得很像
```

但它们不是同一种关系。

------

Executor：

```java
public interface Executor
```

ExecutorService：

```java
public interface ExecutorService
        extends Executor
```

它们是：

```text
接口 → 更强的接口
```

------

而：

```java
Future
```

是接口。

```java
FutureTask
```

是：

```text
实现类
```

而且 FutureTask：

```text
还额外拥有 Runnable 身份
```

------

所以更正确的理解是：

```text
Executor
    ↓
ExecutorService

属于：
执行器抽象体系
```

而：

```text
Future
    ↓
FutureTask

属于：
异步结果 + 可执行任务实现
```

不要因为名字接近，就认为它们：

```text
角色也完全对应。
```

------

# 四十一、Future 的真正使用场景在哪里？

Future 的使用频率其实并不低。

比如：

```java
Future<User> userFuture =
        pool.submit(...);

Future<Order> orderFuture =
        pool.submit(...);

Future<Product> productFuture =
        pool.submit(...);
```

调用方根本不关心：

```text
Future 具体是什么实现
```

只需要：

```java
userFuture.get();
orderFuture.get();
productFuture.get();
```

所以 API 层面通常：

```text
面向 Future 接口编程
```

而不是：

```text
面向 FutureTask 编程
```

------

就像：

```java
List<String> list =
        new ArrayList<>();
```

变量通常写：

```java
List
```

而不是：

```java
ArrayList
```

类似地：

```java
Future<Integer> future =
        pool.submit(...);
```

通常也不需要知道：

```text
底层到底是哪种 Future 实现。
```

------

# 四十二、Future 和 FutureTask 谁更常用？

要分角度。

------

## 业务代码 API 层面

一般：

```text
Future 更常见
```

因为：

```java
Future<T> future =
        executorService.submit(...);
```

非常常见。

------

## 自己直接 new 对象

当然：

```text
FutureTask 更常见
```

因为：

```text
Future 是接口，不能直接 new。
```

------

## 日常线程池开发

通常：

```text
ExecutorService
+
Future
```

更多。

例如：

```java
Future<User> future =
        executor.submit(...);
```

------

## 直接 Thread + Callable

通常：

```text
Callable
+
FutureTask
+
Thread
```

------

所以可以记：

```text
线程池场景：

Callable
   ↓
ExecutorService.submit()
   ↓
Future
```

而：

```text
Thread 场景：

Callable
   ↓
FutureTask
   ↓
Thread
```

------

# 四十三、FutureTask 不只是“包装 Callable”

一定不要只记：

```text
FutureTask = Callable 包装器
```

这不完整。

FutureTask 至少做了：

```text
① 把 Callable 转换成 Runnable 类型任务

② 管理任务执行状态

③ 保存正常执行结果

④ 保存执行异常

⑤ 支持 get() 等待结果

⑥ 支持超时等待

⑦ 支持 cancel()

⑧ 支持任务取消状态查询

⑨ 负责唤醒等待任务结果的线程

⑩ 保证任务通常只真正执行一次
```

所以：

```text
FutureTask 是一个完整的“可执行异步任务对象”。
```

------

# 四十四、FutureTask 中 Callable 的异常去哪了？

例如：

```java
FutureTask<Integer> task =
        new FutureTask<>(() -> {

            throw new RuntimeException("出错了");

        });

new Thread(task).start();
```

异常不会简单地从：

```text
Thread.start()
```

抛到主线程。

而是 FutureTask：

```text
捕获任务异常
    ↓
保存异常
    ↓
状态变为 EXCEPTIONAL
```

之后：

```java
task.get();
```

会：

```text
抛出 ExecutionException
```

例如：

```java
try {

    task.get();

} catch (ExecutionException e) {

    System.out.println(
            e.getCause()
    );
}
```

------

# 四十五、FutureTask.get() 可以被多个线程调用吗？

可以。

例如：

```text
Thread-A
Thread-B
Thread-C
```

都：

```java
futureTask.get();
```

如果任务：

```text
还没有完成
```

三个线程都会等待。

任务完成后：

```text
三个等待线程都会被唤醒
```

然后都可以拿到：

```text
同一个计算结果。
```

------

例如：

```java
FutureTask<Integer> task =
        new FutureTask<>(() -> {

            Thread.sleep(3000);

            return 100;
        });

new Thread(task).start();

new Thread(() -> {
    try {
        System.out.println(task.get());
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}).start();

new Thread(() -> {
    try {
        System.out.println(task.get());
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}).start();
```

最终两个等待线程都可以得到：

```text
100
```

------

# 四十六、FutureTask 的结果会被 get() 取走吗？

不会。

FutureTask 不是：

```text
BlockingQueue.take()
```

这种：

```text
拿走一次就没了
```

FutureTask 保存的结果可以：

```java
task.get();
task.get();
task.get();
```

多次获取。

例如：

```java
Integer a = task.get();

Integer b = task.get();

Integer c = task.get();
```

都会返回同一个：

```text
100
```

前提是任务正常完成。

------

# 四十七、FutureTask 的高级方法

除了 Future 接口的方法，FutureTask 还提供一些扩展点。

------

## done()

```java
protected void done()
```

这是一个：

```text
任务完成后的回调钩子
```

可以继承 FutureTask：

```java
FutureTask<Integer> task =
        new FutureTask<>(() -> 100) {

            @Override
            protected void done() {

                System.out.println(
                        "任务执行结束"
                );
            }
        };
```

任务结束以后会执行：

```java
done();
```

无论：

```text
正常完成

异常

取消
```

都会进入最终完成流程。

这个方法在：

```text
框架扩展
```

中比较有价值。

普通业务代码：

```text
不算特别常用。
```

------

## runAndReset()

FutureTask 还有：

```java
protected boolean runAndReset()
```

它的用途比较高级。

它和普通：

```java
run()
```

不同。

普通：

```text
run()
    ↓
执行任务
    ↓
保存最终结果
    ↓
进入完成状态
```

而：

```text
runAndReset()
```

可以：

```text
执行任务
但是成功后重新回到可执行状态
```

这种能力主要用于：

```text
周期性任务 / 调度框架
```

普通开发基本不会手动使用。

现在知道：

```text
FutureTask 不仅仅是 Callable 包装器
```

即可。

------

# 四十八、Future 的缺陷之一：容易把异步写回同步

这是一个非常常见的问题。

例如：

```java
Future<Integer> future =
        pool.submit(() -> {

            Thread.sleep(3000);

            return 100;
        });

Integer result =
        future.get();
```

如果：

```text
submit()
```

完马上：

```java
get()
```

那么主线程：

```text
还是在这里等三秒
```

从结果来看：

```text
异步任务虽然在线程池执行了

但是主线程立即等待
```

异步收益可能很小。

------

更合理的是：

```java
Future<Integer> future =
        pool.submit(task);

// 先做其他不依赖 result 的事情

doSomething1();

doSomething2();

doSomething3();

// 真正需要结果时
Integer result =
        future.get();
```

这样才能发挥：

```text
并发执行
```

价值。

------

# 四十九、一个典型错误：连续 submit + get

比如：

```java
Future<Integer> f1 =
        pool.submit(task1);

Integer r1 = f1.get();

Future<Integer> f2 =
        pool.submit(task2);

Integer r2 = f2.get();
```

假设：

```text
task1 需要 3 秒
task2 需要 3 秒
```

总时间可能：

```text
约 6 秒
```

因为：

```text
task1
 ↓
等完
 ↓
task2
 ↓
再等
```

------

更好的方式：

```java
Future<Integer> f1 =
        pool.submit(task1);

Future<Integer> f2 =
        pool.submit(task2);

Integer r1 = f1.get();

Integer r2 = f2.get();
```

现在：

```text
task1 ───── 3 秒 ─────→

task2 ───── 3 秒 ─────→
```

两个任务可能并行。

总耗时：

```text
接近 3 秒
```

而不是：

```text
6 秒
```

这就是 Future 使用时非常重要的思维：

```text
先尽可能把任务全部提交出去

再统一获取结果
```

------

# 五十、批量 Future 的经典写法

例如：

```java
ExecutorService pool =
        Executors.newFixedThreadPool(3);

List<Future<Integer>> futures =
        new ArrayList<>();

for (int i = 0; i < 10; i++) {

    int num = i;

    Future<Integer> future =
            pool.submit(() -> {

                Thread.sleep(1000);

                return num * num;
            });

    futures.add(future);
}

for (Future<Integer> future : futures) {

    Integer result =
            future.get();

    System.out.println(result);
}

pool.shutdown();
```

这里的关键是：

```text
第一轮循环
    ↓
快速提交所有任务

第二轮循环
    ↓
统一获取结果
```

而不是：

```text
提交一个
↓
立即 get
↓
再提交下一个
```

------

# 五十一、invokeAll() 和 Future

ExecutorService 还有一个和 Future 很相关的方法：

```java
invokeAll(...)
```

例如：

```java
List<Callable<Integer>> tasks =
        List.of(
                () -> 10,
                () -> 20,
                () -> 30
        );

List<Future<Integer>> futures =
        pool.invokeAll(tasks);
```

然后：

```java
for (Future<Integer> future : futures) {

    System.out.println(
            future.get()
    );
}
```

注意：

```text
invokeAll()
```

和普通：

```text
submit()
```

不同。

`invokeAll()` 会等待：

```text
所有任务完成
```

以后才返回 Future 列表。

所以它本身具有：

```text
阻塞等待所有任务
```

的语义。

这个和：

```text
submit() 不等待任务完成
```

一定要区分。

------

# 五十二、Future 和异常传播

多线程中特别重要的一点：

```text
线程 A 的异常
不会自动跑到线程 B。
```

例如：

```java
pool.execute(() -> {

    throw new RuntimeException("错误");

});
```

这个异常发生在：

```text
线程池线程
```

主线程无法直接：

```java
try {
    pool.execute(...);
} catch (...) {
}
```

捕获任务线程内部后来发生的异常。

------

而：

```java
submit()
+
Future.get()
```

提供了一种：

```text
跨线程传播执行结果 / 执行异常
```

的机制。

即：

```text
任务线程发生异常
       ↓
Future 保存异常
       ↓
调用线程 get()
       ↓
ExecutionException
```

这是 Future 的一个非常重要价值。

------

# 五十三、execute 和 submit 的异常区别

例如：

```java
pool.execute(() -> {

    throw new RuntimeException("boom");

});
```

异常通常直接由：

```text
执行任务的线程
```

处理。

可能：

```text
打印到控制台

交给 UncaughtExceptionHandler
```

------

而：

```java
Future<?> future =
        pool.submit(() -> {

            throw new RuntimeException("boom");

        });
```

submit 任务的异常通常会被：

```text
Future 保存
```

所以如果你一直：

```text
不调用 future.get()
```

可能会发现：

```text
任务明明失败了

但是你没有明显看到异常。
```

所以：

```text
submit() 后完全不处理 Future
```

有时候会导致异常被忽略。

这是实际开发中很值得注意的问题。

------

# 五十四、Future 适合什么场景？

Future 比较适合：

```text
① 提交异步任务

② 之后某个时间点需要结果

③ 任务之间关系比较简单

④ 不需要特别复杂的回调链

⑤ 需要支持取消 / 超时 / 状态查询
```

例如：

```text
同时调用三个远程接口

同时计算三份数据

同时查询多个数据源

后台执行一个耗时计算

批量并行处理
```

------

# 五十五、Future 不适合什么场景？

如果你需要：

```text
A 完成
↓
自动执行 B
↓
B 完成
↓
自动执行 C

或者：

A 和 B 完成
↓
合并结果
↓
执行 C

或者：

任务失败
↓
自动执行异常处理
```

单纯 Future 写起来会比较麻烦。

你可能会写出：

```java
A a = futureA.get();

B b = pool.submit(
        () -> taskB(a)
).get();

C c = pool.submit(
        () -> taskC(b)
).get();
```

大量：

```java
get()
```

使代码：

```text
重新同步化
```

这时更适合：

```java
CompletableFuture
```

------

# 五十六、Future 和 CompletableFuture 的关系

现在先建立一个简单认识：

```text
Future
    ↓
“未来我主动来拿结果”
```

而：

```text
CompletableFuture
    ↓
“结果出来以后自动继续执行下一步”
```

比如 Future：

```java
Integer result =
        future.get();
```

而 CompletableFuture：

```java
CompletableFuture
        .supplyAsync(() -> 100)
        .thenApply(x -> x * 2)
        .thenAccept(System.out::println);
```

形成：

```text
任务 A
 ↓
任务 B
 ↓
任务 C
```

所以你之前感觉：

```text
CompletableFuture 有点像 Stream API
```

其实这个感觉是有道理的。

因为它们都有：

```text
链式调用

函数式接口

数据/结果逐步传递
```

只不过：

```text
Stream
    ↓
数据处理流水线

CompletableFuture
    ↓
异步任务流水线
```

------

# 五十七、FutureTask 和 CompletableFuture 不能简单替代

FutureTask 更接近：

```text
一个具体的可执行任务对象
```

CompletableFuture 更侧重：

```text
异步任务结果的编排
```

所以两者虽然：

```text
都实现 Future
```

但是设计目的不同。

------

可以理解成：

```text
FutureTask：
Runnable + Future

CompletableFuture：
Future + CompletionStage
```

CompletionStage 提供：

```text
任务完成以后
怎么继续下一步
```

的能力。

------

# 五十八、完整知识体系

把目前这些类放在一起：

```text
                 Executor
                    │
                    ▼
              ExecutorService
                    │
       ┌────────────┴────────────┐
       │                         │
    execute()                 submit()
       │                         │
       ▼                         ▼
   Runnable               Runnable / Callable
                                 │
                                 ▼
                              Future
```

另外：

```text
Callable
    │
    │ FutureTask 包装
    ▼
FutureTask
    │
    ├──── Runnable
    │
    └──── Future
```

所以：

```text
FutureTask
```

把两个世界连接了起来：

```text
“任务执行”
+
“任务结果”
```

------

# 五十九、把 Thread / Runnable / Callable / Future / FutureTask 串起来

------

## Runnable 路线

```text
Runnable
   ↓
Thread
   ↓
执行任务
```

例如：

```java
Runnable runnable =
        () -> System.out.println("hello");

new Thread(runnable).start();
```

特点：

```text
没有返回值
```

------

## Callable 路线

Callable：

```text
不能直接给 Thread
```

所以：

```text
Callable
   ↓
FutureTask
   ↓
Thread
```

例如：

```java
Callable<Integer> callable =
        () -> 100;

FutureTask<Integer> task =
        new FutureTask<>(callable);

new Thread(task).start();

Integer result =
        task.get();
```

------

## 线程池路线

```text
Callable
   ↓
ExecutorService.submit()
   ↓
Future
```

例如：

```java
Future<Integer> future =
        pool.submit(() -> 100);

Integer result =
        future.get();
```

------

# 六十、四个角色千万不要混

------

## Runnable

负责：

```text
“我要执行什么代码”
```

特点：

```text
没有返回值
```

------

## Callable

负责：

```text
“我要执行什么代码，
而且执行完有结果”
```

------

## Future

负责：

```text
“任务现在怎么样？”
“结果出来了吗？”
“结果是什么？”
“能不能取消？”
```

------

## FutureTask

负责：

```text
“我既是一个能执行的任务，
又代表这个任务未来的结果。”
```

------

# 六十一、一个特别好用的类比

假设你去奶茶店。

------

Callable：

```text
制作奶茶的订单内容
```

例如：

```text
制作一杯珍珠奶茶
```

------

Thread / Executor：

```text
店员
```

负责真正：

```text
执行订单
```

------

Future：

```text
取餐号码牌
```

它告诉你：

```text
奶茶好了没？

订单取消了吗？

什么时候可以拿？

最后结果是什么？
```

------

Future.get()：

```text
站在取餐口等奶茶
```

如果奶茶还没做好：

```text
等
```

如果已经好了：

```text
直接拿走
```

------

FutureTask：

```text
一个特殊订单对象：
它既包含制作任务，
又自带取餐凭证。
```

它：

```text
可以被店员执行

也可以被顾客用来查询结果
```

------

# 六十二、FutureTask 和 Thread 的经典面试问题

## 问题

为什么 Callable 不能直接交给 Thread？

答案：

```text
因为 Thread 构造方法接受 Runnable，
而 Callable 没有实现 Runnable。
```

------

## 问题

那怎么让 Thread 执行 Callable？

答案：

```text
使用 FutureTask 包装 Callable。

因为 FutureTask 实现了 Runnable，
所以可以交给 Thread。

同时 FutureTask 又实现了 Future，
因此可以通过 get() 获取 Callable 的结果。
```

代码：

```java
FutureTask<Integer> task =
        new FutureTask<>(() -> 100);

new Thread(task).start();

Integer result =
        task.get();
```

------

# 六十三、Future 的经典面试问题

## get() 一定阻塞吗？

不是。

正确答案：

```text
如果任务已经完成，
get() 立即返回。

如果任务没有完成，
get() 会阻塞当前线程。
```

------

## submit() 是阻塞的吗？

普通：

```java
ExecutorService.submit()
```

不会等待：

```text
任务执行完成
```

它提交任务以后：

```text
返回 Future
```

真正可能阻塞的是：

```java
future.get()
```

------

## isDone() == true 是否代表成功？

不是。

可能：

```text
正常成功

异常结束

取消
```

都可能：

```java
isDone() == true
```

------

## cancel(true) 是否一定能停止任务？

不能保证。

因为：

```text
true
```

只是表示：

```text
如果任务正在运行，
允许通过 interrupt()
尝试中断任务线程。
```

任务必须：

```text
正确响应中断
```

才会停止。

------

# 六十四、FutureTask 的经典面试问题

## FutureTask 实现了哪些接口？

直接：

```java
FutureTask<V>
    implements RunnableFuture<V>
```

而：

```java
RunnableFuture<V>
    extends Runnable, Future<V>
```

所以：

```text
FutureTask
同时拥有 Runnable 和 Future 的能力。
```

------

## FutureTask 能直接交给 Thread 吗？

能。

因为：

```text
FutureTask implements Runnable
```

------

## Future 能直接交给 Thread 吗？

不能。

因为：

```text
Future 不一定是 Runnable。
```

------

## FutureTask 能执行多次吗？

正常情况下：

```text
同一个 FutureTask 的任务只真正执行一次。
```

即使多个线程：

```java
task.run();
```

也不会让 Callable 被正常重复计算多次。

------

# 六十五、一个综合示例

```java
import java.util.concurrent.*;

public class FutureDemo {

    public static void main(String[] args)
            throws Exception {

        ExecutorService pool =
                Executors.newFixedThreadPool(2);

        Future<Integer> future1 =
                pool.submit(() -> {

                    System.out.println(
                            "task1 开始"
                    );

                    Thread.sleep(3000);

                    return 100;
                });

        Future<Integer> future2 =
                pool.submit(() -> {

                    System.out.println(
                            "task2 开始"
                    );

                    Thread.sleep(2000);

                    return 200;
                });

        System.out.println(
                "两个任务已经提交"
        );

        System.out.println(
                "主线程先干其他事情"
        );

        Integer result1 =
                future1.get();

        Integer result2 =
                future2.get();

        System.out.println(
                result1 + result2
        );

        pool.shutdown();
    }
}
```

执行过程：

```text
主线程：

submit task1
    ↓
拿到 future1

submit task2
    ↓
拿到 future2

继续执行其他逻辑
```

与此同时：

```text
线程池线程 1：
task1 ───────────────→ 100

线程池线程 2：
task2 ──────────→ 200
```

最后：

```text
future1.get()
future2.get()
```

得到：

```text
100
200
```

最后：

```text
300
```

------

# 六十六、FutureTask 综合示例

```java
import java.util.concurrent.*;

public class FutureTaskDemo {

    public static void main(String[] args)
            throws Exception {

        Callable<Integer> callable = () -> {

            System.out.println(
                    Thread.currentThread().getName()
                            + " 执行任务"
            );

            Thread.sleep(2000);

            return 100;
        };

        FutureTask<Integer> futureTask =
                new FutureTask<>(callable);

        Thread worker =
                new Thread(
                        futureTask,
                        "worker"
                );

        worker.start();

        System.out.println(
                "main 继续执行"
        );

        if (!futureTask.isDone()) {

            System.out.println(
                    "任务还没有完成"
            );
        }

        Integer result =
                futureTask.get();

        System.out.println(
                "结果：" + result
        );

        System.out.println(
                "完成：" +
                futureTask.isDone()
        );
    }
}
```

------

# 六十七、Future 的状态思维

以后看到 Future，可以直接在脑子里想：

```text
             Future
                │
          ┌─────┴─────┐
          │           │
       未完成         已结束
                        │
          ┌─────────────┼────────────┐
          │             │            │
       正常成功       异常失败       取消
```

对应：

```text
未完成：

isDone() == false
```

------

正常完成：

```text
isDone() == true

get() → 结果
```

------

异常完成：

```text
isDone() == true

get() → ExecutionException
```

------

取消：

```text
isDone() == true

isCancelled() == true

get() → CancellationException
```

------

# 六十八、Future.get() 四种常见结果

以后看到：

```java
future.get();
```

脑子里直接想到四种情况。

------

## 情况一：正常完成

```text
返回结果
```

------

## 情况二：任务还没完成

```text
等待
```

------

## 情况三：任务执行异常

```text
ExecutionException
```

------

## 情况四：任务被取消

```text
CancellationException
```

另外等待过程中当前线程被中断：

```text
InterruptedException
```

------

# 六十九、一个非常重要的 Future 使用原则

尽量避免：

```java
Future<A> f1 =
        pool.submit(task1);

A a = f1.get();

Future<B> f2 =
        pool.submit(task2);

B b = f2.get();
```

因为这很容易：

```text
把异步代码写成同步代码。
```

更好的思路是：

```java
Future<A> f1 =
        pool.submit(task1);

Future<B> f2 =
        pool.submit(task2);

Future<C> f3 =
        pool.submit(task3);

// 中间做其他事情

A a = f1.get();

B b = f2.get();

C c = f3.get();
```

核心思想：

```text
先启动并发

后等待结果
```

------

# 七十、Future 还有一个问题：等待顺序

假设：

```text
task1 需要 10 秒

task2 需要 1 秒

task3 需要 2 秒
```

你写：

```java
f1.get();
f2.get();
f3.get();
```

即使：

```text
task2
task3
```

早就完成了，

你还是：

```text
先卡在 f1.get()
```

所以 Future 对：

```text
“谁先完成就先处理谁”
```

支持得并不好。

这也是为什么 JUC 里面还有：

```java
CompletionService
```

比如：

```java
ExecutorCompletionService
```

它可以：

```text
谁先执行完成，
就先取谁的结果。
```

这个可以以后在线程池进阶部分再学。

------

# 七十一、Future API 总结

```java
Future<V>
```

核心：

```java
V get();
```

获取结果，没完成就等。

------

```java
V get(
    long timeout,
    TimeUnit unit
);
```

最多等一段时间。

------

```java
boolean cancel(
    boolean mayInterruptIfRunning
);
```

尝试取消任务。

------

```java
boolean isCancelled();
```

任务是否取消。

------

```java
boolean isDone();
```

任务是否已经结束。

------

# 七十二、FutureTask API 总结

FutureTask 首先拥有 Future 的所有能力：

```text
get()

get(timeout)

cancel()

isDone()

isCancelled()
```

同时因为实现：

```java
Runnable
```

所以还有：

```java
run()
```

可以：

```text
直接交给 Thread

交给 Executor.execute()
```

此外还有一些扩展方法：

```java
done()

runAndReset()

set()

setException()
```

其中：

```text
done()
runAndReset()
set()
setException()
```

更多属于：

```text
框架开发 / FutureTask 扩展
```

普通业务开发不用全部熟练掌握。

------

# 七十三、学习优先级

Future 需要重点掌握：

```text
★★★★★ Future 是什么

★★★★★ get()

★★★★★ get(timeout)

★★★★★ isDone()

★★★★★ cancel(true/false)

★★★★★ ExecutionException

★★★★★ submit() + Future

★★★★★ submit 不等待任务完成

★★★★★ get 可能阻塞

★★★★☆ CancellationException

★★★★☆ Future 的局限性
```

------

FutureTask 重点：

```text
★★★★★ FutureTask = Runnable + Future

★★★★★ Callable → FutureTask → Thread

★★★★★ FutureTask 可以包装 Callable

★★★★★ FutureTask 可以包装 Runnable

★★★★★ 同一个 FutureTask 通常只执行一次

★★★★☆ FutureTask 与 submit 的内部关系

★★★★☆ FutureTask 的状态

★★★☆☆ done()

★★☆☆☆ runAndReset()
```

------

# 七十四、最终知识地图

```text
                         并发任务
                            │
            ┌───────────────┴──────────────┐
            │                              │
         Runnable                       Callable<V>
            │                              │
     void run()                     V call()
       无返回值                       有返回值
            │                              │
            │                              │
            ▼                              ▼
         Thread                       FutureTask
                                           │
                              ┌────────────┴────────────┐
                              │                         │
                           Runnable                   Future
                              │                         │
                              ▼                         ▼
                           Thread                     get()
                                                   cancel()
                                                   isDone()
```

线程池路线：

```text
Runnable / Callable
        │
        ▼
ExecutorService.submit()
        │
        ▼
      Future
        │
        ├── get()
        ├── cancel()
        ├── isDone()
        └── isCancelled()
```

FutureTask 路线：

```text
Callable
   │
   ▼
FutureTask
   │
   ├──────────── Runnable ────────────→ Thread
   │
   └──────────── Future ─────────────→ get()
```

------

# 七十五、最后用几句话彻底记住

第一句话：

```text
Callable 描述一个“有返回值的任务”。
```

第二句话：

```text
Future 描述一个“未来才能拿到的任务结果”。
```

第三句话：

```text
Future 本身通常不负责执行任务。
```

第四句话：

```text
FutureTask 既是 Runnable，又是 Future。
```

第五句话：

```text
所以 FutureTask 可以把 Callable 变成 Thread 能执行的任务。
```

第六句话：

```text
ExecutorService.submit() 提交任务后返回 Future，
submit 本身不会等待任务完成。
```

第七句话：

```text
Future.get() 在任务没完成时会阻塞，
任务已经完成时立即返回。
```

第八句话：

```text
FutureTask 不只是包装 Callable，
它还负责状态、结果、异常、取消以及等待线程的管理。
```

第九句话：

```text
线程池里通常直接用 ExecutorService + Future；
直接使用 Thread 执行 Callable 时，
经典组合是 Callable + FutureTask + Thread。
```

第十句话：

```text
Future 解决的是“未来如何拿结果”，
FutureTask 解决的是“这个任务既要能执行，
又要能表示未来结果”。
```

------

# 七十六、建议你脑子里永久保存的最终模型

以后看到：

```java
Future<Integer> future =
        executorService.submit(callable);
```

脑子里自动翻译成：

```text
我把一个有返回值的任务
交给线程池异步执行，

线程池立即给我一张 Future 凭证，

以后我可以拿着这张凭证：

查询任务有没有结束，

取消任务，

或者通过 get()
等待并拿到最终结果。
```

看到：

```java
FutureTask<Integer> task =
        new FutureTask<>(callable);

new Thread(task).start();
```

脑子里自动翻译成：

```text
Thread 本来只能执行 Runnable，

Callable 又不是 Runnable，

于是我用 FutureTask
把 Callable 包装成一个可以运行的任务，

FutureTask 自己又拥有 Future 能力，

所以任务执行结束以后，

我还能通过 futureTask.get()
拿到 Callable 返回的结果。
```

如果这两个模型彻底理解了：

```text
Future
FutureTask
Callable
Runnable
Thread
Executor
ExecutorService
submit()
execute()
get()
```

这一整块知识就基本串起来了。