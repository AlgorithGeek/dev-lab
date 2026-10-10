# Fastjson JSONObject

## A. Fastjson 核心概念与环境搭建

### 1. Fastjson？

- **序列化**：将 Java 对象转换为 JSON 字符串（后端 -> 前端）
- **反序列化：将 JSON 字符串转换为 Java 对象（前端 -> 后端）

Fastjson 是阿里巴巴开源的一个高性能 JSON 库，它的核心优势在于 **极致的解析速度** 和 **简洁的 API 设计**



### 2. 版本选择：开发中的最大陷阱

Fastjson 目前并存两个大版本，这是新手最容易混淆，也是生产环境中最大的安全隐患来源

#### 2.1 Fastjson 1.x (`com.alibaba.fastjson`)

- **现状**：这是经典的“老版本”，国内存量项目使用极多

- **致命缺陷**：

  - 历史上曾多次爆发严重的 **反序列化安全漏洞**（著名的 AutoType 漏洞），导致攻击者可以远程执行恶意代码

    虽然官方多次发布补丁（如 1.2.83），但维护成本极高

- **建议**：**新项目严禁使用**。维护旧项目时，必须确保升级到最新补丁版本



#### 2.2 Fastjson 2.x (`com.alibaba.fastjson2`)

- **现状**：这是官方重构的“新一代”版本
- **改进**：底层代码完全重写，不仅性能更强，更重要的是从架构层面解决了 1.x 的安全隐患（默认关闭 AutoType）
- **建议**：**新项目首选**

> 🛑 **实战巨坑提示**： 
>
> - 1.x 和 2.x 的包名不同（`fastjson` vs `fastjson2`），API 也有差异。切勿在同一个项目中混用两个版本，否则会出现类冲突或莫名其妙的转换错误



### 3. Maven 依赖配置

根据你的项目情况选择其一

#### 3.1 推荐配置 (Fastjson 2.x)

```xml
<dependency>
    <groupId>com.alibaba.fastjson2</groupId>
    <artifactId>fastjson2</artifactId>
    <!-- 建议始终使用 Maven 仓库中的最新稳定版 -->
    <version>2.0.43</version>
</dependency>
```

#### 3.2 遗留项目维护 (Fastjson 1.x)

如果你被迫维护旧项目，请务必检查版本号，**绝对不要使用低于 1.2.83 的版本**

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <version>1.2.83</version>
</dependency>
```



### 4. Fastjson 的三大核心类体系

#### 4.1 `JSON` (门面类/工具类)

- **角色**：静态工具类，Fastjson 的入口
- **作用**：你几乎所有的操作（对象转字符串、字符串转对象）都从这里开始。它提供了一系列静态方法，如 `toJSONString()` 和 `parseObject()`

#### 4.2 `JSONObject` (JSON 对象)

- **本质**：它本质上是一个 `Map<String, Object>`
- **对应结构**：对应 JSON 中的花括号结构 `{ "key": "value" }`
- **用途**：当你不想定义专门的 Java Bean，或者 JSON 结构动态变化时，用它来存储键值对数据

#### 4.3 `JSONArray` (JSON 数组)

- **本质**：它本质上是一个 `List<Object>`
- **对应结构**：对应 JSON 中的方括号结构 `[ value1, value2 ]`
- **用途**：用于存储一组有序的数据



## B. JSONObject 核心操作

### 创建与写入

#### 1. 深入理解 JSONObject

在 Java 的世界里，`JSONObject` 并不是什么魔法黑盒

- **本质**: `JSONObject` 的本质就是一个 **`Map<String, Object>`**
- **继承关系**：它实现了 `java.util.Map` 接口。这意味着你可以像操作普通 `HashMap` 一样操作它（使用 `put`, `get`, `remove` 等方法）
- **作用**：它用于表示 JSON 格式中的 **对象** 结构，即被大括号 `{ ... }` 包裹的键值对集合



#### 2. 创建 JSONObject 的三种核心方式

在实战中，我们通常通过以下三种方式获得一个 `JSONObject` 实例

##### 2.1 从 JSON 字符串解析（最常用）

这是处理前端传参或第三方 API 响应时的标准做法

**核心方法**：

- `JSON.parseObject(String text)`
  - **作用**：将标准的 JSON 格式字符串转换为内存中的 JSONObject 对象
  - **参数**：`text` - JSON 字符串
  - **返回值**：`JSONObject` 实例。如果字符串为空，则返回 null

**代码示例**：

```java
String jsonStr = "{\"name\":\"张三\",\"age\":25,\"city\":\"北京\"}";

// 推荐使用 JSON 类的静态方法作为入口
JSONObject jsonObject = JSON.parseObject(jsonStr);

System.out.println(jsonObject.get("name")); // 输出：张三
```



##### 2.2 手动构造（用于组装数据）

当你需要主动构建一个 JSON 对象发送给前端或第三方时使用

**核心构造器**：

- `new JSONObject()`
  - **作用**：创建一个空的 JSONObject。底层默认使用 `HashMap`

**代码示例**：

```java
JSONObject json = new JSONObject();
json.put("status", 200);
json.put("message", "success");
```



##### 2.3 从 Java Bean 转换（中转处理）

有时你手头已经有一个实体对象（如 `User`），但需要对其进行动态修改（比如临时加一个字段）后再输出，可以先将其转为 JSONObject

**核心方法**：

- `JSON.toJSON(Object javaObject)`
  - **作用**：将普通的 Java Bean 转换为 `JSONObject`（如果是列表则转为 `JSONArray`）
  - **注意**：这里需要强制类型转换 `(JSONObject)`

**代码示例**：

```java
User user = new User("张三", 25);
// 将实体转为 JSONObject，以便进行后续的动态操作
JSONObject json = (JSONObject) JSON.toJSON(user);
// 临时添加一个不在 User 类定义中的字段
json.put("tempFlag", true);
```





#### 3. 关于字段顺序问题⭐

##### 3.1 现象与原因

- **现象**：代码里明明是按 `name` -> `age` -> `sex` 的顺序 put 的，结果转成 JSON 字符串后变成了 `age` -> `name` -> `sex`（乱序）

- **根本原因**：

  - `JSONObject` 的底层主要依赖 Java 的 Map 实现
  - **默认构造器** `new JSONObject()` 内部默认创建一个 `HashMap`
  - **HashMap 是无序的**：它是根据 Key 的 Hash 值来决定存储位置，而不是插入顺序

- **后果**：

  - **签名校验失败**：在对接银行、支付接口时，通常要求参数按 ASCII 码排序拼接字符串进行 MD5 签名。乱序会导致签名对不上

  - **前端展示跳变**：虽然 JSON 标准不强调顺序，但在某些简单的展示场景下，乱序会导致 UI 忽上忽下



##### 3.2 构造方法详解(解决与避坑)

Fastjson 提供了重载的构造方法来控制底层的 Map 实现

###### **1. 默认构造器 (无序 - 慎用)**

```Java
// 内部 = new HashMap<String, Object>(16);
JSONObject json = new JSONObject();
```

- **特点**：性能略高（HashMap 稍快），但无法保证顺序
- **适用**：不关心字段顺序的普通数据传输



###### **2. 有序构造器 (有序 - 推荐⭐)**

> **我建议一直用这个⭐，养成好习惯**

```java
// 内部 = new LinkedHashMap<String, Object>(16);
// 传入 true 表示开启 ordered (有序)
JSONObject json = new JSONObject(true); 
```

- **特点**：内部使用 `LinkedHashMap`，它维护了一个双向链表来记录插入顺序
- **适用**：**任何** 对顺序敏感的场景



###### **3. 指定容量构造器 (性能优化)**

```Java
// 如果你知道大概要放多少个字段，指定容量可以减少 Map 扩容带来的性能损耗
// 这里的 true 依然代表有序
JSONObject json = new JSONObject(10, true); 
```



##### 3.3 代码对比示例

直接看结果，一目了然：

```Java
import com.alibaba.fastjson.JSONObject;

