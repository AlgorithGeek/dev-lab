# Java 多线程：Runnable / Callable 超详细笔记

## 一、先说结论：Runnable / Callable 到底是什么？

在 Java 多线程中：

```java
Runnable
Callable
```

它们本质上都不是“线程”。

它们是：

> **任务的抽象。**

也就是说：

```text
Thread = 谁来执行
Runnable / Callable = 执行什么
```

这是理解 Java 多线程非常重要的一层思想。

例如：

```java
Runnable task = () -> {
    System.out.println("执行任务");
};
```

这里并没有创建线程。

只是创建了一个：

```text
“待执行任务”
```

至于这个任务：

```text
谁执行？
什么时候执行？
在哪个线程执行？
```

Runnable 自己完全不负责。

可以：

```java
new Thread(task).start();
```

也可以交给线程池：

```java
executorService.submit(task);
```

------

# 二、为什么 Java 要把“线程”和“任务”分开？

早期最直观的创建线程方式是：

```java
class MyThread extends Thread {

    @Override
    public void run() {
        System.out.println("执行任务");
    }
}
```

然后：

```java
new MyThread().start();
```

这种写法把两件事情绑定到了一起：

```text
Thread
│
├── 线程本身
│
└── 线程要执行的任务
```

也就是说：

```text
线程和任务耦合在一起了。
```

后来 Java 更推荐：

```java
Runnable task = new MyTask();

Thread thread = new Thread(task);

thread.start();
```

变成：

```text
任务
 │
 ▼
Runnable
 │
 ▼
执行器
 │
 ├── Thread
 │
 └── ThreadPool
```

这样：

```text
任务负责描述“做什么”

线程负责描述“谁去做”
```

这就是：

> **任务和执行机制解耦。**

这个思想在线程池体系里非常重要。

------

# 三、Runnable 接口

## 1. Runnable 的定义

Java 中：

```java
@FunctionalInterface
public interface Runnable {

    public abstract void run();
}
```

实际上核心只有一个方法：

```java
void run();
```

可以简写理解为：

```java
Runnable
    ↓
run()
```

------

# 四、Runnable 的核心特点

Runnable 有几个非常重要的特点。

## 1. 没有返回值

方法定义：

```java
void run();
```

所以：

```java
Runnable
```

执行完成以后：

```text
不能直接返回一个计算结果。
```

例如：

```java
Runnable task = () -> {

    int a = 10;
    int b = 20;

    int result = a + b;
};
```

`result` 只是任务内部的局部变量。

外部不能：

```java
int result = task.run();
```

因为：

```java
run()
```

返回值是：

```java
void
```

------

## 2. 不能声明受检异常

Runnable：

```java
void run();
```

没有：

```java
throws Exception
```

所以如果里面出现：

```java
IOException
SQLException
InterruptedException
```

这种 Checked Exception（受检异常），必须自己处理。

例如：

```java
Runnable task = () -> {

    try {

        Thread.sleep(1000);

    } catch (InterruptedException e) {

        Thread.currentThread().interrupt();
    }
};
```

不能直接写：

```java
Runnable task = () -> {

    Thread.sleep(1000);
};
```

编译器会报错。

原因：

```java
Thread.sleep()
```

声明了：

```java
throws InterruptedException
```

而 Runnable 的：

```java
run()
```

没有允许向外抛出这个异常。

------

# 五、Runnable 最基本的使用方式

例如：

```java
public class RunnableDemo {

    public static void main(String[] args) {

        Runnable task = new Runnable() {

            @Override
            public void run() {

                System.out.println(
                        Thread.currentThread().getName()
                                + " 正在执行任务"
                );
            }
        };

        Thread thread = new Thread(task);

        thread.start();
    }
}
```

可能输出：

```text
Thread-0 正在执行任务
```

整个结构是：

```text
Runnable
   │
   │ 传入
   ▼
Thread
   │
   │ start()
   ▼
新线程
   │
   ▼
run()
```

------

# 六、最容易混淆的地方：run() 和 start()

例如：

```java
Thread thread = new Thread(task);
```

如果执行：

```java
thread.run();
```

那么：

> 不会创建新线程。

只是普通的方法调用。

相当于：

```java
task.run();
```

还是：

```text
当前线程执行
```

例如：

```java
public static void main(String[] args) {

    Runnable task = () -> {

        System.out.println(
                Thread.currentThread().getName()
        );
    };

    Thread thread = new Thread(task);

    thread.run();
}
```

输出：

```text
main
```

因为：

```text
main线程
   │
   ▼
thread.run()
   │
   ▼
普通方法调用
```

------

如果改成：

```java
thread.start();
```

输出一般：

```text
Thread-0
```

因为：

```text
main线程
   │
   ▼
thread.start()
   │
   ▼
JVM创建新线程
   │
   ▼
新线程执行run()
```

所以：

```text
run()   = 普通方法调用

start() = 启动线程
```

------

# 七、Runnable 最常见的写法

## 写法一：实现 Runnable 接口

```java
class MyTask implements Runnable {

    @Override
    public void run() {

        System.out.println("执行任务");
    }
}
```

使用：

```java
Runnable task = new MyTask();

Thread thread = new Thread(task);

thread.start();
```

------

## 写法二：匿名内部类

```java
Runnable task = new Runnable() {

    @Override
    public void run() {

        System.out.println("执行任务");
    }
};
```

------

## 写法三：Lambda

因为：

```java
Runnable
```

只有一个抽象方法，所以它是：

```java
@FunctionalInterface
```

即：

> 函数式接口。

因此可以使用 Lambda。

```java
Runnable task = () -> {

    System.out.println("执行任务");
};
```

甚至：

```java
new Thread(() -> {

    System.out.println("执行任务");

}).start();
```

------

# 八、Runnable 的运行过程

例如：

```java
Runnable task = () -> {

    System.out.println("A");

    System.out.println("B");

    System.out.println("C");
};

new Thread(task).start();
```

整个过程：

```text
main线程
   │
   │ 创建Runnable
   ▼
Runnable对象
   │
   │ 传入Thread
   ▼
Thread对象
   │
   │ start()
   ▼
JVM创建新线程
   │
   ▼
执行Thread.run()
   │
   ▼
执行Runnable.run()
   │
   ▼
A
B
C
```

注意：

Runnable 自己：

```text
不会启动线程
不会创建线程
不会调度线程
```

Runnable 只是：

```text
任务代码的载体。
```

------

# 九、Thread 和 Runnable 的关系

Thread 本身其实也实现了 Runnable：

```java
public class Thread implements Runnable
```

Thread 中有一个：

```java
run()
```

大致可以理解为：

```java
@Override
public void run() {

    if (target != null) {
        target.run();
    }
}
```

例如：

```java
Runnable task = () -> {

    System.out.println("Hello");
};

Thread thread = new Thread(task);
```

可以大致理解成：

```text
Thread
│
├── target = task
│
└── run()
       │
       ▼
   target.run()
```

所以：

```java
thread.start();
```

之后：

```text
JVM启动新线程
       ↓
Thread.run()
       ↓
task.run()
```

------

# 十、Callable 是什么？

Callable 和 Runnable 非常像。

但是 Callable 解决了 Runnable 的两个主要问题：

```text
Runnable：

① 没有返回值
② 不方便抛出受检异常
```

所以 Java 提供了：

```java
Callable<V>
```

------

# 十一、Callable 接口定义

Callable 定义：

```java
@FunctionalInterface
public interface Callable<V> {

    V call() throws Exception;
}
```

核心方法：

```java
V call() throws Exception;
```

这里有两个重要区别。

------

## 1. 可以返回值

Runnable：

```java
void run();
```

Callable：

```java
V call();
```

例如：

```java
Callable<Integer>
```

代表：

```text
执行任务以后
返回 Integer
```

例如：

```java
Callable<Integer> task = () -> {

    return 100;
};
```

------

## 2. 可以抛出异常

Callable：

```java
V call() throws Exception;
```

