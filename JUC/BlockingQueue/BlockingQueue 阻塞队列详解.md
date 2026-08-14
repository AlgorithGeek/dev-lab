# BlockingQueue 阻塞队列

## 一、BlockingQueue 是什么？

`BlockingQueue` 全称：

```java
java.util.concurrent.BlockingQueue
```

中文一般叫：

> **阻塞队列**

它位于 JUC：

```text
java.util.concurrent
```

`BlockingQueue` 本质上首先是一个：

```text
Queue
```

也就是说，它仍然是一个**队列**。

队列最基本的特点：

```text
先进先出 FIFO
```

例如：

```text
入队：

A → B → C

出队：

A → B → C
```

但 `BlockingQueue` 在普通 `Queue` 的基础上，增加了一个非常重要的能力：

> **当队列满了或者空了的时候，可以让线程阻塞等待。**

这就是“Blocking”的含义。

------

# 二、普通 Queue 有什么问题？

假设现在有一个普通队列：

```java
Queue<String> queue = new LinkedList<>();
```

生产者线程不断往里面放数据：

```java
queue.add("任务");
```

消费者线程不断取数据：

```java
queue.poll();
```

如果消费者取的时候：

```text
队列为空
```

那么：

```java
queue.poll()
```

直接返回：

```java
null
```

它不会等待。

于是我们可能需要自己写：

```java
while (queue.isEmpty()) {
    // 等待
}
```

但是这样写有非常严重的问题。

例如：

```java
while (queue.isEmpty()) {

}
```

这叫：

```text
忙等待
busy waiting
```

线程会一直占着 CPU：

```text
检查
检查
检查
检查
检查
……
```

非常浪费 CPU。

于是我们以前可以使用：

```java
wait()
notify()
```

或者：

```java
Condition.await()
Condition.signal()
```

自己实现生产者消费者模型。

但是自己写比较麻烦。

所以 Java 给我们封装好了：

```text
BlockingQueue
```

------

# 三、BlockingQueue 最核心的作用

BlockingQueue 最典型的用途就是：

```text
生产者 —— BlockingQueue —— 消费者
```

例如：

```text
生产者线程
    ↓
生产任务
    ↓
BlockingQueue
    ↓
消费者线程
    ↓
处理任务
```

假设 BlockingQueue 最大容量：

```text
3
```

当前：

```text
[A][B][C]
```

已经满了。

生产者再调用：

```java
queue.put("D");
```

此时不会直接失败。

而是：

```text
生产者线程阻塞
```

等消费者取走一个元素：

```text
消费者：

take() → A
```

队列变成：

```text
[B][C]
```

出现空位。

这时候之前阻塞的生产者线程就可以继续：

```text
put("D")
```

最终：

```text
[B][C][D]
```

------

# 四、BlockingQueue 的另外一种阻塞情况

如果消费者调用：

```java
queue.take();
```

但是：

```text
队列为空
```

那么消费者不会得到：

```java
null
```

而是：

```text
消费者线程阻塞
```

等待生产者放入元素。

例如：

```text
队列：

[]
```

消费者：

```java
queue.take();
```

进入等待状态。

之后生产者：

```java
queue.put("A");
```

队列中出现元素。

消费者被唤醒：

```java
take() → A
```

------

# 五、BlockingQueue 最核心的一句话

BlockingQueue 可以理解成：

> **一个线程安全的、支持等待机制的 Queue。**

其中两个最重要的阻塞操作：

```java
put()
take()
```

分别表示：

```text
put()
队列满 → 等

take()
队列空 → 等
```

可以记：

```text
put：没地方放，我等

take：没东西拿，我等
```

------

# 六、BlockingQueue 的继承体系

BlockingQueue 是一个接口。

大致结构：

```text
Iterable
   ↑
Collection
   ↑
Queue
   ↑
BlockingQueue
```

源码定义：

```java
public interface BlockingQueue<E> extends Queue<E> {
    ...
}
```

所以：

```text
BlockingQueue
```

本身并不是一个具体的队列对象。

我们一般使用它的实现类。

常见实现：

```text
BlockingQueue
│
├── ArrayBlockingQueue
│
├── LinkedBlockingQueue
│
├── PriorityBlockingQueue
│
├── DelayQueue
│
├── SynchronousQueue
└── LinkedTransferQueue
```

其中你现在最需要掌握的是：

```text
ArrayBlockingQueue

LinkedBlockingQueue

SynchronousQueue
```

线程池里面经常会碰到这些。

------

# 七、BlockingQueue 四组核心 API

BlockingQueue 最重要的地方之一就是：

> 对同一种操作，Java 提供了四种不同处理方式。

可以分成：

```text
1. 抛异常

2. 返回特殊值

3. 一直阻塞

4. 超时阻塞
```

这是 BlockingQueue 必须背熟的表。

## 添加元素

| 行为           | 方法                   |
| -------------- | ---------------------- |
| 失败抛异常     | `add(e)`               |
| 失败返回 false | `offer(e)`             |
| 满了阻塞       | `put(e)`               |
| 最多等一段时间 | `offer(e, time, unit)` |

## 删除元素

| 行为           | 方法               |
| -------------- | ------------------ |
| 失败抛异常     | `remove()`         |
| 失败返回 null  | `poll()`           |
| 空了阻塞       | `take()`           |
| 最多等一段时间 | `poll(time, unit)` |

## 查看队头元素

| 行为          | 方法        |
| ------------- | ----------- |
| 空时抛异常    | `element()` |
| 空时返回 null | `peek()`    |

这里没有：

```text
阻塞版查看
```

因为“查看”不会消费数据，一般没有必要设计成阻塞 API。

------

# 八、第一组：抛异常的方法

添加：

```java
add(E e)
```

取出：

```java
remove()
```

查看：

```java
element()
```

特点：

> 操作做不到，直接抛异常。

------

## 1. add()

例如：

```java
BlockingQueue<String> queue =
        new ArrayBlockingQueue<>(2);

queue.add("A");
queue.add("B");
queue.add("C");
```

前两个：

```text
A
B
```

可以正常加入。

第三次：

```java
queue.add("C");
```

因为队列已经满了。

会抛：

```text
IllegalStateException
```

所以：

```text
add
```

适合：

> 你认为“队列满了”属于程序异常情况的时候。

------

## 2. remove()

假设：

```text
[]
```

调用：

```java
queue.remove();
```

会抛：

```text
NoSuchElementException
```

------

## 3. element()

查看队头：

```java
queue.element();
```

如果队列为空：

```text
[]
```

同样：

```text
NoSuchElementException
```

------

# 九、第二组：返回特殊值

添加：

```java
offer(E e)
```

取出：

```java
poll()
```

查看：

```java
peek()
```

特点：

> 操作失败不抛异常，而是通过返回值告诉你。

------

## 1. offer()

```java
BlockingQueue<String> queue =
        new ArrayBlockingQueue<>(2);

System.out.println(queue.offer("A"));
System.out.println(queue.offer("B"));
System.out.println(queue.offer("C"));
```

结果：

```text
true
true
false
```

因为：

```text
A → 成功

B → 成功

C → 队列满了
```

所以第三次返回：

```java
false
```

------

## 2. poll()

```java
String value = queue.poll();
```

如果存在元素：

```text
[A][B]
```

返回：

```text
A
```

并且删除 A。

变成：

```text
[B]
```

如果队列为空：

```text
[]
```

则：

```java
poll()
```

返回：

```java
null
```

------

## 3. peek()

```java
queue.peek();
```

只是查看队头。

例如：

```text
[A][B][C]
```

调用：

```java
peek()
```

返回：

```text
A
```

但是队列仍然：

```text
[A][B][C]
```

如果为空：

```text
[]
```

返回：

```java
null
```

------

# 十、第三组：一直阻塞

这一组就是 BlockingQueue 的精髓：

```java
put(E e)

take()
```

------

# 十一、put() 详解

方法：

```java
void put(E e) throws InterruptedException
```

注意：

```java
throws InterruptedException
```

这说明：

> `put()` 的阻塞是可以被 interrupt 打断的。

例如：

```java
BlockingQueue<String> queue =
        new ArrayBlockingQueue<>(2);

queue.put("A");
queue.put("B");

queue.put("C");
```

执行到：

```java
queue.put("C");
```

因为队列：

```text
[A][B]
```

已经满了。

所以当前线程进入阻塞等待。

直到其他线程：