public class OrderTest {
    public static void main(String[] args) {
        // ❌ 错误示范：默认无序
        JSONObject unordered = new JSONObject(); 
        unordered.put("3_third", "C");
        unordered.put("1_first", "A");
        unordered.put("2_second", "B");
        System.out.println("默认无序: " + unordered.toJSONString());
        // 结果可能是: {"1_first":"A", "3_third":"C", "2_second":"B"} (顺序看运气/Hash值)

        System.out.println("-------------------------");

        // ✅ 正确示范：开启有序 (true)
        JSONObject ordered = new JSONObject(true);
        ordered.put("3_third", "C");
        ordered.put("1_first", "A");
        ordered.put("2_second", "B");
        System.out.println("指定有序: " + ordered.toJSONString());
        // 结果绝对是: {"3_third":"C", "1_first":"A", "2_second":"B"} (怎么存就怎么取)
    }
}
```



#### 4. 写入数据：`put` 与 `fluentPut`

构建 JSON 数据时，主要通过以下两个方法

##### 4.1 常规添加：`put`

这是实现 Map 接口的标准方法

- `Object put(String key, Object value)`

  - **作用**：添加一个键值对
  - **返回值**：如果该 Key 之前存在，返回旧值；否则返回 null

  - **代码示例**

    ```java
    // 1. 创建 JSONObject 对象
    JSONObject user = new JSONObject();
    
    // 2. 使用 put 逐个添加 (常规写法)
    user.put("userId", 1001);
    user.put("username", "admin");
    
    // put 的返回值演示：如果 key 存在，返回旧值
    Object oldValue = user.put("username", "root"); // 返回 "admin"
    
    System.out.println("最终结果: " + user);
    System.out.println("被替换的旧值: " + oldValue);
    ```

    ```java
    最终结果: {"userId":1001,"username":"root"}
    被替换的旧值: admin
    ```



##### 4.2 链式添加：`fluentPut` (推荐)

这是 Fastjson 特有的增强方法，让代码更优雅

- `JSONObject fluentPut(String key, Object value)`

  - **作用**：添加键值对

  - **区别**：它操作完后 **返回当前的 JSONObject 对象本身**

  - **优势**：允许你像链条一样连续调用，代码可读性极高

  - **代码示例**

    ```java
    // 使用 fluentPut 链式调用 (一行代码搞定)
    JSONObject product = new JSONObject()
            .fluentPut("productId", "P-2024001")
            .fluentPut("name", "机械键盘")
            .fluentPut("price", 299.00)
            .fluentPut("inStock", true);
    
    System.out.println("链式构建结果: " + product);
    ```

    ```java
    链式构建结果: {"productId":"P-2024001","name":"机械键盘","price":299.0,"inStock":true}
    ```



##### 5. 综合代码演示

下面这段代码展示了如何优雅地构建一个复杂的 JSON 结构

```JAVA
public class JsonBuildDemo {
    public static void main(String[] args) {
        // 1. 创建顶层对象（使用有序构造器，养成好习惯）
        JSONObject root = new JSONObject(true);

        // 2. 使用 fluentPut 链式构建基本数据
        root.fluentPut("code", 200)
            .fluentPut("message", "查询成功")
            .fluentPut("timestamp", System.currentTimeMillis());

        // 3. 构建嵌套对象
        JSONObject data = new JSONObject(true);
        data.put("userId", "U1001");
        data.put("username", "Admin");
        
        // 4. 构建数组
        JSONArray roles = new JSONArray();
        roles.add("Manager");
        roles.add("Developer");
        
        // 将数组放入 data
        data.put("roles", roles);

        // 5. 将 data 放入 root
        root.put("result", data);

        // 6. 输出结果
        System.out.println(root.toJSONString());
    }
}
```

**输出结果：**

```JS
{
    "code": 200,
    "message": "查询成功",
    "timestamp": 1701234567890,
    "result": {
        "userId": "U1001",
        "username": "Admin",
        "roles": ["Manager", "Developer"]
    }
}
```



### 读取与类型转换

#### 1. 读取数据的痛点与解决方案

`JSONObject` 本质是 Map，你可以用 `get(key)` 方法获取值。但原生的 `get()` 方法有一个巨大的缺陷：**它返回的是 `Object` 类型**

- **痛点**：

  - 拿到 `Object` 后，你必须手动进行强制类型转换（如 `(String) obj`）

    如果 JSON 中的实际类型和你想的不一样（比如把数字 `1` 强转为 `String`），程序就会直接崩溃

- **解决方案**：

  - Fastjson 提供了一系列以 `get` 开头的 **类型安全方法**（Type-Safe Getters），它们能自动处理类型转换

    > `getXXX()`




#### 2. 常用类型获取方法

Fastjson 的强大之处在于它的 **智能类型转换**。即使 JSON 原文是字符串 `"123"`，你也可以直接用 `getInteger` 拿到数字 `123`



##### 2.1 基础类型获取

Fastjson 的最大优势是 **自动类型转换**（String 转 Int，String 转 Boolean 等），非常灵活

| **方法签名**           | **返回值**   | **说明** | **实战避坑 / 建议**                                          |
| ---------------------- | ------------ | -------- | ------------------------------------------------------------ |
| **【字符串】**         |              |          |                                                              |
| `getString(key)`       | `String`     | 字符串   | **万能兜底**。无论原值是数字、布尔还是 null，都能拿。若原值为 null，则返回 null |
| **【整型】**           |              |          |                                                              |
| `getInteger(key)`      | `Integer`    | 包装类   | **⭐⭐⭐ 强力推荐**。Key 不存在或值为 null 时返回 `null`，能正确区分“空值”和“0” |
| `getIntValue(key)`     | `int`        | 基本类型 | **❌ 慎用**。Key 不存在或值为 null 时**自动转为 0**。无法区分“库存是0”还是“没查到库存” |
| `getLong(key)`         | `Long`       | 包装类   | **推荐**。用于处理 ID (Snowflake)、时间戳等大整数。支持 null 判断 |
| `getLongValue(key)`    | `long`       | 基本类型 | **❌ 慎用**。Key 不存在或值为 null 时**自动转为 0L**          |
| **【浮点型】**         |              |          |                                                              |
| `getBigDecimal(key)`   | `BigDecimal` | 高精度   | **⭐⭐⭐ 金额必选**。<br />涉及**钱/交易**必须用它。避免 Double 计算导致的精度丢失（如 0.1+0.2!=0.3） |
| `getDouble(key)`       | `Double`     | 包装类   | **推荐**。处理经纬度、温度等非金额数据。支持 null 判断（如“传感器无数据”） |
| `getDoubleValue(key)`  | `double`     | 基本类型 | **❌ 慎用**。Key 不存在或值为 null 时**自动转为 0.0**。存在歧义且有精度问题 |
| **【布尔型】**         |              |          |                                                              |
| `getBoolean(key)`      | `Boolean`    | 包装类   | **推荐**。**极度智能**。支持 `true`/`false`，也兼容字符串 `"true"`/`"false"` 或数字 `1`/`0` |
| `getBooleanValue(key)` | `boolean`    | 基本类型 | **❌ 慎用**。Key 不存在或值为 null 时 **自动转为 false**。无法区分“默认拒绝”还是“未配置” |



**代码示例**

```java
JSONObject json = new JSONObject();
json.put("id", 1001);
json.put("count", "50"); // 注意这里存的是字符串
json.put("isValid", 1);  // 用 1 代表 true
// json.put("score", null); // 假设 score 不存在

// 1. 自动类型转换：虽是 String "50"，但能当 Integer 取
Integer count = json.getInteger("count"); // 结果：50

// 2. 智能布尔值：虽是数字 1，但能当 Boolean 取
Boolean valid = json.getBoolean("isValid"); // 结果：true

