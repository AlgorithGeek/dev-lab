# `ReentrantLock`

------

# 一、ReentrantLock 是什么

`ReentrantLock` 可以拆成两个单词：

- `Reentrant`：可重入
- `Lock`：锁

所以它的中文名称就是：

> **可重入锁**

它是一种用于多线程同步的：

> **可重入、独占、互斥锁。**

同一时刻，只允许一个线程持有一把 `ReentrantLock`。

```java
ReentrantLock lock = new ReentrantLock();
```

当线程 A 获得锁以后：

```text
线程 A：获得锁
线程 B：等待
线程 C：等待
```

只有线程 A 释放锁，线程 B、线程 C 才有机会继续竞争

`ReentrantLock` 实现了 `Lock` 接口，并且与 `synchronized` 具有相同的基本互斥语义和内存同步语义，但提供了可中断获取、尝试获取、超时获取、公平策略、多个条件队列以及状态监控等扩展能力

------

# 二、类继承关系与 `Lock` 接口

```text
Object
  └── ReentrantLock
        ├── implements Lock
        └── implements Serializable
```

简化后的类定义：

```java
public class ReentrantLock implements Lock, Serializable {
}
```

`Lock` 接口定义的核心方法包括：

```java
public interface Lock {

    // 获取锁。拿不到就一直等待，等待期间一般不能通过中断退出
    void lock();

    // 获取锁。拿不到就等待，但等待期间可以被 interrupt() 中断
    void lockInterruptibly() throws InterruptedException;

    // 立即尝试获取锁，成功返回 true，失败立即返回 false，不会等待
    boolean tryLock();

    // 在指定时间内尝试获取锁，超时返回 false；等待期间可以被中断
    boolean tryLock(long time, TimeUnit unit)
            throws InterruptedException;

    // 释放当前线程持有的锁
    void unlock();

    // 创建一个与该锁绑定的 Condition，用于线程之间的等待和唤醒
    Condition newCondition();
}
```

------

# 三、最标准的使用方式

## 方式

```java
private final ReentrantLock lock = new ReentrantLock();

public void doSomething() {
    lock.lock();

    try {
        // 需要保护的共享资源
    } finally {
        lock.unlock();
    }
}
```

必须记住这个固定模板：

```java
lock.lock();
try {
    // 临界区
} finally {
    lock.unlock();
}
```

官方也推荐在成功执行 `lock()` 后立即进入 `try`，并在 `finally` 中释放锁。



## 为什么一定要在 finally 中解锁

假设这样写：

```java
lock.lock();

doSomething();

lock.unlock();
```

如果 `doSomething()` 抛出异常：

```java
private void doSomething() {
    throw new RuntimeException("发生异常");
}
```

那么：

```java
lock.unlock();
```

不会执行。

锁将一直被当前线程占用，其他线程可能永远拿不到锁。

正确写法：

```java
lock.lock();

try {
    doSomething();
} finally {
    lock.unlock();
}
```

无论临界区是否出现异常，`finally` 都会释放锁。

------

# 四、什么叫“可重入”

## 1. 基本定义

可重入指的是：

> 一个线程已经持有某把锁时，可以再次获取同一把锁，而不会把自己阻塞。

例如：

```java
private final ReentrantLock lock = new ReentrantLock();

public void methodA() {
    lock.lock();

    try {
        System.out.println("执行 methodA");
        methodB();
    } finally {
        lock.unlock();
    }
}

public void methodB() {
    lock.lock();

    try {
        System.out.println("执行 methodB");
    } finally {
        lock.unlock();
    }
}
```

线程执行 `methodA()` 时：

```text
第一次 lock()：
锁持有次数 = 1

进入 methodB()：
第二次 lock()
锁持有次数 = 2

methodB() 执行 unlock()：
锁持有次数 = 1

methodA() 执行 unlock()：
锁持有次数 = 0
锁真正释放
```

因为第二次加锁的是同一个线程，所以不会阻塞。

## 2. 为什么需要可重入

假设锁不可重入：

```java
methodA() 获得锁
    ↓
methodA() 调用 methodB()
    ↓
methodB() 尝试获得同一把锁
    ↓
等待锁
```

可是这把锁已经被当前线程自己持有。

于是当前线程等待自己释放锁，但自己又必须等 `methodB()` 返回后才能释放锁，最终形成自我死锁。

可重入机制解决了这个问题。

## 3. 重入次数必须和解锁次数一致

```java
lock.lock();
lock.lock();

try {
    // 临界区
} finally {
    lock.unlock();
    lock.unlock();
}
```

每执行一次成功的 `lock()`，都必须匹配一次 `unlock()`。

错误示例：

```java
lock.lock();
lock.lock();

try {
    // 临界区
} finally {
    // ❌ 只有一次
    lock.unlock();
}
```

执行完以后，锁持有次数仍然为 `1`，锁没有真正释放。

`ReentrantLock` 使用一个整数记录当前线程的持有次数；当前线程重复获得锁时计数增加，释放时计数减少，只有减少到 `0` 时才真正释放。JDK 17 中最大重入次数为 `Integer.MAX_VALUE`，继续重入会抛出 `Error`。

------

# 五、ReentrantLock 的核心状态

> state 和 owner

## 一、先记住最核心的一句话

一把 `ReentrantLock` 想要知道自己当前的状态，至少需要回答两个问题：

```text
1. 现在有没有线程拿着这把锁？
2. 如果有，是哪个线程拿着？它拿了几次？
```

对应的两个核心变量就是：

```text
state：拿了几次
owner：是谁拿的
```

例如：

```text
state = 3
owner = Thread-A
```

表示：

> `Thread-A` 当前持有这把锁，并且总共成功执行了三次 `lock()`，还没有执行对应的三次 `unlock()`。

------

## 二、ReentrantLock 内部结构

创建一把锁：

```java
ReentrantLock lock = new ReentrantLock();
```

表面上我们操作的是 `ReentrantLock`，但真正负责加锁、排队和释放锁的，是它内部的 `Sync` 对象。

简化结构：

```text
ReentrantLock
    │
    └── Sync
          │
          └── AbstractQueuedSynchronizer
                  │
                  └── AbstractOwnableSynchronizer
```

更准确地说，继承关系是：

```text
AbstractOwnableSynchronizer
            ↑
AbstractQueuedSynchronizer
            ↑
          Sync
```

它们分别负责不同的东西：

```text
AbstractOwnableSynchronizer
    └── exclusiveOwnerThread：记录谁持有锁

AbstractQueuedSynchronizer
    ├── state：记录锁被持有了几次
    └── 同步队列：记录正在等待获取锁的线程

ConditionObject
    └── 条件队列：记录调用 await() 等待条件的线程
```

因此，比较准确的结构图是：

```text
┌─────────────────────────────────┐
│         ReentrantLock           │
│                                 │
│  ┌───────────────────────────┐  │
│  │           Sync            │  │
│  │                           │  │
│  │ state：锁持有次数           │  │
│  │ owner：当前持锁线程         │  │
│  │ syncQueue：等待获取锁的线程  │  │
│  └───────────────────────────┘  │
│                                 │
│  Condition 1：自己的条件队列      │
│  Condition 2：自己的条件队列      │
└─────────────────────────────────┘
```

需要注意：

> 条件队列不是 ReentrantLock 天生只有一个。

每次调用：

```java
Condition condition = lock.newCondition();
```

都会创建一个 `Condition` 对象，每个 `Condition` 都有自己的条件等待队列。

------

## 三、state 到底是什么

在 `ReentrantLock` 中，`state` 是一个整数。

它表示：

> 当前持锁线程还持有这把锁多少层。

可以把它看成一个计数器：

```text
state = 0
没有线程持有锁

state = 1
某个线程成功获得锁一次

state = 2
同一个线程总共成功获得锁两次

state = 3
同一个线程总共成功获得锁三次
```