```java
queue.take();
```

例如取走 A：

```text
[B]
```

此时出现空位。

生产者才可以：

```text
put C
```

变成：

```text
[B][C]
```

------

# 十二、take() 详解

方法：

```java
E take() throws InterruptedException
```

如果队列里面有：

```text
[A][B]
```

调用：

```java
String value = queue.take();
```

得到：

```text
A
```

队列：

```text
[B]
```

但是如果：

```text
[]
```

调用：

```java
take()
```

线程会：

```text
阻塞等待
```

直到有生产者：

```java
put("A")
```

------

# 十三、put/take 与 interrupt

这个地方和你前面学过的：

```text
lockInterruptibly()

Condition.await()

Thread.sleep()

Object.wait()
```

非常类似。

这些阻塞操作很多都是：

```text
可中断等待
```

例如：

```java
Thread t = new Thread(() -> {

    try {
        queue.take();
    } catch (InterruptedException e) {
        System.out.println("等待被中断");
    }

});
```

假设队列一直为空：

```text
[]
```

线程卡在：

```java
queue.take();
```

这时候：

```java
t.interrupt();
```

线程会结束等待，并抛出：

```java
InterruptedException
```

------

# 十四、第四组：超时等待

还有两种非常实用的方法：

```java
offer(E e, long timeout, TimeUnit unit)

poll(long timeout, TimeUnit unit)
```

它们的思想是：

> 我愿意等，但不会一直等。

------

## 1. offer 超时版

例如：

```java
boolean success =
        queue.offer("A", 3, TimeUnit.SECONDS);
```

意思：

```text
尝试放入 A

如果有位置：
    立即放入

如果没位置：
    最多等待 3 秒

3 秒之内出现位置：
    放入 → true

3 秒之后还没有位置：
    false
```

------

## 2. poll 超时版

```java
String value =
        queue.poll(3, TimeUnit.SECONDS);
```

意思：

```text
如果有元素：
    立即返回

如果没元素：
    最多等待 3 秒

3 秒内出现元素：
    返回元素

3 秒后还是没有：
    返回 null
```

------

# 十五、四种策略总结

这张表最好记下来。

```text
                  插入          删除         查看

抛异常            add()        remove()     element()

特殊值            offer()      poll()       peek()

一直等待          put()        take()       -

超时等待          offer(...)   poll(...)    -
```

可以按照这一排记：

```text
add/remove/element
offer/poll/peek
put/take
offer(timeout)/poll(timeout)
```

------

# 十六、BlockingQueue 最经典的生产者消费者模型

BlockingQueue 最大价值之一：

> 极大简化生产者消费者模型。

以前如果自己实现：

```java
List<Object> queue
```

我们需要考虑：

```text
synchronized

wait

notifyAll

队列满判断

队列空判断

线程安全

虚假唤醒

中断
```

非常麻烦。

BlockingQueue 已经帮你做了。

------

# 十七、生产者消费者完整例子

```java
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ProducerConsumerDemo {

    private static final BlockingQueue<String> queue =
            new ArrayBlockingQueue<>(3);

    public static void main(String[] args) {

        Thread producer = new Thread(() -> {

            for (int i = 1; i <= 10; i++) {

                try {

                    String task = "任务-" + i;

                    queue.put(task);

                    System.out.println(
                            "生产者生产：" + task
                    );

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();

                    break;
                }
            }

        }, "Producer");

        Thread consumer = new Thread(() -> {

            while (!Thread.currentThread().isInterrupted()) {

                try {

                    String task = queue.take();

                    System.out.println(
                            "消费者消费：" + task
                    );

                    Thread.sleep(1000);

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();

                    break;
                }
            }

        }, "Consumer");

        producer.start();
        consumer.start();
    }
}
```

整个结构：

```text
Producer
   │
   │ put()
   ▼
┌───────────────┐
│ BlockingQueue │
│ 最大容量 = 3 │
└───────────────┘
   │
   │ take()
   ▼
Consumer
```

------

# 十八、生产速度大于消费速度时

假设：

生产者：

```text
每 100ms 生产一个
```

消费者：

```text
每 1000ms 消费一个
```

那么：

```text
生产 > 消费
```

队列会越来越满：

```text
[]

[A]

[A][B]

[A][B][C]
```

容量 3 已满。

生产者：

```java
queue.put("D");
```

此时：

```text
Producer 阻塞
```

消费者取走：

```text
A
```

队列：

```text
[B][C]
```

生产者继续：

```text
[B][C][D]
```

------

# 十九、消费速度大于生产速度时

假设：

生产者：

```text
每 1 秒生产一个
```

消费者：

```text
每 100ms 消费一个
```

很快队列：

```text
[]
```

消费者调用：

```java
take()
```

没有东西。

消费者阻塞。

直到：

```text
Producer → put("A")
```

消费者继续运行。

------

# 二十、为什么 BlockingQueue 特别适合生产者消费者？

因为它天然帮助我们完成：

```text
生产者太快 → 限速生产者

消费者太快 → 让消费者等待
```

相当于在两者之间放了一个：

```text
缓冲区
```

结构：

```text
生产者
   ↓
   ↓
┌────────────────┐
│    缓冲区       │
│ BlockingQueue  │
└────────────────┘
   ↓
   ↓
消费者
```

这样生产者和消费者不需要速度完全一致。

------

# 二十一、BlockingQueue 是线程安全的吗？

答案：

```text
是。
```

BlockingQueue 的实现类本身负责线程安全。

例如多个线程同时：

```java
queue.put(...)
```

或者：

```java
queue.take()
```

不需要你再额外写：

```java
synchronized(queue)
```

例如：

```java
queue.put("A");
```

不需要：

```java
synchronized (queue) {
    queue.put("A");
}
```

BlockingQueue 自己会保证：

```text
并发安全
```

------

# 二十二、BlockingQueue 和 Condition 的关系

这个地方非常重要，因为正好能和你刚刚学完的：

```text
ReentrantLock

Condition
```

串起来。

例如：

```java
ArrayBlockingQueue
```

内部实现思想就非常类似于：

```java
ReentrantLock lock

Condition notEmpty

Condition notFull
```

可以理解成：

```text
lock
│
├── notEmpty
│
└── notFull
```

其中：

```text
notEmpty
```

表达：

> 队列不为空这个等待条件。

```text
notFull
```

表达：

> 队列没有满这个等待条件。

------

# 二十三、ArrayBlockingQueue 内部模型

可以把它想象成：

```text
ArrayBlockingQueue

┌─────────────────────────────┐
│ Object[] items              │
│                             │
│ [A][B][C][ ][ ]             │
│                             │
│ count                       │
│ takeIndex                   │
│ putIndex                    │
│                             │
│ ReentrantLock lock          │
│                             │
│ Condition notEmpty          │
│ Condition notFull           │
└─────────────────────────────┘
```

这里最关键的就是：

```text
lock

notEmpty

notFull
```

------

# 二十四、put() 底层思想

假设：

```java
queue.put("A");
```

内部逻辑可以粗略理解成：

```java
lock.lockInterruptibly();

try {

    while (队列满了) {
        notFull.await();
    }

    插入元素;

    notEmpty.signal();

} finally {

    lock.unlock();
}
```

注意：

这不是让你背源码，而是理解逻辑。

------

## 为什么用 while？

你前面学习 Condition 的时候应该已经见过：

```java
while (条件不满足) {
    condition.await();
}
```

这里同样如此。

因为线程被唤醒：

```text
不代表条件一定已经满足
```

所以醒来之后：

```text
重新检查条件
```

------

# 二十五、put 为什么等 notFull？

假设队列：

```text
[A][B][C]
```

已经满了。

生产者：

```java
put("D")
```

发现：

```text
full
```

于是：

```java
notFull.await();
```

也就是：

> 我要等待“队列不满”这个条件。

等待过程中：

```text
释放 lock
```

于是消费者可以拿到锁。

消费者：

```java
take();
```

取走 A。

队列：

```text
[B][C]
```

这时候：

```text
队列不满了
```

消费者：

```java
notFull.signal();
```

唤醒等待空间的生产者。

------

# 二十六、take() 底层思想

大概可以理解：

```java
lock.lockInterruptibly();

try {

    while (队列为空) {
        notEmpty.await();
    }

    E value = 删除队头元素;

    notFull.signal();

    return value;

} finally {

    lock.unlock();
}
```

------

# 二十七、take 为什么等待 notEmpty？

