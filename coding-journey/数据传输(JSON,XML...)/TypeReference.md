---
title: TypeReference 从入门到原理与实战
description: Java 泛型擦除、反射、匿名子类、Jackson、Fastjson2 与 Spring 的系统学习笔记
created: 2026-10-10
tags:
  - Java
  - 泛型
  - 反射
  - JSON
  - Jackson
  - Fastjson2
  - Spring
---

# TypeReference 从入门到原理与实战

> **定位**：面向已经学过 Java 基础、正在阅读 Spring Boot 后端项目代码的开发者。目标不是背会一个 JSON API，而是弄懂：**为什么需要它、它如何工作、它能做什么、它不能做什么，以及项目里怎么判断该用哪一种。**
>
> **Java 版本**：基础示例尽量兼容 **JDK 8**；涉及 Spring `RestClient` 的代码额外注明版本要求。示例中的 `User`、`ApiResponse` 等均是为了说明问题定义的普通 Java 类。
>
> **阅读顺序**：第一次学习建议先看第 1～7 节，再动手运行第 8、9、10 节；第 11 节以后适合在实际项目中查阅。

[TOC]

---

## 1. 先用 60 秒建立整体认识

### 1.1 一句话解释

**`TypeReference<T>` 是一种“携带完整类型信息的类型描述对象”。**

当你想把 JSON 转成 `List<User>`、`Map<String, User>`、`ApiResponse<List<User>>` 等**带泛型的类型**时，很多 JSON 框架需要从 `TypeReference` 中读取完整的类型信息。

先看最典型的一行：

```java
List<User> users = JSON.parseObject(
    json,
    new TypeReference<List<User>>() {}
);
```

这里的 `TypeReference` **不负责自己解析 JSON**。它提供一个“目标类型说明”：

```text
JSON 字符串
    │
    ▼
JSON.parseObject / ObjectMapper.readValue  ←  TypeReference<List<User>>
    │                                         告诉框架目标类型是 List<User>
    ▼
List<User>（其中的 JSON 对象被反序列化为 User）
```

### 1.2 五个结论先记住

1. **它不是 JDK 标准库提供的 `TypeReference` 类**，而是多个第三方框架分别提供了相似名字的类。
2. **它主要解决“如何向框架描述复杂泛型类型”**，不是让 Java 泛型擦除机制失效。
3. `new TypeReference<List<User>>() {}` 中最后的 `{}` 表示**创建匿名子类**，不是写错了，也不是普通对象初始化块。
4. 在 `TypeReference` 的典型实现中，框架通过反射读取**匿名子类声明的泛型父类**，得到 `List<User>`。
5. **即使使用 `TypeReference`，也不能保证任意 JSON 都能转换成功**：JSON 结构、对象字段、构造方法、反序列化配置等仍然必须匹配。

### 1.3 它们都叫 TypeReference，是约定好的吗？

不是 Java 语言规范要求的统一名称，也不存在所有框架共同继承的 `java.lang.TypeReference`。

| 框架 / 库 | 常见完整类名 | 主要用途 |
|---|---|---|
| Jackson | `com.fasterxml.jackson.core.type.TypeReference` | 向 Jackson 描述反序列化目标类型 |
| Fastjson 1.x | `com.alibaba.fastjson.TypeReference` | 向 Fastjson 1.x 描述复杂类型 |
| Fastjson2 | `com.alibaba.fastjson2.TypeReference` | 向 Fastjson2 描述复杂类型 |
| Hutool | `cn.hutool.core.lang.TypeReference` | 捕获或传递反射中的泛型 `Type` |
| Spring | `org.springframework.core.ParameterizedTypeReference` | 为 HTTP 客户端等提供参数化类型描述 |
| Gson（对照） | `com.google.gson.reflect.TypeToken` | 功能相似，但名称不同 |

原因主要是**设计模式相似、命名语义直观、框架之间互相借鉴**。这种设计通常称为 **super type token（超类型令牌）** 模式。

> **项目中第一件事：看 `import`。** 两个类即使都叫 `TypeReference`，也不是同一个类，不一定能互相传给对方框架的 API。

---

## 2. 为什么会需要 TypeReference？先看问题

### 2.1 场景 A：JSON 是一个普通对象

假设有一个普通实体类：

```java
public class User {
    private Long id;
    private String name;

    public User() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
```

JSON：

```json
{"id": 1, "name": "张三"}
```

如果使用 Jackson：

```java
ObjectMapper mapper = new ObjectMapper();
User user = mapper.readValue(json, User.class);
```

这里的 `User.class` 就能清楚描述目标类型，**完全不需要 TypeReference**。

### 2.2 场景 B：JSON 是一组 User

现在后端返回：

```json
[
  {"id": 1, "name": "张三"},
  {"id": 2, "name": "李四"}
]
```

希望转成：

```java
List<User>
```

你可能想直接写：

```java
List<User>.class    // ❌ Java 语法不允许
```

Java 只能写：

```java
List.class          // ✅ 合法，但没有携带 User 这个类型参数
```

如果给 Jackson 传 `List.class`：

```java
List<?> values = mapper.readValue(json, List.class);
System.out.println(values.get(0).getClass());
```

对于上面这段 JSON，在 Jackson 的常见默认配置下，会打印：

```text
class java.util.LinkedHashMap
```

原因：Jackson 知道要创建一个 `List`，但是不知道每个 `{...}` 应当转换成哪一种 Java 实体对象，于是采用通用的 `Map` 表示 JSON 对象。

于是改成：

```java
List<User> users = mapper.readValue(
    json,
    new TypeReference<List<User>>() {}
);

System.out.println(users.get(0).getClass());
// class 你实际代码中的包名.User
```

这次 Jackson 明确得知元素类型为 `User`，就能按 `User` 的属性规则进行反序列化。

### 2.3 如果只写 List.class，为什么可能 ClassCastException？

下面这段代码通常会给出**未经检查的赋值警告**（即 `unchecked` 警告）：

```java
@SuppressWarnings("unchecked")
List<User> users = mapper.readValue(json, List.class);

User first = users.get(0); // 这里可能抛 ClassCastException
```

因为实际放进列表的是 `LinkedHashMap`；而你告诉 Java 编译器“它是 `List<User>`”，编译器就会在取出元素并赋给 `User` 时插入必要的类型转换。

等价理解：

```java
Object element = users.get(0); // 实际是 LinkedHashMap
User first = (User) element;    // LinkedHashMap 不能直接当 User
```

**重要区分**：错误不是 `List` 自己突然变成了 `User`，而是**容器里的元素类型与代码承诺的不一致**。

---

## 3. Class、Type 和 TypeReference 究竟是什么关系？

这是理解整件事最重要的知识结构。

### 3.1 `Class` 是什么？

平时经常见到：

```java
User.class
String.class
Integer.class
List.class
```

`User.class` 的值是一个 `Class<User>` 对象，描述 `User` 这个类。

例如：

```java
Class<User> userClass = User.class;
System.out.println(userClass.getName());
```

但需要注意：`Class` 描述的是一个类 / 接口 / 数组等的**运行时类型**。对于 `List<User>` 和 `List<String>`，它们通常共用 `List.class` 这个原始类描述，**不存在各自独立的 `List<User>.class`**。

### 3.2 `Type` 是什么？

JDK 提供接口：

```java
java.lang.reflect.Type
```

它是 Java 反射体系中对“类型”的更广泛抽象。

常见相关类型：

```text
java.lang.reflect.Type
├── Class<?>                 → User、String、List 等
├── ParameterizedType         → List<User>、Map<String, User>
├── TypeVariable<?>           → T、E、K、V
├── WildcardType              → ?、? extends Number
└── GenericArrayType          → T[]、List<User>[] 等泛型数组类型
```

特别要理解：**`Class<?>` 本身也实现了 `Type`**。但是不是所有 `Type` 都是 `Class`。

例子：