// 3. 💣 经典空指针陷阱演示
// 场景：score 字段不存在
Integer scoreObj = json.getInteger("score"); // 结果：null (安全，你可以判断 if scoreObj == null)
int scoreVal = json.getIntValue("score");    // 结果：0 (危险！你以为考了0分，其实是缺考)
```



##### 2.2 特殊类型获取：金额与时间

这两类数据在电商和金融场景极其敏感，Fastjson 提供了专门的封装

- 💰**金额处理：`getBigDecimal(String key)`**

  - **场景**：涉及价格、交易额、账户余额等
  - **痛点**：Java 的 `Double` 计算有精度丢失问题（例如 `0.1 + 0.2` 并不等于 `0.3`，而是 `0.30000000000000004`）
  - **最佳实践**：**严禁使用 `getDouble` 处理钱！ 必须使用 `BigDecimal`。**Fastjson 会帮你处理好精度转换

  - **示例**：

    ```java
    JSONObject product = new JSONObject();
    product.put("price", "19.90"); // 建议价格传输时用字符串，防止传输层精度丢失
    
    // ✅ 正确做法
    BigDecimal price = product.getBigDecimal("price"); 
    // 此时你可以安全地进行加减乘除：price.add(new BigDecimal("0.1"));
    ```



- ⏰**日期处理：`getDate(String key)`**

  - **场景**：`createTime` (创建时间), `payTime` (支付时间)

  - **智能特性**：

    - 这是一个“全能”方法，不需要你指定格式，它能自动识别常见格式

      - **标准字符串**：`"2023-12-01 12:00:00"`
      - **ISO-8601**：`"2023-12-01T12:00:00"`
      - **时间戳**：`1701398400000` (毫秒 Long 值)

    - **代码示例**

      ```java
      JSONObject timeJson = new JSONObject();
      timeJson.put("t1", "2025-11-11 10:00:00"); // 数据库常见的 datetime 格式
      timeJson.put("t2", 1762826400000L);        // 毫秒时间戳
      
      // Fastjson 自动帮你 new Date()
      java.util.Date date1 = timeJson.getDate("t1"); 
      java.util.Date date2 = timeJson.getDate("t2");
      
      System.out.println(date1); // 输出正常的时间对象
      ```



#### 3. 处理嵌套结构

现实中的 JSON 往往层层嵌套（对象里包对象，对象里包数组），只用基础类型不够用

| **方法签名**                | **返回值类型** | **说明**             | **实战建议**                                                 |
| --------------------------- | -------------- | -------------------- | ------------------------------------------------------------ |
| `getJSONObject(String key)` | `JSONObject`   | 获取嵌套的 JSON 对象 | 当某个字段的值依然是一个 `{...}` 结构时使用<br />拿到后可继续链式调用 `.getString(...)` |
| `getJSONArray(String key)`  | `JSONArray`    | 获取嵌套的 JSON 数组 | 当某个字段的值是 `[...]` 时使用<br />返回的是 Fastjson 的 List 实现，可进行遍历 |





#### 4. 实战代码演示：全能取值

下面这段代码涵盖了上述所有知识点，并展示了 Fastjson 强大的容错能力

```java
import com.alibaba.fastjson2.JSONObject;
import java.math.BigDecimal;
import java.util.Date;

public class JsonGetDemo {
    public static void main(String[] args) {
        // 模拟一份复杂的 JSON 数据
        // 注意：age 是字符串，score 是数字，isAdmin 是数字 1
        String jsonStr = """
        {
            "name": "张三",
            "age": "25",
            "score": 98.5,
            "balance": "100.05",
            "isAdmin": 1,
            "createTime": 1701234567890,
            "address": {
                "city": "北京",
                "code": 100000
            }
        }
        """;

        JSONObject json = JSONObject.parseObject(jsonStr);

        // 1. 自动类型转换演示
        // 原文是字符串 "25"，但可以直接取为 Integer
        Integer age = json.getInteger("age"); 
        System.out.println("年龄(Integer): " + age);

        // 原文是数字 1，可以直接取为 Boolean (1 -> true, 0 -> false)
        Boolean isAdmin = json.getBoolean("isAdmin");
        System.out.println("是否管理员: " + isAdmin);

        // 2. 金额处理 (必须用 BigDecimal)
        BigDecimal balance = json.getBigDecimal("balance");
        System.out.println("余额: " + balance);

        // 3. 日期处理 (自动转换时间戳)
        Date createTime = json.getDate("createTime");
        System.out.println("创建时间: " + createTime);

        // 4. 嵌套对象获取
        JSONObject address = json.getJSONObject("address");
        String city = address.getString("city");
        System.out.println("城市: " + city);

        // 5. 避坑演示：Key 不存在的情况
        String notExist = json.getString("ghost");
        System.out.println("不存在的Key返回: " + notExist); // 输出 null

        int notExistInt = json.getIntValue("ghost");
        System.out.println("不存在的Key用Value方法返回: " + notExistInt); // 输出 0
    }
}
```



#### 5. 开发陷阱：包装类 vs 基本类型

在选择 `getInteger` 还是 `getIntValue` 时，必须极其小心

**场景假设**：你有一个字段 `status`，`0` 代表禁用，`1` 代表启用。如果数据库中某条记录没有这个字段（即为 null）

- **错误做法**：

  ```java
  // 如果 JSON 中没有 "status"，这行代码会返回 0 (默认值)
  // 系统会误以为该用户是“禁用”状态，引发业务事故！
  int status = json.getIntValue("status"); 
  ```

- **正确做法**：

  ```java
  // 返回 null，你可以明确知道“没有这个数据”，而不是“数据是0”
  Integer status = json.getInteger("status");
  if (status != null) {
      // 处理逻辑
  }
  ```





## C. `JSONArray`

### 1. 深入理解 `JSONArray`

`JSONArray` 是 Fastjson 中用于处理 **数组** 结构的核心类

- **本质**：它本质上是一个 **`List<Object>`**
- **继承关系**：它实现了 `java.util.List` 接口
  - 这意味着你可以直接对它使用 `add()`, `remove()`, `get()`, `iterator()` 等所有 List 的标准方法

- **对应结构**：对应 JSON 中的方括号结构 `[ value1, value2, ... ]`



### 2. 创建 JSONArray 的常用方式

我们在开发中通常通过以下三种方式获得一个 `JSONArray` 对象

#### 2.1 方式一：解析 JSON 字符串（最常用）

通常从前端或第三方接口拿到的都是一个字符串，需要反序列化

- **方法**：`JSON.parseArray(String text)`

- **场景**：处理接口返回值

- **作用**：将 JSON 字符串解析为 `JSONArray`

```java
// 模拟接口返回的 JSON 数组字符串
String jsonStr = "[\"Java\", \"Python\", \"Go\"]";

// 解析为 JSONArray
JSONArray languages = JSON.parseArray(jsonStr);
System.out.println(languages.getString(0)); 	// 输出: Java
```



#### 2.2 方式二：手动创建（组装数据）

当你需要从零构建一个 JSON 数组发给前端时使用

- **方法**：`new JSONArray()`
- **作用**：创建一个空数组
- **进阶技巧**：使用 **`fluentAdd`** 进行链式添加 (代码更优雅)

```java
// 1. 创建空数组
JSONArray users = new JSONArray();

// 2. 链式添加数据 (推荐写法)
users.fluentAdd(new JSONObject().fluentPut("name", "Alice").fluentPut("age", 18))
     .fluentAdd(new JSONObject().fluentPut("name", "Bob").fluentPut("age", 20));

System.out.println(users);
// 输出: [{"name":"Alice","age":18}, {"name":"Bob","age":20}]
```



#### 2.3 方式三：Java List 转 JSONArray

如果你手头已经有一个 Java 的 `List` (比如从数据库查出来的)，想把它转成 `JSONArray` 以便使用它的特定功能 (如 `toJavaList` 或方便的 JSON 操作)

- **方法**：`(JSONArray) JSON.toJSON(Object javaObject)`

- **原理**：`JSON.toJSON` 是智能的。如果传入的是 List 类型，它底层实际返回的就是 `JSONArray` 实例，所以可以强转

```java
// 假设这是从数据库查出来的 List
List<String> list = Arrays.asList("Spring", "MyBatis", "Redis");

// ❌ 错误做法：直接 toString() 得到的是 Java 格式的字符串，不是 JSON 对象
// System.out.println(list.toString()); 

// ✅ 正确做法：转换为 JSONArray
JSONArray array = (JSONArray) JSON.toJSON(list); 