消费者：

```java
take();
```

发现：

```text
[]
```

没有数据。

于是：

```java
notEmpty.await();
```

含义：

> 我要等到“队列不为空”。

生产者：

```java
put("A")
```

加入成功：

```text
[A]
```

这时候：

```java
notEmpty.signal();
```

消费者被唤醒。

------

# 二十八、BlockingQueue 和你之前学的 Condition 完整对应关系

你之前学习的是：

```java
Lock lock = new ReentrantLock();

Condition hasFood =
        lock.newCondition();

Condition hasSpace =
        lock.newCondition();
```

然后：

生产者：

```java
while (没有空间) {
    hasSpace.await();
}

生产();

hasFood.signal();
```

消费者：

```java
while (没有食物) {
    hasFood.await();
}

消费();

hasSpace.signal();
```

BlockingQueue 就相当于：

> Java 已经把这一套生产者消费者逻辑封装起来了。

你直接：

```java
queue.put();
```

和：

```java
queue.take();
```

就可以了。

所以可以理解成：

```text
BlockingQueue
=
Queue
+
Lock
+
Condition
+
生产者消费者控制逻辑
```

这句话非常重要。

------

# 二十九、BlockingQueue 和 wait/notify 的关系

更早的时候：

```text
synchronized
+
wait
+
notifyAll
```

可以实现生产者消费者。

之后：

```text
ReentrantLock
+
Condition
```

也可以实现。

而 BlockingQueue：

```text
直接把这些东西封装起来了。
```

发展关系可以理解：

```text
第一层：

synchronized
wait
notify

        ↓

第二层：

ReentrantLock
Condition

        ↓

第三层：

BlockingQueue
```

并不是说后者替代前者。

而是：

```text
BlockingQueue 是针对“队列协作场景”的更高层封装。
```

------

# 三十、BlockingQueue 中能不能放 null？

答案：

```text
不能。
```

例如：

```java
queue.put(null);
```

会抛：

```text
NullPointerException
```

为什么？

因为很多 Queue API 用：

```java
null
```

表示：

```text
队列为空
```

比如：

```java
queue.poll()
```

队列为空返回：

```java
null
```

如果队列本身允许存：

```java
null
```

就无法区分：

```text
到底是取到了 null

还是队列为空？
```

所以 BlockingQueue 禁止：

```text
null
```

------

# 三十一、ArrayBlockingQueue

完整类名：

```java
java.util.concurrent.ArrayBlockingQueue
```

特点：

```text
基于数组

有界队列

FIFO

容量创建之后不能改变
```

创建：

```java
BlockingQueue<String> queue =
        new ArrayBlockingQueue<>(10);
```

意味着：

```text
最大只能放 10 个元素
```

------

# 三十二、ArrayBlockingQueue 内部结构

可以想象：

```text
Object[] items

┌────┬────┬────┬────┬────┐
│ A  │ B  │ C  │    │    │
└────┴────┴────┴────┴────┘
```

并且通过索引记录：

```text
putIndex

takeIndex

count
```

例如：

```text
takeIndex
   ↓
[A][B][C][ ][ ]
         ↑
      putIndex
```

它本质上类似：

```text
循环数组 / 环形队列
```

------

# 三十三、为什么要使用循环数组？

假设容量：

```text
5
```

开始：

```text
[ ][ ][ ][ ][ ]
```

加入：

```text
A B C D E
```

变成：

```text
[A][B][C][D][E]
```

取走：

```text
A
B
```

逻辑上变成：

```text
[ ][ ][C][D][E]
```

这时候不会真的把：

```text
C D E
```

全部往前搬。

而是通过：

```text
takeIndex

putIndex
```

循环使用数组空间。

所以类似：

```text
环形缓冲区
Ring Buffer
```

------

# 三十四、ArrayBlockingQueue 的公平性

ArrayBlockingQueue 还有一个构造：

```java
new ArrayBlockingQueue<>(10, true);
```

第二个参数：

```text
fair
```

表示锁是否公平。

例如：

```java
new ArrayBlockingQueue<>(10, true);
```

表示：

```text
公平锁模式
```

而：

```java
new ArrayBlockingQueue<>(10);
```

默认：

```text
false
```

也就是：

```text
非公平
```

------

# 三十五、公平是什么意思？

例如多个生产者都因为队列满而等待：

```text
Producer-A
Producer-B
Producer-C
```

公平模式更倾向：

```text
先等的先获得机会
```

非公平模式则可能：

```text
刚来的线程也抢到锁
```

和你前面学：

```text
ReentrantLock(true)

ReentrantLock(false)
```

基本是同一个思想。

------

# 三十六、为什么默认非公平？

因为：

```text
公平
```

虽然听起来很好，但是通常意味着：

```text
更多线程调度

更多上下文切换

吞吐量可能下降
```

所以大多数并发组件默认偏向：

```text
吞吐量
```

而不是绝对公平。

------

# 三十七、LinkedBlockingQueue

完整类名：

```java
java.util.concurrent.LinkedBlockingQueue
```

特点：

```text
链表结构

FIFO

可以指定容量

不指定容量时容量非常大
```

例如：

```java
BlockingQueue<String> queue =
        new LinkedBlockingQueue<>(100);
```

最大：

```text
100
```

------

# 三十八、不写容量会怎样？

可以：

```java
new LinkedBlockingQueue<>();
```

这时候最大容量大致相当于：

```java
Integer.MAX_VALUE
```

也就是：

```text
2147483647
```

所以从实际使用角度看：

```text
近似无界
```

注意：

并不是真的无限大。

只是：

```text
大得几乎不容易因为容量本身而满
```

------

# 三十九、为什么无界队列可能危险？

假设：

```text
生产速度 >>> 消费速度
```

如果是：

```java
new LinkedBlockingQueue<>();
```

生产者可能不停添加：

```text
10 个

100 个

10000 个

100 万个

1000 万个
```

队列不断积压。

最终可能导致：

```text
内存占用越来越大

↓

GC 压力越来越大

↓

OutOfMemoryError
```

所以实际项目中：

> 通常应该非常慎重地使用无界队列。

很多情况下更推荐：

```java
new LinkedBlockingQueue<>(1000);
```

明确设置容量。

------

# 四十、ArrayBlockingQueue 和 LinkedBlockingQueue 区别

## ArrayBlockingQueue

```text
数组
固定容量
一个数组提前创建
有界
```

## LinkedBlockingQueue

```text
链表节点
容量可指定
默认接近无界
```

简单对比：

| 特点             | ArrayBlockingQueue | LinkedBlockingQueue |
| ---------------- | ------------------ | ------------------- |
| 数据结构         | 数组               | 链表                |
| 是否有界         | 必须有界           | 可有界/近似无界     |
| 是否预先分配容量 | 是                 | 否                  |
| FIFO             | 是                 | 是                  |
| 常见用途         | 固定缓冲区         | 任务队列            |

------

# 四十一、LinkedBlockingQueue 内部锁设计

这个地方属于稍微深入一点。

ArrayBlockingQueue 的核心通常可以理解成：

```text
一把 lock
```

同时控制：

```text
put

take
```

而 LinkedBlockingQueue 的设计更加细一些。

它内部有类似：

```text
takeLock

putLock
```

也就是：

```text
取元素一把锁

放元素另一把锁
```

这样在某些情况下：

```text
生产者
```

和：

```text
消费者
```

可以同时操作。

因此理论上可以提高：

```text
并发吞吐量
```

------

# 四十二、为什么链表可以这样设计？

例如：

```text
HEAD → A → B → C → TAIL
```

消费者主要操作：

```text
头部
```

生产者主要操作：

```text
尾部
```

所以：

```text
take
```

和：

```text
put
```

操作区域天然相对分离。

于是可以设计：

```text
putLock

takeLock
```

减少竞争。

------

# 四十三、PriorityBlockingQueue

完整名称：

```java
PriorityBlockingQueue
```

它和普通 FIFO BlockingQueue 不同。

它是：

```text
优先级阻塞队列
```

也就是说：

> 元素不是严格按照放入顺序取出，而是按照优先级排序。

例如：

```text
加入：

5
1
3
```

取出可能：

```text
1
3
5
```

------

# 四十四、PriorityBlockingQueue 示例

```java
BlockingQueue<Integer> queue =
        new PriorityBlockingQueue<>();

queue.put(5);
queue.put(1);
queue.put(3);

System.out.println(queue.take());
System.out.println(queue.take());
System.out.println(queue.take());
```