```java
Type t1 = User.class;  // 一个 Class，也是一个 Type

Type t2 = new TypeReference<List<User>>() {}.getType();
// 通常得到一个 ParameterizedType，表示 List<User>
```

> 第二行 `TypeReference` 的具体 `import` 取决于你用的框架；这里只说明抽象关系。

### 3.3 `TypeReference` 又是什么？

它是一种**“帮你取得并保存 Type 的工具对象”**。它不是 `Type` 接口本身，也不是 Java 编译器的内置能力。

```text
User.class
   │
   ▼
Class<User> ───────────────┐
                           ├── 都能够作为反射类型信息
TypeReference<List<User>> ─┘
   │
   └── getType() → Type（通常是 ParameterizedType）
```

可以把它理解成下面这个关系：

- `Class<User>`：一张只写了 `User` 的类型说明卡。
- `TypeReference<List<User>>`：一个能提供“`List` 中放的是 `User`”这类复杂描述的载体。
- `Type`：框架实际读取或传递的类型描述接口。
- JSON 解析器：拿到类型描述后，真正执行对象创建、字段赋值和类型转换。

### 3.4 对比表

| 写法 | 是否合法 | 表示什么 | 典型场景 |
|---|---|---|---|
| `User.class` | ✅ | `User` 类型 | 普通对象反序列化 |
| `List.class` | ✅ | `List` 原始类型，不含 `User` 信息 | 不关心元素类型时 |
| `List<User>.class` | ❌ | Java 不允许该表达式 | 不适用 |
| `new TypeReference<List<User>>() {}` | ✅ | 可捕获 `List<User>` 的类型描述 | 泛型 JSON 反序列化 |
| `new TypeReference<Map<String, List<User>>>() {}` | ✅ | 可捕获嵌套泛型 | 复杂响应结构 |

---

## 4. 泛型擦除：问题究竟出在哪？

### 4.1 先理解“泛型”

泛型让你在写代码时就声明容器里应该存放什么：

```java
List<String> names = new ArrayList<String>();
List<User> users = new ArrayList<User>();
```

编译器据此限制元素类型、提供代码提示，并在需要时插入类型转换。

例如：

```java
List<String> names = new ArrayList<String>();
names.add("张三");
// names.add(123); // 编译报错：不能把 Integer 放进 List<String>
```

### 4.2 什么叫“擦除”？

Java 泛型主要通过**编译期类型检查 + 编译时擦除类型参数**来实现。

示意（不是逐字节展示真实字节码）：

```java
// 源代码
class Box<T> {
    private T value;
    T getValue() { return value; }
}
```

如果 `T` 没有指定上界，可近似理解为编译后类型参数位置变成 `Object`：

```java
// 帮助理解的近似形式
class Box {
    private Object value;
    Object getValue() { return value; }
}
```

如果 `T extends Number`，则擦除时会以其上界参与类型替换。**实际字节码还会包含必要的强制类型转换、桥接方法等细节。**

### 4.3 运行一个小实验

```java
List<String> a = new ArrayList<String>();
List<Integer> b = new ArrayList<Integer>();

System.out.println(a.getClass() == b.getClass()); // true
System.out.println(a.getClass());                  // class java.util.ArrayList
```

为什么是 `true`？

因为两个对象的运行时类都是 `java.util.ArrayList`。JVM 没有专门创建一个 `ArrayList<String>` 类和另一个 `ArrayList<Integer>` 类。

### 4.4 一个必须避免的误解

**“Java 泛型擦除”不等于“所有位置的泛型文字都彻底消失”。**

准确的理解是：

1. **普通泛型对象的运行时类通常不保留每个实例创建时传入的类型实参**。因此，对象只靠 `getClass()`，通常无法推导某个列表最初写的是 `List<User>`。
2. 但是**某些类、字段、方法、父类和接口的泛型声明**，可以保存在 class 文件的签名元数据中，通过反射 API 读取。
3. `TypeReference` 的常见实现正是**利用第 2 点**，把具体类型写进一个**匿名子类的泛型父类声明**中。

这两点一点都不矛盾。

### 4.5 因而这些写法也不允许

```java
// 错误示意，不能直接编译
List<User>.class
new T()
if (obj instanceof List<User>) { }
```

但如下写法是合法的：

```java
if (obj instanceof List<?>) {
    // 只能确认它是 List，不能借此确认元素都是 User
}
```

**一句话：不能单靠对象的运行时类恢复任意已经擦除的泛型实参。**

---

## 5. 把那段“奇怪语法”完全拆开

经典代码：

```java
new TypeReference<List<User>>() {}
```

它包含四个部分：

| 片段 | 含义 |
|---|---|
| `TypeReference` | 第三方框架提供的类型描述类 |
| `<List<User>>` | 具体要描述的目标泛型 |
| `()` | 调用构造方法 |
| `{}` | 声明一个**匿名子类**的类体 |

### 5.1 为什么后面还有一对 `{}`？

典型的 Jackson `TypeReference` 是**抽象类**，不能直接 `new TypeReference<...>()` 实例化。

但是你可以创建它的子类：

```java
class UserListType extends TypeReference<List<User>> {
}

TypeReference<List<User>> ref = new UserListType();
```

或者直接创建**匿名子类**：

```java
TypeReference<List<User>> ref =
        new TypeReference<List<User>>() {};
```

这两种写法的设计思路相同。匿名子类只是省去了声明命名类的麻烦。

### 5.2 匿名子类为什么非常重要？

因为它的父类声明里明确写着：

```text
匿名类（编译器生成的一个类）
    extends TypeReference<List<User>>
```

框架随后读取这个匿名子类的**泛型父类声明**，就有机会拿到 `List<User>` 这段完整类型信息。

如果只创建普通的 `ArrayList<User>` 实例：

```java
new ArrayList<User>()
```

它的运行时类通常就是 `ArrayList`，不等于我们额外创建了一个捕获 `User` 类型参数的匿名子类。

但如果写：

```java
new ArrayList<User>() {}
```

这同样创建了一个匿名子类，它的泛型父类声明中也可以体现 `ArrayList<User>`。区别只是，**我们通常使用专门的 TypeReference/TypeToken 类来规范化这一模式。**

### 5.3 这是不是“双大括号初始化”？

**不是。**

```java
new TypeReference<List<User>>() {}
```

这里只有一个匿名类的类体 `{}`。

而下面才是常说的“双大括号初始化”，里面还包含一个实例初始化块：

```java
Map<String, Integer> scores = new HashMap<String, Integer>() {{
    put("A", 100);
}};
```

两者不要混淆。

### 5.4 为什么有时有人写 `new TypeReference<>() {}`？

Java 9 起放宽了在匿名内部类中使用菱形语法 `<>` 的限制；**如果你要兼容 JDK 8，就使用显式的泛型参数写法**：

```java
new TypeReference<List<User>>() {} // JDK 8 可用
```

本笔记统一采用这种写法，以便在老项目里直接参考。

---

## 6. 背后的核心反射：`getGenericSuperclass()`

理解下列两种方法的区别：

```java
getClass().getSuperclass()
getClass().getGenericSuperclass()
```

- `getSuperclass()`：拿到**原始的直接父类 `Class`**；
- `getGenericSuperclass()`：拿到**包含泛型声明信息的直接父类 `Type`**（如果有）。

对于一个匿名子类：

```java
TypeReference<List<User>> ref =
        new TypeReference<List<User>>() {};
```

概念上：

```text
ref.getClass()                        → 某个编译器生成的匿名类
ref.getClass().getSuperclass()        → TypeReference.class
ref.getClass().getGenericSuperclass() → TypeReference<List<User>>
```

再往下走：

```java
Type genericParent = ref.getClass().getGenericSuperclass();
ParameterizedType parent = (ParameterizedType) genericParent;
Type targetType = parent.getActualTypeArguments()[0];
```

最终得到的 `targetType` 描述：