// 现在你可以用 JSONArray 的专属方法了
System.out.println("数组长度：" + array.size());
```



### 3. 核心 API 与操作

`JSONArray` 实现了 `List` 接口，所以 `add`, `remove`, `iterator` 等基础操作不再赘述。我们重点关注 **获取数据** 和 **类型转换** 的特有方法



#### 3.1 类型安全的获取方法

这些方法通过 **索引（index）** 来获取元素，并自动转换类型

| **方法签名**               | **返回值**   | **说明**         | **实战场景/建议**                                            |
| -------------------------- | ------------ | ---------------- | ------------------------------------------------------------ |
| `getJSONObject(int index)` | `JSONObject` | 获取对象         | **👑 最常用**。处理标准结构 `[{}, {}]` (如用户列表、商品列表) |
| `getString(int index)`     | `String`     | 获取字符串       | 处理简单标签列表 `["TagA", "TagB"]`                          |
| `getJSONArray(int index)`  | `JSONArray`  | **获取嵌套数组** | **🆕 必须补充**。处理二维数组或矩阵数据 `[[1,2], [3,4]]`      |
| `getInteger(int index)`    | `Integer`    | 获取数字         | 避免使用 `(Integer) list.get(i)` 强转，防止类型转换异常      |



#### 3.2 集合转换与状态检查(高频)

除了取单个值，这两个操作在实战中必不可少

| **方法签名**                 | **说明**              | **实战场景/建议**                                            |
| ---------------------------- | --------------------- | ------------------------------------------------------------ |
| `toJavaList(Class<T> clazz)` | **转 Java Bean 列表** | **🚀 神器 (重中之重)**<br />不要在 `for` 循环里一个个 `getJSONObject` 再手动 set 值<br />直接 `array.toJavaList(User.class)` 一步到位，效率最高 |
| `size()`                     | 获取长度              | **🛡️ 防御性编程**<br />在 `get(i)` 之前，**必须** 判断 `if (array != null && array.size() > i)`，防止 `IndexOutOfBoundsException` |
| `isEmpty()`                  | 判断是否为空          | 等价于 `size() == 0`，代码可读性更好                         |



#### 3.3 实战代码

```java
// 场景：地图坐标点集合 [[116.4, 39.9], [121.5, 31.2]]
JSONArray points = JSON.parseArray("[[116.4, 39.9], [121.5, 31.2]]");

// 取出第一个坐标点，它依然是个数组
JSONArray firstPoint = points.getJSONArray(0); 
System.out.println(firstPoint.get(0)); // 116.4
```



```java
// ❌ 新手易错：直接取值，不看长度
// 如果数组是空的 []，这行直接报错 IndexOutOfBoundsException
JSONObject user = array.getJSONObject(0); 

// ✅ 老手习惯：先判空
if (!array.isEmpty()) {
    JSONObject user = array.getJSONObject(0);
}
```



```java
public class JsonArrayDemo {
    public static void main(String[] args) {
        String jsonStr = """
            [
                {"id": 1, "name": "Google", "url": "[www.google.com](https://www.google.com)"},
                {"id": 2, "name": "Baidu", "url": "[www.baidu.com](https://www.baidu.com)"},
                {"id": 3, "name": "Bing", "url": "[www.bing.com](https://www.bing.com)"}
            ]
        """;

        JSONArray sites = JSON.parseArray(jsonStr);

        // 1. 普通遍历 (配合 getJSONObject)
        System.out.println("--- 普通遍历 ---");
        for (int i = 0; i < sites.size(); i++) {
            // 关键点：这里直接获取为 JSONObject，方便后续取值
            JSONObject site = sites.getJSONObject(i);
            System.out.println(site.getString("name"));
        }

        // 2. 转换为 Java List (强类型，推荐用于业务逻辑)
        System.out.println("--- 转换为 Java List ---");
        // 假设有一个 Site 类包含 id, name, url 字段
        List<Site> siteList = sites.toJavaList(Site.class);
        for (Site s : siteList) {
            System.out.println(s.getName());
        }
    }
    
    // 简单的静态内部类用于演示
    public static class Site {
        private String name;
        // 省略 getter/setter
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
```



### 4. 进阶：JSONArray 与 Java 8 Stream

这是处理复杂 JSON 数组的 **“杀手锏”**

由于 `JSONArray` 实现了 List 接口，我们可以直接使用 **Stream API** 进行过滤、映射等操作，无需先转为 Java List

- 直接`.stream()`，之后就可以按照 stream API 进行操作了



**场景**：从一堆用户数据中，筛选出年龄大于 20 岁的所有用户的名字

```java
String jsonStr = """
    [
        {"name": "张三", "age": 18},
        {"name": "李四", "age": 25},
        {"name": "王五", "age": 30}
    ]
""";
JSONArray users = JSON.parseArray(jsonStr);

// 链式调用：流化 -> 映射类型 -> 过滤 -> 提取字段 -> 收集结果
List<String> names = users.stream()
    // 1. 关键：必须先强转为 JSONObject，因为 stream() 默认泛型是 Object
    .map(obj -> (JSONObject) obj)
    // 2. 过滤年龄 > 20
    .filter(user -> user.getIntValue("age") > 20)
    // 3. 提取名字
    .map(user -> user.getString("name"))
    // 4. 转为 List (Java 16+ 写法，旧版本用 .collect(Collectors.toList()))
    .toList(); 

System.out.println(names); // 输出: [李四, 王五]
```



### 5. 实战避坑：数组越界与空指针

在处理动态的 JSON 数据时， **“防御性编程”** 是绝对的原则

- 💣 **陷阱 1：数组本身为 Null (最易忽略)**
  
  - **现象**：调用 `JSON.parseArray(null)` 或解析空字符串时，返回的不是空列表 `[]`，而是 **`null`**
  - **后果**：直接调用 `list.size()` 或 `list.get(0)` 会报 **`NullPointerException` (NPE)**
  - **对策**：操作前必须先判空！`if (list != null && !list.isEmpty())`
  
  

- 💣 **陷阱 2：数组越界 (IndexOutOfBounds)**

  - **现象**：数组只有 3 个元素，却尝试 `list.getString(5)`

  - **对策**：
    1. **首选**：使用 `for-each` 循环（根本不会越界）
    2. **次选**：如果必须用下标，务必保证 `index < list.size()`

  

- 💣 **陷阱 3：元素为 Null (Null Elements)**

  - **现象**：JSON 规范允许数组包含 `null`，例如 `[1, null, 3]`

  - **后果**：如果使用 `int val = list.getIntValue(1)`，会得到 `0`（数据污染）；如果直接解包 `int val = list.getInteger(1)`，会报 NPE

  - **对策**：取值后，务必判断元素是否非空



**最佳实践代码模板**

建议将这段代码作为标准处理流程：

```java
String jsonStr = "[100, null, 200]"; 
JSONArray list = JSON.parseArray(jsonStr);

// 🛡️ 第一道防线：先判断数组本身是否安全
if (list != null && !list.isEmpty()) {
    
    // 🛡️ 第二道防线：使用 for-each 避免下标越界
    for (int i = 0; i < list.size(); i++) {
        
        // 🛡️ 第三道防线：判断具体元素是否为 null
        Integer val = list.getInteger(i);
        
        if (val != null) { 
            System.out.println("有效数据: " + val);
        } else {
            System.out.println("警告: 第 " + i + " 个元素是空值，已跳过");
        }
    }
} else {
    System.out.println("数组为空或解析失败");
}
```



## D. 序列化进阶与注解控制

### 1. 控制输出的核心：`SerializerFeature`

默认情况下，Fastjson 在序列化时会 **自动剔除值为 `null` 的字段**

- **初衷**：为了减少 JSON 体积，节省网络带宽
- **问题**：前端/客户端通常需要根据字段是否存在来做逻辑。如果没有该字段，可能会导致 `undefined` 错误或破坏接口契约

我们需要在 `JSON.toJSONString(......)` 时传入 `SerializerFeature` 枚举来改变这一行为



**核心概念：“作用域”**

在使用 `SerializerFeature` 之前，必须明确它的生效范围，避免误解：

- **不是“项目全局”**：它不会影响你项目中其他地方的序列化逻辑
- **而是“对象全局”**：它仅对 **本次** `toJSONString(......)` 调用生效
  - **深度穿透**：规则会应用到 **当前对象、子对象、孙子对象** 等整个对象树
  - **一次性**：只管这一行代码，管完即走，互不干扰



#### 1.1 常用特性枚举

##### 场景 a：处理 Null 值 (最常用)

| **特性枚举**             | **作用**                   | **实战建议**                                              |
| ------------------------ | -------------------------- | --------------------------------------------------------- |
| `WriteMapNullValue`      | **保留 null 字段**         | **⭐⭐⭐**让前端明确知道“字段存在但为空”，而不是“字段不存在” |
| `WriteNullListAsEmpty`   | List 为 null 时输出 `[]`   | 避免前端遍历 null 数组导致崩溃                            |
| `WriteNullStringAsEmpty` | String 为 null 时输出 `""` | 减少前端对字符串的判空逻辑                                |



##### 场景 B：格式与安全 (必须注意)

| **特性枚举**                     | **作用**             | **实战建议**                                                 |
| -------------------------------- | -------------------- | ------------------------------------------------------------ |
| `PrettyFormat`                   | **格式化输出**       | 调试日志专用。将紧凑的 JSON 变为带缩进和换行的格式，方便人眼阅读 |
| `WriteDateUseDateFormat`         | **使用标准日期格式** | 将 `Date` 对象由默认的“毫秒时间戳”转为 `yyyy-MM-dd HH:mm:ss` |
| `DisableCircularReferenceDetect` | **禁止循环引用检测** | **⭐⭐⭐ 避坑神器**。防止出现 `{"$ref":".."}` 这种前端无法解析的奇怪引用符号 |



#### 1.2 实战代码：定制化输出

```java
public class SerializerFeatureDemo {
    public static void main(String[] args) {
        User user = new User();
        user.setName("张三");
        user.setAge(null);  // 空值
        user.setEmail(null); // 空值

        // 1. 默认行为：节省空间，忽略 null
        System.out.println("--- 1. 默认输出 (缺字段) ---");
        System.out.println(JSON.toJSONString(user));
        // 输出: {"name":"张三"}  <-- age 和 email 丢了

        // 2. 常用组合：保留 Null + 格式化 + 关闭循环引用
        System.out.println("\n--- 2. 标准接口输出 (推荐) ---");
        String json = JSON.toJSONString(user, 
            SerializerFeature.WriteMapNullValue,            // 1. 保留 null
            SerializerFeature.DisableCircularReferenceDetect, // 2. 防止 $ref 乱码
            SerializerFeature.WriteDateUseDateFormat        // 3. 日期转 yyyy-MM-dd
        );
        System.out.println(json);
        // 输出: {"age":null, "email":null, "name":"张三"}

        // 3. 调试模式：美化输出
        System.out.println("\n--- 3. 调试日志 (Pretty) ---");
        System.out.println(JSON.toJSONString(user, 
            SerializerFeature.WriteMapNullValue,
            SerializerFeature.PrettyFormat // 开启美化
        ));
    }