所以：

```java
Callable<String> task = () -> {

    Thread.sleep(1000);

    return "完成";
};
```

可以直接写。

不用：

```java
try {
} catch (...) {
}
```

因为：

```java
call()
```

本身允许：

```java
throws Exception
```

------

# 十二、Callable 泛型 V 是什么？

例如：

```java
Callable<Integer>
```

这里：

```java
Integer
```

表示：

> 任务执行完成以后返回的数据类型。

例如：

```java
Callable<Integer> task = () -> {

    return 100;
};
```

------

如果：

```java
Callable<String>
```

则：

```java
Callable<String> task = () -> {

    return "Hello";
};
```

------

甚至可以：

```java
Callable<User>
```

例如：

```java
Callable<User> task = () -> {

    User user = queryUser();

    return user;
};
```

所以：

```text
Callable<V>

V
↓
任务返回值类型
```

------

# 十三、Callable 为什么不能直接传给 Thread？

这是一个非常经典的问题。

Runnable 可以：

```java
new Thread(runnable);
```

但是：

```java
Callable<Integer> callable = () -> 100;
```

不能：

```java
new Thread(callable);
```

因为 Thread 构造方法接收的是：

```java
Runnable
```

而不是：

```java
Callable
```

Thread 常见构造方法：

```java
Thread(Runnable target)
```

所以：

```text
Thread
只认识 Runnable
```

不认识：

```text
Callable
```

------

# 十四、那 Callable 怎么交给 Thread 执行？

这就需要：

```java
FutureTask
```

FutureTask 非常重要。

结构：

```text
Callable
   │
   ▼
FutureTask
   │
   ▼
Runnable
   │
   ▼
Thread
```

因为 FutureTask：

```java
public class FutureTask<V>
        implements RunnableFuture<V>
```

而：

```java
RunnableFuture<V>
```

又继承：

```java
Runnable
Future<V>
```

也就是说 FutureTask 同时具备：

```text
Runnable能力
+
Future能力
```

------

# 十五、Callable + FutureTask + Thread

完整示例：

```java
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class CallableDemo {

    public static void main(String[] args)
            throws Exception {

        Callable<Integer> callable = () -> {

            System.out.println(
                    Thread.currentThread().getName()
                            + " 正在计算"
            );

            Thread.sleep(1000);

            return 100;
        };

        FutureTask<Integer> futureTask =
                new FutureTask<>(callable);

        Thread thread =
                new Thread(futureTask);

        thread.start();

        Integer result =
                futureTask.get();

        System.out.println(
                "结果：" + result
        );
    }
}
```

可能输出：

```text
Thread-0 正在计算

结果：100
```

------

# 十六、Callable + FutureTask 的执行链

重点理解：

```text
Callable<Integer>
       │
       │ 封装
       ▼
FutureTask<Integer>
       │
       │ FutureTask实现Runnable
       ▼
Thread
       │
       │ start()
       ▼
新线程
       │
       ▼
FutureTask.run()
       │
       ▼
Callable.call()
       │
       ▼
return 100
       │
       ▼
FutureTask保存结果
       │
       ▼
futureTask.get()
       │
       ▼
100
```

因此 FutureTask 可以理解为：

> Callable 和 Thread 之间的适配器。

同时还是：

> 任务执行结果的保存容器。

------

# 十七、FutureTask.get() 是什么意思？

例如：

```java
Integer result = futureTask.get();
```

意思是：

> 获取 Callable 的执行结果。

但是这里有一个极其重要的特性：

```text
如果结果还没计算完成，
get() 会阻塞当前线程。
```

例如：

```java
Callable<Integer> task = () -> {

    Thread.sleep(5000);

    return 100;
};
```

然后：

```java
thread.start();

Integer result = futureTask.get();
```

那么：

```text
main线程
   │
   ▼
futureTask.get()
   │
   ├── 结果完成？
   │
   ├── 否
   │
   ▼
阻塞
   │
   │ 等待
   ▼
Callable执行完成
   │
   ▼
返回结果
```

所以：

```java
get()
```

不是：

```text
立即拿到结果
```

而是：

```text
如果结果已经好了 → 直接返回

如果结果没好 → 等
```

------

# 十八、这就体现出了“异步执行”

例如：

```java
thread.start();

System.out.println("继续执行其他逻辑");

Integer result = futureTask.get();
```

过程可能是：

```text
main线程                    工作线程

thread.start()
    │
    ├──────────────────────► 开始执行Callable
    │
    ▼
继续执行其他逻辑
    │
    ▼
处理业务A
    │
    ▼
处理业务B
    │
    ▼
futureTask.get()
    │
    │
    │                     Callable完成
    │◄────────────────────────┘
    ▼
获取结果
```

这样你就可以：

```text
先启动耗时任务

↓

主线程继续干别的事情

↓

真正需要结果的时候再get()
```

这也是异步编程的重要思想。

------

# 十九、一个实际例子

假设你有一个接口：

```text
查询用户信息
```

里面：

```text
调用A系统
需要3秒

本地业务处理
需要1秒
```

同步写法：

```java
User user = requestSystemA();

doLocalBusiness(user);
```

可能：

```text
等待A系统3秒
+
本地1秒

总共4秒
```

如果本地业务和 A 系统暂时没有依赖关系：

```java
Callable<User> task = () -> {

    return requestSystemA();
};

FutureTask<User> futureTask =
        new FutureTask<>(task);

new Thread(futureTask).start();

doLocalBusiness();

User user = futureTask.get();
```

运行过程：

```text
时间轴：

0秒
│
├── 线程A请求远程系统
│
└── main线程处理本地业务

1秒
│
└── 本地业务完成

3秒
│
└── 远程请求完成
```

理论总时间：

```text
约3秒
```

而不是：

```text
3 + 1 = 4秒
```

------

# 二十、Runnable 与 Callable 核心区别

| 对比项            | Runnable   | Callable         |
| ----------------- | ---------- | ---------------- |
| 接口              | `Runnable` | `Callable<V>`    |
| 核心方法          | `run()`    | `call()`         |
| 返回值            | 没有       | 有               |
| 返回值类型        | `void`     | `V`              |
| Checked Exception | 不能直接抛 | 可以抛           |
| Thread直接执行    | 可以       | 不可以           |
| FutureTask封装    | 可以       | 可以             |
| 线程池execute     | 可以       | 不可以直接使用   |
| 线程池submit      | 可以       | 可以             |
| 常见用途          | 普通任务   | 有计算结果的任务 |

可以简单记：

```text
Runnable：

只管干活
不管结果


Callable：

干完活
还把结果给你
```

------

# 二十一、Runnable 和 Callable 的名字为什么不同？

Runnable：

```java
run()
```

意思类似：

```text
运行
```

Callable：

```java
call()
```

可以理解为：

```text
调用一个任务
获得返回结果
```

但是不要过度纠结名字。

真正重点：

```text
Runnable
    ↓
无返回值任务


Callable
    ↓
有返回值任务
```

------

# 二十二、Callable 也可以用 Lambda

因为 Callable：

```java
@FunctionalInterface
```

所以可以：

```java
Callable<Integer> task = () -> {

    return 100;
};
```

只有一条 return：

```java
Callable<Integer> task =
        () -> 100;
```

例如：

```java
Callable<String> task =
        () -> "Hello";
```

------

# 二十三、Runnable Lambda

Runnable：

```java
Runnable task = () -> {

    System.out.println("Hello");
};
```

一条语句甚至：

```java
Runnable task =
        () -> System.out.println("Hello");
```

------

# 二十四、一个非常容易误解的问题

假设：

```java
Runnable task = () -> {

    System.out.println("Hello");
};
```

调用：

```java
task.run();
```

会不会启动线程？

答案：

```text
不会。
```

Runnable 只是普通对象。

调用：

```java
task.run();
```

就是：

```text
普通方法调用。
```

当前是什么线程：

```text
就是什么线程执行。
```

例如：