这里有一个容易说错的地方：

```text
state = 2
```

更加准确的意思是：

> 总共加锁两次，其中第一次是初次获取，第二次是重入。

不是“重入了两次”。

### 示例

```java
lock.lock();
lock.lock();
lock.lock();
```

假设这三次都是线程 A 执行的：

```text
第一次 lock()：
state = 1

第二次 lock()：
state = 2

第三次 lock()：
state = 3
```

此时：

```text
owner = Thread-A
state = 3
```

------

## 四、owner 到底是什么

`owner` 可以理解为：

> 当前真正持有这把锁的是哪个线程。

它对应的底层变量叫：

```java
private transient Thread exclusiveOwnerThread;
```

例如：

```text
owner = Thread-A
```

说明当前持锁线程是线程 A。

如果锁没有被任何线程持有，一般可以理解为：

```text
owner = null
state = 0
```

------

## 五、为什么既需要 state，又需要 owner

只记录 `state` 不够。

假设：

```text
state = 2
```

我们只能知道：

> 这把锁被某个线程持有了两层。

但是不知道是哪个线程。

所以还需要：

```text
owner = Thread-A
```

组合起来才能表示完整状态：

```text
owner = Thread-A
state = 2
```

含义是：

> Thread-A 持有这把锁，总共加锁两次。

反过来，只记录 `owner` 也不够。

假设只知道：

```text
owner = Thread-A
```

只能知道 A 持有锁，却不知道它到底调用了几次 `lock()`，也不知道需要执行多少次 `unlock()` 才能真正释放。

因此：

```text
owner：解决“是谁”的问题
state：解决“拿了几次”的问题
```

------

## 六、完整执行过程

假设有三个线程：

```text
Thread-A
Thread-B
Thread-C
```

初始状态：

```text
state = 0
owner = null
```

表示锁没有被任何线程持有。

------

### 第一步：Thread-A 第一次加锁

Thread-A 执行：

```java
lock.lock();
```

发现：

```text
state = 0
```

说明锁空闲。

于是 Thread-A 尝试把：

```text
state：0 → 1
```

成功以后，同时记录：

```text
owner = Thread-A
```

最终状态：

```text
state = 1
owner = Thread-A
```

意思是：

> Thread-A 第一次获得了锁。

------

### 第二步：Thread-A 再次加锁

Thread-A 又执行：

```java
lock.lock();
```

此时：

```text
state = 1
owner = Thread-A
```

锁已经被持有，但持锁线程正好就是当前线程自己。

因此允许重入：

```text
state：1 → 2
```

owner 不变：

```text
owner = Thread-A
```

最终：

```text
state = 2
owner = Thread-A
```

意思是：

> Thread-A 总共加锁两次。

------

### 第三步：Thread-B 尝试加锁

Thread-B 执行：

```java
lock.lock();
```

此时发现：

```text
state = 2
owner = Thread-A
```

Thread-B 会进行判断：

```text
当前线程是 Thread-B
owner 是 Thread-A
```

不是同一个线程。

所以 Thread-B 不能重入，也不能获得锁，只能进入同步队列等待：

```text
同步队列：

Thread-B
```

锁的状态没有改变：

```text
state = 2
owner = Thread-A
```

------

### 第四步：Thread-C 也尝试加锁

Thread-C 也执行：

```java
lock.lock();
```

同样发现锁被 Thread-A 持有，于是也进入同步队列：

```text
同步队列：

Thread-B → Thread-C
```

当前状态依然是：

```text
state = 2
owner = Thread-A
```

------

### 第五步：Thread-A 第一次解锁

Thread-A 执行：

```java
lock.unlock();
```

因为 Thread-A 总共加锁了两次，所以第一次解锁只是让计数减一：

```text
state：2 → 1
```

此时：

```text
state = 1
owner = Thread-A
```

锁仍然没有真正释放。

Thread-B 和 Thread-C 仍然不能获得锁。

------

### 第六步：Thread-A 第二次解锁

Thread-A 再执行一次：

```java
lock.unlock();
```

这次：

```text
state：1 → 0
```

当 `state` 变成 `0` 时，表示重入层数已经全部释放。

于是清空 owner：

```text
owner = null
```

最终：

```text
state = 0
owner = null
```

这时锁才算真正释放。

同步队列中的线程才有机会被唤醒并重新竞争：

```text
Thread-B → Thread-C
```

------

## 七、用表格看完整流程

| 操作                | state | owner    | 同步队列 |
| ------------------- | ----- | -------- | -------- |
| 初始状态            | 0     | null     | 空       |
| A 第一次 `lock()`   | 1     | Thread-A | 空       |
| A 第二次 `lock()`   | 2     | Thread-A | 空       |
| B 执行 `lock()`     | 2     | Thread-A | B        |
| C 执行 `lock()`     | 2     | Thread-A | B → C    |
| A 第一次 `unlock()` | 1     | Thread-A | B → C    |
| A 第二次 `unlock()` | 0     | null     | B → C    |
| B 成功获得锁        | 1     | Thread-B | C        |

------

## 八、用代码观察重入次数

```java
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockStateDemo {

    private static final ReentrantLock LOCK =
            new ReentrantLock();

    public static void main(String[] args) {
        LOCK.lock();

        try {
            System.out.println(
                    "第一次加锁：" + LOCK.getHoldCount()
            );

            LOCK.lock();

            try {
                System.out.println(
                        "第二次加锁：" + LOCK.getHoldCount()
                );
            } finally {
                LOCK.unlock();

                System.out.println(
                        "释放一次后：" + LOCK.getHoldCount()
                );
            }
        } finally {
            LOCK.unlock();

            System.out.println(
                    "全部释放后：" + LOCK.getHoldCount()
            );
        }
    }
}
```

输出：

```text
第一次加锁：1
第二次加锁：2
释放一次后：1
全部释放后：0
```

`getHoldCount()` 返回的是：

> 当前线程对这把锁的持有次数。

------

## 九、类比理解：酒店房门和登记表

可以把 `ReentrantLock` 想象成一间只能由一个客人占用的房间。

锁内部有两项登记：

```text
owner：当前入住的人是谁
state：这个人拿了多少张同一房间的门卡
```

假设：

```text
owner = 张三
state = 3
```

意思是：

> 张三住在这个房间里，并且连续申请了三次使用权。

张三归还一张门卡：

```text
state：3 → 2
```

房间仍然属于张三。

再归还一张：

```text
state：2 → 1
```

房间仍然属于张三。

最后一张也归还：

```text
state：1 → 0
owner：张三 → null
```

这时候房间才真正空出来，下一个排队的人才能入住。

其他人不能替张三归还门卡，因为：

```text
当前操作人 != owner
```

所以其他线程调用 `unlock()` 会抛出：

```text
IllegalMonitorStateException
```

------

## 十、state 为什么要使用 volatile

AQS 中的 `state` 类似于：

```java
private volatile int state;
```

`volatile` 主要保证：

```text
一个线程修改了 state，
其他线程能够看到最新值。
```

例如 Thread-A 释放锁：

```text
state：1 → 0
```

正在竞争锁的 Thread-B 需要能够看到：

```text
state 已经变成了 0
```

除此之外，多个线程竞争时还需要使用 CAS。

例如两个线程同时发现：

```text
state = 0
```

它们都会想获得锁，但最终只能有一个线程成功执行：

```text
CAS：state 从 0 修改为 1
```

假设：

```text
Thread-A：CAS 成功
Thread-B：CAS 失败
```

那么：

```text
Thread-A 获得锁
Thread-B 进入等待流程
```

所以：

```text
volatile：保证状态的可见性
CAS：保证竞争修改的原子性
```