    // 静态内部类 (为了演示方便)
    static class User {
        private String name;
        private Integer age;
        private String email;
        // 省略 getter/setter
        public void setName(String name) { this.name = name; }
        public String getName() { return name; }
        public void setAge(Integer age) { this.age = age; }
        public Integer getAge() { return age; }
        public void setEmail(String email) { this.email = email; }
        public String getEmail() { return email; }
    }
}
```



### 2. 精准控制：@JSONField 注解

`SerializerFeature` 是全局开关，一旦开启对所有字段生效

- 如果我们只想控制 **某个特定字段** 的行为（比如给 password 字段改名，或者不输出它），就需要用到 `@JSONField` 注解

这是 Fastjson 中 **最重要、使用频率最高** 的注解



#### 2.1 核心属性

| **属性**         | **作用**     | **说明**                    | **实战场景**                                                 |
| ---------------- | ------------ | --------------------------- | ------------------------------------------------------------ |
| `name`           | **改名**     | 指定序列化后的字段名        | 解决 Java 驼峰 (`userId`) 对接下划线接口 (`user_id`) 的问题  |
| `format`         | **格式化**   | 指定日期格式                | 如 `"yyyy-MM-dd"`。**注意：** 优先级高于全局的 `WriteDateUseDateFormat` |
| `serialize`      | **是否输出** | `false` = 隐藏字段          | **🔒 隐藏密码**。序列化成 JSON 时忽略该字段                   |
| `deserialize`    | **是否注入** | `false` = 拒收数据          | **🛡️ 保护字段**。前端传过来也不赋值（如 `role` 权限字段，防止被恶意篡改） |
| `ordinal`        | **排序**     | 指定字段顺序 (数值越小越前) | 对接需要“参数按顺序签名”的银行/支付接口时必用                |
| `alternateNames` | **备用名**   | **(容错神器)**              | 允许反序列化时匹配多个 Key。如 `{"id", "ID", "u_id"}` 都能映射到同一个字段 |



#### 2.2 实战代码：全能注解演示

```java
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import java.util.Date;

public class JsonFieldDemo {
    public static void main(String[] args) {
        User user = new User();
        user.setId(10086);
        user.setUserName("SuperAdmin");
        user.setPassword("123456"); // 密码
        user.setRegisterTime(new Date());

        // 序列化：Java -> JSON
        System.out.println("--- 序列化结果 ---");
        String json = JSON.toJSONString(user);
        System.out.println(json);
        
        // 结果分析：
        // 1. id 排在第一 (ordinal=1)
        // 2. userName 变成了 user_name (name)
        // 3. password 消失了 (serialize=false)
        // 4. registerTime 变成了中文格式 (format)
        // 输出: {"id":10086,"user_name":"SuperAdmin","registerTime":"2025年12月02日"}
    }

    static class User {
        // 1. ordinal: 强行让 ID 排在第一个 (默认是按字母序)
        @JSONField(ordinal = 1)
        private int id;

        // 2. name: 标准驼峰转下划线
        @JSONField(name = "user_name", ordinal = 2)
        private String userName;

        // 3. serialize: "只进不出"。
        // 前端传参时可以接收密码(deserialize=true)，但返回给前端时绝不显示(serialize=false)
        @JSONField(serialize = false)
        private String password;

        // 4. format: 局部定制日期格式
        @JSONField(format = "yyyy年MM月dd日", ordinal = 3)
        private Date registerTime;