```text
java.util.List<你的包名.User>
```

**为什么要 `getActualTypeArguments()[0]`？**

`TypeReference<T>` 的父类泛型参数只有一个 `T`。在匿名子类里，它被填成了 `List<User>`，因此父类实际类型参数数组的第 0 项就是 `List<User>`。

对于更复杂的 `Map<String, List<User>>`，第 0 项则代表**整个 `Map<String, List<User>>`**，不是只代表 `String` 或 `User`。

---

## 7. 看懂 TypeReference 的简化源码

为了看清思想，先不要把 Jackson 的其他细节混进来。我们自己实现一个教学用版本：

```java
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class MiniTypeReference<T> {

    private final Type type;

    protected MiniTypeReference() {
        // 1. 取得当前匿名子类的泛型父类
        Type genericParent = getClass().getGenericSuperclass();

        // 2. 确认父类描述带有具体的泛型声明
        if (!(genericParent instanceof ParameterizedType)) {
            throw new IllegalArgumentException("缺少父类泛型信息");
        }

        // 3. 取出 TypeReference<T> 中的 T 对应的实际类型
        ParameterizedType parameterizedType =
                (ParameterizedType) genericParent;

        this.type = parameterizedType.getActualTypeArguments()[0];
    }

    public Type getType() {
        return type;
    }
}
```

按步骤执行：

```java
MiniTypeReference<List<User>> ref =
        new MiniTypeReference<List<User>>() {};

Type type = ref.getType();
System.out.println(type.getTypeName());
```

得到描述 `List<User>` 的 `Type`。

这段小代码的核心和真实 Jackson `TypeReference` 的捕获方式相似：

```java
Type superClass = getClass().getGenericSuperclass();
_type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
```

> **注意**：`MiniTypeReference` 是解释原理的玩具实现，不是对 Jackson、Fastjson2、Spring 全部功能的完整复刻。实际框架还包含边界检查、类型规范化、类型解析等其他逻辑；多层继承和类型变量的特殊情况也需要额外处理。

### 7.1 最重要的设计细节

```java
new MiniTypeReference<List<User>>() {}
```

保存的信息**不是**：

- 这个 List 中已经存在多少个元素；
- 某个 List 实例当前元素的真实类型；
- 当前 JVM 里的所有 `User` 对象。

保存的信息是：

> “我创建的这个匿名子类，在源码 / class 元数据里声明了自己的泛型父类是 `MiniTypeReference<List<User>>`。”

换言之，**它读取的是类型声明，不是去扫描对象里的数据。**

---
## 8. 动手实验一：不依赖任何第三方库，观察泛型信息

**强烈建议亲手运行一次。** 这是整篇最能把抽象知识变具体的实验。

### 8.1 创建 `TypeReferenceReflectionLab.java`

以下代码是**完整 Java 文件**，只使用 JDK 反射包。为了让你复制后就能运行，把需要的类全部写在同一个文件里。

```java
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TypeReferenceReflectionLab {

    // 一个普通的测试类
    static class User {
    }

    // 为学习原理而实现的简化版 TypeReference
    static abstract class MiniTypeReference<T> {
        private final Type type;

        protected MiniTypeReference() {
            Type superType = getClass().getGenericSuperclass();
            if (!(superType instanceof ParameterizedType)) {
                throw new IllegalArgumentException("请通过带泛型的子类创建对象");
            }
            this.type = ((ParameterizedType) superType)
                    .getActualTypeArguments()[0];
        }

        public Type getType() {
            return type;
        }
    }

    public static void main(String[] args) {
        // 实验 1：两个不同泛型参数的 ArrayList，运行时类一样
        List<String> strings = new ArrayList<String>();
        List<Integer> numbers = new ArrayList<Integer>();
        System.out.println("实验1：" +
                (strings.getClass() == numbers.getClass()));

        // 实验 2：通过匿名子类捕获 List<User>
        MiniTypeReference<List<User>> listRef =
                new MiniTypeReference<List<User>>() {};
        System.out.println("实验2：" + listRef.getType().getTypeName());

        // 实验 3：观察普通父类与泛型父类的差异
        System.out.println("实验3-普通父类：" +
                listRef.getClass().getSuperclass());
        System.out.println("实验3-泛型父类：" +
                listRef.getClass().getGenericSuperclass());

        // 实验 4：拆解复杂类型 Map<String, List<User>>
        MiniTypeReference<Map<String, List<User>>> mapRef =
                new MiniTypeReference<Map<String, List<User>>>() {};
        Type mapType = mapRef.getType();
        ParameterizedType mapPT = (ParameterizedType) mapType;
        System.out.println("实验4-原始类型：" + mapPT.getRawType());
        System.out.println("实验4-Key类型：" +
                mapPT.getActualTypeArguments()[0]);
        System.out.println("实验4-Value类型：" +
                mapPT.getActualTypeArguments()[1]);

        // 实验 5：泛型方法中的 T 不会自动变成 User
        showGenericMethodTrap(User.class);
    }

    private static <T> void showGenericMethodTrap(Class<T> clazz) {
        MiniTypeReference<List<T>> ref =
                new MiniTypeReference<List<T>>() {};
        System.out.println("实验5-传入Class：" + clazz.getTypeName());
        System.out.println("实验5-捕获的类型：" + ref.getType().getTypeName());
    }
}
```

### 8.2 运行方法

IDEA：在类里的 `main` 左边点击绿色运行按钮即可。

或者在终端运行：

```bash
javac -encoding UTF-8 TypeReferenceReflectionLab.java
java TypeReferenceReflectionLab
```

关键观察点：

```text
实验1：true
实验2：java.util.List<TypeReferenceReflectionLab$User>
实验3-普通父类：class TypeReferenceReflectionLab$MiniTypeReference
实验3-泛型父类：TypeReferenceReflectionLab$MiniTypeReference<java.util.List<TypeReferenceReflectionLab$User>>
实验4-原始类型：interface java.util.Map
实验4-Key类型：class java.lang.String
实验4-Value类型：java.util.List<TypeReferenceReflectionLab$User>
实验5-传入Class：TypeReferenceReflectionLab$User
实验5-捕获的类型：java.util.List<T>
```

> 实际输出的包名前缀、内部类符号、空格等可能因你放置的包名和 JDK 展示形式略有差别。**重点看实验 3 和实验 5 的区别。**

### 8.3 实验 3 说明什么？

`getSuperclass()` 只能得到 `MiniTypeReference` 这个类。

`getGenericSuperclass()` 却可以得到：

```java
MiniTypeReference<List<User>>
```

**这就是 TypeReference 能工作的直接证据。**

### 8.4 实验 5 为什么是 `List<T>` 而不是 `List<User>`？

我们给方法传入的是：

```java
User.class
```

但匿名子类**在方法源代码中声明的**父类类型是：

```java
MiniTypeReference<List<T>>
```

这里的 `T` 是一个**类型变量**，不等于编译器生成了一个 `List<User>` 的新类签名。因此反射通常得到的是 `List<T>`，不会因为方法运行时传了 `User.class` 就自动替换为 `List<User>`。

这也是实际项目里非常常见的一类坑。后文会单独展开解决方法。

---

## 9. 动手实验二：Jackson 的完整可运行示例

Jackson 在 Spring Boot 项目中很常见。你可能平时没显式写过 `ObjectMapper`，但 Spring 的 HTTP 消息转换机制可能已经在后台使用它。

### 9.1 依赖与环境

普通 Maven 项目可以加入：

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.22.3</version>
</dependency>
```

> 这里用的是写文档时可查到的 Jackson 2.x 版本示例。**如果已经在 Spring Boot 项目中使用依赖管理（BOM/parent），通常应由项目统一管理 Jackson 版本，不要为了运行示例擅自覆盖生产项目中的版本。**
>
> 如果项目已有 Jackson，只要能导入 `com.fasterxml.jackson.databind.ObjectMapper` 和 `com.fasterxml.jackson.core.type.TypeReference`，通常不必重复添加依赖。

### 9.2 完整示例：`JacksonTypeReferenceDemo.java`

这个例子把单个对象、列表、嵌套列表和错误示范放到一个 `main` 里。

```java
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