结果：

```text
1
3
5
```

因为 Integer 默认：

```text
自然排序
```

------

# 四十五、自定义优先级

例如任务：

```java
class Task {

    int priority;

    String name;

}
```

可以通过：

```java
Comparator
```

决定优先级。

例如：

```java
PriorityBlockingQueue<Task> queue =
        new PriorityBlockingQueue<>(
                11,
                Comparator.comparingInt(
                        task -> task.priority
                )
        );
```

------

# 四十六、PriorityBlockingQueue 一个重要特点

PriorityBlockingQueue：

```text
实际上是无界队列
```

所以：

```java
put()
```

通常不会因为：

```text
队列满
```

而阻塞。

它的阻塞能力主要体现在：

```java
take()
```

当：

```text
队列为空
```

消费者等待。

这一点要注意。

------

# 四十七、DelayQueue

```java
DelayQueue
```

叫：

```text
延迟队列
```

它的特殊之处：

> 元素必须等到指定延迟时间到了之后，才能被取出。

例如：

```text
任务 A → 5 秒后执行

任务 B → 10 秒后执行

任务 C → 1 秒后执行
```

队列会根据：

```text
到期时间
```

安排元素。

------

# 四十八、DelayQueue 的典型场景

例如：

```text
订单 30 分钟未支付自动取消

验证码 5 分钟过期

缓存超时

延迟任务

定时重试
```

都可以联想到：

```text
DelayQueue
```

当然实际大型分布式系统里可能会使用：

```text
RocketMQ 延迟消息

RabbitMQ 延迟队列

Redis

定时任务系统
```

但是 DelayQueue 是理解：

```text
Java 本地延迟任务
```

非常好的工具。

------

# 四十九、DelayQueue 元素要求

DelayQueue 中的元素需要实现：

```java
Delayed
```

例如：

```java
class DelayTask implements Delayed {
    ...
}
```

`Delayed` 又继承：

```java
Comparable<Delayed>
```

所以元素必须能够比较：

```text
谁先到期。
```

------

# 五十、SynchronousQueue

这是 BlockingQueue 中非常特殊的一个。

名字：

```java
SynchronousQueue
```

很多初学者第一次看都会理解错。

它可以理解成：

> **一个没有存储空间的队列。**

注意：

```text
容量 = 0
```

不是：

```text
容量 = 1
```

而是真的：

```text
不存东西
```

------

# 五十一、SynchronousQueue 怎么工作？

普通 BlockingQueue：

```text
Producer

   ↓ put

[ Queue ]

   ↓ take

Consumer
```

SynchronousQueue：

```text
Producer
   │
   │ 直接交接
   ▼
Consumer
```

中间没有：

```text
真正的缓冲区
```

所以它更加类似：

```text
线程之间直接交接数据
```

------

# 五十二、SynchronousQueue 示例

```java
BlockingQueue<String> queue =
        new SynchronousQueue<>();
```

生产者：

```java
queue.put("A");
```

如果此时没有消费者：

```java
queue.take();
```

那么生产者：

```text
会一直阻塞
```

因为：

```text
SynchronousQueue 没地方存 A
```

必须等消费者亲自来接。

------

# 五十三、SynchronousQueue 图解

普通队列：

```text
Producer

    ↓

[A][B][C]

    ↓

Consumer
```

SynchronousQueue：

```text
Producer
    │
    │ A
    ▼
Consumer
```

相当于：

```text
面对面交货
```

没有仓库。

------

# 五十四、为什么线程池会用 SynchronousQueue？

这个非常重要。

Java：

```java
Executors.newCachedThreadPool()
```

底层就使用：

```java
SynchronousQueue
```

为什么？

因为 CachedThreadPool 的设计思想是：

> 来一个任务，如果有空闲线程就直接交给它；没有空闲线程就创建线程。

它不希望：

```text
任务先大量堆积在队列里。
```

所以使用：

```text
SynchronousQueue
```

直接：

```text
任务 ↔ 工作线程
```

交接。

------

# 五十五、LinkedTransferQueue

这是一个更高级的队列：

```java
LinkedTransferQueue
```

它实现：

```text
BlockingQueue

TransferQueue
```

支持：

```java
transfer()
```

这个操作的意思大致是：

> 不仅把元素放进去，而且可以等待消费者真正把这个元素取走。

例如：

```java
queue.transfer("A");
```

如果没有消费者：

```text
生产者可能等待
```

直到：

```text
某个消费者真正接收 A
```

它结合了一些：

```text
LinkedBlockingQueue
+
SynchronousQueue
```

的思想。

这个阶段知道即可。

------

# 五十六、BlockingDeque

除了：

```java
BlockingQueue
```

Java 还有：

```java
BlockingDeque
```

也就是：

```text
阻塞双端队列
```

Deque：

```text
Double Ended Queue
```

可以：

```text
头部插入

尾部插入

头部删除

尾部删除
```

实现类：

```java
LinkedBlockingDeque
```

例如：

```java
BlockingDeque<String> deque =
        new LinkedBlockingDeque<>();
```

支持：

```java
putFirst()

putLast()

takeFirst()

takeLast()
```

但是 BlockingQueue 初学阶段：

```text
知道即可。
```

------

# 五十七、BlockingQueue 在线程池中的作用

这个非常非常重要。

后面学：

```java
ThreadPoolExecutor
```

一定会看到 BlockingQueue。

构造方法：

```java
new ThreadPoolExecutor(
        corePoolSize,
        maximumPoolSize,
        keepAliveTime,
        unit,
        workQueue
);
```

其中：

```java
workQueue
```

类型就是：

```java
BlockingQueue<Runnable>
```

也就是说：

```text
线程池中的任务队列
```

实际上就是：

```text
BlockingQueue
```

------

# 五十八、线程池里的 BlockingQueue

线程池大致结构：

```text
                     ┌─────────────┐
提交任务 ───────────→ │ 核心线程     │
                     └─────────────┘
                            │
                   核心线程忙不过来
                            ↓
                     ┌─────────────┐
                     │ BlockingQueue│
                     │   任务队列   │
                     └─────────────┘
                            │
                            ↓
                         工作线程
```

例如：

```java
BlockingQueue<Runnable> workQueue =
        new ArrayBlockingQueue<>(100);
```

线程池处理不了的任务：

```text
暂时放到队列里等待。
```

------

# 五十九、线程池为什么不直接无限创建线程？

假设同时来：

```text
10000 个请求
```

如果：

```text
来一个任务创建一个线程
```

可能瞬间：

```text
10000 个线程
```

导致：

```text
巨大内存占用

上下文切换爆炸

CPU 压力巨大

系统崩溃
```

所以：

```text
线程数量有限

多余任务进入 BlockingQueue
```

这实际上就是：

```text
削峰
```

------

# 六十、BlockingQueue 的削峰作用

例如系统：

```text
每秒只能处理 100 个任务
```

突然来了：

```text
500 个
```

如果没有缓冲：

```text
500 个瞬间全部压到业务代码
```

可能把系统打爆。

有 BlockingQueue：

```text
来了 500

↓     

先处理 100

↓     

剩余进入队列

↓     

慢慢消费
```

这就是：

```text
缓冲

削峰

解耦
```

------

# 六十一、BlockingQueue 的三个核心价值

可以总结为：

```text
1. 线程安全

2. 线程阻塞协作

3. 生产者消费者解耦
```

------

# 六十二、什么叫解耦？

假设没有队列：

```text
Producer
     ↓
Consumer
```

生产者可能必须：

```text
直接调用消费者
```

双方绑定非常紧。

加入 BlockingQueue：

```text
Producer

     ↓

BlockingQueue

     ↓

Consumer
```

生产者只负责：

```text
往队列放
```

消费者只负责：

```text
从队列拿
```

双方甚至：

```text
不需要知道对方是谁。
```

这就叫：

```text
解耦。
```

------

# 六十三、BlockingQueue 和消息队列 MQ 很像吗？

非常像。

例如：

```text
BlockingQueue

生产者
  ↓
Queue
  ↓
消费者
```

MQ：

```text
Producer
   ↓
RabbitMQ / Kafka / RocketMQ
   ↓
Consumer
```

思想高度相似：

```text
生产

↓

缓冲

↓

消费
```

但是 BlockingQueue 是：

```text
JVM 进程内部
```