        // 省略 getter/setter
        public void setId(int id) { this.id = id; }
        public int getId() { return id; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getUserName() { return userName; }
        public void setPassword(String password) { this.password = password; }
        public String getPassword() { return password; }
        public void setRegisterTime(Date registerTime) { this.registerTime = registerTime; }
        public Date getRegisterTime() { return registerTime; }
    }
}
```

```js
{"id":10086,"user_name":"SuperAdmin","registerTime":"2025年12月02日"}
```

(注：`registerTime` 的具体日期取决于运行代码当天的日期)



## E. 泛型与复杂类型(`TypeReference`)

### 1. 为什么需要 TypeReference？

在 Java 中，存在 **泛型擦除** 机制。这意味着 `List<User>` 和 `List<Order>` 在编译后的字节码中，都变成了简单的 `List`

当调用 `JSON.parseObject(json, List.class)` 时，由于 `List.class` 无法携带 `<User>` 这一泛型参数，Fastjson 无法推断集合内部的具体类型

- **默认行为**： 为了保证解析不中断，Fastjson 会将 JSON 数组中的元素默认解析为 **`JSONObject`**（本质是 `Map<String, Object>`），而非您预期的 Java Bean



**运行时异常示例**：

```java
// ❌ 错误写法：只传了 List.class，丢失了 <User> 信息
List<User> users = JSON.parseObject(json, List.class);

// 编译时不报错，但运行时数据类型不对
// users.get(0) 实际是 JSONObject 类型
User user = users.get(0); 
// 💥 抛出异常：java.lang.ClassCastException: JSONObject cannot be cast to User
```

为了解决这个问题，我们需要通过一种手段能在运行时保留泛型信息。这就是引入 `TypeReference` 的根本原因



### 2. 核心类：`TypeReference`

`TypeReference` 是 Fastjson 提供的工具类，用于处理带泛型的复杂类型转换

- **全限定名**：`com.alibaba.fastjson2.TypeReference`

- **核心原理**：

  - 利用 Java 的 **匿名内部类** 机制

    匿名内部类中，子类继承父类时，是可以保留父类的泛型信息的

    通过创建一个 `TypeReference` 的匿名子类，Fastjson 就能在运行时通过反射拿到完整的类型定义

- **语法结构**

  ```java
  // 注意末尾的一对花括号 {}
  new TypeReference<List<User>>() {}
  ```

  >  注意那对 **花括号 `{}`**，这代表创建了一个匿名子类，这是关键所在，千万不能漏掉



### 3. 三大常见场景实战

#### 3.1 场景一：解析对象列表 `List<T>`

虽然 Fastjson 提供了 `parseArray` 方法专门用于解析数组，但在泛型处理的统一性上，使用 `TypeReference` 是更推荐的做法，它能保持代码风格的一致性

```JAVA
String json = "[{\"name\":\"张三\"}, {\"name\":\"李四\"}]";

// 方式一：使用 parseArray (针对 List 的专用方法)
// List<User> list = JSON.parseArray(json, User.class);

// 方式二：使用 TypeReference (通用泛型方法，推荐)
// 能够显式指定 List 容器及其内部元素的类型
List<User> list = JSON.parseObject(json, new TypeReference<List<User>>(){});
```



#### 3.2 场景二：解析通用响应结果 `Result<T>` (最常用)

这是 Spring Boot 开发中的标准范式。后端通常会返回一个统一的包装类 `Result<T>`

前后端分离开发中，API 接口通常会使用统一的响应包装类 `Result<T>`。这是 `TypeReference` 使用频率最高的场景

假设结构如下：

```JAVA
public class Result<T> {
    private int code;
    private String msg;
    private T data; // 这里可能是对象，也可能是 List
    // getter/setter...
}
```



**解析代码**：需要将 JSON 中的 `data` 字段准确映射为 `User` 对象，而非 `JSONObject`

```JAVA
String json = "{ \"code\": 200, \"msg\": \"success\", \"data\": { \"name\": \"张三\", \"age\": 25 } }";

// ✅ 正确做法：通过 TypeReference 明确指定 T 为 User 类型
Result<User> result = JSON.parseObject(json, new TypeReference<Result<User>>(){});

// 类型安全：可以直接获取 User 对象
User user = result.getData();
```



#### 3.3 场景三：多层嵌套泛型 `Result<List<T>>`

当 `Result` 的 `data` 字段本身也是一个集合（如分页查询结果）时，就构成了多层泛型嵌套

- `TypeReference` 支持任意深度的泛型定义

```JAVA
String json = "{ \"code\": 200, \"data\": [{\"name\":\"张三\"}, {\"name\":\"李四\"}] }";

// ✅ 正确做法：嵌套定义泛型类型 Result<List<User>>
// Fastjson 会递归解析：先解析 Result，再解析 List，最后解析 User
Result<List<User>> result = JSON.parseObject(json, new TypeReference<Result<List<User>>>(){});

// 类型安全：直接获取 User 列表
List<User> users = result.getData();
System.out.println("用户数量: " + users.size());
```



### 4. 完整代码演示

```JAVA
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import java.util.List;
import java.util.Map;

public class GenericParseDemo {
    public static void main(String[] args) {
        // 模拟复杂的 Map<String, List<User>> 结构
        // 这种结构常见于按部门分组的用户列表
        String json = """
            {
                "tech_dept": [
                    {"id": 1, "name": "张三"},
                    {"id": 2, "name": "李四"}
                ],
                "sales_dept": [
                    {"id": 3, "name": "王五"}
                ]
            }
        """;

        System.out.println("--- 开始解析复杂泛型 ---");

        // 使用 TypeReference 定义 Map<String, List<User>>
        Map<String, List<User>> deptMap = JSON.parseObject(
            json, 
            new TypeReference<Map<String, List<User>>>(){}
        );

        // 验证解析结果
        List<User> techUsers = deptMap.get("tech_dept");
        // 这里的 techUsers.get(0) 已经是 User 对象，而不是 JSONObject
        System.out.println("技术部人数: " + techUsers.size());
        System.out.println("技术部第一名员工: " + techUsers.get(0).getName());
    }

    // 简单的实体类
    static class User {
        private int id;
        private String name;
        // getter/setter 省略
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
    }
}
```



### 5. 巨坑预警：ClassCastException

新手在处理泛型时最容易犯的错误

**场景**： 你定义了 `Result<User>`，但为了偷懒，解析时用了 `Result.class`

```JAVA
// ❌ 错误做法：泛型信息丢失
Result result = JSON.parseObject(json, Result.class);

// Fastjson 不知道 data 是 User，所以把它解析成了 JSONObject
Object data = result.getData(); 

// 💣 运行时报错：java.lang.ClassCastException: JSONObject cannot be cast to User
User user = (User) data; 
```

**最佳实践**： 

- 只要你的目标对象中包含泛型（无论是 `List<T>`, `Map<K,V>`, 还是自定义的 `Result<T>`），**永远使用 `TypeReference`**，不要心存侥幸使用 `.class`



## F. JSONPath 高级提取

### 1. 什么是 JSONPath？

JSONPath 是一种用于在 JSON 结构中定位和提取数据的查询语言，其设计理念参考了 XML 中的 XPath

- **核心价值**：解决深层嵌套（Deeply Nested）或动态结构数据的读取难题
- **对比**：
  - **传统方式**：`json.getJSONObject("store").getJSONArray("book").getJSONObject(0).getString("title")`（繁琐、易空指针）
  - **JSONPath**：`$.store.book[0].title`（声明式、简洁）
- **类全限定名**：`com.alibaba.fastjson.JSONPath` (v1) / `com.alibaba.fastjson2.JSONPath` (v2)



### 2. 基础语法速查

JSONPath 的语法非常直观，掌握以下符号即可覆盖绝大多数场景

| 符号          | 类型          | 描述  | 示例                            | 结果说明                                                     |
| ------------- | ------------- | ----- | ------------------------------- | ------------------------------------------------------------ |
| `$`           | **根节点**    | `$`   | `$`                             | 整个 JSON 对象                                               |
| `.`           | **子节点**    | `.`   | `$.store.book`                  | 获取 store 下的 book 节点                                    |
| `[]`          | **下标/属性** | `[]`  | `$.book[0]``$['store']['book']` | 数组第 1 个元素。支持用 key 字符串取值                       |
| `*`           | **通配符**    | `*`   | `$.store.book[*]``$.store.*`    | book 数组的所有元素。store 下的所有属性值                    |
| `..`          | **递归下降**  | `..`  | `$..price`                      | **慎用**。扫描整个 JSON 树，提取所有名为 price 的字段（无论层级深浅） |
| `?()`         | **过滤器**    | `?()` | `[?(@.price < 10)]`             | 筛选符合条件的对象（类似 SQL where）                         |
| `[,]`         | **多选**      | `[,]` | `['author', 'title']`           | 同时提取 author 和 title 两个字段                            |
| `[start:end]` | **切片**      | `:`   | `$.book[0:2]`                   | 提取数组前两个元素（左闭右开区间）                           |



### 3. 核心 API：不仅仅是查询

Fastjson 的 `JSONPath` 工具类不仅支持 **读取 (`eval`)**，还支持直接 **修改 (`set`)** JSON 内部的值，这在数据脱敏或局部更新场景下非常有用

#### 3.1 读取数据：`eval`

```java
public static Object eval(Object rootObject, String path)
```

- **作用**：根据路径提取数据

- **返回值机制 (重要)**：

  - **明确路径**：返回具体的对象或值（如 `String`, `Integer`）
  - **不明确路径**（包含 `*`, `..`, `?()`）：**永远返回 `List`**，即使只匹配到一个元素

- **代码示例**

  ```java
  String jsonStr = 
      "{ \"store\": { \"book\": [ { \"title\": \"Java核心技术\", \"price\": 80 }, { \"title\": \"高性能MySQL\", \"price\": 100 } ] } }";
  Object root = JSON.parse(jsonStr);
  
  // 1. 明确路径 -> 返回单个对象 (Integer)
  Integer price = (Integer) JSONPath.eval(root, "$.store.book[0].price");
  System.out.println("单价: " + price); // 输出: 80
  