```java
System.out.println(
        Thread.currentThread().getName()
);

task.run();
```

如果当前是：

```text
main
```

那么任务也是：

```text
main线程执行。
```

------

# 二十五、Callable.call() 直接调用呢？

一样。

例如：

```java
Callable<Integer> task =
        () -> 100;

Integer result = task.call();
```

这里：

```text
没有创建线程。
```

只是普通方法调用。

所以：

```text
Runnable.run()
Callable.call()
```

本身都：

```text
不会创建线程。
```

真正创建线程的是：

```java
Thread.start()
```

或者线程池。

------

# 二十六、Runnable / Callable 本质上是“任务对象”

推荐形成这个思维：

```text
以前：

Thread = 线程


现在：

任务
+
执行任务的人
```

例如：

```text
任务：

Runnable
Callable


执行者：

Thread
Executor
ExecutorService
ThreadPoolExecutor
ForkJoinPool
```

所以：

```text
Runnable / Callable

≠

线程
```

而是：

```text
Task
任务
```

------

# 二十七、为什么实际开发中更推荐线程池？

实际开发一般很少：

```java
new Thread(...).start();
```

大量创建线程。

更常见：

```java
ExecutorService executor =
        Executors.newFixedThreadPool(10);
```

然后：

```java
executor.submit(task);
```

关系变成：

```text
Runnable / Callable
        │
        ▼
ExecutorService
        │
        ▼
线程池
        │
        ▼
工作线程
```

------

# 二十八、Runnable 在线程池中的使用

例如：

```java
ExecutorService executorService =
        Executors.newFixedThreadPool(2);

Runnable task = () -> {

    System.out.println(
            Thread.currentThread().getName()
                    + " 执行任务"
    );
};

executorService.execute(task);
```

------

# 二十九、execute() 和 Runnable

Executor：

```java
void execute(Runnable command);
```

所以：

```java
execute()
```

只接受：

```java
Runnable
```

例如：

```java
executor.execute(() -> {

    System.out.println("执行任务");
});
```

------

# 三十、Callable 在线程池中的使用

Callable 一般使用：

```java
submit()
```

例如：

```java
ExecutorService executor =
        Executors.newFixedThreadPool(2);

Callable<Integer> task =
        () -> 100;

Future<Integer> future =
        executor.submit(task);

Integer result =
        future.get();
```

这里：

```text
Callable
    ↓
submit()
    ↓
线程池执行
    ↓
Future
    ↓
get()
    ↓
结果
```

这也是 Callable 在实际开发中更加常见的使用方式。

------

# 三十一、这里为什么返回 Future？

例如：

```java
Future<Integer> future =
        executor.submit(task);
```

因为：

```text
任务提交之后

不一定马上执行完。
```

线程池可能：

```text
正在执行其他任务
```

或者：

```text
任务正在运行
```

所以不能：

```java
Integer result = executor.submit(task);
```

于是线程池先返回一个：

```java
Future<Integer>
```

Future 可以理解为：

> 未来某个时间会产生的结果。

------

# 三十二、Future 是什么？

Future 核心方法：

```java
V get()

V get(long timeout, TimeUnit unit)

boolean cancel(boolean mayInterruptIfRunning)

boolean isCancelled()

boolean isDone()
```

可以简单理解：

```text
Future
│
├── get()
│      获取结果
│
├── isDone()
│      是否执行完成
│
├── cancel()
│      尝试取消任务
│
└── isCancelled()
       是否取消
```

------

# 三十三、FutureTask 和 Future 的关系

FutureTask：

```java
FutureTask<V>
```

既是：

```java
Runnable
```

也是：

```java
Future<V>
```

所以：

```text
                  Runnable
                     ▲
                     │
Callable ─────► FutureTask
                     │
                     ▼
                 Future<V>
```

因此 FutureTask 有两种身份：

```text
身份1：

它可以被线程执行


身份2：

它可以保存和获取执行结果
```

------

# 三十四、FutureTask 为什么这么设计？

Callable 本身存在一个问题：

```text
Callable不是Runnable
```

而：

```text
Thread只接受Runnable
```

于是：

```text
Callable
    ×
Thread
```

中间加：

```text
FutureTask
```

变成：

```text
Callable
    ↓
FutureTask
    ↓
Runnable
    ↓
Thread
```

同时 FutureTask 还能：

```text
保存Callable的结果
```

所以设计非常巧妙。

------

# 三十五、Runnable 也可以被 FutureTask 包装

FutureTask 不仅能包装 Callable：

```java
new FutureTask<>(callable);
```

也可以包装 Runnable：

```java
new FutureTask<>(runnable, result);
```

例如：

```java
Runnable runnable = () -> {

    System.out.println("执行任务");
};

FutureTask<String> futureTask =
        new FutureTask<>(
                runnable,
                "任务完成"
        );

new Thread(futureTask).start();

String result =
        futureTask.get();
```

输出：

```text
执行任务
任务完成
```

但是这里：

```text
任务本身并没有返回“任务完成”
```

这个：

```java
"任务完成"
```

是你提前指定的固定结果。

------

# 三十六、Runnable 异常问题

考虑：

```java
Runnable task = () -> {

    throw new RuntimeException("出错了");
};
```

如果通过：

```java
new Thread(task).start();
```

运行。

异常会发生在：

```text
子线程
```

而不是：

```text
main线程
```

main线程不能直接：

```java
try {

    new Thread(task).start();

} catch (RuntimeException e) {

}
```

捕获子线程内部的异常。

因为：

```text
两个线程有各自独立的调用栈。
```

大致：

```text
main线程调用栈

main()
  ↓
start()


Thread-0调用栈

run()
  ↓
异常
```

两边不是同一个调用栈。

------

# 三十七、Callable 异常如何传回来？

例如：

```java
Callable<Integer> task = () -> {

    throw new RuntimeException("计算失败");
};
```

线程池：

```java
Future<Integer> future =
        executor.submit(task);
```

然后：

```java
future.get();
```

会抛出：

```java
ExecutionException
```

原始异常会被包装进去。

例如：

```java
try {

    Integer result = future.get();

} catch (ExecutionException e) {

    Throwable cause = e.getCause();

    System.out.println(
            cause.getMessage()
    );
}
```

可能：

```text
计算失败
```

所以：

```text
Callable执行异常
      ↓
Future记录异常
      ↓
get()
      ↓
ExecutionException
```

------

# 三十八、Callable 为什么 throws Exception？

定义：

```java
V call() throws Exception;
```

意味着：

```text
Callable允许任务把异常交给执行框架处理。
```

例如：

```java
Callable<String> task = () -> {

    Thread.sleep(1000);

    Files.readString(
            Path.of("test.txt")
    );

    return "完成";
};
```

这里：

```text
InterruptedException
IOException
```

都可以直接抛。

这对于：

```text
IO任务
数据库任务
远程调用
```

非常方便。

------

# 三十九、Runnable 如果一定需要“返回结果”怎么办？

理论上也能绕一下。

例如共享变量：

```java
AtomicInteger result =
        new AtomicInteger();

Runnable task = () -> {

    result.set(100);
};
```

然后：

```java
Thread thread =
        new Thread(task);

thread.start();

thread.join();

System.out.println(
        result.get()
);
```

确实可以。

但是非常麻烦。

Callable 就是专门解决这个问题的：

```java
Callable<Integer> task =
        () -> 100;
```

然后：

```java
Future<Integer> future =
        executor.submit(task);

Integer result =
        future.get();
```

所以：

```text
需要结果
优先想到Callable
```

------

# 四十、Runnable / Callable 与线程池之间的体系关系

建议记这个：

```text
                任务

        ┌─────────────────┐
        │                 │
        ▼                 ▼

    Runnable          Callable<V>
        │                 │
        │                 │
        └────────┬────────┘
                 │
                 ▼

          ExecutorService

                 │
                 ▼

             submit()

                 │
                 ▼

             Future<V>

                 │
                 ▼

              get()

                 │
                 ▼

               结果
```