但不能理解成：

> 因为 state 是 volatile，所以 ReentrantLock 就只靠 volatile 实现了锁。

实际上它还涉及：

```text
CAS
owner
AQS 同步队列
LockSupport.park()
LockSupport.unpark()
```

------

## 十一、同步队列和条件队列不是一回事

### 同步队列

线程因为拿不到锁而等待：

```java
lock.lock();
```

当前锁被其他线程持有，于是进入同步队列：

```text
同步队列：

Thread-B → Thread-C
```

它们等待的是：

> 什么时候可以获得锁。

------

### 条件队列

线程已经获得锁，但发现业务条件不成立，于是执行：

```java
condition.await();
```

它会释放锁，进入这个 `Condition` 自己的条件队列：

```text
条件队列：

Thread-D → Thread-E
```

它们等待的是：

> 什么时候业务条件成立。

被 `signal()` 通知后，也不会直接执行，而是先从条件队列转移到同步队列，再重新竞争锁：

```text
条件队列
    ↓ signal()
同步队列
    ↓ 重新竞争
获得锁
    ↓
await() 返回
```

------

## 十二、最终需要记住的内容

```text
state = 0：
锁空闲

state > 0：
锁正在被某个线程持有

owner：
记录持锁线程是谁

同一个线程再次 lock：
state++

每执行一次 unlock：
state--

state 还大于 0：
锁仍然属于原线程

state 减少到 0：
锁真正释放，owner 清空
```

一句话总结：

> `owner` 记录“谁拿着锁”，`state` 记录“这个线程拿了几次”；只有持锁线程执行足够次数的 `unlock()`，让 `state` 归零，这把锁才真正释放。



------



------

# 七、lockInterruptibly()：可中断地获取锁

```java
lock.lockInterruptibly();
```

它表示：

> 等待锁期间，可以响应中断并放弃获取锁。

完整写法：

```java
public void execute() {
    boolean locked = false;

    try {
        lock.lockInterruptibly();
        locked = true;

        // 临界区
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        System.out.println("等待锁时被中断");
    } finally {
        if (locked) {
            lock.unlock();
        }
    }
}
```

## 为什么需要 locked 变量

`lockInterruptibly()` 可能在获得锁之前抛出 `InterruptedException`。

错误写法：

```java
try {
    lock.lockInterruptibly();
    // 临界区
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
} finally {
    lock.unlock();
}
```

如果线程还没有获得锁就被中断，`finally` 仍然调用 `unlock()`，会抛出：

```text
IllegalMonitorStateException
```

所以需要记录是否真正获得了锁：

```java
boolean locked = false;
```

## lock() 与 lockInterruptibly() 对比

| 方法                  | 等待时响应中断 | 被中断后                    |
| --------------------- | -------------- | --------------------------- |
| `lock()`              | 不取消获取     | 继续等锁                    |
| `lockInterruptibly()` | 响应中断       | 抛出 `InterruptedException` |

`lockInterruptibly()` 在进入方法时已经存在中断标记，或者等待过程中被中断，都会抛出 `InterruptedException`，并清除中断标记。该实现会优先处理中断，而不是优先完成锁获取。

业务层通常应根据语义选择：

```java
catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    return;
}
```

重新设置中断标记，是为了让更上层代码仍然能够知道线程曾被中断。

------

# 八、tryLock()：立即尝试获得锁

```java
boolean acquired = lock.tryLock();
```

特点：

- 能立即获得锁：返回 `true`；
- 当前线程已经持锁：重入并返回 `true`；
- 其他线程正在持锁：立即返回 `false`；
- 不会一直等待。

标准写法：

```java
if (lock.tryLock()) {
    try {
        // 获得锁后执行
    } finally {
        lock.unlock();
    }
} else {
    // 没有获得锁
}
```

## 常见错误

```java
try {
    lock.tryLock();
    // 临界区
} finally {
    lock.unlock();
}
```

这是错误的。

因为 `tryLock()` 可能返回 `false`，当前线程根本没有持锁，却执行了 `unlock()`。

正确写法：

```java
boolean acquired = lock.tryLock();

if (!acquired) {
    return;
}

try {
    // 临界区
} finally {
    lock.unlock();
}
```

## tryLock() 的典型用途

适合：

- 当前操作不是必须执行；
- 获取不到锁可以跳过；
- 获取不到锁可以走降级逻辑；
- 不希望线程无限等待；
- 尝试避免多锁死锁。

------

# 九、带超时时间的 tryLock()

```java
boolean acquired = lock.tryLock(3, TimeUnit.SECONDS);
```

含义：

> 最多等待三秒。

结果有三种：

```text
等待期间获得锁：
返回 true

等待三秒仍未获得锁：
返回 false

等待期间被中断：
抛出 InterruptedException
```

标准写法：

```java
boolean acquired = false;

try {
    acquired = lock.tryLock(3, TimeUnit.SECONDS);

    if (!acquired) {
        System.out.println("获取锁超时");
        return;
    }

    // 临界区
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
} finally {
    if (acquired) {
        lock.unlock();
    }
}
```

官方文档规定，带超时的 `tryLock` 支持中断；超时时间小于或等于零时不会等待。

------

# 十、四种锁获取方式对比

| 方法                     | 获取不到锁时 | 支持中断   | 支持超时 | 返回值    |
| ------------------------ | ------------ | ---------- | -------- | --------- |
| `lock()`                 | 一直等待     | 不取消获取 | 否       | `void`    |
| `lockInterruptibly()`    | 一直等待     | 是         | 否       | `void`    |
| `tryLock()`              | 立即返回     | 不等待     | 否       | `boolean` |
| `tryLock(timeout, unit)` | 等到超时     | 是         | 是       | `boolean` |

简单选择方式：

```text
一定要拿到锁：
lock()

需要支持取消任务：
lockInterruptibly()

拿不到就算了：
tryLock()

最多允许等一段时间：
tryLock(timeout, unit)
```

------

# 十一、公平锁与非公平锁

## 1. 创建非公平锁

```java
ReentrantLock lock = new ReentrantLock();
```

等价于：

```java
ReentrantLock lock = new ReentrantLock(false);
```

默认是非公平锁。

## 2. 创建公平锁

```java
ReentrantLock lock = new ReentrantLock(true);
```

## 3. 什么叫公平锁

假设等待顺序为：

```text
线程 A 先等待
线程 B 后等待
线程 C 最后等待
```

公平锁尽量按照：

```text
A → B → C
```

分配锁。

它会优先考虑等待时间较长的线程。

## 4. 什么叫非公平锁

非公平锁允许新来的线程直接参与竞争。

例如：

```text
线程 A 正在队列中等待
线程 B 正在队列中等待

线程 C 此时刚好到达
```

当锁被释放时，线程 C 可能直接抢到锁：

```text
C 抢到锁
A、B 继续等待
```

这种行为叫：

> 插队，英文常称 barging。

## 5. 为什么默认使用非公平锁

因为非公平锁通常拥有更高吞吐量。

公平锁需要：

- 检查是否有前驱等待线程；
- 尽量维护等待顺序；
- 让刚刚被唤醒的线程重新获得 CPU；
- 可能产生更多线程切换。

而非公平锁允许当前正在运行的新线程直接获取锁。

这可能减少：

```text
线程挂起
    ↓
唤醒其他线程
    ↓
操作系统重新调度
    ↓
上下文切换
```

官方文档说明，公平锁在高竞争下通常吞吐量更低，但获取锁的等待时间波动更小，并提供避免饥饿的锁分配保证；不过锁公平并不等于操作系统线程调度公平。

## 6. 公平锁也不保证绝对执行顺序

即使使用：

```java
new ReentrantLock(true);
```

也不能理解成：

```text
线程启动顺序 = 获得锁顺序 = 执行结束顺序
```