  // 2. 不明确路径 (使用了 ..) -> 返回 List
  // 即使只有一个 price 字段，它也会包装在 List 里
  List<Object> allPrices = (List<Object>) JSONPath.eval(root, "$..price");
  System.out.println("所有价格: " + allPrices); // 输出: [80, 100]
  ```

  

#### 3.2 修改数据：`set` (隐藏神技)

```java
public static boolean set(Object rootObject, String path, Object value)
```

- **作用**：直接修改指定路径的值。如果路径不存在，部分实现会自动创建结构

- **场景**：无需反序列化整个对象，直接修改 JSON 某深层字段（如隐藏手机号、更新状态）

- **返回值**：`true` 表示修改成功，`false` 表示路径不存在或修改失败。

- **代码示例**：

  ```java
  String userInfo = "{ \"id\": 1001, \"info\": { \"phone\": \"13800001234\", \"role\": \"admin\" } }";
  Object root = JSON.parse(userInfo);
  
  // 需求：在不把 JSON 转成 User 对象的情况下，把手机号隐藏掉
  boolean success = JSONPath.set(root, "$.info.phone", "****");
  
  if (success) {
      System.out.println("脱敏后: " + JSON.toJSONString(root));
      // 输出: {"id":1001,"info":{"phone":"****","role":"admin"}}
  }
  
  // 需求：修改 deep 路径，自动创建层级 (部分版本支持)
  // JSONPath.set(root, "$.info.extra.verified", true);
  ```



### 4. 实战代码演示

以下代码展示了从简单的属性提取到复杂的条件过滤，以及数据修改

```java
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONPath;
import java.util.List;

public class JsonPathDemo {
    public static void main(String[] args) {
        // 模拟一个复杂的书店 JSON
        String jsonStr = """
        {
            "store": {
                "book": [
                    { "category": "reference", "author": "Nigel", "title": "Sayings", "price": 8.95 },
                    { "category": "fiction", "author": "Evelyn", "title": "Sword", "price": 12.99 },
                    { "category": "fiction", "author": "Herman", "title": "Moby Dick", "price": 8.99 },
                    { "category": "fiction", "author": "J.R.R. Tolkien", "title": "The Lord of the Rings", "price": 22.99 }
                ],
                "bicycle": { "color": "red", "price": 19.95 }
            }
        }
        """;
        
        Object root = JSON.parse(jsonStr);

        // 1. 基础提取：拿第一本书的标题
        // 路径：根 -> store -> book数组 -> 第1个 -> title
        Object title = JSONPath.eval(root, "$.store.book[0].title");
        System.out.println("第一本书: " + title); // Sayings

        // 2. 递归扫描：拿所有的价格 (无论是在 book 里还是 bicycle 里)
        // ⚠️ 注意：返回的是 List
        Object allPrices = JSONPath.eval(root, "$..price");
        System.out.println("所有价格: " + allPrices); // [8.95, 12.99, 8.99, 22.99, 19.95]

        // 3. 高级过滤：拿所有价格 < 10 的书名
        // ?(@.price < 10) 表示过滤条件，@ 代表当前节点
        List<Object> cheapBookTitles = (List<Object>) JSONPath.eval(root, "$.store.book[?(@.price < 10)].title");
        System.out.println("便宜的书: " + cheapBookTitles); // ["Sayings", "Moby Dick"]

        // 4. 数据修改：将自行车的颜色改成 "blue"
        JSONPath.set(root, "$.store.bicycle.color", "blue");
        System.out.println("修改后的颜色: " + JSONPath.eval(root, "$.store.bicycle.color"));
        
        // 5. 数组切片：获取前两本书
        Object firstTwoBooks = JSONPath.eval(root, "$.store.book[0:2]");
        System.out.println("前两本书数量: " + ((List)firstTwoBooks).size());
    }
}

```



### 5. 进阶：过滤器表达式详解

过滤器 `[?(@...)]` 是 JSONPath 最强大的功能。支持逻辑运算和正则匹配。

| 表达式     | 说明         | 示例                                            |
| ---------- | ------------ | ----------------------------------------------- |
| `@`        | **当前节点** | 过滤器内部使用 `@` 指代正在处理的元素           |
| `==`, `!=` | **比较**     | `[?(@.author == 'Nigel')]`                      |
| `<`, `>`   | **数字比较** | `[?(@.price < 10)]`                             |
| `&&`, `    |              | `                                               |
| `in`       | **集合包含** | `[?(@.category in ['fiction', 'history'])]`     |
| `like`     | **模糊匹配** | `[?(@.title like 'Moby%')]` (Fastjson 特有扩展) |



### 6. 性能与风险提示

1. **慎用 `..` (递归下降)**：
   - `$..key` 会遍历整棵 JSON 树。如果 JSON 非常大（如几兆），由于需要全量扫描，性能会显著下降
2. **返回值类型强转**：
   - 使用 `eval` 时，务必清楚你的路径是返回 **单个对象** 还是 **List**
   - 建议：如果不确定，先用 `instanceof List` 判断，或者直接视为 List 处理



## G. Spring Boot 集成 Fastjson

### 1. 为什么要替换 Jackson？

Spring Boot 内置了 Jackson。但在以下场景中，你可能需要切换到 Fastjson：

- **🚀 极致性能要求**
  - 在处理 **超大数据量** 或 **高并发** 序列化场景下，Fastjson 2 凭借其重写的底层算法，在 Benchmark 测试中往往表现出更优异的性能（序列化速度和内存占用）
- **🛠️ 开发者体验 (DX)**
  - Fastjson 的 API 设计倾向于 **静态方法** 调用（如 `JSON.toJSONString`），相比 Jackson 依赖 `ObjectMapper` 实例注入的方式，在工具类编写和快速调试上更为便捷

- **迁移老项目**：旧代码大量使用了 Fastjson 的注解（如 `@JSONField`），迁移到 Jackson 成本太高



### 2. 核心依赖

为了获得最佳的 Spring Boot 支持，**强烈建议**使用官方提供的 Starter，而不是仅引入核心 jar 包

#### 2.1 Maven 配置

在 `pom.xml` 中添加以下依赖。建议使用最新稳定版（此处以 `2.0.43` 为例）：

```xml
<dependencies>
    <!-- 1. Fastjson 2 核心包 (必须) -->
    <!-- 提供了底层的 JSONReader, JSONWriter 等核心能力 -->
    <dependency>
        <groupId>com.alibaba.fastjson2</groupId>
        <artifactId>fastjson2</artifactId>
        <version>2.0.43</version>
    </dependency>

    <!-- 2. Spring Boot Starter (强烈推荐) -->
    <!-- 提供了 FastJsonHttpMessageConverter 等自动配置类，简化集成 -->
    <dependency>
        <groupId>com.alibaba.fastjson2</groupId>
        <artifactId>fastjson2-extension-spring-boot-starter</artifactId>
        <version>2.0.43</version>
    </dependency>
</dependencies>
```



### 3. 核心配置：接管 Spring MVC 转换器

要让 Spring Boot 默认使用 Fastjson 处理 `@RequestBody` 和 `@ResponseBody`，

我们需要实现 `WebMvcConfigurer` 接口，并重写 `configureMessageConverters` 方法

显式配置 `FastJsonHttpMessageConverter` 来替换 Spring Boot 默认的 Jackson 解析器



#### 3.1 关键配置说明

1. **`FastJsonHttpMessageConverter`**
   - **作用**：这是一个“转换器”，它是连接 Spring MVC 和 Fastjson 的桥梁。它负责拦截 HTTP 请求的输入流（反序列化）和输出流（序列化）
2. **`FastJsonConfig`**
   - **作用**：配置载体。你需要在这个类里设置日期格式、序列化特性（是否输出 null 等）
3. **`WriterFeatures` (序列化特性)**：控制 **Java 对象 -> JSON** 的输出行为（如日期格式、Null 值处理、大数字转换)
4. **`ReaderFeatures` (反序列化特性)**：控制 **JSON -> Java 对象** 的读取行为（如容错机制、字段匹配策略）



#### 3.2 生产级配置代码 (Fastjson 2.x)

```java
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring.http.converter.FastJsonHttpMessageConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Configuration
public class FastJsonConfiguration implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 1. 初始化转换器
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();