而 MQ 通常是：

```text
独立的中间件

跨进程

跨服务器
```

------

# 六十四、BlockingQueue vs MQ

BlockingQueue：

```text
Java JVM 内部

内存数据

进程挂了数据通常没了

非常快

使用简单
```

MQ：

```text
独立服务器

支持持久化

支持跨服务

支持分布式

功能更多

成本更高
```

所以：

```text
BlockingQueue
```

可以理解成：

> Java 进程内部的小型生产者消费者消息通道。

------

# 六十五、BlockingQueue 与线程状态

例如线程执行：

```java
queue.take();
```

队列为空。

线程不会一直：

```text
RUNNABLE
```

烧 CPU。

而是会进入：

```text
等待状态
```

底层最终会使用 JVM 的线程等待机制。

等到：

```text
有元素
```

再被唤醒。

所以 BlockingQueue 的价值之一就是：

```text
避免忙等待。
```

------

# 六十六、put/take 是否释放锁？

如果 BlockingQueue 内部：

```text
await()
```

那么：

```text
await 会释放对应的 ReentrantLock。
```

例如：

```text
Producer
```

发现队列满：

```text
拿锁
↓
发现 full
↓
notFull.await()
↓
释放锁
↓
Producer 等待
```

于是消费者：

```text
可以拿到这个锁
```

然后：

```text
take()
```

如果不释放锁：

```text
消费者永远拿不到锁

↓

无法 take

↓

生产者永远等

↓

死锁
```

所以这一点与：

```java
Object.wait()
```

非常类似：

```text
等待的时候释放关联锁。
```

------

# 六十七、BlockingQueue 的 happens-before

这是你学过 happens-before 后应该知道的一点。

Java 并发包对 BlockingQueue 提供了内存可见性保证。

可以简单理解：

假设线程 A：

```java
task.name = "AAA";

queue.put(task);
```

线程 B：

```java
Task task = queue.take();

System.out.println(task.name);
```

通过 BlockingQueue 完成任务交接时：

```text
put 之前线程 A 对数据的修改
```

能够对：

```text
take 之后线程 B 的读取
```

正确可见。

也就是说：

```text
BlockingQueue
```

不仅解决：

```text
线程等待
```

也解决了很多：

```text
内存可见性
```

问题。

------

# 六十八、BlockingQueue 为什么不需要额外 volatile？

比如：

```java
class Task {
    String name;
}
```

生产者：

```java
Task task = new Task();

task.name = "hello";

queue.put(task);
```

消费者：

```java
Task task = queue.take();

System.out.println(task.name);
```

一般不需要为了这次安全发布特意：

```java
volatile String name;
```

因为 BlockingQueue 的并发操作本身已经建立了：

```text
happens-before
```

关系。

当然：

> 如果拿到对象之后多个线程继续同时修改这个对象，那又是另一个线程安全问题。

BlockingQueue 只保证：

```text
安全交接
```

并不会让：

```text
Task 本身自动变成线程安全类。
```

------

# 六十九、BlockingQueue 是不是所有方法都会阻塞？

不是。

这是非常常见的误区。

BlockingQueue：

```text
有阻塞能力
```

并不代表：

```text
所有方法都会阻塞。
```

例如：

```java
add()
offer()
poll()
peek()
```

都不会无限阻塞。

真正典型的阻塞方法：

```java
put()

take()
```

超时阻塞：

```java
offer(timeout)

poll(timeout)
```

------

# 七十、offer 和 put 怎么选？

## put()

```java
queue.put(task);
```

意思：

> 这个任务必须放进去，没有位置我就一直等。

适用于：

```text
不能丢数据

愿意进行背压
```

------

## offer()

```java
boolean success = queue.offer(task);
```

意思：

> 能放就放，不能放就算了。

适合：

```text
允许失败

自己处理拒绝逻辑
```

例如：

```java
if (!queue.offer(task)) {
    System.out.println("队列已满");
}
```

------

## offer(timeout)

```java
boolean success =
        queue.offer(task, 3, TimeUnit.SECONDS);
```

属于折中：

```text
我愿意等等

但是不会无限等。
```

实际项目里这种方式往往非常实用。

------

# 七十一、poll 和 take 怎么选？

## take()

```java
Task task = queue.take();
```

适合：

```text
消费者就是专门消费任务的

没任务时可以一直等
```

例如工作线程。

------

## poll()

```java
Task task = queue.poll();
```

适合：

```text
现在有就拿

没有就做别的事情
```

------

## poll(timeout)

```java
Task task =
        queue.poll(5, TimeUnit.SECONDS);
```

适合：

```text
最多等一会

没有任务就执行其他逻辑
```

------

# 七十二、生产者消费者结束问题

初学时经常写：

```java
while (true) {

    String task = queue.take();

    process(task);
}
```

但是有个问题：

> 消费者什么时候结束？

如果生产者已经彻底不生产了：

```text
消费者仍然可能永远阻塞在 take()
```

------

# 七十三、解决方案一：interrupt

例如：

```java
consumer.interrupt();
```

消费者：

```java
try {

    while (true) {

        String task = queue.take();

        process(task);
    }

} catch (InterruptedException e) {

    Thread.currentThread().interrupt();
}
```

通过：

```text
中断
```

结束消费者。

------

# 七十四、解决方案二：毒丸 Poison Pill

还有一种经典设计：

```text
Poison Pill
```

中文：

```text
毒丸消息
```

例如：

```java
private static final String STOP = "__STOP__";
```

生产者结束时：

```java
queue.put(STOP);
```

消费者：

```java
while (true) {

    String task = queue.take();

    if (STOP.equals(task)) {
        break;
    }

    process(task);
}
```

结构：

```text
任务1
任务2
任务3
STOP
```

消费者拿到：

```text
STOP
```

就知道：

```text
没有后续任务了。
```

------

# 七十五、多个消费者时毒丸要注意

如果有：

```text
3 个消费者
```

通常需要：

```text
3 个 STOP
```

否则：

```text
Consumer-A → STOP → 结束

Consumer-B → 还在等

Consumer-C → 还在等
```

所以：

```text
消费者几个

通常就准备几个终止信号。
```

------

# 七十六、BlockingQueue 的容量设计为什么重要？

假设：

```java
new ArrayBlockingQueue<>(100);
```

这个：

```text
100
```

并不是随便写的。

容量太小：

```text
生产者频繁阻塞

吞吐量可能下降
```

容量太大：

```text
大量任务堆积

占内存

延迟越来越高
```

所以队列容量实际上属于：

```text
系统容量设计
```

的一部分。

------

# 七十七、队列大不一定好

很多人初学线程池认为：

```text
队列越大越好
```

错误。

例如：

```text
消费者处理能力：

100 task/s
```

突然堆积：

```text
1000000 个任务
```

即使队列能装下：

```text
任务也要等待：

1000000 / 100
= 10000 秒
```

也就是接近：

```text
2.8 小时
```

所以：

```text
队列没有拒绝任务
```

不代表系统健康。

可能只是：

```text
问题被藏在队列里。
```

------

# 七十八、什么叫背压 Backpressure？

BlockingQueue 中一个很重要的并发思想：

```text
Backpressure
```

中文：

```text
背压
```

意思：

> 下游处理不过来的时候，反过来限制上游生产速度。

例如：

```text
生产者：

1000/s

↓

队列容量 100

↓

消费者：

100/s
```

很快队列满。

这时候：

```java
put()
```

让生产者阻塞。

于是：

```text
生产速度被迫下降
```

这就是一种：

```text
背压机制。
```

------

# 七十九、为什么有界 BlockingQueue 通常更安全？

假设：

```text
队列容量 1000
```

那么无论生产者多快：

```text
最多积压 1000 个。
```

队列满后：

```text
阻塞

失败

超时

拒绝
```

由你决定。

这相当于给系统设了一条：

```text
安全边界。
```

所以生产环境通常强调：

```text
有界资源。
```

包括：

```text
线程池线程数有界

BlockingQueue 容量有界

数据库连接池有界
```

------

# 八十、BlockingQueue 的 bulk 方法

还有一个比较有用的方法：

```java
drainTo(Collection<? super E> c)
```

意思：

> 一次性把当前队列中的多个元素转移到 Collection。

例如：

```java
List<String> list = new ArrayList<>();

queue.drainTo(list);
```

假设：

```text
queue：

[A][B][C]
```

执行后：