公平策略只针对锁竞争队列。

线程什么时候被操作系统调度，仍由线程调度器决定。

## 7. tryLock() 会破坏公平性

这是一个非常重要的细节。

即使创建的是公平锁：

```java
ReentrantLock lock = new ReentrantLock(true);
```

调用：

```java
lock.tryLock();
```

仍然允许插队。

只要调用瞬间锁是空闲的，它就会直接获取锁，而不会尊重正在等待的线程。

官方建议，需要尊重公平策略时，可以使用：

```java
lock.tryLock(0, TimeUnit.SECONDS);
```

它几乎等同于立即尝试，但会遵循公平策略，并且检测中断。

------

# 十二、ReentrantLock 底层为什么依赖 AQS

`ReentrantLock` 本身没有从头实现：

- 等待队列；
- 线程挂起；
- 线程唤醒；
- 中断取消；
- 超时取消；
- 条件队列；
- CAS 状态管理。

这些通用能力由：

```java
AbstractQueuedSynchronizer
```

提供，简称：

```text
AQS
```

AQS 可以理解为：

> JUC 中用于构建锁和同步器的基础框架。

很多并发工具都建立在 AQS 之上，例如：

- `ReentrantLock`
- `ReentrantReadWriteLock`
- `Semaphore`
- `CountDownLatch`

AQS 使用一个原子整数状态和一个先进先出的等待队列，负责通用的排队、阻塞和唤醒机制；具体的状态含义由子类决定。

------

# 十三、ReentrantLock 的内部类结构

简化结构：

```text
ReentrantLock
    │
    ├── Sync
    │     └── extends AbstractQueuedSynchronizer
    │
    ├── NonfairSync
    │     └── extends Sync
    │
    └── FairSync
          └── extends Sync
```

近似源码：

```java
public class ReentrantLock implements Lock {

    private final Sync sync;

    abstract static class Sync
            extends AbstractQueuedSynchronizer {
    }

    static final class NonfairSync extends Sync {
    }

    static final class FairSync extends Sync {
    }

    public ReentrantLock() {
        sync = new NonfairSync();
    }

    public ReentrantLock(boolean fair) {
        sync = fair
                ? new FairSync()
                : new NonfairSync();
    }
}
```

因此：

```java
new ReentrantLock();
```

内部创建的是：

```text
NonfairSync
```

而：

```java
new ReentrantLock(true);
```

内部创建的是：

```text
FairSync
```

JDK 17 的 `ReentrantLock` 正是通过 `Sync`、`NonfairSync` 和 `FairSync` 三层结构区分公平与非公平策略。

------

# 十四、非公平锁的加锁流程

执行：

```java
lock.lock();
```

大致流程：

```text
1. 尝试 CAS 将 state 从 0 改为 1
2. 成功：记录当前线程为 owner
3. 失败：判断当前线程是不是 owner
4. 是 owner：执行重入，state++
5. 不是 owner：进入 AQS 同步队列
6. 当前线程可能被 park
7. 前面的线程释放锁后，被唤醒
8. 再次尝试获得锁
```

简化伪代码：

```java
boolean tryAcquire() {
    Thread current = Thread.currentThread();
    int state = getState();

    if (state == 0) {
        if (compareAndSetState(0, 1)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    } else if (current == getExclusiveOwnerThread()) {
        setState(state + 1);
        return true;
    }

    return false;
}
```

关键点是：

```java
compareAndSetState(0, 1)
```

只有一个线程能够成功把 `state` 从 `0` 改成 `1`。

其他线程 CAS 失败后，就需要进入等待流程。

JDK 17 的非公平实现会首先进行不检查队列的 CAS 抢锁；当前线程已经是 owner 时则增加 `state`，其他情况交给 AQS 排队获取。

------

# 十五、公平锁的加锁流程

公平锁与非公平锁最大的区别，是获得锁之前要判断：

> 自己前面是否已经有等待线程。

核心判断可以抽象成：

```java
!hasQueuedPredecessors()
```

简化伪代码：

```java
boolean tryAcquire() {
    Thread current = Thread.currentThread();
    int state = getState();

    if (state == 0) {
        if (!hasQueuedPredecessors()
                && compareAndSetState(0, 1)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    } else if (current == getExclusiveOwnerThread()) {
        setState(state + 1);
        return true;
    }

    return false;
}
```

含义是：

```text
锁空闲
并且
自己前面没有等待线程
并且
CAS 成功
```

才能获得锁。

AQS 本身虽然使用 FIFO 等待队列，但不会自动强制所有同步器都按照严格 FIFO 获取；公平同步器通常通过检查 `hasQueuedPredecessors()` 禁止新线程插队。

------

# 十六、AQS 同步队列

当一个线程获取锁失败时，会被包装成一个队列节点：

```text
Node
```

然后加入 AQS 同步队列。

可以简化理解为：

```text
head
  ↓
[虚拟头节点] ← [线程 A] ← [线程 B] ← [线程 C]
                                         ↑
                                        tail
```

或者：

```text
线程 A：等待时间最长
线程 B：第二个等待
线程 C：刚刚进入队列
```

AQS 使用的是一种经过修改的 CLH 队列，节点中包含：

```java
volatile Node prev;
volatile Node next;
Thread waiter;
volatile int status;
```

队列主要承担：

- 记录等待锁的线程；
- 维护前驱、后继关系；
- 取消超时或中断的节点；
- 锁释放时寻找后继线程；
- 通过 `LockSupport` 阻塞和唤醒线程。

JDK 17 的 AQS 源码将其描述为 CLH 队列的变体，增加了双向链接、等待状态以及用于阻塞同步器的唤醒机制。

------

# 十七、AQS 与 LockSupport 的关系

线程获取锁失败后，并不是一直疯狂循环。

大致过程是：

```text
尝试获取锁
    ↓
失败
    ↓
加入同步队列
    ↓
再次检查是否可以获取
    ↓
仍然失败
    ↓
LockSupport.park()
```

线程被挂起后，不再持续占用 CPU。

锁释放时，会寻找适合唤醒的后继节点，然后执行类似：

```java
LockSupport.unpark(waiterThread);
```

被唤醒的线程不会直接获得锁，而是：

```text
从 park 返回
    ↓
重新检查状态
    ↓
重新尝试获取锁
    ↓
成功或再次等待
```

因此必须牢记：

> `unpark()` 只表示线程可以继续运行，不表示线程已经获得锁。

AQS 获取流程会在适当时调用 `LockSupport.park()` 或 `parkNanos()`，锁释放时通过后继节点信号推动等待线程重新竞争。线程被唤醒后仍需再次执行获取判断。

------

# 十八、unlock() 的底层流程

执行：

```java
lock.unlock();
```

大致步骤：

```text
1. 检查当前线程是不是锁的 owner
2. state = state - 1
3. state 不等于 0：
   只是减少重入次数，不真正释放锁
4. state 等于 0：
   清空 owner
   锁真正释放
5. 通知同步队列中的后继线程重新竞争
```

简化伪代码：

```java
boolean tryRelease(int releases) {
    int newState = getState() - releases;

    if (Thread.currentThread()
            != getExclusiveOwnerThread()) {
        throw new IllegalMonitorStateException();
    }

    boolean free = newState == 0;

    if (free) {
        setExclusiveOwnerThread(null);
    }

    setState(newState);
    return free;
}
```

只有 `state` 减少到 `0`，AQS 才会执行后续唤醒流程。

JDK 17 中，`tryRelease` 在持有次数降到零时清空 owner，并由 AQS 的 `release` 通知同步队列中的后继节点。

------

# 十九、为什么其他线程不能 unlock

假设：

```text
线程 A 获得锁
线程 B 调用 unlock()
```

线程 B 会抛出：