Runnable 还可以：

```text
Runnable
   │
   ▼
execute()
   │
   ▼
线程池
```

------

# 四十一、execute 和 submit 的区别先简单认识

现在先知道：

```java
execute(Runnable)
```

一般：

```text
只负责执行任务
```

没有：

```text
Future返回值
```

例如：

```java
executor.execute(task);
```

------

而：

```java
submit(...)
```

会返回：

```java
Future
```

例如：

```java
Future<?> future =
        executor.submit(runnable);
```

或者：

```java
Future<Integer> future =
        executor.submit(callable);
```

所以：

```text
execute
    ↓
执行任务


submit
    ↓
执行任务
+
返回Future
```

这个在线程池那里还会详细学习。

------

# 四十二、submit(Runnable) 为什么也有 Future？

例如：

```java
Runnable task = () -> {

    System.out.println("任务执行");
};

Future<?> future =
        executor.submit(task);
```

虽然：

```java
Runnable
```

本身没有返回结果。

但是：

```java
future.get()
```

依然可以用于：

> 等待这个任务执行完成。

任务正常结束时：

```java
future.get()
```

一般得到：

```java
null
```

因为 Runnable：

```java
void run()
```

没有结果。

所以：

```text
Runnable + submit

Future主要用于：

等待任务完成
检查状态
取消任务
捕获异常
```

------

# 四十三、submit(Callable) 的 Future

Callable：

```java
Callable<Integer> task =
        () -> 100;
```

提交：

```java
Future<Integer> future =
        executor.submit(task);
```

get：

```java
Integer result =
        future.get();
```

得到：

```text
100
```

这里：

```text
Callable返回值
       ↓
Future保存
       ↓
get获取
```

------

# 四十四、Runnable 和 Callable 的选择

非常简单。

如果你的任务：

```text
不关心结果
```

例如：

```text
发送日志
写缓存
异步通知
后台清理
```

可以：

```java
Runnable
```

------

如果任务：

```text
需要计算结果
```

例如：

```text
查询数据库
调用远程接口
计算统计结果
返回用户信息
```

可以：

```java
Callable<V>
```

------

但是实际开发还有：

```java
CompletableFuture
```

后面你会学到。

它会进一步解决：

```text
Future.get()容易阻塞
多个异步任务组合麻烦
任务之间依赖关系不好处理
```

的问题。

------

# 四十五、一个重要思想：任务 ≠ 异步

很多初学者会觉得：

```java
Runnable
Callable
```

就代表：

```text
异步
```

这是错误的。

例如：

```java
Runnable task = () -> {

    System.out.println("Hello");
};

task.run();
```

这是：

```text
同步执行。
```

Callable：

```java
Callable<Integer> task =
        () -> 100;

task.call();
```

一样：

```text
同步执行。
```

只有交给：

```java
Thread
线程池
```

等其他线程执行时，才可能成为：

```text
异步任务。
```

所以：

```text
Runnable / Callable
只是“任务定义”
```

而不是：

```text
异步机制。
```

------

# 四十六、什么叫同步和异步？

假设：

```text
A任务
B任务
```

同步：

```text
A执行
  ↓
等待A完成
  ↓
B执行
```

例如：

```java
A();

B();
```

------

异步：

```text
main线程
   │
   ├── 提交A任务 ──────► 工作线程
   │
   ▼
继续执行B任务
```

于是：

```text
A
B
```

可能并行/并发运行。

Runnable / Callable 常常只是：

```text
被异步执行的任务载体。
```

------

# 四十七、Runnable / Callable 和方法有什么区别？

其实可以把它们理解成：

> 把“一段方法逻辑”封装成了对象。

例如原来：

```java
public void doTask() {

    System.out.println("任务");
}
```

现在：

```java
Runnable task = () -> {

    System.out.println("任务");
};
```

于是：

```text
任务本身变成了一个对象。
```

就可以：

```java
传递
保存
放进集合
放进队列
提交给线程池
```

例如：

```java
List<Runnable> tasks =
        new ArrayList<>();
```

然后：

```java
tasks.add(task1);

tasks.add(task2);

tasks.add(task3);
```

这就是函数式编程中常见的：

> 行为参数化。

------

# 四十八、Runnable 为什么适合线程池？

因为线程池内部核心思想就是：

```text
生产任务
        ↓
任务队列
        ↓
工作线程
        ↓
不断获取任务执行
```

例如：

```text
                    BlockingQueue

Runnable1 ───► ┌───────────────────┐
Runnable2 ───► │ task1 task2 task3 │
Runnable3 ───► └───────────────────┘
                       │
              ┌────────┴────────┐
              ▼                 ▼
          Worker-1          Worker-2
              │                 │
              ▼                 ▼
          task.run()        task.run()
```

你之前学习 BlockingQueue 时看到的：

```text
任务队列
```

里面非常常见的就是：

```java
Runnable
```

线程池本质上就是：

```text
Runnable任务的生产者消费者模型。
```

------

# 四十九、ThreadPoolExecutor 中的 Runnable

ThreadPoolExecutor 的：

```java
execute(Runnable command)
```

参数就是：

```java
Runnable
```

例如：

```java
threadPoolExecutor.execute(
        new Runnable() {

            @Override
            public void run() {

                System.out.println("任务");
            }
        }
);
```

Lambda：

```java
threadPoolExecutor.execute(() -> {

    System.out.println("任务");
});
```

这个 Runnable 后面可能：

```text
直接交给核心线程

或者

进入BlockingQueue

或者

创建非核心线程

或者

触发拒绝策略
```

所以 Runnable 会贯穿整个 Java 线程池体系。

------

# 五十、Callable 在线程池内部最终也是怎么执行的？

这是一个很值得理解的问题。

我们写：

```java
Callable<Integer> callable =
        () -> 100;

Future<Integer> future =
        executor.submit(callable);
```

但是：

```text
线程池核心 execute()
```

接收的是：

```java
Runnable
```

那 Callable 怎么进去？

答案还是：

```text
被包装。
```

大致：

```text
Callable
   │
   ▼
RunnableFuture
   │
   ▼
FutureTask
   │
   ▼
Runnable
   │
   ▼
execute()
```

也就是说：

```java
submit(callable)
```

内部大致可以想象：

```java
RunnableFuture<Integer> future =
        new FutureTask<>(callable);

execute(future);

return future;
```

注意：

这是帮助理解的简化版本。

------

# 五十一、所以 submit() 的本质

可以形成这个理解：

```text
submit()
   │
   ▼
把任务包装成 FutureTask
   │
   ▼
调用execute()
   │
   ▼
线程池执行FutureTask
   │
   ▼
FutureTask调用Callable.call()
   │
   ▼
保存结果
   │
   ▼
返回Future
```

因此：

```text
submit
```

和：

```text
FutureTask
```

关系非常密切。

------

# 五十二、FutureTask 可以保证任务只执行一次

例如：

```java
FutureTask<Integer> futureTask =
        new FutureTask<>(() -> {

            System.out.println("执行");

            return 100;
        });
```

两个线程：

```java
new Thread(futureTask).start();

new Thread(futureTask).start();
```

FutureTask 的任务不会正常重复计算两次。

它内部有状态控制。

可以粗略理解：

```text
NEW
 ↓
RUNNING
 ↓
COMPLETED
```

一旦任务执行完成：

```text
再次run()
```

不会重新执行 Callable。

这个性质有时候非常有用。

------

# 五十三、Callable 的异常传播过程

例如：

```java
Callable<Integer> callable = () -> {

    int a = 1 / 0;

    return a;
};
```

执行：

```java
Future<Integer> future =
        executor.submit(callable);
```

任务线程发生：

```java
ArithmeticException
```

Future 会把异常保存下来。

之后：

```java
future.get();
```

抛：

```java
ExecutionException
```

关系：

```text
ArithmeticException
       │
       ▼
Future保存
       │
       ▼
ExecutionException
       │
       ▼
getCause()
       │
       ▼
ArithmeticException
```