public class JacksonTypeReferenceDemo {

    // 定义在一个文件里，方便直接复制运行
    public static class User {
        private Long id;
        private String name;

        public User() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        @Override
        public String toString() {
            return "User{id=" + id + ", name='" + name + "'}";
        }
    }

    public static class ApiResponse<T> {
        private Integer code;
        private String message;
        private T data;

        public ApiResponse() {}

        public Integer getCode() { return code; }
        public void setCode(Integer code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
    }

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        String userJson = "{\"id\":1,\"name\":\"张三\"}";
        String listJson = "["
                + "{\"id\":1,\"name\":\"张三\"},"
                + "{\"id\":2,\"name\":\"李四\"}"
                + "]";

        // 1. 普通对象：用 Class 就够了
        User single = mapper.readValue(userJson, User.class);
        System.out.println("单个用户：" + single);

        // 2. 错误思路：List.class 不能指定 User
        List<?> rawList = mapper.readValue(listJson, List.class);
        System.out.println("只传 List.class 时的元素类型："
                + rawList.get(0).getClass());

        // 3. 正确思路：声明 List<User>
        List<User> users = mapper.readValue(
                listJson,
                new TypeReference<List<User>>() {}
        );
        System.out.println("TypeReference 元素类型："
                + users.get(0).getClass());
        System.out.println("第一个用户：" + users.get(0));

        // 4. 模拟典型的统一响应结构：ApiResponse<List<User>>
        String responseJson = "{\"code\":0,\"message\":\"ok\","
                + "\"data\":" + listJson + "}";

        ApiResponse<List<User>> response = mapper.readValue(
                responseJson,
                new TypeReference<ApiResponse<List<User>>>() {}
        );
        System.out.println("响应码：" + response.getCode());
        System.out.println("响应里的第一个用户名："
                + response.getData().get(0).getName());

        // 5. Map<String, List<User>> 的嵌套泛型
        String mapJson = "{\"teamA\":" + listJson + "}";
        Map<String, List<User>> grouped = mapper.readValue(
                mapJson,
                new TypeReference<Map<String, List<User>>>() {}
        );
        System.out.println("teamA 人数：" + grouped.get("teamA").size());
    }
}
```

### 9.3 预期的关键信息

```text
单个用户：User{id=1, name='张三'}
只传 List.class 时的元素类型：class java.util.LinkedHashMap
TypeReference 元素类型：class JacksonTypeReferenceDemo$User
第一个用户：User{id=1, name='张三'}
响应码：0
响应里的第一个用户名：张三
teamA 人数：2
```

### 9.4 为什么这里的嵌套类要写成 `static`？

为了让 JSON 框架容易创建对应对象。非 `static` 的成员内部类通常隐式持有外部类实例，可能使反序列化构造变得复杂。

这里的 `public static class User` 和 `public static class ApiResponse<T>` 只是为了**单文件演示**。实际 Spring Boot 工程中，建议把实体类和统一响应类分别放到正常的 `.java` 文件中。

### 9.5 Jackson 接收的是 TypeReference，但真正的转换是谁做的？

```java
mapper.readValue(json, new TypeReference<List<User>>() {})
```

逐步理解：

```text
new TypeReference<List<User>>() {}
            │
            ▼
对象保存代表 List<User> 的 Type
            │
            ▼
ObjectMapper.readValue(...)
            │
            ▼
Jackson 的类型系统把它解析为可用于绑定的类型描述
            │
            ▼
根据 JSON + 类型描述构造 List，并反序列化 User
```

**TypeReference 是提供信息的，ObjectMapper 才是干活的。**

---

## 10. 动手实验三：Fastjson2 的写法

如果你的项目引入了 Fastjson2，核心用法非常接近 Jackson，但类名、包名和反序列化入口不同。

### 10.1 Maven 依赖

```xml
<dependency>
    <groupId>com.alibaba.fastjson2</groupId>
    <artifactId>fastjson2</artifactId>
    <version>2.0.65</version>
</dependency>
```

> 版本用于演示。真实项目优先遵守现有 BOM/版本管理及安全升级流程。Fastjson2 在 2026 年 9 月发布的 2.0.65 包含 AutoType 相关安全修复：解析不可信 JSON 时尤其不要随意打开 `SupportAutoType`。

### 10.2 使用示例

```java
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

import java.util.List;
import java.util.Map;

public class Fastjson2Demo {

    public static class User {
        private Long id;
        private String name;