```text
IllegalMonitorStateException
```

因为锁的基本要求是：

> 谁获得，谁释放。

示例：

```java
lock.lock();

new Thread(() -> {
    lock.unlock();
}).start();
```

子线程没有持有这把锁，因此不能释放。

官方文档规定，当前线程不是锁持有者时，调用 `unlock()` 会抛出 `IllegalMonitorStateException`。

------

# 二十、Condition 是什么

使用 `synchronized` 时，可以使用：

```java
wait();
notify();
notifyAll();
```

使用 `ReentrantLock` 时，对应的是：

```java
Condition.await();
Condition.signal();
Condition.signalAll();
```

创建 Condition：

```java
private final ReentrantLock lock = new ReentrantLock();
private final Condition condition = lock.newCondition();
```

对应关系：

| synchronized         | ReentrantLock           |
| -------------------- | ----------------------- |
| `Object.wait()`      | `Condition.await()`     |
| `Object.notify()`    | `Condition.signal()`    |
| `Object.notifyAll()` | `Condition.signalAll()` |

`Condition` 将对象监视器中的等待通知机制拆分成独立对象，让同一把锁可以关联多个等待集合。

------

# 二十一、Condition 最大的优势：多个等待队列

一个 Java 对象的监视器只有一个等待集合。

```java
synchronized (lock) {
    lock.wait();
}
```

不同原因而等待的线程，都在同一个等待集合中。

而一把 `ReentrantLock` 可以创建多个 `Condition`：

```java
private final ReentrantLock lock = new ReentrantLock();

private final Condition notEmpty =
        lock.newCondition();

private final Condition notFull =
        lock.newCondition();
```

可以分别表示：

```text
notEmpty：
队列不能为空

notFull：
队列不能已满
```

于是：

```text
消费者等待 notEmpty
生产者等待 notFull
```

可以精确唤醒对应类型的线程，而不是把所有等待线程都唤醒。

------

# 二十二、await() 的完整语义

```java
condition.await();
```

必须先持有对应的锁：

```java
lock.lock();

try {
    condition.await();
} finally {
    lock.unlock();
}
```

`await()` 大致完成：

```text
1. 检查当前线程是否持有锁
2. 将当前线程加入 Condition 条件队列
3. 保存当前锁的重入次数
4. 完全释放锁
5. 挂起当前线程
6. 等待 signal、signalAll、中断或虚假唤醒
7. 转移到 AQS 同步队列
8. 重新竞争锁
9. 恢复原来的重入次数
10. 获得锁以后，await() 才返回
```

最重要的三个结论：

## 结论一：await() 会释放锁

```java
condition.await();
```

等待过程中，其他线程可以获得这把锁。

不释放锁的话，其他线程无法进入临界区修改条件，也就无法调用 `signal()`。

## 结论二：await() 返回前必须重新获得锁

`await()` 被唤醒后，不会直接从方法返回。

它必须：

```text
先重新获得锁
再从 await() 返回
```

所以执行完：

```java
condition.await();
```

后，当前线程仍然持有锁。

## 结论三：会恢复原来的重入次数

假设：

```java
lock.lock();
lock.lock();

condition.await();
```

进入 `await()` 前：

```text
state = 2
```

等待时会完全释放锁：

```text
state = 0
```

重新获得锁后：

```text
state = 2
```

官方文档明确规定，Condition 等待会原子地释放关联锁，返回之前重新获得锁，并恢复调用等待方法时的锁持有次数。

------

# 二十三、为什么 await() 必须放在 while 中

错误写法：

```java
if (queue.isEmpty()) {
    notEmpty.await();
}
```

正确写法：

```java
while (queue.isEmpty()) {
    notEmpty.await();
}
```

原因之一是：

> Condition 允许出现虚假唤醒。

也就是线程可能在没有真正满足业务条件时从等待中返回。

另一个更常见的原因是：

```text
线程 A 被 signal
线程 A 开始等待重新获得锁

线程 B 抢先获得锁
线程 B 消耗了资源
线程 B 释放锁

线程 A 获得锁
```

此时线程 A 被通知过，但业务条件可能又不成立了。

所以必须重新判断：

```java
while (!条件成立) {
    condition.await();
}
```

Condition 官方文档要求应用程序始终假设虚假唤醒可能发生，因此应在循环中检查等待条件。

------

# 二十四、signal() 与 signalAll()

## 1. signal()

```java
condition.signal();
```

作用：

> 将一个等待线程从 Condition 条件队列转移到 AQS 同步队列。

它不是：

```text
让等待线程立即继续执行
```

而是：

```text
Condition 队列
    ↓
AQS 同步队列
    ↓
等待重新获得锁
    ↓
获得锁后 await() 返回
```

## 2. signalAll()

```java
condition.signalAll();
```

作用：

> 将该 Condition 中所有等待线程转移到同步队列。

这些线程仍然要逐个竞争锁。

## 3. signal 后当前线程仍然持有锁

```java
lock.lock();

try {
    condition.signal();
    // 当前线程仍然持有 lock
    doSomething();
} finally {
    lock.unlock();
}
```

被通知的线程通常仍需等待当前线程执行：

```java
lock.unlock();
```

然后才能竞争锁。

JDK 17 的 `ConditionObject.signal()` 会把等待时间最长的条件节点转移到锁的同步队列；`signalAll()` 会转移全部节点。被转移的线程仍必须重新获得锁，才能从 `await()` 返回。

------

# 二十五、Condition 的两个队列

使用 Condition 后，会同时存在两类队列。

## 1. 同步队列

等待获取锁的线程：

```text
AQS Sync Queue

[线程 A] → [线程 B] → [线程 C]
```

这些线程的状态是：

```text
我想获得锁，但是锁被其他线程持有。
```

## 2. 条件队列

调用 `await()` 等待某个业务条件的线程：

```text
Condition Queue

[线程 D] → [线程 E]
```

这些线程的状态是：

```text
我曾经获得锁，但业务条件不满足，
所以主动释放锁并等待通知。
```

## 3. signal 发生了什么

```text
Condition Queue：

[D] → [E]

执行 signal()

Condition Queue：

[E]

Sync Queue：

[A] → [B] → [D]
```

`signal()` 并不是让 D 直接执行，而是把 D 转移到同步队列。

可以概括为：

```text
lock 获取失败：
直接进入同步队列

await：
先进入条件队列

signal：
从条件队列转移到同步队列
```

AQS 源码中，Condition 节点通过额外链接维护 FIFO 条件队列；`await` 加入条件队列，`signal` 再将节点加入主同步队列。

------

# 二十六、生产者消费者完整示例

```java
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BoundedQueue<E> {

    private final Queue<E> queue = new ArrayDeque<>();

    private final int capacity;

    private final ReentrantLock lock =
            new ReentrantLock();

    /**
     * 队列未满条件。
     */
    private final Condition notFull =
            lock.newCondition();

    /**
     * 队列非空条件。
     */
    private final Condition notEmpty =
            lock.newCondition();

    public BoundedQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "capacity must be greater than zero");
        }

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

            /*
             * 放入元素后，队列一定非空。
             * 通知等待取元素的消费者。
             */
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

            /*
             * 取走元素后，队列一定没有原来那么满。
             * 通知等待放元素的生产者。
             */
            notFull.signal();

            return element;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();

        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }
}
```

这里使用两个 Condition：

```text
notEmpty：
消费者等待

notFull：
生产者等待
```

它比所有线程使用同一个等待集合更加精确。

JDK 官方的 Condition 文档也使用了有界缓冲区示例，通过 `notFull` 和 `notEmpty` 两个条件队列分别管理生产者和消费者。

实际业务中优先使用已经实现好的：

```java
ArrayBlockingQueue
LinkedBlockingQueue
```

不需要重复造轮子。

------