```text
list：

[A][B][C]

queue：

[]
```

------

# 八十一、drainTo 有什么用？

例如：

```text
批量处理任务
```

消费者不想：

```text
一个一个处理
```

而是：

```text
一次取一批
```

可以：

```java
List<Task> batch = new ArrayList<>();

queue.drainTo(batch, 100);
```

表示：

```text
最多取 100 个
```

然后：

```java
batchProcess(batch);
```

例如：

```text
批量写数据库

批量发 MQ

批量提交日志

批量网络请求
```

都可能用到类似思想。

------

# 八十二、remainingCapacity()

还有：

```java
queue.remainingCapacity();
```

表示：

```text
还剩多少容量。
```

例如：

```java
BlockingQueue<String> queue =
        new ArrayBlockingQueue<>(10);

queue.put("A");
queue.put("B");
```

那么：

```java
queue.size()
```

为：

```text
2
```

而：

```java
queue.remainingCapacity()
```

为：

```text
8
```

------

# 八十三、size() 能不能用来控制并发？

比如有人写：

```java
if (queue.size() < 10) {
    queue.add(task);
}
```

这种逻辑一般存在并发问题。

因为：

```text
检查 size
```

和：

```text
add
```

不是一个原子整体。

例如：

```text
Thread-A：

size = 9

Thread-B：

size = 9
```

两个线程都认为：

```text
还能放
```

然后同时执行。

所以：

> 不要自己通过 `size()` 做 BlockingQueue 的并发控制。

应该直接使用：

```java
offer()

put()
```

这些已经保证线程安全的 API。

------

# 八十四、BlockingQueue 的复合操作问题

虽然 BlockingQueue 本身线程安全：

```java
queue.offer()

queue.poll()

queue.put()

queue.take()
```

都是安全的。

但是：

```text
多个安全方法组合起来
```

不一定自动成为：

```text
原子操作。
```

例如：

```java
if (!queue.contains(task)) {
    queue.put(task);
}
```

虽然：

```text
contains
```

安全。

```text
put
```

也安全。

但整体：

```text
contains + put
```

不是原子的。

两个线程可能：

```text
同时 contains → false

然后同时 put
```

导致重复。

这个思想和：

```text
ConcurrentHashMap
```

一样。

> 线程安全类的单个方法安全，不意味着你随便组合多个方法就是原子的。

------

# 八十五、BlockingQueue 和 ConcurrentLinkedQueue 区别

还有一个常见队列：

```java
ConcurrentLinkedQueue
```

它也是：

```text
线程安全 Queue
```

但它不是：

```text
BlockingQueue
```

区别：

```text
ConcurrentLinkedQueue

线程安全
非阻塞队列
```

而：

```text
BlockingQueue

线程安全
支持阻塞等待
```

例如 ConcurrentLinkedQueue：

```java
queue.poll();
```

没有数据：

```text
直接 null
```

不会：

```text
等待生产者。
```

------

# 八十六、什么时候用 ConcurrentLinkedQueue？

如果业务模式是：

```text
有就处理

没有就算了
```

可能使用：

```text
ConcurrentLinkedQueue
```

而如果：

```text
消费者应该等任务到来
```

更适合：

```text
BlockingQueue
```

------

# 八十七、BlockingQueue 是否一定 FIFO？

不一定。

接口本身：

```text
不保证所有实现都是 FIFO。
```

例如：

```text
ArrayBlockingQueue
LinkedBlockingQueue
```

一般：

```text
FIFO
```

但是：

```text
PriorityBlockingQueue
```

按照：

```text
优先级
```

而：

```text
DelayQueue
```

按照：

```text
延迟到期时间
```

所以要看：

```text
具体实现。
```

------

# 八十八、BlockingQueue 常见实现完整总结

## ArrayBlockingQueue

```text
数组

有界

FIFO

容量固定

可选公平模式
```

适合：

```text
固定大小缓冲区
```

------

## LinkedBlockingQueue

```text
链表

FIFO

可指定容量

默认近似无界
```

适合：

```text
普通生产者消费者

线程池任务队列
```

------

## PriorityBlockingQueue

```text
优先级

无界

不是 FIFO
```

适合：

```text
任务存在优先级
```

------

## DelayQueue

```text
延迟队列

元素到期后才能取
```

适合：

```text
延迟任务
```

------

## SynchronousQueue

```text
容量 0

不存储元素

生产者消费者直接交接
```

适合：

```text
直接任务交接

CachedThreadPool
```

------

## LinkedTransferQueue

```text
链表

支持 transfer

生产者可以等待消费者真正接收
```

属于：

```text
更高级并发队列。
```

------

# 八十九、一个非常重要的思维模型

以后看到 BlockingQueue，不要只想到：

```text
一个装东西的集合。
```

你应该脑子里出现：

```text
                  put
Producer ───────────────────┐
                           ↓
                 ┌─────────────────┐
                 │ BlockingQueue   │
                 │                 │
                 │ [A][B][C]       │
                 │                 │
                 └─────────────────┘
                           ↓
Consumer ───────────────────┘
                  take
```

并且马上想到两个条件：

```text
队列满
    ↓
生产者等

队列空
    ↓
消费者等
```

------

# 九十、把 BlockingQueue 和你已经学过的知识串起来

现在整个知识链可以连接成：

```text
线程
│
├── synchronized
│      │
│      ├── wait
│      ├── notify
│      └── notifyAll
│
├── ReentrantLock
│      │
│      └── Condition
│              │
│              ├── await
│              ├── signal
│              └── signalAll
│
└── BlockingQueue
        │
        ├── put
        ├── take
        ├── offer
        └── poll
```

其中：

```text
BlockingQueue
```

属于更高层的：

```text
线程协作工具。
```

------

# 九十一、三个层次的生产者消费者实现

## 第一层：wait/notify

```java
synchronized (lock) {

    while (queue.isEmpty()) {
        lock.wait();
    }

}
```

你需要：

```text
自己管理队列

自己加锁

自己等待

自己唤醒
```

------

## 第二层：ReentrantLock + Condition

```java
lock.lock();

try {

    while (queue.isEmpty()) {
        notEmpty.await();
    }

} finally {

    lock.unlock();
}
```

相比：

```text
wait/notify
```

更灵活。

可以拥有：

```text
多个条件队列。
```

------

## 第三层：BlockingQueue

直接：

```java
queue.take();
```

Java 已经帮你：

```text
加锁

判断

await

signal

维护队列
```

全部封装好了。

------

# 九十二、为什么学了 BlockingQueue 还要学 Condition？

可能你会想：

```text
既然 BlockingQueue 都封装好了，

那为什么还学 Condition？
```

因为：

```text
BlockingQueue
```

只能解决：

```text
队列型生产者消费者问题。
```

但是实际业务可能是：

```text
余额足够才能付款

库存够才能下单

线程 A 等状态变为 READY

线程 B 等连接建立

线程 C 等任务完成
```

这些不一定是：

```text
Queue
```

问题。

这时候：

```text
Condition
```

更加通用。

所以：

```text
Condition
=
底层一些的线程协作工具

BlockingQueue
=
针对队列场景的高级封装
```

------

# 九十三、为什么 BlockingQueue 里需要两个 Condition？

假设只有一个 Condition：

```java
Condition condition;
```

那么：

生产者等待：

```text
队列不满
```

消费者等待：

```text
队列不空
```

所有线程都进同一个等待区。

例如：

```text
生产者 P1
生产者 P2
消费者 C1
消费者 C2
```

全部混在一起：

```text
Condition Queue
```

唤醒时：

```text
可能唤醒错误类型线程。
```

------

# 九十四、两个 Condition 的优势

所以可以：

```text
notFull

专门放：
等待空间的生产者
```

和：

```text
notEmpty

专门放：
等待数据的消费者
```

结构：

```text
                ReentrantLock
                     │
          ┌──────────┴──────────┐
          ↓                     ↓
     notEmpty                notFull
          │                     │
    Consumer 等待            Producer 等待
```

这样：

生产者添加元素之后：

```java
notEmpty.signal();
```

只唤醒：

```text
消费者
```

消费者删除元素之后：

```java
notFull.signal();
```

只唤醒：

```text
生产者
```

这正好体现了：

> `Condition` 可以给一把 Lock 创建多个不同意义的条件队列。

------

# 九十五、BlockingQueue 内部等待线程去哪了？

假设消费者：

```java
queue.take();
```

队列为空。