        // 2. 构建配置规则
        FastJsonConfig config = new FastJsonConfig();
        
        // 2.1 全局日期格式
        config.setDateFormat("yyyy-MM-dd HH:mm:ss");

        // 2.2 序列化特性配置 (Server -> Client)
        config.setWriterFeatures(
            // 策略：保留 Map 中的 null 字段，明确告知前端"无数据"而非"字段缺失"
            JSONWriter.Feature.WriteMapNullValue,
            // 策略：Null List 转为 []，Null String 转为 ""，降低前端判空成本
            JSONWriter.Feature.WriteNullListAsEmpty,
            JSONWriter.Feature.WriteNullStringAsEmpty,
            // 策略：浏览器兼容模式，解决 Long 类型前端精度丢失问题 (自动转 String)
            JSONWriter.Feature.BrowserCompatible
        );

        // 2.3 反序列化特性配置 (Client -> Server)
        config.setReaderFeatures(
            // 策略：智能匹配，支持 下划线/驼峰 自动转换 (user_name -> userName)
            JSONReader.Feature.SupportSmartMatch
        );

        converter.setFastJsonConfig(config);

        // 3. 规范响应编码与类型 (防止乱码)
        converter.setDefaultCharset(StandardCharsets.UTF_8);
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));

        // 4. 优先级注入 (关键步骤)
        // 将 Fastjson 转换器插入到列表首位 (Index 0)
        // Spring Boot 默认会加载 Jackson，若不插队，Fastjson 将无法生效
        converters.add(0, converter);
    }
}

```



### 4. Controller 层实战

配置完成后，Controller 层业务代码无需感知底层 JSON 库的存在。Fastjson 会自动接管 `@RequestBody` (输入) 和 `@ResponseBody` (输出)

#### 4.1 接收与响应

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    /**
     * 输入场景：反序列化
     * 依赖 ReaderFeatures 配置
     * * 演示：前端传入 {"user_name": "admin"}，
     * 即使 Java 字段是 userName，配置了 SupportSmartMatch 后也能自动注入。
     */
    @PostMapping
    public Result<Order> create(@RequestBody Order order) {
        // 业务处理...
        return Result.success(order);
    }

    /**
     * 输出场景：序列化
     * 依赖 WriterFeatures 配置
     * * 演示：
     * 1. createTime 字段会自动格式化为 "yyyy-MM-dd HH:mm:ss"
     * 2. remark 字段若为 null，会输出 "remark": null (因配置了 WriteMapNullValue)
     * 3. id 字段若超过 17 位，会自动转为 String (因配置了 BrowserCompatible)
     */
    @GetMapping("/{id}")
    public Order getInfo(@PathVariable Long id) {
        Order order = new Order();
        order.setId(id);
        order.setCreateTime(new Date());
        order.setRemark(null); 
        return order;
    }
}
```



### 5. 实战巨坑：Long 类型精度丢失

- **现象**：后端传给前端一个 Long 类型的 ID（如 `1825764391234567890`），前端接收后最后几位变成了 `000`
- **原因**：JavaScript 的 `Number` 类型最大只能安全表示 2^53 左右的整数，超出会丢失精度
- **Fastjson 解决方案**：

在全局配置中添加 `BrowserCompatible` 特性，或者单独将 Long 转为 String

```java
// 在 FastJsonConfiguration 中添加特性
config.setWriterFeatures(
    JSONWriter.Feature.WriteMapNullValue,
    // 开启浏览器兼容模式：会自动把大数字转为 String 输出
    JSONWriter.Feature.BrowserCompatible 
);
```



## H. 常见报错与避坑指南

### 1. 前端杀手：循环引用 (`$ref`)

这是 Fastjson 中引发前后端联调冲突最高频的问题

- **现象**：前端接收到的 JSON 数据结构异常，出现类似 `$ref` 的引用标识，前端收到这样的 JSON，直接懵了：

  ```js
  [
      { "id": 1, "name": "Admin" },
      { "$ref": "$[0]" }  // 预期应该是完整的 Admin 对象，结果变成了引用
  ]
  ```

- **原因**：

  - Fastjson 默认开启了“循环引用检测”

  - 在序列化过程中，如果检测到当前的 Java 对象在之前已经序列化过（即内存地址相同），为了避免 JSON 体积膨胀或死循环，Fastjson 会直接输出该对象的引用路径（Reference Path），而非再次输出完整内容

- **后果**：前端的 JavaScript 解析器通常不支持这种语法，导致数据渲染失败

- **解决方案**：前端 JavaScript 通常无法直接解析这种语法。建议后端显式关闭此功能

  - **方案 A：单次禁用 (局部)**

    ```java
    JSON.toJSONString(list, JSONWriter.Feature.DisableCircularReferenceDetect);
    ```

  - **方案 B：全局禁用 (Spring Boot 配置)**

    ```java
    // 在 FastJsonConfiguration 中配置
    config.setWriterFeatures(JSONWriter.Feature.DisableCircularReferenceDetect);
    ```



### 2. 灵异事件：字段凭空消失

- **现象**：Java 对象中字段明明有值，但生成的 JSON 字符串中却没有该字段
- **排查步骤**：
  1. **检查 Getter 方法 (最常见)**
     - **原理**：Fastjson 的序列化核心依赖于 **反射调用 `public` 的 getter 方法**。
     - **自查**：如果你使用了 Lombok，请确保类上加了 `@Data` 或 `@Getter`。如果是手写代码，请检查 getter 方法是否遵循标准的命名规范（如 `getUserId()`），且修饰符必须是 `public`。
  2. **检查 Null 值策略**
     - **原理**：默认情况下，`null` 值的字段会被忽略，不参与序列化。
     - **解决**：如果必须输出 null，请开启 `WriteMapNullValue` 特性



### 3. 性能陷阱：重复解析

- **错误示范**： 在处理同一个 JSON 字符串时，为了获取不同字段，多次调用 `parseObject`

  ```java
  String jsonStr = "{...海量数据...}";
  
  // ❌ 错误：每次调用都会触发完整的词法分析和语法分析，时间复杂度 O(N) * 2
  String name = JSON.parseObject(jsonStr).getString("name");
  int age = JSON.parseObject(jsonStr).getIntValue("age");
  ```

- **优化方案**： 解析一次，持有对象，多次读取

  ```java
  String jsonStr = "{...海量数据...}";
  // ✅ 正确：只解析一次，后续读取仅为哈希查找，时间复杂度 O(1)
  JSONObject json = JSON.parseObject(jsonStr);
  String name = json.getString("name");
  int age = json.getIntValue("age");
  ```



### 4. 安全红线：AutoType 反序列化漏洞

Fastjson 2.x **默认禁用 AutoType**，但**并不是默认开启 SafeMode**。SafeMode 是可额外启用的更严格保护模式（如 JVM 参数 `-Dfastjson2.parser.safeMode=true`）；开启后，即使代码显式指定 `SupportAutoType`，自动类型识别也不会生效。处理不可信 JSON 时应保持 AutoType 禁用，不要全局打开。

> 参考：[Fastjson2 AutoType 与 SafeMode 官方说明](https://github.com/alibaba/fastjson2/blob/main/docs/autotype_cn.md)

- **漏洞背景**： 

  - Fastjson 允许在 JSON 中使用 `@type` 字段指定目标类名，以便将 JSON 多态反序列化为特定的 Java 子类

    这曾被攻击者利用，通过构造恶意的 `@type`（如远程连接类）在服务器端执行任意代码（RCE）

- **安全防御准则**：

  1. **默认关闭**：永远不要在全局配置中手动开启 AutoType，除非你完全理解其风险
  2. **来源管控**：对于面向公网（Internet）的接口，**严禁** 接受包含 `@type` 的 JSON 数据
  3. **类型明确**：反序列化时，尽量使用明确的泛型或 Class，而非 `Object`
     - ✅ `JSON.parseObject(json, User.class)`
     - ❌ `JSON.parseObject(json, Object.class)`