# 二十七、Condition 的其他等待方法

## 1. await()

```java
condition.await();
```

- 支持中断；
- 被中断时抛出 `InterruptedException`；
- 抛出时清除中断标记。

## 2. awaitUninterruptibly()

```java
condition.awaitUninterruptibly();
```

等待过程中不因中断而退出。

如果等待期间发生过中断，最终正常返回后，中断标记仍然会保留。

## 3. awaitNanos()

```java
long remaining =
        condition.awaitNanos(timeoutNanos);
```

返回剩余等待时间，适合在循环中重新计算超时。

```java
long nanos =
        TimeUnit.SECONDS.toNanos(3);

while (!conditionSatisfied) {
    if (nanos <= 0L) {
        return false;
    }

    nanos = condition.awaitNanos(nanos);
}
```

## 4. await(time, unit)

```java
boolean signalled =
        condition.await(3, TimeUnit.SECONDS);
```

通常：

- 条件通知前返回：`true`；
- 超时返回：`false`。

但无论如何，仍应重新检查业务条件。

## 5. awaitUntil()

```java
boolean signalled =
        condition.awaitUntil(deadline);
```

等待到某个绝对时间点。

Condition 接口定义了可中断等待、不可中断等待、相对超时等待和绝对截止时间等待等形式。

------

# 二十八、ReentrantLock 的内存可见性

ReentrantLock 不只是解决：

```text
同一时刻只有一个线程执行
```

它还提供内存可见性保证。

假设：

```java
private int value = 0;
private final ReentrantLock lock = new ReentrantLock();
```

线程 A：

```java
lock.lock();

try {
    value = 100;
} finally {
    lock.unlock();
}
```

线程 B：

```java
lock.lock();

try {
    System.out.println(value);
} finally {
    lock.unlock();
}
```

当线程 B 在 A 释放同一把锁后成功获得锁时，能够看到 A 在释放锁前对共享变量所做的修改。

可以理解为：

```text
线程 A 在 unlock() 前的操作
        happens-before
线程 B 随后的成功 lock() 后的操作
```

`Lock` 接口要求成功的加锁和解锁操作具有与内置监视器锁相同的内存同步效果。

但是必须满足：

> 对共享数据的读和写，使用的是同一把锁。

错误示例：

```java
private final ReentrantLock lock1 =
        new ReentrantLock();

private final ReentrantLock lock2 =
        new ReentrantLock();
```

线程 A 用 `lock1` 写：

```java
lock1.lock();

try {
    value = 100;
} finally {
    lock1.unlock();
}
```

线程 B 用 `lock2` 读：

```java
lock2.lock();

try {
    System.out.println(value);
} finally {
    lock2.unlock();
}
```

两把不同的锁无法建立正确的互斥和锁同步关系。

------

# 二十九、ReentrantLock 与 synchronized 对比

| 对比项       | synchronized         | ReentrantLock         |
| ------------ | -------------------- | --------------------- |
| 是否可重入   | 是                   | 是                    |
| 是否互斥     | 是                   | 是                    |
| 是否自动释放 | 是                   | 否                    |
| 等待锁可中断 | 不直接支持           | `lockInterruptibly()` |
| 非阻塞尝试   | 不支持               | `tryLock()`           |
| 超时获取     | 不支持               | `tryLock(timeout)`    |
| 公平策略     | 不提供选择           | 可选择公平锁          |
| 条件队列     | 每个对象一个等待集合 | 可创建多个 Condition  |
| 状态监控     | 较少                 | 提供多种查询方法      |
| 底层基础     | JVM Monitor          | AQS                   |
| 锁作用域     | 结构化代码块         | 可跨方法，但风险更高  |

## 什么时候优先使用 synchronized

代码只需要简单互斥：

```java
synchronized (lock) {
    // 临界区
}
```

优点：

- 语法简单；
- 自动释放锁；
- 不容易忘记解锁；
- 异常退出时 JVM 自动释放；
- 可读性通常更好。

因此简单同步场景中，不必为了“显得高级”而强行使用 `ReentrantLock`。

## 什么时候使用 ReentrantLock

需要以下能力时：

```text
需要尝试获取锁
需要设置锁等待超时
需要中断正在等待锁的线程
需要公平锁
需要多个 Condition
需要获取锁队列监控信息
需要在不同作用域中控制锁
```

`Lock` 提供的灵活性更强，但也把正确释放锁的责任交给了开发者。

------

# 三十、不要混用 synchronized 和 lock()

下面两个操作没有关系：

```java
synchronized (lock) {
    // 获得的是 lock 对象的 Monitor
}
```

以及：

```java
lock.lock();

try {
    // 获得的是 ReentrantLock 内部同步器的锁
} finally {
    lock.unlock();
}
```

即使这里的 `lock` 是同一个对象，它们使用的也不是同一套加锁机制。

错误理解：

```text
synchronized(lock)
等价于
lock.lock()
```

正确理解：

```text
synchronized(lock)：
获取 lock 对象自身的 Monitor

lock.lock()：
调用 ReentrantLock 内部 AQS 加锁逻辑
```

`Lock` 官方文档明确说明，将 Lock 对象本身作为 `synchronized` 的目标，与调用该对象的 `lock()` 方法没有规定的关联，通常不应混用。

------

# 三十一、锁对象一般应该声明为 final

推荐：

```java
private final ReentrantLock lock =
        new ReentrantLock();
```

不推荐：

```java
private ReentrantLock lock =
        new ReentrantLock();
```

如果锁对象可以被替换：

```java
lock = new ReentrantLock();
```

可能出现：

```text
线程 A 使用旧锁保护共享数据
线程 B 使用新锁保护同一个共享数据
```

两个线程都认为自己获得了锁，但实际上使用的是不同锁，互斥彻底失效。

所以通常应声明：

```java
private final
```

------

# 三十二、实例变量与静态变量应该使用什么锁

## 1. 保护实例数据

```java
public class Counter {

    private int count;

    private final ReentrantLock lock =
            new ReentrantLock();
}
```

每个 `Counter` 对象拥有自己的：

```text
count
lock
```

适合保护实例级数据。

## 2. 保护静态数据

```java
public class GlobalCounter {

    private static int count;

    private static final ReentrantLock LOCK =
            new ReentrantLock();
}
```

因为静态变量被所有实例共享，所以用于保护它的锁也应该被所有实例共享。

错误示例：

```java
private static int count;

private final ReentrantLock lock =
        new ReentrantLock();
```

每个对象都有不同的 `lock`，却共同修改同一个静态 `count`，无法形成完整互斥。

------

# 三十三、保护的不是代码，而是共享资源

锁真正保护的是：

> 多个线程共同访问的数据以及与这些数据相关的不变量。

例如：

```java
private int balance;
private final ReentrantLock lock =
        new ReentrantLock();
```

以下所有访问 `balance` 的操作都应遵循同一规则：

```java
public void deposit(int amount) {
    lock.lock();

    try {
        balance += amount;
    } finally {
        lock.unlock();
    }
}

public void withdraw(int amount) {
    lock.lock();

    try {
        balance -= amount;
    } finally {
        lock.unlock();
    }
}

public int getBalance() {
    lock.lock();

    try {
        return balance;
    } finally {
        lock.unlock();
    }
}
```

不能只给写操作加锁，却让普通读操作完全绕开同步策略，除非使用其他能够提供正确可见性的设计。

------

# 三十四、临界区应该尽量小

不推荐：

```java
lock.lock();

try {
    queryDatabase();
    callRemoteService();
    sleep();
    calculate();
    updateSharedState();
} finally {
    lock.unlock();
}
```

问题是：

- 数据库查询可能很慢；
- 远程接口可能超时；
- `sleep()` 会长时间占锁；
- 其他线程全部被阻塞。