如果底层采用 Condition 思想，那么：

```text
Consumer
```

会进入：

```text
notEmpty 条件队列
```

类似：

```text
notEmpty：

Consumer-A
   ↓
Consumer-B
   ↓
Consumer-C
```

当生产者：

```java
put()
```

之后：

```java
notEmpty.signal();
```

某个消费者：

```text
从 Condition Queue
```

转移到：

```text
Lock 的同步竞争队列
```

注意：

> 被 signal 并不等于立刻执行。

它还需要：

```text
重新获得 lock
```

这一点和你前面学习 Condition 完全一致。

------

# 九十六、put/take 完整状态变化

例如：

```text
队列满
```

生产者调用：

```java
put(D)
```

过程：

```text
Producer

拿 lock

↓

发现队列满

↓

notFull.await()

↓

释放 lock

↓

进入 notFull 条件队列

↓

等待
```

消费者：

```text
take()
```

过程：

```text
Consumer

拿 lock

↓

取走 A

↓

队列出现空位

↓

notFull.signal()

↓

Producer 被唤醒

↓

Producer 进入锁竞争

↓

Consumer unlock

↓

Producer 获得 lock

↓

再次检查队列是否满

↓

put(D)
```

完整结构：

```text
ArrayBlockingQueue

           ┌───────────────────┐
           │ ReentrantLock     │
           └───────────────────┘
               ↑           ↑
               │           │
      ┌────────┘           └────────┐
      │                             │
 notEmpty                       notFull
      │                             │
消费者等待                      生产者等待
```

------

# 九十七、BlockingQueue 的经典面试题

## 问：BlockingQueue 是什么？

可以回答：

> BlockingQueue 是 JUC 提供的线程安全阻塞队列接口，它在 Queue 基础上增加了阻塞操作。当队列为空时，take() 可以阻塞消费者；当队列满时，put() 可以阻塞生产者，因此非常适合实现生产者消费者模型以及线程池任务队列。

------

# 九十八、面试题：put 和 offer 的区别

回答：

```text
put：

队列满时阻塞

可被 interrupt 中断

返回 void
offer：

普通版队列满立即返回 false

超时版可以等待指定时间
```

------

# 九十九、面试题：take 和 poll 区别

```text
take：

队列空时一直阻塞

直到有元素或者被中断
poll：

普通版队列空返回 null

超时版等待指定时间后返回 null
```

------

# 一百、面试题：ArrayBlockingQueue 和 LinkedBlockingQueue 区别

核心回答：

```text
ArrayBlockingQueue：

数组实现
必须指定容量
固定有界
通常一把锁控制 put/take
LinkedBlockingQueue：

链表实现
容量可以指定
默认容量非常大
内部对 put/take 的并发控制更加细化
```

------

# 一百零一、面试题：SynchronousQueue 是什么？

答案：

> SynchronousQueue 是一种容量为 0 的阻塞队列，它本身不存储元素，每一次 put 都必须等待对应的 take，因此主要用于线程之间直接的数据交接。

经典使用：

```java
Executors.newCachedThreadPool()
```

------

# 一百零二、面试题：BlockingQueue 为什么不能存 null？

因为：

```text
poll()
```

等 API 使用：

```text
null
```

表示：

```text
没有元素。
```

如果允许：

```text
null
```

作为正常元素，就会产生歧义。

------

# 一百零三、面试题：BlockingQueue 怎么实现线程阻塞？

回答的时候不用上来背源码。

可以回答：

> 不同 BlockingQueue 实现细节不同，例如 ArrayBlockingQueue 内部基于 ReentrantLock 和两个 Condition：notEmpty 和 notFull。队列为空时消费者在 notEmpty 上 await，队列满时生产者在 notFull 上 await；添加或者删除元素后，再 signal 对应等待线程。

这个答案已经非常不错。

------

# 一百零四、面试题：BlockingQueue 有什么使用场景？

可以回答：

```text
1. 生产者消费者

2. 线程池任务队列

3. 异步任务缓冲

4. 日志缓冲

5. 请求削峰

6. 本地消息传递

7. 批量任务处理

8. 延迟任务
```

------

# 一百零五、常见错误一：无脑用无界 LinkedBlockingQueue

例如：

```java
new LinkedBlockingQueue<>();
```

如果生产速度长期超过消费速度：

```text
队列无限积压

↓

内存上涨

↓

Full GC

↓

OOM
```

所以生产代码更应该考虑：

```text
队列到底能积压多少任务？
```

------

# 一百零六、常见错误二：吞掉 InterruptedException

错误：

```java
try {

    queue.take();

} catch (InterruptedException e) {

}
```

这样：

```text
中断信号被直接吞掉。
```

更常见写法：

```java
try {

    queue.take();

} catch (InterruptedException e) {

    Thread.currentThread().interrupt();

    return;
}
```

也就是：

```text
恢复中断标记

然后结束当前任务。
```

------

# 一百零七、为什么 catch 后重新 interrupt？

因为：

```text
InterruptedException
```

抛出来的时候：

```text
中断状态通常会被清除。
```

所以：

```java
Thread.currentThread().interrupt();
```

相当于告诉上层：

> 我确实被要求中断过。

这与你之前学习：

```text
Thread.interrupted()
```

会清除中断标志的知识可以联系起来。

------

# 一百零八、常见错误三：生产者永久卡死

例如：

```java
queue.put(task);
```

如果：

```text
消费者已经挂了
```

而队列又满：

```text
生产者可能永远等待。
```

所以真实系统有时更倾向：

```java
queue.offer(
        task,
        3,
        TimeUnit.SECONDS
);
```

超过时间：

```text
失败

报警

降级

丢弃

重试
```

而不是：

```text
永久等。
```

------

# 一百零九、常见错误四：忽略消费异常

例如：

```java
while (true) {

    Task task = queue.take();

    process(task);
}
```

如果：

```java
process(task)
```

抛 RuntimeException：

```text
消费者线程可能直接死亡。
```

以后没人消费：

```text
队列越来越满。
```

所以实际消费者通常会保护：

```java
while (!Thread.currentThread().isInterrupted()) {

    try {

        Task task = queue.take();

        try {
            process(task);
        } catch (Exception e) {
            // 记录业务异常
        }

    } catch (InterruptedException e) {

        Thread.currentThread().interrupt();

        break;
    }
}
```

------

# 一百一十、BlockingQueue 和 Semaphore 的区别

以后你可能还会学：

```java
Semaphore
```

不要混。

BlockingQueue：

```text
核心是：

数据排队
+
线程等待
```

Semaphore：

```text
核心是：

控制同时允许多少线程进入。
```

比如：

```text
BlockingQueue：

任务在哪里？
```

而：

```text
Semaphore：

最多允许几个人同时进去？
```

是不同问题。

------

# 一百一十一、BlockingQueue 和 CountDownLatch 的区别

BlockingQueue：

```text
持续生产

持续消费
```

属于：

```text
长期线程协作。
```

CountDownLatch：

```text
等待一批任务完成
```

更像：

```text
一次性倒计时门闩。
```

------

# 一百一十二、BlockingQueue 和 Future 的区别

Future：

```text
一个任务

对应一个未来结果
```

BlockingQueue：

```text
很多任务

排队传递给消费者
```

例如：

```text
Future：

我等 A 的计算结果
BlockingQueue：

这里有 1000 个任务，工作线程慢慢拿。
```

------

# 一百一十三、BlockingQueue 和 CompletableFuture 的区别

CompletableFuture：

```text
更偏：

任务依赖

异步编排

结果链
```

例如：

```text
A 完成
↓
拿 A 的结果执行 B
↓
B 完成执行 C
```

BlockingQueue：

```text
更偏：

任务缓冲

生产消费者
```

两者可以：

```text
配合使用
```

而不是互相替代。

------

# 一百一十四、自己实现一个简化版 BlockingQueue

为了彻底理解，可以看一个教学版。

```java
public class MyBlockingQueue<E> {

    private final Queue<E> queue =
            new LinkedList<>();

    private final int capacity;

    private final ReentrantLock lock =
            new ReentrantLock();

    private final Condition notEmpty =
            lock.newCondition();

    private final Condition notFull =
            lock.newCondition();

    public MyBlockingQueue(int capacity) {

        this.capacity = capacity;
    }

    public void put(E element)
            throws InterruptedException {

        lock.lockInterruptibly();

        try {

            while (queue.size() == capacity) {

                notFull.await();
            }

            queue.offer(element);

            notEmpty.signal();

        } finally {

            lock.unlock();
        }
    }

    public E take()
            throws InterruptedException {

        lock.lockInterruptibly();

        try {

            while (queue.isEmpty()) {

                notEmpty.await();
            }

            E element = queue.poll();

            notFull.signal();

            return element;

        } finally {

            lock.unlock();
        }
    }
}
```