        public User() {}
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static void main(String[] args) {
        String json = "[{\"id\":1,\"name\":\"张三\"}]";

        List<User> users = JSON.parseObject(
                json,
                new TypeReference<List<User>>() {}
        );

        System.out.println(users.get(0).getName()); // 张三

        String mapJson = "{\"group\":" + json + "}";
        Map<String, List<User>> grouped = JSON.parseObject(
                mapJson,
                new TypeReference<Map<String, List<User>>>() {}
        );
        System.out.println(grouped.get("group").get(0).getName());
    }
}
```

### 10.3 Fastjson 1.x 和 Fastjson2 要分清

常见 `import` 对照：

```java
// Fastjson 1.x
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
```

```java
// Fastjson2
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
```

**它们不是简单修改一位数字就完全相同的 API。** 项目迁移、依赖兼容和安全配置都要分别检查。

如果使用历史遗留 Fastjson 1.x，请先确认真实依赖版本、框架约定与安全策略，不要仅为了一行示例就往项目中额外混入一套 JSON 库。

### 10.4 Jackson 和 Fastjson2 能不能混着传 TypeReference？

一般**不能直接混传**：

```java
// ❌ 示例：这里的 TypeReference 是 Fastjson2 的
com.alibaba.fastjson2.TypeReference<List<User>> fastjsonRef =
        new com.alibaba.fastjson2.TypeReference<List<User>>() {};

// Jackson 没有接收这个 Fastjson2 TypeReference 的对应重载
// mapper.readValue(json, fastjsonRef); // 编译不通过
```

尽管多个库的 `TypeReference` 都可以提供 `java.lang.reflect.Type`，**接收它们的 API 参数类型仍然是不同的 Java 类**。

若确实需要跨库转接，可以研究目标框架接受 `Type` 或自身类型描述对象的重载；但正常业务代码更推荐**统一选用项目已经采用的 JSON 框架**。

---

## 11. Spring 为什么又有一个 ParameterizedTypeReference？

Spring 使用的是：

```java
org.springframework.core.ParameterizedTypeReference<T>
```

名字长一点，但其核心用途同样是**捕获并传递泛型 `Type`**。Spring 把它用于 HTTP 客户端等需要保留完整目标类型的场景。

### 11.1 RestTemplate：项目里常见的用法

假设远程接口返回：

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    {"id": 1, "name": "张三"}
  ]
}
```

接口包装类：

```java
public class ApiResponse<T> {
    private Integer code;
    private String message;
    private T data;

    public ApiResponse() {}
    // 这里省略与字段一一对应的 getter / setter
}
```

在已有 Spring HTTP 消息转换器配置的工程里，可以这样调用：

```java
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

RestTemplate restTemplate = new RestTemplate();

ResponseEntity<ApiResponse<List<User>>> response =
        restTemplate.exchange(
                "https://api.example.com/users",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<List<User>>>() {}
        );

ApiResponse<List<User>> body = response.getBody();
```

这里将 `ApiResponse<List<User>>` 传给 Spring，Spring 再把这种类型信息用于实际的响应读取和消息转换。

> 注意：`https://api.example.com/users` 是讲解用占位 URL，不是需要你实际发请求的服务。`ApiResponse`、`User` 的字段和访问器也需要在工程里真实存在。
>
> **版本提醒**：`RestTemplate` 在老 Spring Boot 项目里很常见；Spring Framework 7 已将其标记为弃用，新的同步客户端通常优先考虑 `RestClient`。不要因此机械地在旧项目中大范围替换。

### 11.2 RestClient：Spring Framework 6.1+ 的风格

```java
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;

RestClient client = RestClient.create("https://api.example.com");

ApiResponse<List<User>> result = client.get()
        .uri("/users")
        .retrieve()
        .body(new ParameterizedTypeReference<ApiResponse<List<User>>>() {});
```

`RestClient` 是 Spring Framework 6.1 起引入的客户端 API；Spring Framework 6.x 基于 Java 17，所以**不能直接把这段代码当作 JDK 8 老项目的通用示例**。

### 11.3 WebClient：响应式接口中的相似用法

```java
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

WebClient client = WebClient.create("https://api.example.com");

Mono<List<User>> mono = client.get()
        .uri("/users")
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<User>>() {});
```

`bodyToMono` 需要知道响应体的目标类型。这里的 `ParameterizedTypeReference<List<User>>` 就提供了这一信息。

> `Mono` 的异步 / 响应式处理是另一个知识点；此处关注类型信息，不建议为了学习 `TypeReference` 就在不熟悉的项目里到处调用 `.block()`。

### 11.4 为什么写 Spring Controller 时不一定需要它？

你可能早就写过：

```java
@PostMapping("/users")
public void saveUsers(@RequestBody List<User> users) {
    // ...
}
```

却没有手动写 `TypeReference`。

原因是**框架已经能从 Controller 方法的参数签名读取 `List<User>` 的泛型声明**（通过方法参数和反射相关机制），所以你不必再把它手动包装成类型令牌。

这和匿名子类类型令牌的思想并不冲突：

- **你写在方法签名里的泛型**：Spring 可以读取声明。
- **你在某个 JSON 解析方法调用现场临时传递复杂类型**：`TypeReference` / `ParameterizedTypeReference` 是一种方便的类型描述方式。

---
## 12. 进阶：当具体泛型参数要到运行时才知道，怎么办？

这是 `TypeReference` 最容易被误用的地方，**尤其是在封装通用工具方法的时候**。

### 12.1 典型错误：在泛型方法内部直接写 `TypeReference<T>`

假设你想实现一个万能的 Jackson JSON 工具方法：

```java
public static <T> T parse(String json) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(json, new TypeReference<T>() {});
}
```

乍一看很漂亮：调用者需要什么类型，`T` 就是什么类型。

但是，**方法体中的匿名子类只声明了 `TypeReference<T>`**，其中的 `T` 仍然是类型变量。

这不意味着框架一定会“神奇地知道”调用者在某个地方想要的是 `User` 或 `List<User>`。对于无法正确解析的类型变量，反序列化行为可能退化为通用 `Object` / `Map`，也可能因框架及上下文不同而失败。

**编译器对返回值的推断，不等于框架在运行时拿到了完整类型信息。**

### 12.2 如果目标是普通类：显式传 `Class<T>`

正确而简单的工具方法：

```java
public static <T> T parseObject(
        String json,
        Class<T> targetClass
) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(json, targetClass);
}
```

调用：

```java
User user = parseObject(json, User.class);
```

这样就把 `User.class` **明确传给运行中的方法**，而不是寄希望于 Java 自动恢复已经擦除的类型实参。

> 在生产项目里，Jackson 的 `ObjectMapper` 通常应作为可复用、经过统一配置的组件管理；上面为教学简化了创建过程。

### 12.3 如果目标是 `List<T>`：使用 Jackson 的 `JavaType`

Jackson 提供 `TypeFactory`，可以在运行时**组合类型**：

```java
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class JsonUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static <T> List<T> parseList(
            String json,
            Class<T> elementClass
    ) throws Exception {
        JavaType listType = MAPPER.getTypeFactory()
                .constructCollectionType(List.class, elementClass);

        return MAPPER.readValue(json, listType);
    }
}
```

调用：

```java
List<User> users = JsonUtil.parseList(json, User.class);
```

解析器明确拿到了：

```text
List.class + User.class
       ↓
JavaType: List<User>
```

这才是“类型直到方法运行时才决定”时更合适的处理方案。

### 12.4 如果目标是 `ApiResponse<List<T>>`：继续组合 JavaType

统一响应格式：

```java
public class ApiResponse<T> {
    private Integer code;
    private T data;
    // 省略 getter / setter / 无参构造
}
```

在 `ObjectMapper` 已正确配置、实体定义完整的前提下：

```java
public static <T> ApiResponse<List<T>> parseListResponse(
        String json,
        Class<T> elementClass,
        ObjectMapper mapper
) throws Exception {
    JavaType listType = mapper.getTypeFactory()
            .constructCollectionType(List.class, elementClass);

    JavaType responseType = mapper.getTypeFactory()
            .constructParametricType(ApiResponse.class, listType);

    return mapper.readValue(json, responseType);
}
```

这样 `ApiResponse<List<User>>` 是在运行时**用明确的 `User.class` 组合得到的**，不会因为方法定义中的 `T` 未解析而丢失具体元素类型。

### 12.5 已经有 `Type`，不一定要重新 new TypeReference

例如，在 Jackson 中：

```java
Type type = new TypeReference<List<User>>() {}.getType();
JavaType javaType = mapper.getTypeFactory().constructType(type);

List<User> users = mapper.readValue(json, javaType);
```

这里展示了从 `TypeReference` 提供的反射 `Type`，再转换为 Jackson 内部 `JavaType` 的过程。

Spring 的 `ParameterizedTypeReference.forType(type)` 也可将一个已经得到的 `Type` 包装成 Spring 的类型引用对象（具体 API 取决于项目使用的 Spring 版本）。

### 12.6 从字段声明中获取 Type

其实 Java 反射还可以直接从字段的泛型签名获取类型：

```java
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;

class Holder {
    private List<User> users;
}

Field field = Holder.class.getDeclaredField("users");
Type type = field.getGenericType();
System.out.println(type.getTypeName()); // java.util.List<User>（含实际包名）
```

这个 `Type` 来自字段声明，而不是 TypeReference。

所以你应当建立一个更完整的概念：

> **TypeReference 是得到复杂 `Type` 的一种方便方式，不是 Java 世界唯一的类型信息来源。**

---

## 13. 项目里最常见的 10 个坑

### 坑 1：用 `List.class` 转换，再强转成 `List<User>`

**危险示例**：

```java
List<User> users = (List<User>) mapper.readValue(json, List.class);
```

即使强转这一刻看起来成功，也不能证明元素真的是 `User`。泛型的强制转换通常无法检查所有元素，真正报错可能发生在 `users.get(0)` 被当成 `User` 使用时。

**建议**：写明 `new TypeReference<List<User>>() {}`，或者使用框架对应的参数化类型构造 API。

### 坑 2：只看类名，不看 `import`

```java
import com.alibaba.fastjson2.TypeReference;
```

和：

```java
import com.fasterxml.jackson.core.type.TypeReference;
```

是两个不同的类。如果 IDEA 提示“参数类型不匹配”，优先检查导包，而不是改一堆泛型。

### 坑 3：在通用泛型方法中用 `new TypeReference<T>() {}`

**根因**：匿名子类的签名可能只记录 `T`，而不是调用者期望的实际类型。

**处理**：给方法增加 `Class<T>`、`Type`、`JavaType` 或由调用者提供正确的 TypeReference；不要以为 Java 会自动把方法泛型“填”进反射签名。

### 坑 4：JSON 结构和目标 Java 类型根本不匹配

JSON 实际是统一响应对象：

```json
{
  "code": 0,
  "data": [{"id": 1, "name": "张三"}]
}
```

你却写：

```java
new TypeReference<List<User>>() {}
```

JSON 顶层是 `{...}` 对象，不是 `[...]` 数组。TypeReference 并不能把顶层对象自动“掰成”数组。

**应该描述完整结构**：

```java
new TypeReference<ApiResponse<List<User>>>() {}
```

### 坑 5：以为 TypeReference 就能把对象随意强转

**不能。** 它不等于：

```java
(User) someLinkedHashMap
```

强转和 JSON 反序列化是两回事。

- **强转**：要求现有对象本身在运行时就是目标类型或其子类型。
- **反序列化 / 数据绑定**：根据源数据的结构和目标类定义创建或填充新对象。

如果手里已经是通用 `Map`，Jackson 可以考虑：

```java
User user = mapper.convertValue(map, User.class);
```

这是数据转换，不是把 `LinkedHashMap` 原地变成 `User`。

### 坑 6：忘了 POJO 的反序列化条件

例如：

- 字段名称对不上；
- 没有适合的构造器 / 创建器；
- Getter / Setter、字段可见性设置不符合框架要求；
- JSON 中是字符串，Java 里却要求一个无法转换的复杂对象；
- 枚举值与定义不匹配；
- 时间字段没有配置好格式或模块。

这些都不是 `TypeReference` 单独能解决的。

### 坑 7：TypeReference 里只写最外层类型

```java
new TypeReference<ApiResponse>() {} // 使用原始类型
```

这会丢失 `ApiResponse` 的 `T` 信息。如果 `data` 是一个包含对象的列表，可能又得到 `Map` 或其他通用表示。

应该写：

```java
new TypeReference<ApiResponse<List<User>>>() {}
```

每一层泛型都应与实际 JSON 模型匹配。

### 坑 8：把“类型声明可反射”和“所有实例都保存泛型实参”混为一谈

```java
List<User> users = new ArrayList<User>();
```

这不意味着以后任何人拿到 `users.getClass()` 都能恢复 `User`。

`TypeReference` 起效，是因为你创建了**具有泛型父类声明的额外匿名子类**，反射的是**声明元数据**。

### 坑 9：在热路径反复创建匿名 TypeReference，以为是零成本

```java
for (...) {
    TypeReference<List<User>> ref =
            new TypeReference<List<User>>() {};
    // ...
}
```

通常没有必要反复创建相同类型的引用对象。对固定的类型，可以考虑复用：

```java
private static final TypeReference<List<User>> USER_LIST_TYPE =
        new TypeReference<List<User>>() {};
```

不过应以真实性能数据为依据，**不要把这点对象分配优化当成首要问题**。反序列化自身、网络和数据库开销往往更重要。

### 坑 10：把它当成处理不可信 JSON 的安全防护

**TypeReference 不是安全边界，也不是 JSON 结构校验器。**

它只是告诉框架“希望得到什么 Java 类型”。解析外部输入仍要遵守：

- 不信任客户端直接传来的类名 / 类型信息；
- 不随意启用危险的多态反序列化功能；
- 使用维护中的框架版本并及时跟进安全修复；
- 对关键业务字段进行显式验证（如 Bean Validation 或业务校验）；
- 限制不可信输入大小、深度以及不必要的复杂结构。

---

## 14. 深一点：五种 Type 实现，到底怎么分辨？

当你在 IDEA 中进入源码或者调试 `ref.getType()` 时，很可能会见到：

```java
Type
ParameterizedType
TypeVariable
WildcardType
GenericArrayType
Class
```

不要慌，可以这样理解：

| 类型 | 典型 Java 写法 | 看点 |
|---|---|---|
| `Class` | `User`、`String`、`int[]` | 已经有明确运行时类 |
| `ParameterizedType` | `List<User>` | 有原始类型以及具体泛型实参 |
| `TypeVariable` | `T`、`E` | 代表类型变量，不代表已经解析出的 `User` |
| `WildcardType` | `? extends Number` | 通配符及上下界 |
| `GenericArrayType` | `T[]`、`List<User>[]` | 组件类型涉及泛型的数组声明 |

### 14.1 `ParameterizedType` 的三个常用方法

```java
ParameterizedType pt = (ParameterizedType) type;

Type rawType = pt.getRawType();
Type[] args = pt.getActualTypeArguments();
Type ownerType = pt.getOwnerType();
```

以 `Map<String, List<User>>` 为例：

```text
rawType    = Map
args[0]    = String
args[1]    = List<User>
ownerType  = 通常为 null（Map 是顶级接口）
```

如果继续拆 `args[1]`：

```java
ParameterizedType nested =
        (ParameterizedType) pt.getActualTypeArguments()[1];

System.out.println(nested.getRawType());
// interface java.util.List

System.out.println(nested.getActualTypeArguments()[0]);
// class 你的包名.User
```

你会发现，`Type` 实际上可以表示一棵**嵌套的类型结构树**：

```text
Map<String, List<User>>
│
├── rawType: Map
├── Key: String
└── Value: List<User>
           │
           ├── rawType: List
           └── 元素: User
```

### 14.2 为什么 `Type` 不一定能直接强转成 `Class`？

以下代码是错误思路：

```java
Type type = new TypeReference<List<User>>() {}.getType();
Class<?> clazz = (Class<?>) type; // 通常 ClassCastException
```

因为 `type` 往往是 `ParameterizedType`，而不是 `Class`。

需要根据实际类型判断：

```java
if (type instanceof Class<?>) {
    Class<?> clazz = (Class<?>) type;
    System.out.println(clazz.getName());
} else if (type instanceof ParameterizedType) {
    ParameterizedType pt = (ParameterizedType) type;
    System.out.println(pt.getRawType());
}
```

### 14.3 为什么叫“类型令牌”？

`TypeReference`、`TypeToken` 这类类的本质，是用一个对象**代表另一种类型**，好让方法能接收它。

比方说：

```java
void parse(String json, /* 这里必须能接收某种类型描述 */)
```

Java 不能把 `List<User>` 当作一个可以直接传递的值，但可以传递：

```java
List.class
new TypeReference<List<User>>() {}
Type
JavaType
```

这就是“拿一个对象作为类型的令牌”的基本思想。

---

## 15. 常见疑问 FAQ

### Q1. `TypeReference` 是 Java 原生类吗？

**不是。** JDK 提供的是 `Type`、`Class`、`ParameterizedType` 等反射类型能力。多个第三方框架各自设计了 `TypeReference` 或类似类型令牌。

### Q2. Jackson 和 Fastjson2 的 TypeReference 是同一个类吗？

不是。完整包名不同，类型不同，方法和实现细节也可能不同。**看 `import` 最可靠。**

### Q3. 为什么不给 Java 增加 `List<User>.class`？

Java 泛型和运行时类型模型采用了类型擦除设计，`List<User>` 不会对应一个与 `List<String>` 完全独立的运行时类。`.class` 语法不能直接表达这种带具体类型实参的参数化类型。

### Q4. 用了 `TypeReference` 就不会有类型擦除了吗？

不会。**擦除机制仍然存在**。它利用匿名子类的泛型父类声明元数据提供额外的类型描述。

### Q5. 最后的 `{}` 能不能去掉？

对于典型的 Jackson / Spring 抽象类型令牌写法，不能简单去掉。`{}` 创建匿名子类，既让抽象类能够被具体化，也让框架拿到匿名子类的泛型父类声明。

### Q6. `new TypeReference<User>() {}` 有意义吗？

**可以用，但通常没必要。** 如果 API 同时支持 `User.class`，普通实体对象直接用 `Class` 更简单清晰。

### Q7. `new TypeReference<List<String>>() {}` 和 `List<User>` 有何区别？

元素的目标类型不同。框架会按 `String` 或 `User` 的规则反序列化元素；如果 JSON 不符合目标类型，可能失败或发生框架允许的类型转换。

### Q8. `TypeReference` 是否只给 JSON 用？

不是。它是一种**捕获和传递泛型类型信息**的设计模式。凡是需要运行时类型描述的框架都有可能采用类似方案，如 HTTP 客户端、通用转换工具等。

### Q9. 为什么 Spring MVC 的 `@RequestBody List<User>` 不必手写 TypeReference？

Spring 读取 Controller 方法参数签名中已经声明的泛型信息，再交给消息转换器使用。你不需要额外提供匿名子类类型令牌。

### Q10. `TypeReference` 和 `Class<T>` 哪个更好？

**没有谁“更高级”**。具体类型是普通 `User` 时优先 `Class<User>`；需要临时传递 `List<User>`、`ApiResponse<List<User>>` 等完整泛型类型时使用 `TypeReference`；需要运行时动态组合类型时，Jackson 的 `JavaType` 等类型构建方式可能更合适。

### Q11. `TypeReference` 与泛型通配符 `<?>` 是一回事吗？

不是。`<?>` 是 Java 泛型语法，用于表示未知类型参数；`TypeReference` 是某个库中的 Java 类。可以出现 `TypeReference<List<?>>`，但此时列表元素具体是什么类型仍未明确。

### Q12. 我只想把 `JSONObject` 变成 `User`，是不是也用 TypeReference？

未必。Fastjson2 有按目标类转换的 API；Jackson 有 `convertValue(source, User.class)`。**已经拿到的内存对象和 JSON 字符串，处理路径不完全一样**。先确认你的数据类型，再挑选对应 API。

### Q13. IDE 能推断左侧 `List<User>`，JSON 框架为什么不能直接知道？

IDE 的代码提示 / Java 编译器的泛型推断发生在**编译阶段**；解析 JSON 的代码在**运行时**执行。对于常见反序列化 API，运行时要拿到明确、可用的类型描述，不能仅凭某个赋值语句左侧的泛型就可靠地恢复目标类型。

### Q14. TypeReference 这个设计是由哪一个框架独创的吗？

不宜把它归功于某个框架的独家发明。经典 Jackson 源码还引用了 Neal Gafter 讨论的 **super type token** 思路；多种框架后来采用了类似的模式和命名。

### Q15. 打印 `getType()` 为什么看到的是 `java.util.List<T>`？

这通常说明**你捕获的是类型变量 `T`，而不是具体的 `User`**。重点检查是否在泛型方法或泛型类内部直接写了 `new TypeReference<List<T>>() {}`。如必须动态构造 `List<User>`，应将真实的类型信息显式传进去。

---
## 16. 学完之后做 8 道练习题

> 不建议看一眼答案就结束。先自己判断“需要什么类型信息”，再决定写 `Class`、`TypeReference`，还是动态构造的类型描述。

### 练习 1：单个对象

JSON：

```json
{"id": 1, "name": "张三"}
```

目标：`User`。使用 Jackson 时，应当传什么？

<details>
<summary>查看参考答案</summary>

```java
User user = mapper.readValue(json, User.class);
```

普通对象直接使用 `Class` 即可，没有必要人为增加 `TypeReference`。

</details>

### 练习 2：列表

JSON：

```json
[{"id": 1, "name": "张三"}]
```

目标：`List<User>`。你该怎么写？

<details>
<summary>查看参考答案</summary>

```java
List<User> users = mapper.readValue(
    json, new TypeReference<List<User>>() {}
);
```

</details>

### 练习 3：再加一层 Map

JSON：

```json
{
  "研发组": [
    {"id": 1, "name": "张三"},
    {"id": 2, "name": "李四"}
  ]
}
```

目标：键是组名，值是用户列表。

<details>
<summary>查看参考答案</summary>

```java
Map<String, List<User>> grouped = mapper.readValue(
    json, new TypeReference<Map<String, List<User>>>() {}
);
```

</details>

### 练习 4：统一响应包装

JSON：

```json
{
  "code": 200,
  "message": "success",
  "data": [{"id": 1, "name": "张三"}]
}
```

目标：`ApiResponse<List<User>>`。

<details>
<summary>查看参考答案</summary>

```java
ApiResponse<List<User>> response = mapper.readValue(
    json,
    new TypeReference<ApiResponse<List<User>>>() {}
);
```

前提是 Java 的 `ApiResponse<T>` 有相应字段和可用的反序列化访问方式。

</details>

### 练习 5：识别陷阱

以下代码有什么问题？

```java
List<User> users = mapper.readValue(json, List.class);
System.out.println(users.get(0).getName());
```

<details>
<summary>查看参考答案</summary>

编译阶段通常会有未经检查的转换警告。实际元素可能是 `LinkedHashMap`，取出并当作 `User` 使用时抛出 `ClassCastException`。

修正为 `new TypeReference<List<User>>() {}`，或者构造合适的 `JavaType`。

</details>

### 练习 6：看懂匿名子类

下面代码的 `{}` 表示什么？

```java
new TypeReference<List<User>>() {}
```

<details>
<summary>查看参考答案</summary>

是一个**匿名子类的类体**。它让匿名子类在泛型父类声明中写下 `TypeReference<List<User>>`，从而可以通过 `getGenericSuperclass()` 读取这份声明。

</details>

### 练习 7：通用工具方法

为什么下列写法不一定能反序列化为期望的 `T`？

```java
public static <T> T read(String json) throws Exception {
    return mapper.readValue(json, new TypeReference<T>() {});
}
```

<details>
<summary>查看参考答案</summary>

因为反射捕获到的可能仅仅是类型变量 `T`，不包含调用处的实际类型。可以改成接收 `Class<T>`、`Type`、`JavaType`，或者把已经构造好的类型令牌由调用者传入。

</details>

### 练习 8：源码阅读

阅读下面的简化代码：

```java
Type parent = getClass().getGenericSuperclass();
ParameterizedType pt = (ParameterizedType) parent;
Type target = pt.getActualTypeArguments()[0];
```

假设实例化方式为：

```java
new TypeReference<Map<String, List<User>>>() {}
```

请说出：`parent` 和 `target` 分别代表什么？

<details>
<summary>查看参考答案</summary>

- `parent`：整个泛型父类声明，即 `TypeReference<Map<String, List<User>>>`。
- `target`：父类 `TypeReference<T>` 中 `T` 实际对应的类型，即 `Map<String, List<User>>`。

如果还要找到 `User`，则继续递归检查 `Map` 的 Value 类型和 `List` 的元素类型。

</details>

---

## 17. 在公司代码里遇到它，应该怎么阅读和排查？

看到 `TypeReference` 时，可以按照这个顺序，不用急着去钻完整框架源码。

### 第一步：看 import

```java
import com.fasterxml.jackson.core.type.TypeReference;
```

先确认是哪一个库。不同库会影响你后面应该搜索哪些方法和官方文档。

### 第二步：看类型括号里究竟写了什么

```java
new TypeReference<ApiResponse<List<CampaignDto>>>() {}
```

把它展开：

```text
ApiResponse
└── data: List
          └── CampaignDto
```

这里的 `CampaignDto` 仅代表一个示例业务数据类型；你在项目中可能会看到广告、订单、用户、任务等各种 DTO。

**不要直接跳到这个 JSON 框架怎么解析**，先确定数据结构。

### 第三步：向前追 JSON 从哪里来

常见来源：

- HTTP 接口响应的 `String` / 响应体；
- Redis 读取的序列化字符串；
- 数据库 JSON 字段；
- 消息队列消费得到的 JSON 字符串；
- 本地文件中的 JSON。

来源影响数据是不是可信、是否保证字段完整、是否存在旧版本结构等。

### 第四步：向后追解析出来的数据怎么用

例如：

```java
ApiResponse<List<CampaignDto>> result = ...;
List<CampaignDto> campaigns = result.getData();

for (CampaignDto campaign : campaigns) {
    // 真正的业务逻辑在这里
}
```

你读代码的重点不只是“解析成功没有”，而是：

1. 数据在解析后如何传给业务逻辑？
2. `data` 可能为 `null` 或空列表吗？
3. 是否区分接口业务失败与 JSON 解析失败？
4. 如果 DTO 字段新增、修改，调用方会怎样？

### 第五步：怀疑类型不对时，直接打印

例如：

```java
TypeReference<List<User>> ref =
        new TypeReference<List<User>>() {};

System.out.println("目标类型: " + ref.getType().getTypeName());
```

反序列化后：

```java
if (users != null && !users.isEmpty()) {
    System.out.println("第一个元素实际类: "
            + users.get(0).getClass().getName());
}
```

排查顺序建议为：

```text
1. import 导包对吗？
       ↓
2. 目标类型与 JSON 顶层结构一致吗？
       ↓
3. TypeReference 捕获的是 User 还是 T？
       ↓
4. 解析后元素实际是什么类？
       ↓
5. DTO 字段、构造器、配置、时间格式等是否匹配？
       ↓
6. 最后再查框架更深层的反序列化行为
```

这个顺序特别适合你读公司代码时，不容易一上来就被大量源码和框架细节淹没。

---

## 18. 一页速查表：实际写代码怎么选？

| 我真正要得到的类型 | Jackson 推荐写法 | Fastjson2 推荐写法 |
|---|---|---|
| `User` | `mapper.readValue(json, User.class)` | `JSON.parseObject(json, User.class)` |
| `List<User>` | `mapper.readValue(json, new TypeReference<List<User>>() {})` | `JSON.parseObject(json, new TypeReference<List<User>>() {})` |
| `Map<String, User>` | `mapper.readValue(json, new TypeReference<Map<String, User>>() {})` | `JSON.parseObject(json, new TypeReference<Map<String, User>>() {})` |
| `ApiResponse<User>` | `mapper.readValue(json, new TypeReference<ApiResponse<User>>() {})` | `JSON.parseObject(json, new TypeReference<ApiResponse<User>>() {})` |
| `ApiResponse<List<User>>` | `mapper.readValue(json, new TypeReference<ApiResponse<List<User>>>() {})` | `JSON.parseObject(json, new TypeReference<ApiResponse<List<User>>>() {})` |
| 元素类在运行时才知道的 `List<T>` | `TypeFactory.constructCollectionType(List.class, elementClass)` 后使用 `JavaType` | 根据 Fastjson2 的 `Type` 或类型构造 API 显式传入运行时类型 |

> 表格是快速决策提示，不表示任何形状的 JSON 都能按这些代码成功转换。要以实际字段、类型和框架配置为准。注意每列 `TypeReference` 的 **import 互不相同**。

### 18.1 最值得背下来的三行

```java
// 普通对象
User user = mapper.readValue(json, User.class);

// 泛型对象
List<User> users = mapper.readValue(json,
        new TypeReference<List<User>>() {});

// 运行时才知道元素类型
JavaType listType = mapper.getTypeFactory()
        .constructCollectionType(List.class, User.class);
```

### 18.2 记住这张概念图

```text
                 Java 泛型
                    │
                    ▼
                类型擦除
                    │
                    ▼
      List<User>.class 并不存在
                    │
                    ▼
       需要额外的完整类型描述
                    │
         ┌──────────┴──────────┐
         ▼                     ▼
  TypeReference<T>       其他反射 / 类型构造方式
         │              Class / Type / JavaType
         ▼                     │
 匿名子类的泛型父类签名           │
         │                     │
         └──────────┬──────────┘
                    ▼
            JSON 框架读取目标类型
                    │
                    ▼
              执行反序列化
                    │
                    ▼
          List<User> / ApiResponse<T>
```

---

## 19. 建议如何掌握这个知识点？

理解到下面四个层次就足够你从“看到发懵”进步到“能看懂常见公司代码”。

### 第一层：知道它干什么

能够说出：

> TypeReference 主要用于向框架传递包含泛型参数的目标类型信息，例如 `List<User>`。

### 第二层：知道为什么普通 Class 不够

能够解释：

> `List.class` 无法携带 `User`，而 `List<User>.class` 不是合法的 Java 写法。反序列化框架需要其他方式得到完整类型描述。

### 第三层：知道 `{}` 和反射的原理

能够解释：

> 通过匿名子类声明 `TypeReference<List<User>>`，然后利用 `getGenericSuperclass()` 读取泛型父类签名，并取得 `List<User>` 对应的 `Type`。

### 第四层：能够排查坑

能够判断：

- 反序列化为何得到 `LinkedHashMap`；
- 为什么 `ClassCastException` 发生在取元素时；
- 为什么泛型方法里的 `T` 不能自动反推出 `User`；
- 为什么不同框架名字相同却不能直接混用；
- JSON 包装层次改变后应如何调整目标类型。

**学到第四层，已经足以应对日常项目里大多数 TypeReference 相关场景。** 反射、Java 类型系统和 Jackson 内部 `JavaType` 的更深原理，可以在真实需求碰到时再研究。

---

## 20. 官方资料与源码入口

以下链接优先指向官方或项目源码，方便你以后查阅，而不必仅凭网络博客的转述：

1. **Oracle：Type Erasure（泛型擦除）**  
   https://docs.oracle.com/javase/tutorial/java/generics/erasure.html

2. **Oracle：Non-Reifiable Types（运行时不完整保留的泛型类型）**  
   https://docs.oracle.com/javase/tutorial/java/generics/nonReifiableVarargsType.html

3. **Oracle：ParameterizedType Java 8 API**  
   https://docs.oracle.com/javase/8/docs/api/java/lang/reflect/ParameterizedType.html

4. **Jackson 官方源码：`TypeReference.java`**  
   https://github.com/FasterXML/jackson-core/blob/2.x/src/main/java/com/fasterxml/jackson/core/type/TypeReference.java

5. **Fastjson2 官方：README / 基础使用与依赖**  
   https://github.com/alibaba/fastjson2

6. **Fastjson2 官方：FAQ / 泛型解析示例**  
   https://github.com/alibaba/fastjson2/blob/main/docs/FAQ_en.md

7. **Fastjson2 官方源码：`TypeReference.java`**  
   https://github.com/alibaba/fastjson2/blob/main/core/src/main/java/com/alibaba/fastjson2/TypeReference.java

8. **Spring 官方：`ParameterizedTypeReference` Javadoc**  
   https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/ParameterizedTypeReference.html

9. **Spring 官方：REST Clients**  
   https://docs.spring.io/spring-framework/reference/integration/rest-clients.html

10. **Fastjson2 2.0.65 发布说明（包括安全修复）**  
    https://github.com/alibaba/fastjson2/releases

11. **Maven Central：Jackson Databind 版本信息**  
    https://central.sonatype.com/artifact/com.fasterxml.jackson.core/jackson-databind/versions

> 文档编写日期：**2026-10-10**。框架 API、维护状态和安全建议可能继续变化；以后升级依赖时应以对应版本的官方文档为准。

---

## 最后的总结：真正需要理解的是什么？

如果只留下最重要的四句话，它们是：

1. **Java 不能直接提供 `List<User>.class`，而 JSON 框架又需要知道元素应当是什么类。**
2. **`TypeReference<List<User>>` 是一种传递完整泛型类型描述的方式，本身不负责解析 JSON。**
3. **匿名子类 `{}` 是关键：它把具体泛型写进可通过反射读取的父类声明里。**
4. **它没有消除类型擦除；对于方法中的未知 `T`、错误的 JSON 结构以及框架混用，仍然需要正确处理。**

当你下次再看到：

```java
new TypeReference<ApiResponse<List<User>>>() {}
```

你就可以在脑中自动翻译成：

> “这里要向框架说明：反序列化的目标不是一个随便的 `Object`，也不是原始的 `ApiResponse`，而是一个 `ApiResponse`，其中 `data` 应当是由 `User` 对象构成的列表。”

**这就是真正读懂 TypeReference。**