更合理的设计：

```java
Data data = queryDatabase();
Result result = callRemoteService(data);

lock.lock();

try {
    updateSharedState(result);
} finally {
    lock.unlock();
}
```

原则：

> 只把必须保持原子性和一致性的共享状态操作放入临界区。

但也不能机械地把锁拆得过小。

例如：

```java
if (balance >= amount) {
    balance -= amount;
}
```

判断与扣减必须作为整体受锁保护：

```java
lock.lock();

try {
    if (balance >= amount) {
        balance -= amount;
    }
} finally {
    lock.unlock();
}
```

------

# 三十五、避免死锁：固定锁顺序

假设账户转账需要同时获得两把锁：

```text
账户 A 的锁
账户 B 的锁
```

线程一：

```text
先锁 A
再锁 B
```

线程二：

```text
先锁 B
再锁 A
```

可能形成：

```text
线程一持有 A，等待 B
线程二持有 B，等待 A
```

这就是死锁。

更好的方式是固定顺序：

```java
public static void transfer(
        Account from,
        Account to,
        int amount) {

    Account first;
    Account second;

    if (from.getId() < to.getId()) {
        first = from;
        second = to;
    } else {
        first = to;
        second = from;
    }

    first.lock.lock();
    second.lock.lock();

    try {
        if (from.balance < amount) {
            throw new IllegalStateException("余额不足");
        }

        from.balance -= amount;
        to.balance += amount;
    } finally {
        second.lock.unlock();
        first.lock.unlock();
    }
}
```

核心规则：

```text
所有线程按照同一个顺序获取多把锁。
```

释放时通常使用相反顺序：

```text
先获得 A
再获得 B

先释放 B
再释放 A
```

------

# 三十六、使用 tryLock 避免无限死锁等待

也可以使用超时尝试：

```java
boolean firstLocked = false;
boolean secondLocked = false;

try {
    firstLocked =
            firstLock.tryLock(
                    1,
                    TimeUnit.SECONDS
            );

    if (!firstLocked) {
        return;
    }

    secondLocked =
            secondLock.tryLock(
                    1,
                    TimeUnit.SECONDS
            );

    if (!secondLocked) {
        return;
    }

    // 同时持有两把锁
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
} finally {
    if (secondLocked) {
        secondLock.unlock();
    }

    if (firstLocked) {
        firstLock.unlock();
    }
}
```

这样即使拿不到第二把锁，也会在超时后释放第一把锁，而不是无限等待。

不过需要注意：

> `tryLock` 能降低无限等待风险，但不能自动解决所有业务一致性、活锁和重试风暴问题。

------

# 三十七、ReentrantLock 的状态查询方法

## 1. getHoldCount()

```java
int count = lock.getHoldCount();
```

返回：

> 当前线程对这把锁的持有次数。

注意是：

```text
当前线程
```

不是所有线程。

例如：

```java
lock.lock();
lock.lock();

try {
    System.out.println(lock.getHoldCount());
} finally {
    lock.unlock();
    lock.unlock();
}
```

输出：

```text
2
```

如果由其他线程调用，即使锁被某个线程持有，也可能返回：

```text
0
```

## 2. isHeldByCurrentThread()

```java
boolean held =
        lock.isHeldByCurrentThread();
```

判断当前线程是否持有锁。

适合：

- 断言；
- 调试；
- 测试；
- 防御性检查。

```java
private void updateInternal() {
    if (!lock.isHeldByCurrentThread()) {
        throw new IllegalStateException(
                "调用该方法前必须持有锁");
    }

    // 更新内部状态
}
```

## 3. isLocked()

```java
boolean locked = lock.isLocked();
```

判断是否有任意线程持有锁。

但不要这样写同步逻辑：

```java
if (!lock.isLocked()) {
    lock.lock();
}
```

因为：

```text
isLocked() 返回 false
    ↓
线程切换
    ↓
其他线程获得锁
    ↓
当前线程再执行 lock()
```

这属于典型的检查后使用竞争。

应该直接调用：

```java
lock.lock();
```

或者：

```java
lock.tryLock();
```

## 4. isFair()

```java
boolean fair = lock.isFair();
```

判断是否为公平锁。

## 5. hasQueuedThreads()

```java
boolean waiting =
        lock.hasQueuedThreads();
```

判断是否可能有线程正在等待获取锁。

## 6. hasQueuedThread(thread)

```java
boolean queued =
        lock.hasQueuedThread(thread);
```

判断指定线程是否在同步队列中。

## 7. getQueueLength()

```java
int length =
        lock.getQueueLength();
```

返回等待获取锁的线程数量估计值。

注意：

> 它是估计值，不是绝对精确值。

线程可能随时：

- 获得锁；
- 超时；
- 被中断；
- 取消等待；
- 刚刚进入队列。

## 8. Condition 监控方法

```java
lock.hasWaiters(condition);

lock.getWaitQueueLength(condition);
```

调用这些方法时，当前线程通常必须持有对应的锁。

这些查询接口主要用于监控、测试和诊断，不应作为业务同步控制依据；其结果通常只是瞬时状态或近似值。

------

# 三十八、常见错误汇总

## 错误一：没有 finally

```java
lock.lock();
doSomething();
lock.unlock();
```

异常时无法释放锁。

正确：

```java
lock.lock();

try {
    doSomething();
} finally {
    lock.unlock();
}
```

------

## 错误二：tryLock 失败后仍然 unlock

```java
try {
    lock.tryLock();
} finally {
    lock.unlock();
}
```

正确：

```java
boolean acquired = lock.tryLock();

if (acquired) {
    try {
        // 临界区
    } finally {
        lock.unlock();
    }
}
```

------

## 错误三：不同方法使用不同锁保护同一资源

```java
private final ReentrantLock lock1 =
        new ReentrantLock();

private final ReentrantLock lock2 =
        new ReentrantLock();
```

写操作使用 `lock1`，读操作使用 `lock2`，无法互斥。

------

## 错误四：await 使用 if

```java
if (!ready) {
    condition.await();
}
```

正确：

```java
while (!ready) {
    condition.await();
}
```

------

## 错误五：没有持锁就调用 await

```java
condition.await();
```

会抛出：

```text
IllegalMonitorStateException
```

正确：

```java
lock.lock();

try {
    condition.await();
} finally {
    lock.unlock();
}
```

------

## 错误六：没有持锁就调用 signal

```java
condition.signal();
```

也会抛出：

```text
IllegalMonitorStateException
```

正确：

```java
lock.lock();

try {
    ready = true;
    condition.signal();
} finally {
    lock.unlock();
}
```

------

## 错误七：signal 后认为对方立即运行

```java
condition.signal();
```

只表示把等待线程转入锁竞争流程。

当前线程没有释放锁前，对方不能从 `await()` 返回并继续访问临界区。

------

## 错误八：重入两次，只解锁一次

```java
lock.lock();
lock.lock();

try {
    // 临界区
} finally {
    lock.unlock();
}
```

锁仍然被当前线程持有。

------

## 错误九：用实例锁保护静态变量

```java
private static int count;

private final ReentrantLock lock =
        new ReentrantLock();
```

不同对象使用不同锁，却修改同一个静态变量。

------

## 错误十：在持锁期间执行慢操作

```java
lock.lock();

try {
    remoteService.call();
} finally {
    lock.unlock();
}
```

远程调用可能导致大量线程长时间等待锁。

------

## 错误十一：暴露锁对象

不推荐：

```java
public final ReentrantLock lock =
        new ReentrantLock();
```

外部代码可以随意：

```java
object.lock.lock();
```

导致内部锁协议被破坏。

推荐：

```java
private final ReentrantLock lock =
        new ReentrantLock();
```

------

## 错误十二：认为 ReentrantLock 一定比 synchronized 快