------

# 一百一十五、这个自定义 BlockingQueue 最值得看哪里？

其实就是四个东西：

```java
Queue<E> queue;

ReentrantLock lock;

Condition notEmpty;

Condition notFull;
```

对应：

```text
queue
    ↓
真正保存数据

lock
    ↓
保证队列操作线程安全

notEmpty
    ↓
消费者等待

notFull
    ↓
生产者等待
```

------

# 一百一十六、put 的完整逻辑拆解

```java
public void put(E element)
        throws InterruptedException {
```

开始放元素。

------

获取锁：

```java
lock.lockInterruptibly();
```

为什么：

```text
修改共享 queue
```

必须加锁。

并且支持：

```text
等待锁时被 interrupt。
```

------

检查是否满：

```java
while (queue.size() == capacity) {
```

如果：

```text
满了
```

就不能生产。

------

等待：

```java
notFull.await();
```

含义：

```text
等待队列出现空位。
```

并且：

```text
释放 lock。
```

------

真正添加：

```java
queue.offer(element);
```

------

然后：

```java
notEmpty.signal();
```

因为现在：

```text
至少可能有东西了。
```

所以：

```text
唤醒消费者。
```

------

最后：

```java
lock.unlock();
```

释放锁。

------

# 一百一十七、take 的完整逻辑拆解

获取锁：

```java
lock.lockInterruptibly();
```

------

如果为空：

```java
while (queue.isEmpty()) {
```

------

消费者等待：

```java
notEmpty.await();
```

------

取元素：

```java
E element = queue.poll();
```

------

取走之后意味着：

```text
队列一定少了一个元素。
```

于是：

```java
notFull.signal();
```

告诉生产者：

```text
可能有位置了。
```

------

最后返回：

```java
return element;
```

------

# 一百一十八、为什么是 signal 而不是 signalAll？

在队列场景中：

```text
增加一个元素
```

通常只需要：

```text
一个消费者
```

来取。

例如：

```text
3 个消费者等待

现在只生产 1 个任务
```

如果：

```java
signalAll();
```

3 个消费者全部醒：

```text
Consumer-A
Consumer-B
Consumer-C
```

但是只有：

```text
1 个任务
```

最终：

```text
2 个又得重新睡。
```

产生：

```text
无意义竞争

上下文切换
```

所以一些实现会尽量精准唤醒。

实际 JDK 内部实现细节由具体队列决定，不应该机械地认为所有 BlockingQueue 都完全使用同一种锁和 Condition 实现。

------

# 一百一十九、BlockingQueue 解决的本质问题

从最底层看，它其实是在协调两种资源：

```text
数据

空间
```

消费者需要：

```text
数据
```

生产者需要：

```text
空间
```

于是：

```text
消费者等待：

有数据
notEmpty
生产者等待：

有空间
notFull
```

这个模型特别漂亮：

```text
BlockingQueue

资源 1：
元素

资源 2：
空位
```

------

# 一百二十、最推荐记住的模型

假设容量：

```text
3
```

队列：

```text
[A][B][C]
```

生产者：

```text
我要放 D

↓

没有空位

↓

等待 notFull
```

消费者：

```text
take A

↓

产生空位

↓

signal notFull
```

然后：

```text
Producer 醒

↓

重新竞争 lock

↓

检查条件

↓

put D
```

反过来：

```text
[]
```

消费者：

```text
我要 take

↓

没有元素

↓

等待 notEmpty
```

生产者：

```text
put A

↓

产生元素

↓

signal notEmpty
```

消费者：

```text
醒

↓

竞争 lock

↓

检查条件

↓

take A
```

------

# 一百二十一、你现在应该掌握到什么程度？

学习 BlockingQueue 后，建议达到以下程度。

## 第一层：必须掌握

看到：

```java
BlockingQueue
```

知道：

```text
线程安全阻塞队列。
```

------

知道：

```text
put 满则等

take 空则等
```

------

知道四组 API：

```text
add/remove/element

offer/poll/peek

put/take

offer(timeout)/poll(timeout)
```

------

知道：

```text
BlockingQueue
```

经典场景：

```text
生产者消费者

线程池
```

------

# 一百二十二、第二层：应该掌握

知道：

```text
ArrayBlockingQueue

LinkedBlockingQueue

SynchronousQueue
```

区别。

------

知道：

```text
ArrayBlockingQueue

数组
有界
```

------

知道：

```text
LinkedBlockingQueue

链表
默认近似无界
```

------

知道：

```text
SynchronousQueue

容量 0
直接交接
```

------

# 一百二十三、第三层：理解即可

知道 BlockingQueue 的阻塞：

```text
不是 while 死循环检查。
```

而是类似：

```text
Lock
+
Condition
```

的真正线程等待。

------

尤其 ArrayBlockingQueue 可以帮助理解：

```text
notEmpty

notFull
```

两个 Condition。

------

# 一百二十四、第四层：以后深入

后面学习线程池的时候重点研究：

```text
ArrayBlockingQueue

LinkedBlockingQueue

SynchronousQueue
```

为什么会影响：

```text
corePoolSize

maximumPoolSize

线程创建

任务排队

拒绝策略
```

这是 BlockingQueue 真正在工作里特别重要的地方。

------

# 一百二十五、最终知识脑图

```text
BlockingQueue
│
├── 本质
│     ├── Queue
│     ├── 线程安全
│     └── 支持阻塞
│
├── 阻塞
│     ├── put
│     │     └── 满了等待
│     │
│     └── take
│           └── 空了等待
│
├── 四类 API
│     │
│     ├── 抛异常
│     │     ├── add
│     │     ├── remove
│     │     └── element
│     │
│     ├── 特殊值
│     │     ├── offer
│     │     ├── poll
│     │     └── peek
│     │
│     ├── 永久等待
│     │     ├── put
│     │     └── take
│     │
│     └── 超时等待
│           ├── offer(timeout)
│           └── poll(timeout)
│
├── 常见实现
│     │
│     ├── ArrayBlockingQueue
│     │     ├── 数组
│     │     ├── 有界
│     │     └── FIFO
│     │
│     ├── LinkedBlockingQueue
│     │     ├── 链表
│     │     ├── FIFO
│     │     └── 默认近似无界
│     │
│     ├── PriorityBlockingQueue
│     │     └── 优先级
│     │
│     ├── DelayQueue
│     │     └── 延迟
│     │
│     ├── SynchronousQueue
│     │     └── 容量 0 / 直接交接
│     │
│     └── LinkedTransferQueue
│           └── transfer
│
├── 底层思想
│     │
│     ├── Lock
│     │
│     ├── notEmpty
│     │     └── 消费者等待
│     │
│     └── notFull
│           └── 生产者等待
│
├── 使用场景
│     │
│     ├── 生产者消费者
│     ├── 线程池
│     ├── 任务缓冲
│     ├── 削峰
│     ├── 解耦
│     └── 异步处理
│
└── 注意事项
      │
      ├── 不允许 null
      ├── 注意 InterruptedException
      ├── 尽量考虑有界队列
      ├── 无界队列可能 OOM
      ├── 单方法线程安全 ≠ 复合操作原子
      └── 队列越大不一定越好
```

# 一百二十六、一句话彻底理解 BlockingQueue

最后可以把 BlockingQueue 记成这一句话：

> **BlockingQueue 就是 Java 已经替我们封装好的“线程安全生产者消费者缓冲区”：生产者通过 put 放数据，满了就等；消费者通过 take 拿数据，空了就等；它内部负责线程安全、等待和唤醒，我们不需要自己再写 wait/notify 或 Condition。**

再压缩成脑子里的条件反射：

```text
BlockingQueue

        =

Queue
+
线程安全
+
阻塞等待
+
生产者消费者
```

而它和你刚学过的 `Condition` 最关键的联系就是：

```text
生产者：
    满了
      ↓
    等 notFull

消费者：
    空了
      ↓
    等 notEmpty
```

这两个条件一旦真正理解，BlockingQueue 基本就算学通了。