所以：

```java
catch (ExecutionException e) {

    Throwable cause =
            e.getCause();
}
```

------

# 五十四、Future.get() 为什么抛 InterruptedException？

Future：

```java
V get()
    throws InterruptedException,
           ExecutionException;
```

因为调用：

```java
get()
```

的线程可能：

```text
正在等待任务执行完成。
```

等待期间：

```java
interrupt()
```

可能中断等待。

例如：

```text
main线程

future.get()
    ↓
WAITING
    ↓
别人调用mainThread.interrupt()
    ↓
InterruptedException
```

这和你前面学的：

```java
sleep
wait
await
join
```

有相似之处：

> 都属于可能发生阻塞等待的方法。

------

# 五十五、get() 的三个重要结果

调用：

```java
future.get();
```

可能出现三种主要情况。

## 情况一：任务正常完成

```text
返回结果
```

例如：

```text
100
```

------

## 情况二：任务内部异常

抛：

```java
ExecutionException
```

------

## 情况三：等待过程中线程被中断

抛：

```java
InterruptedException
```

所以：

```java
get()
```

不是一个简单方法。

它本质上：

```text
等待 + 获取结果 + 异常传播
```

------

# 五十六、Future.get(timeout)

除了：

```java
future.get();
```

还有：

```java
future.get(
        3,
        TimeUnit.SECONDS
);
```

表示：

> 最多等待 3 秒。

如果 3 秒还没完成：

```java
TimeoutException
```

例如：

```java
try {

    Integer result =
            future.get(
                    3,
                    TimeUnit.SECONDS
            );

} catch (TimeoutException e) {

    System.out.println("超时");
}
```

这对于：

```text
远程调用
IO任务
计算任务
```

很重要。

因为无限：

```java
get()
```

可能一直等。

------

# 五十七、Callable 并不一定比 Runnable“高级”

不要理解成：

```text
Callable > Runnable
```

它们只是适用场景不同。

Runnable：

```text
轻量
简单
无需结果
```

Callable：

```text
需要结果
或者
需要异常传播
```

例如：

```java
executor.execute(() -> {

    cleanCache();
});
```

这种任务没必要非得：

```java
Callable<Void>
```

虽然理论上也能写：

```java
Callable<Void> task = () -> {

    cleanCache();

    return null;
};
```

但明显 Runnable 更自然。

------

# 五十八、Callable

有时候 Callable 不需要返回实际数据，但是又希望：

```text
允许throws Exception
```

可以：

```java
Callable<Void> task = () -> {

    doSomething();

    return null;
};
```

这里：

```java
Void
```

不是：

```java
void
```

而是：

```java
java.lang.Void
```

最终返回：

```java
null
```

------

# 五十九、Runnable 和 Callable 对变量的访问

Lambda 中可以访问：

```text
成员变量

静态变量

final局部变量

effectively final局部变量
```

例如：

```java
int count = 10;

Runnable task = () -> {

    System.out.println(count);
};
```

可以。

因为：

```java
count
```

后续没有修改，是：

```text
effectively final
```

但是：

```java
int count = 10;

Runnable task = () -> {

    System.out.println(count);
};

count = 20;
```

会编译失败。

这是 Lambda 捕获局部变量的规则。

------

# 六十、多个任务共享变量时要注意线程安全

例如：

```java
int count = 0;
```

不能直接让多个 Runnable：

```java
count++;
```

因为：

```text
Runnable只是任务抽象
```

并不会自动提供：

```text
线程安全
```

例如：

```java
class CounterTask implements Runnable {

    private int count = 0;

    @Override
    public void run() {

        count++;
    }
}
```

如果多个线程共享：

```java
CounterTask task =
        new CounterTask();

new Thread(task).start();

new Thread(task).start();
```

那么：

```text
多个线程
    ↓
同一个task对象
    ↓
同一个count变量
```

可能产生：

```text
线程安全问题。
```

所以：

```text
Runnable
Callable
```

和：

```text
线程安全
```

是两个不同概念。

------

# 六十一、一个 Runnable 对象可以被多个线程共享

例如：

```java
Runnable task = () -> {

    System.out.println(
            Thread.currentThread().getName()
    );
};

new Thread(task, "线程A").start();

new Thread(task, "线程B").start();
```

这里：

```text
只有一个Runnable对象
```

但是：

```text
两个Thread
```

结构：

```text
         Runnable
          /    \
         /      \
        ▼        ▼
   Thread-A   Thread-B
```

所以：

```text
任务对象
```

和：

```text
线程对象
```

完全不是一一绑定的。

------

# 六十二、Runnable 的对象状态可能被多个线程共享

比如：

```java
class Task implements Runnable {

    private int count = 0;

    @Override
    public void run() {

        count++;
    }
}
```

如果：

```java
Task task = new Task();

new Thread(task).start();

new Thread(task).start();
```

那么：

```text
两个线程
    ↓
共享同一个Task
    ↓
共享count
```

因此：

```text
存在竞态条件。
```

这也是为什么学习 Runnable 以后，会逐渐进入：

```text
synchronized
Lock
Atomic
volatile
CAS
```

这些线程安全机制。

------

# 六十三、继承 Thread 和实现 Runnable 怎么选？

一般更推荐：

```java
implements Runnable
```

而不是：

```java
extends Thread
```

原因主要有几个。

------

## 原因一：Java 单继承

如果：

```java
class MyThread extends Thread
```

那么：

```text
已经使用了唯一的继承位置。
```

以后不能再：

```java
extends 其他类
```

而 Runnable 是接口：

```java
class MyTask
        extends SomeClass
        implements Runnable
```

完全没问题。

------

## 原因二：任务和线程解耦

Thread：

```java
class MyThread extends Thread {

    public void run() {
    }
}
```

任务和线程绑在一起。

Runnable：

```java
class MyTask implements Runnable {

    public void run() {
    }
}
```

可以：

```text
Thread执行
线程池执行
其他执行器执行
```

更加灵活。

------

## 原因三：更符合线程池设计

线程池接收的就是：

```java
Runnable
Callable
```

而不是让你不停：

```java
new MyThread()
```

所以现代 Java 并发编程核心思想更加倾向：

```text
任务
+
执行器
```

------

# 六十四、实际开发真正推荐的层次

学习阶段：

```java
new Thread(runnable).start();
```

非常适合帮助理解线程原理。

但是实际项目通常：

```text
不推荐随意new Thread()
```

更加常见：

```java
ExecutorService
ThreadPoolExecutor
CompletableFuture
```

所以你可以把学习路线理解成：

```text
Thread
   ↓
Runnable
   ↓
Callable
   ↓
Future
   ↓
FutureTask
   ↓
Executor
   ↓
ExecutorService
   ↓
ThreadPoolExecutor
   ↓
CompletableFuture
```

------

# 六十五、Runnable / Callable 在整个并发体系中的位置

可以画成：

```text
Java并发任务体系
│
├── Thread
│
│     直接创建线程
│
├── Runnable
│
│     无返回值任务
│
├── Callable<V>
│
│     有返回值任务
│
├── Future<V>
│
│     表示未来结果
│
├── FutureTask<V>
│
│     Runnable + Future
│
├── Executor
│
│     执行任务
│
├── ExecutorService
│
│     提交任务
│
├── ThreadPoolExecutor
│
│     线程池实现
│
└── CompletableFuture
      更强大的异步任务编排
```

------

# 六十六、Runnable 可以理解为“命令模式”

从设计模式角度：

```java
Runnable
```

很像：

```text
Command
```

即：

> 把要执行的一段操作封装成对象。

例如：

```java
Runnable saveTask =
        () -> save();

Runnable emailTask =
        () -> sendEmail();

Runnable logTask =
        () -> writeLog();
```

然后执行器：

```java
executor.execute(saveTask);

executor.execute(emailTask);

executor.execute(logTask);
```

执行器完全不需要知道：