锁性能取决于：

- JDK 版本；
- 竞争程度；
- 临界区长度；
- 线程数量；
- CPU 架构；
- 是否频繁阻塞；
- 是否使用公平策略。

不应该仅因为“听说 ReentrantLock 更快”就替换所有 `synchronized`。

选择重点应是：

```text
是否需要 ReentrantLock 提供的扩展能力。
```

------

# 三十九、一个线程安全计数器示例

```java
import java.util.concurrent.locks.ReentrantLock;

public class LockCounter {

    private int count;

    private final ReentrantLock lock =
            new ReentrantLock();

    public void increment() {
        lock.lock();

        try {
            count++;
        } finally {
            lock.unlock();
        }
    }

    public void decrement() {
        lock.lock();

        try {
            count--;
        } finally {
            lock.unlock();
        }
    }

    public int get() {
        lock.lock();

        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}
```

`count++` 并不是原子操作，可以拆成：

```text
读取 count
    ↓
加一
    ↓
写回 count
```

锁保证同一时间只有一个线程执行这组复合操作。

不过单纯计数场景通常也可以考虑：

```java
AtomicInteger
LongAdder
```

具体取决于业务是否只需要单变量原子更新，还是需要保护多个状态之间的整体一致性。

------

# 四十、保护多个变量的一致性

假设订单状态包括：

```java
private int stock = 10;
private int sold = 0;
```

业务要求始终满足：

```text
stock + sold = 10
```

更新时必须整体加锁：

```java
public boolean sell() {
    lock.lock();

    try {
        if (stock <= 0) {
            return false;
        }

        stock--;
        sold++;

        return true;
    } finally {
        lock.unlock();
    }
}
```

使用两个独立的原子变量：

```java
AtomicInteger stock;
AtomicInteger sold;
```

只能保证每个变量自己的更新原子性，不自动保证两个变量之间的整体不变量。

这正是互斥锁的重要用途：

> 保护一组共享状态的整体一致性。

------

# 四十一、ReentrantLock 源码流程总览

## 加锁

```text
lock.lock()
    ↓
Sync.lock()
    ↓
initialTryLock()
    ↓
┌────────────────────────────┐
│ 成功：                     │
│ state 0 → 1                │
│ owner = currentThread      │
└────────────────────────────┘
    或
┌────────────────────────────┐
│ 当前线程已经持锁：         │
│ state++                    │
└────────────────────────────┘
    或
┌────────────────────────────┐
│ 获取失败：                 │
│ AQS.acquire()              │
│ 加入同步队列               │
│ park 等待                  │
└────────────────────────────┘
```

## 解锁

```text
lock.unlock()
    ↓
Sync.release(1)
    ↓
tryRelease(1)
    ↓
state--
    ↓
state 是否为 0？
    │
    ├── 否：仍然处于重入持锁状态
    │
    └── 是：
          owner = null
          唤醒同步队列后继节点
```

## Condition 等待

```text
condition.await()
    ↓
加入 Condition 条件队列
    ↓
保存 state
    ↓
完全释放锁
    ↓
park
    ↓
等待 signal / 中断 / 超时
    ↓
进入 AQS 同步队列
    ↓
重新获取锁
    ↓
恢复之前的 state
    ↓
await 返回
```

------

# 四十二、必须掌握的核心区别

## lock() 与 await()

```text
lock() 获取失败：
线程进入同步队列
线程还没有获得锁

await()：
线程原本已经获得锁
因为业务条件不满足
主动释放锁并进入条件队列
```

## signal() 与 unlock()

```text
signal()：
通知某个等待条件的线程
把它转入同步队列
但当前线程仍然持有锁

unlock()：
当前线程真正减少或释放锁持有权
其他线程才有机会获得锁
```

## park() 与 await()

```text
park()：
底层线程阻塞工具
本身不知道 ReentrantLock 的业务语义

await()：
高级条件等待操作
内部涉及条件队列、释放锁、park、
重新进入同步队列和恢复锁状态
```

------

# 四十三、面试常见问题

## 1. ReentrantLock 为什么叫可重入锁

因为同一个线程已经持有锁时，可以再次成功获得同一把锁。

内部通过：

```text
owner 判断持锁线程
state 记录持有次数
```

实现。

------

## 2. ReentrantLock 默认公平吗

不公平。

```java
new ReentrantLock();
```

等价于：

```java
new ReentrantLock(false);
```

------

## 3. 公平锁一定严格按照线程启动顺序执行吗

不是。

公平性针对的是锁等待队列，不是操作系统线程调度顺序。

------

## 4. tryLock 会遵守公平策略吗

无参数：

```java
tryLock()
```

不会，允许插队。

带时间参数：

```java
tryLock(timeout, unit)
```

公平锁模式下会遵循公平策略。

------

## 5. lock() 能响应中断吗

等待时不会因为中断直接放弃获取锁。

它会继续等待，最终获得锁，并保留或恢复中断信息。

需要中断取消时使用：

```java
lockInterruptibly()
```

------

## 6. unlock 为什么必须写在 finally 中

保证临界区发生异常时仍然释放锁。

------

## 7. Condition.await 会释放锁吗

会完全释放关联锁。

返回之前会重新获得锁，并恢复原来的重入次数。

------

## 8. signal 后线程会立即运行吗

不会。

它只是进入同步队列，仍需重新竞争锁。

------

## 9. 为什么 await 要用 while

因为：

- 可能发生虚假唤醒；
- 被唤醒后，其他线程可能先获得锁并再次改变条件。

------

## 10. ReentrantLock 的底层是什么

AQS：

```text
AbstractQueuedSynchronizer
```

核心包括：

```text
volatile state
owner 线程
同步等待队列
CAS
LockSupport.park/unpark
Condition 条件队列
```

------

## 11. state 在 ReentrantLock 中代表什么

```text
0：未加锁
1：持有一次
2：重入一次
n：当前线程共持有 n 次
```

------

## 12. 公平锁和非公平锁源码区别是什么

公平锁获取前会检查前面是否有排队线程。

非公平锁允许新线程直接 CAS 抢锁。

------

## 13. synchronized 和 ReentrantLock 如何选择

简单互斥：

```text
优先考虑 synchronized
```

需要以下能力：

```text
可中断
超时
尝试获取
公平策略
多个 Condition
锁状态监控
```

考虑 `ReentrantLock`。

------

# 四十四、最终记忆口诀

## 基本使用

```text
先 lock
立刻 try
最后 finally unlock
lock.lock();

try {
    // 临界区
} finally {
    lock.unlock();
}
```

## 可重入

```text
同一线程可以重复加锁
加几次就要释放几次
state 归零才真正释放
```

## 四种获取方式

```text
lock：
必须拿到，不因中断取消

lockInterruptibly：
必须拿到，但可以中断取消

tryLock：
立即尝试，失败就返回

tryLock(timeout)：
最多等待指定时间
```

## 公平性

```text
默认非公平，吞吐量通常更高

公平锁照顾等待较久线程

无参数 tryLock 即使在公平锁中也允许插队
```

## Condition

```text
await：
加入条件队列
完全释放锁
被通知后重新抢锁
获得锁后才返回

signal：
条件队列转同步队列
不会立即运行

await 必须放 while
await 和 signal 都必须先持锁
```

## AQS

```text
state 管锁状态
owner 管持锁线程
CAS 管竞争
同步队列管排队
park 管阻塞
unpark 管唤醒
Condition 管条件等待
```

------

# 四十五、一句话总结

> `ReentrantLock` 是基于 AQS 实现的可重入独占锁，它通过 `state` 记录持有次数、通过 owner 记录持锁线程、通过同步队列管理锁竞争，并额外提供公平策略、可中断获取、超时获取、非阻塞尝试和多个 Condition 条件队列等能力。