```text
任务具体干什么。
```

只需要：

```java
task.run();
```

这就是：

```text
面向接口编程。
```

------

# 六十七、一个非常重要的抽象

线程池内部可以认为：

```java
Worker worker
```

不断执行：

```java
while (...) {

    Runnable task =
            queue.take();

    task.run();
}
```

这其实就是你之前学的：

```text
BlockingQueue
+
生产者消费者模型
+
Runnable
```

组合起来。

生产者：

```text
业务线程
   ↓
submit任务
```

消费者：

```text
Worker线程
   ↓
从BlockingQueue取任务
   ↓
task.run()
```

于是：

```text
BlockingQueue
Runnable
Thread
```

一下就串起来了。

------

# 六十八、Runnable 为什么没有泛型？

因为：

```java
Runnable.run()
```

没有返回值：

```java
void
```

所以没必要：

```java
Runnable<T>
```

------

Callable 因为：

```java
V call()
```

要返回结果，所以需要：

```java
Callable<V>
```

例如：

```text
Callable<Integer>

Callable<String>

Callable<User>

Callable<List<User>>
```

------

# 六十九、Callable<List> 示例

例如：

```java
Callable<List<User>> task = () -> {

    return userService.list();
};
```

提交：

```java
Future<List<User>> future =
        executor.submit(task);
```

获取：

```java
List<User> users =
        future.get();
```

泛型关系严格对应：

```text
Callable<List<User>>
        ↓
Future<List<User>>
        ↓
List<User>
```

------

# 七十、Future.get() 不代表“异步就一定更快”

这是一个非常重要的点。

例如：

```java
Future<Integer> future =
        executor.submit(task);

Integer result =
        future.get();

doSomethingElse();
```

如果：

```text
提交之后立刻get()
```

那么：

```text
main线程马上阻塞等待
```

效果可能和同步执行差不多。

真正发挥异步优势：

```java
Future<Integer> future =
        executor.submit(task);

// 做其他事情
doA();

doB();

doC();

// 真正需要结果时
Integer result =
        future.get();
```

结构：

```text
错误思路：

submit
 ↓
get
 ↓
等待
 ↓
后续业务


更合理：

submit
 ↓
后续无依赖业务
 ↓
后续无依赖业务
 ↓
真正需要结果
 ↓
get
```

------

# 七十一、并不是所有任务都适合异步

假设：

```text
任务B完全依赖任务A的结果
```

例如：

```java
User user =
        queryUser();

saveUserLog(user);
```

如果：

```text
saveUserLog()
```

必须拿到：

```text
user
```

那么即使：

```java
Future<User> future =
        executor.submit(...);
```

你马上：

```java
User user =
        future.get();
```

还是得等。

这种情况下异步不一定有意义。

真正适合并发的是：

```text
A任务
B任务
C任务

彼此之间至少有一部分没有依赖。
```

------

# 七十二、Runnable 和 Callable 与 Stream 的相似之处

你以后可能会感觉它们和：

```java
Supplier
Consumer
Function
Predicate
```

有一些像。

因为本质都是：

```text
把行为抽象成对象。
```

例如：

```java
Runnable
```

类似：

```text
无参数
无返回值
```

可以记：

```text
() -> void
```

------

Callable：

```text
无参数
有返回值
```

可以记：

```text
() -> V
```

类似于：

```java
Supplier<V>
```

但是 Callable：

```java
throws Exception
```

而普通 Supplier：

```java
T get()
```

不支持直接抛 Checked Exception。

------

# 七十三、用函数签名快速记忆

Runnable：

```text
() -> void
```

Callable：

```text
() -> V
```

Consumer：

```text
T -> void
```

Supplier：

```text
() -> T
```

Function：

```text
T -> R
```

Predicate：

```text
T -> boolean
```

这样会非常容易理解。

------

# 七十四、Runnable 和 Callable 都是无参数任务吗？

从接口方法来看：

```java
Runnable.run()
Callable.call()
```

确实都是：

```text
没有参数。
```

但是实际任务需要数据怎么办？

通过：

```text
成员变量
构造方法
Lambda捕获变量
```

传入。

例如：

```java
int userId = 1001;

Callable<User> task =
        () -> userService.getById(userId);
```

这里：

```text
userId
```

被 Lambda 捕获。

------

# 七十五、使用类时通过构造方法传参

例如：

```java
class QueryUserTask
        implements Callable<User> {

    private final Long userId;

    public QueryUserTask(Long userId) {

        this.userId = userId;
    }

    @Override
    public User call() {

        return queryUser(userId);
    }
}
```

使用：

```java
Callable<User> task =
        new QueryUserTask(1001L);
```

所以：

```text
call()虽然没有参数

任务对象本身可以保存参数。
```

------

# 七十六、为什么 run()/call() 不设计参数？

因为执行器需要统一执行任务。

例如：

```java
task.run();
```

如果每个任务：

```text
参数数量不同
类型不同
```

执行器就没办法统一调用。

通过：

```text
任务对象提前保存上下文
```

最终执行器只需要：

```java
run()
```

或者：

```java
call()
```

即可。

这就是一种很经典的对象设计。

------

# 七十七、Runnable 中 this 是谁？

例如：

```java
class MyTask implements Runnable {

    @Override
    public void run() {

        System.out.println(this);
    }
}
```

这里：

```java
this
```

指：

```text
MyTask对象
```

不是：

```text
当前Thread对象
```

想拿当前线程应该：

```java
Thread.currentThread();
```

例如：

```java
Thread current =
        Thread.currentThread();
```

这一点非常重要。

------

# 七十八、Runnable 对象和 Thread.currentThread()

例如：

```java
Runnable task = () -> {

    System.out.println(
            Thread.currentThread().getName()
    );
};

Thread t =
        new Thread(
                task,
                "worker-1"
        );

t.start();
```

Runnable 本身不知道：

```text
自己一定被哪个线程执行。
```

因为同一个 Runnable：

```java
task
```

可能今天被：

```text
worker-1
```

执行。

也可能被：

```text
worker-2
```

执行。

甚至：

```java
main线程直接task.run()
```

执行。

------

# 七十九、任务和线程解耦的真正好处

假设你写：

```java
Runnable task =
        () -> generateReport();
```

今天：

```java
new Thread(task).start();
```

明天：

```java
executor.execute(task);
```

后天：

```java
scheduledExecutor.schedule(
        task,
        10,
        TimeUnit.SECONDS
);
```

任务本身：

```java
generateReport()
```

完全不用改。

这就是：

```text
任务与调度方式解耦。
```

------

# 八十、Runnable 的生命周期吗？

严格来说：

```text
Runnable没有线程生命周期。
```

Runnable 就是普通 Java 对象。

所以不能说：

```text
Runnable进入RUNNABLE状态
Runnable进入WAITING状态
```

错误。

线程状态属于：

```java
Thread
```

例如：

```text
NEW
RUNNABLE
BLOCKED
WAITING
TIMED_WAITING
TERMINATED
```

Runnable 本身：

```text
没有这些状态。
```

------

# 八十一、Callable 也没有线程状态

同理：

```java
Callable
```

只是普通任务对象。

所以：

```text
Callable被阻塞了
```

这种说法严格来说不准确。

更准确：

```text
执行Callable任务的线程被阻塞了。
```

例如：

```java
Callable<Integer> task = () -> {

    Thread.sleep(1000);

    return 1;
};
```

这里：

```text
sleep的是当前执行call()的Thread
```

不是 Callable 对象。

------

# 八十二、一个线程可以执行多个 Runnable

尤其在线程池中：

```text
Worker-1
   │
   ├── Runnable1
   │
   ├── Runnable2
   │
   ├── Runnable3
   │
   └── Runnable4
```

同一个线程：

```text
重复从任务队列获取任务
```

所以：

```text
线程 != 任务
```

更不是：

```text
一个任务对应永久一个线程。
```

------

# 八十三、一个 Runnable 也可能被多个线程执行

例如：

```text
             Runnable
             /     \
            /       \
           ▼         ▼
       Thread-1   Thread-2
```

因此 Runnable 内部如果有：

```text
共享可变状态
```

必须考虑线程安全。

------

# 八十四、Runnable 的 run() 执行结束意味着什么？

如果：

```java
new Thread(task).start();
```

这个 Thread 执行：

```java
task.run()
```

当：

```java
run()
```

执行结束：

```text
线程任务完成
```

如果 Thread 没有其他逻辑：

```text
线程进入TERMINATED。
```

但是在线程池中不同。

线程池 Worker：

```text
执行完一个Runnable
```

通常不会死。

而是：

```text
继续取下一个Runnable。
```

例如：

```text
Worker线程：

while (...) {

    Runnable task =
            getTask();

    task.run();
}
```

所以：

```text
Runnable结束

≠

线程池线程结束
```

------

# 八十五、这正是线程池为什么高效

如果每个任务：

```java
new Thread(task).start();
```

那么：

```text
创建线程
执行任务
销毁线程

创建线程
执行任务
销毁线程

创建线程
执行任务
销毁线程
```

开销很大。

线程池：

```text
创建Worker线程
       │
       ├── task1.run()
       ├── task2.run()
       ├── task3.run()
       └── task4.run()
```

复用线程。

因此：

```text
Runnable作为任务

Thread作为执行资源
```

这个拆分非常关键。

------

# 八十六、Callable 和线程池的关系更自然

虽然你可以：

```java
FutureTask<Integer> futureTask =
        new FutureTask<>(callable);

new Thread(futureTask).start();
```

但是实际开发更常见：

```java
Future<Integer> future =
        executor.submit(callable);
```

因为线程池内部会自动：

```text
包装Callable
+
调度线程
+
管理线程
+
保存结果
```

你不需要手动创建：

```java
FutureTask
Thread
```

------

# 八十七、Runnable / Callable 常见面试题

## 问题一

### Runnable 和 Callable 有什么区别？

标准回答：

```text
1. Runnable核心方法是run()

2. Callable核心方法是call()

3. Runnable没有返回值

4. Callable可以通过泛型返回结果

5. Runnable.run()不能声明throws Exception

6. Callable.call()可以throws Exception

7. Runnable可以直接交给Thread

8. Callable不能直接交给Thread，
   一般通过FutureTask或者ExecutorService执行

9. Callable执行结果通常通过Future获取
```

------

# 八十八、面试题：为什么 Callable 不能直接传给 Thread？

因为：

```java
Thread
```

构造器接受：

```java
Runnable
```

而：

```java
Callable
```

没有继承：

```java
Runnable
```

所以需要：

```java
FutureTask
```

进行适配。

------

# 八十九、面试题：FutureTask 是什么？

可以回答：

> FutureTask 是 Runnable 和 Future 的一个实现，它既可以作为 Runnable 被线程或线程池执行，又可以通过 Future 的 get() 获取异步任务结果。它常用于包装 Callable，使 Callable 能够被只接受 Runnable 的执行体系执行。

结构：

```text
Callable
   ↓
FutureTask
   ↓
Runnable + Future
```

------

# 九十、面试题：调用 Runnable.run() 会创建新线程吗？

不会。

```java
task.run();
```

只是：

```text
普通方法调用。
```

只有：

```java
Thread.start()
```

才会真正启动新线程。

------

# 九十一、面试题：Callable.call() 直接调用会异步吗？

不会。

例如：

```java
Integer result =
        callable.call();
```

只是：

```text
当前线程直接执行call()
```

不存在异步。

------

# 九十二、面试题：Future.get() 会阻塞吗？

会。

如果：

```text
任务尚未完成
```

那么调用：

```java
future.get()
```

的线程会阻塞等待。

如果任务已经完成：

```text
直接返回结果。
```

------

# 九十三、面试题：submit 后马上 get 有什么问题？

例如：

```java
Future<Integer> future =
        executor.submit(task);

Integer result =
        future.get();
```

这会导致：

```text
当前线程立刻等待异步任务完成
```

异步带来的并行收益可能很小。

通常更合理：

```java
Future<Integer> future =
        executor.submit(task);

doOtherBusiness();

Integer result =
        future.get();
```

------

# 九十四、面试题：Runnable 的异常和 Callable 的异常区别

Runnable：

```text
run()不能直接抛Checked Exception
```

任务运行时异常通常发生在执行线程内部。

Callable：

```text
call()可以throws Exception
```

在线程池/FutureTask场景中，异常通常会被：

```text
Future保存
```

然后：

```java
future.get()
```

通过：

```java
ExecutionException
```

传递给调用方。

------

# 九十五、面试题：FutureTask 为什么既是 Runnable 又是 Future？

因为它需要同时完成两个职责：

```text
Runnable：

让任务能够被线程执行


Future：

让调用者能够获取任务状态和结果
```

所以：

```text
FutureTask
=
可执行任务
+
任务结果句柄
```

------

# 九十六、常见错误：把 Runnable 当线程

错误说法：

```text
创建了一个Runnable线程。
```

严格来说：

```text
错误。
```

正确：

```text
创建了一个Runnable任务。
```

然后：

```text
由某个线程执行这个Runnable任务。
```

------

# 九十七、常见错误：认为一个 Runnable 一定对应一个线程

不是。

可能：

```text
Runnable A
   │
   ├── Thread-1
   ├── Thread-2
   └── main
```

反过来：

```text
ThreadPool Worker-1
   │
   ├── Runnable A
   ├── Runnable B
   ├── Runnable C
   └── Runnable D
```

所以：

```text
线程和任务是两个维度。
```

------

# 九十八、常见错误：Callable 自动异步

错误：

```java
Callable<Integer> task =
        () -> 100;
```

这样只是：

```text
创建任务对象。
```

甚至：

```java
task.call();
```

也是同步。

只有：

```text
交给其他线程/线程池执行
```

才是异步。

------

# 九十九、常见错误：get() 永远立即返回

错误。

```java
future.get();
```

如果任务没结束：

```text
阻塞。
```

所以生产环境需要注意：

```text
无限等待问题。
```

有时候会使用：

```java
future.get(
        3,
        TimeUnit.SECONDS
);
```

限制等待时间。

------

# 一百、常见错误：Runnable 天然线程安全

完全错误。

Runnable：

```text
只是接口。
```

它不会：

```text
加锁
保证可见性
保证原子性
防止竞态条件
```

线程安全还是需要：

```text
synchronized
ReentrantLock
volatile
Atomic类
并发容器
```

等机制。

------

# 一百零一、一个完整 Runnable 示例

```java
public class RunnableDemo {

    public static void main(String[] args)
            throws InterruptedException {

        Runnable task = () -> {

            for (int i = 1; i <= 5; i++) {

                System.out.println(
                        Thread.currentThread()
                                .getName()
                                + "："
                                + i
                );
            }
        };

        Thread t1 =
                new Thread(
                        task,
                        "线程A"
                );

        Thread t2 =
                new Thread(
                        task,
                        "线程B"
                );

        t1.start();

        t2.start();

        t1.join();

        t2.join();

        System.out.println(
                "main结束"
        );
    }
}
```

这里：

```text
一个Runnable

两个Thread
```

结构：

```text
           task
          /    \
         ▼      ▼
       t1        t2
```

------

# 一百零二、一个完整 Callable 示例

```java
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class CallableDemo {

    public static void main(String[] args)
            throws Exception {

        Callable<Integer> callable = () -> {

            System.out.println(
                    Thread.currentThread()
                            .getName()
                            + " 开始计算"
            );

            Thread.sleep(2000);

            int sum = 0;

            for (int i = 1; i <= 100; i++) {

                sum += i;
            }

            return sum;
        };

        FutureTask<Integer> futureTask =
                new FutureTask<>(callable);

        Thread thread =
                new Thread(
                        futureTask,
                        "计算线程"
                );

        thread.start();

        System.out.println(
                "main线程继续工作"
        );

        Integer result =
                futureTask.get();

        System.out.println(
                "计算结果："
                        + result
        );
    }
}
```

结果：

```text
main线程继续工作

计算线程 开始计算

计算结果：5050
```

------

# 一百零三、线程池版 Callable 示例

实际开发更推荐：

```java
import java.util.concurrent.*;

public class CallablePoolDemo {

    public static void main(String[] args)
            throws Exception {

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        Callable<Integer> task = () -> {

            Thread.sleep(1000);

            return 100;
        };

        Future<Integer> future =
                executor.submit(task);

        System.out.println(
                "主线程继续执行"
        );

        Integer result =
                future.get();

        System.out.println(
                result
        );

        executor.shutdown();
    }
}
```

------

# 一百零四、多个 Callable 并发执行

例如三个远程接口：

```text
查询用户
查询订单
查询优惠券
```

它们互相不依赖。

可以：

```java
Future<User> userFuture =
        executor.submit(
                () -> queryUser()
        );

Future<List<Order>> orderFuture =
        executor.submit(
                () -> queryOrders()
        );

Future<List<Coupon>> couponFuture =
        executor.submit(
                () -> queryCoupons()
        );
```

三个任务：

```text
线程1 → 查询用户

线程2 → 查询订单

线程3 → 查询优惠券
```

之后：

```java
User user =
        userFuture.get();

List<Order> orders =
        orderFuture.get();

List<Coupon> coupons =
        couponFuture.get();
```

如果三者分别需要：

```text
2秒
3秒
1秒
```

同步：

```text
2 + 3 + 1
=
6秒
```

理论并发：

```text
max(2,3,1)
≈
3秒
```

这就是并发调用的价值。

------

# 一百零五、但是 Future 有一个明显缺点

假设：

```text
A任务完成后
才能执行B

B完成后
才能执行C
```

Future 写起来可能：

```java
Future<A> futureA =
        executor.submit(...);

A a =
        futureA.get();

Future<B> futureB =
        executor.submit(
                () -> doB(a)
        );

B b =
        futureB.get();

Future<C> futureC =
        executor.submit(
                () -> doC(b)
        );
```

会出现很多：

```java
get()
```

并且：

```text
任务组合非常麻烦。
```

所以后来 Java 8 引入：

```java
CompletableFuture
```

就可以：

```java
CompletableFuture
        .supplyAsync(...)
        .thenApply(...)
        .thenCompose(...)
        .thenAccept(...);
```

这也是你后面学习 CompletableFuture 时要解决的核心问题。

------

# 一百零六、Runnable / Callable 与 CompletableFuture 的联系

CompletableFuture 常见方法：

```java
runAsync()
```

适合：

```text
无返回值任务
```

类似：

```java
Runnable
```

例如：

```java
CompletableFuture.runAsync(() -> {

    System.out.println("任务");
});
```

------

还有：

```java
supplyAsync()
```

适合：

```text
有返回值任务
```

例如：

```java
CompletableFuture<Integer> future =
        CompletableFuture.supplyAsync(
                () -> 100
        );
```

概念上类似：

```text
Callable / Supplier
```

所以现在学习：

```text
Runnable
Callable
Future
```

是学习：

```text
CompletableFuture
```

的重要基础。

------

# 一百零七、Runnable / Callable 的核心思想其实不是“创建线程”

这一点一定要牢牢记住。

它们真正重要的思想是：

> **任务抽象。**

过去：

```text
我要创建一条线程
```

现代并发编程更倾向：

```text
我要创建一个任务

↓

把任务交给执行器
```

所以思维从：

```text
面向线程
```

逐渐转变为：

```text
面向任务。
```

------

# 一百零八、整个演化过程

可以这样看 Java 多线程 API 的演化：

```text
第一阶段：

Thread

自己管理线程


        ↓


第二阶段：

Runnable

线程和任务解耦


        ↓


第三阶段：

Callable + Future

任务可以返回结果


        ↓


第四阶段：

ExecutorService

统一管理任务执行


        ↓


第五阶段：

ThreadPoolExecutor

线程池复用线程


        ↓


第六阶段：

CompletableFuture

异步任务编排
```

这是整个知识体系非常重要的一条主线。

------

# 一百零九、最终总图

```text
                         Java并发任务体系

                              Task
                               │
                 ┌─────────────┴─────────────┐
                 │                           │
                 ▼                           ▼

             Runnable                   Callable<V>
                 │                           │
                 │                           │
        void run()                  V call() throws Exception
                 │                           │
                 │                           │
          无返回值任务                  有返回值任务
                 │                           │
                 │                           ▼
                 │                       FutureTask<V>
                 │                           │
                 └──────────────┬────────────┘
                                │
                                ▼

                         ExecutorService

                                │
                 ┌──────────────┴──────────────┐
                 │                             │
                 ▼                             ▼

             execute()                     submit()
                 │                             │
                 ▼                             ▼

             Runnable                      Future<V>
                                               │
                                               ▼
                                             get()
                                               │
                                               ▼
                                             结果
```

------

# 一百一十、一句话理解每一个东西

## Thread

```text
真正执行代码的线程。
```

------

## Runnable

```text
没有返回值的任务。
```

核心：

```java
void run();
```

------

## Callable

```text
可以返回结果的任务。
```

核心：

```java
V call() throws Exception;
```

------

## Future

```text
未来任务结果的凭证。
```

核心：

```java
V get();
```

------

## FutureTask

```text
既能被线程执行，
又能保存和获取结果的任务。
```

可以理解：

```text
Runnable + Future
```

------

## Executor

```text
任务执行器。
```

------

## ExecutorService

```text
可以提交任务、
管理任务生命周期的执行器。
```

------

## ThreadPoolExecutor

```text
真正的线程池实现。
```

------

# 一百一十一、最终必须背住的知识点

这一部分建议直接背下来。

```text
1.

Runnable 和 Callable 都不是线程，
而是任务。


2.

Runnable：

void run()

没有返回值。


3.

Callable<V>：

V call() throws Exception

可以返回值，
可以抛异常。


4.

Runnable 可以直接传给 Thread。


5.

Callable 不能直接传给 Thread。


6.

Callable 可以使用 FutureTask 包装：

Callable
→ FutureTask
→ Thread


7.

FutureTask：

Runnable
+
Future


8.

Future.get()：

任务完成 → 返回结果

任务未完成 → 阻塞等待


9.

Runnable.run()
和
Callable.call()

直接调用都不会创建线程。


10.

真正启动Thread新线程的是：

start()


11.

现代Java并发编程更加推荐：

任务
+
线程池

而不是大量：

new Thread()


12.

Runnable适合：

不需要返回结果的任务。


13.

Callable适合：

需要返回结果
或者
需要向外传播异常的任务。


14.

线程池中的Callable，
最终通常会被包装成FutureTask一类的Runnable任务。


15.

Runnable / Callable
本质上的最大意义：

将“任务”
和
“执行任务的线程”

解耦。
```

------

# 一百一十二、最后用一张图彻底记住

```text
【无返回值】

Runnable
   │
   │ run()
   ▼
任务
   │
   ├── Thread
   │
   └── ExecutorService
           │
           ▼
        工作线程


──────────────────────────────


【有返回值】

Callable<V>
    │
    │ call()
    ▼
FutureTask<V>
    │
    ├── Runnable
    │      │
    │      ▼
    │    Thread
    │
    └── Future<V>
           │
           ▼
          get()
           │
           ▼
          结果


──────────────────────────────


【实际开发】

Runnable / Callable
        │
        ▼
ExecutorService
        │
        ▼
ThreadPoolExecutor
        │
        ▼
BlockingQueue
        │
        ▼
Worker Thread
        │
        ▼
task.run()
```

最核心的一句话：

> **Thread 是执行者，Runnable / Callable 是任务；Runnable 只负责干活，Callable 干完活还能把结果带回来。**