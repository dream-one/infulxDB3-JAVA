

## 一、 引言：为什么选择InfluxDB 3？

在我们的隧道风机监控系统中，实时数据的采集、存储和高效查询是至关重要的核心需求。风机运行产生的振动、倾角、电流、温度等参数是典型的时序数据，具有高并发写入、数据量持续增长以及对近期数据查询性能要求高的特点。传统关系型数据库在应对这类场景时，往往面临性能瓶颈和复杂的查询优化问题。

InfluxDB作为业界领先的时序数据库，其最新版本InfluxDB 3带来了诸多令人振奋的特性。我们选择InfluxDB 3 Core主要基于以下几点考量：

*   **开源与社区支持**：InfluxDB 3 Core作为开源版本，为我们提供了灵活的部署和定制能力，同时拥有活跃的社区支持。
*   **高性能时序引擎**：专为时序数据优化，能够高效处理大规模时间序列数据的写入和查询。
*   **原生SQL支持**：引入了对标准SQL的查询支持，极大地降低了开发人员的学习曲线，使得熟悉SQL的团队成员可以快速上手，并能利用现有的SQL生态工具。
*   **Apache Arrow Flight集成**：通过Apache Arrow Flight进行数据交互，旨在提供更高的数据传输和查询效率。
*   **简化的架构**：支持直接与对象存储集成或使用本地磁盘，简化了部署和运维。

相较于InfluxDB的早期版本，InfluxDB 3在查询语言的通用性（SQL的加入）和底层存储引擎的现代化方面有了显著的进步。本仓库记录了我们将InfluxDB 3 Core集成到基于Java 11和Spring Boot (RuoYi)框架的隧道风机监控系统中的实践过程，旨在分享经验，为其他有类似需求的开发者提供参考。

InfluxDB 3 Core官方文档：https://docs.influxdb.org.cn/influxdb3/core/
---

**二、 环境准备与InfluxDB 3 Core的安装与启动 (Windows 11)**

*   **下载与解压**：
    * 在官方文档中获取InfluxDB 3 Core 压缩包，并解压到目录。
    * 我的解压目录位于`G:\浏览器下载\influxdb3-core-3.0.1-windows_amd64\`
*   **启动InfluxDB 3 Core服务**：
   		使用 influxdb3 serve 命令来启动服务器，并可以通过参数指定对象存储类型和数据目录。
   		我的启动命令如下：
   		
```
.\influxdb3.exe serve --object-store file --node-id mywindowsnode --data-dir "G:\浏览器下载\influxdb3-core-3.0.1-windows_amd64\data"
```

参数解释：
	- `object-store file`: 指定使用本地文件系统作为对象存储。这是默认选项，但明确指出总是个好习惯。
	- `node-id <你的节点ID>`: 为你的InfluxDB节点指定一个唯一的ID，例如 mynode 或 win11node。
	- `data-dir "<你的数据目录>"`: 指定数据存储的目录。

*   **创建管理员令牌 (Admin Token)**：
     在你启动InfluxDB 3服务器之后，你就可以创建管理员令牌了。管理员令牌拥有对InfluxDB 3实例所有操作的最高权限。
好的，我们来逐一解答你的问题。

### 如何创建管理员令牌 (Admin Token)

在你启动InfluxDB 3服务器之后，你就可以创建管理员令牌了。管理员令牌拥有对InfluxDB 3实例所有操作的最高权限。

**步骤如下：**

1.  **确保InfluxDB 3服务器正在运行**：
    你在上一步已经启动了服务器，它应该在第一个命令提示符/PowerShell窗口中运行。

2.  **打开一个新的命令提示符 (CMD) 或 PowerShell 窗口**：
    *   不要关闭正在运行服务器的那个窗口。你需要一个新的窗口来执行`influxdb3`的客户端命令。

3.  **导航到InfluxDB 3的目录**（如果`influxdb3.exe`不在系统PATH中）：
    在新打开的窗口中，输入：
    ```bash
    cd G:\浏览器下载\influxdb3-core-3.0.1-windows_amd64
    ```

4.  **执行创建管理员令牌的命令**：
    根据文档，使用 `influxdb3 create token --admin` 子命令。你可能还需要指定InfluxDB服务器的地址（如果不是默认的 `http://localhost:8181`）。
    ```bash
    .\influxdb3.exe create token --admin --host http://localhost:8181
    ```
    或者，如果 `influxdb3.exe` 已经在你的系统路径 (PATH) 中，或者你就在其目录下，可以简化为：
    ```bash
    influxdb3 create token --admin --host http://localhost:8181
    ```
    *   `--admin`：表示你正在创建一个管理员级别的令牌。
    *   `--host http://localhost:8181`：指定了InfluxDB服务器正在监听的地址和端口。如果你的服务器运行在其他地址或端口，请相应修改。

5.  **保存令牌**：
    执行命令后，它会在窗口中**直接输出一个很长的字符串**，这就是你的管理员令牌。
    **非常重要**：文档中强调 "InfluxDB lets you view the token string only when you create the token. Store your token in a secure location, as you cannot retrieve it from the database later."
    这意味着：
    *   **立即复制这个令牌字符串。**
    *   **将它保存在一个安全的地方** (比如密码管理器或一个受保护的文本文件中)。
    *   一旦你关闭了这个窗口或者执行了其他命令，你就无法再次从InfluxDB中找回这个确切的令牌字符串了。InfluxDB内部只存储令牌的哈希值。

6.  **（可选但推荐）设置环境变量**：
    为了方便以后使用`influxdb3` CLI而不需要每次都输入 `--token` 参数，你可以将这个令牌设置到一个环境变量中。文档推荐的环境变量名是 `INFLUXDB3_AUTH_TOKEN`。
    在**新的**命令提示符窗口中设置（仅对当前窗口有效）：
    ```bash
    set INFLUXDB3_AUTH_TOKEN=你的令牌字符串粘贴在这里
    ```
    或者在PowerShell中（仅对当前窗口有效）：
    ```powershell
    $env:INFLUXDB3_AUTH_TOKEN="你的令牌字符串粘贴在这里"
    ```
    如果你想让这个环境变量在系统重启后依然有效，你需要通过系统属性来设置它（搜索“编辑系统环境变量”）。
现在你就有了一个管理员令牌，可以用它来进行后续的数据库创建、数据写入和查询等操作了。

**三、 Java项目集成：Spring Boot (RuoYi) 与 InfluxDB 3的连接**

*   **选择合适的Java客户端库**：
    对于InfluxDB 3.x，官方推荐使用 `influxdb3-java` 这个新的、轻量级的、社区维护的客户端库。
*   **Maven依赖配置**：
 在 `pom.xml` 中添加 `influxdb3-java` (最新稳定版) 的依赖。
```bash
   <dependency>
            <groupId>com.influxdb</groupId>
            <artifactId>influxdb3-java</artifactId>
            <version>1.0.0</version> <!-- 请使用最新的稳定版本 -->
   </dependency>
```

  
*   **关键JVM参数配置**：
由于 influxdb3-java 底层使用了Apache Arrow Flight，它可能需要访问一些通常被模块系统封装的JDK内部API。你需要在启动你的Spring Boot应用程序时，添加JVM参数。
在 IntelliJ IDEA 中添加 JVM 参数：
```bash
--add-opens=java.base/java.nio=ALL-UNNAMED
```
在命令行中运行 JAR 包时：

```bash
java --add-opens=java.base/java.nio=ALL-UNNAMED -jar your-application.jar
```

*   **Spring Boot配置文件 (`application.yml` 或 `.properties`)**：
    *   配置InfluxDB 3的连接信息 (`host`, `token`, `database`)。
```yaml
influxdb:
  client3:
    host: http://localhost:8181
    token: apiv3_60Fa_JuZFuH5563Lfu6Ag63yml9KJnG-GDDa2dCQllf9JTQXebf76OiNIZOHaPnosx78hB61Q_e1ziO26F585g
    database: tunnel_fengji # 你的InfluxDB 3数据库名
```

*   **创建InfluxDB服务层 (`IInfluxDBService` 接口和 `InfluxDBService` 实现类)**：
    *   `InfluxDBService` 实现：
        *   `@PostConstruct` 初始化 `InfluxDBClient.getInstance()`。
        *   `@PreDestroy` 关闭客户端 `client.close()`。
        *   **数据库自动创建机制**：解释InfluxDB 3在首次写入时会自动创建数据库和表。


## 四、 核心操作：数据写入与查询

### 数据模型设计 (针对隧道风机监控)

在InfluxDB 3中，数据组织的核心概念包括数据库(Database)、表(Table，在InfluxDB语境下也常称为Measurement)、标签(Tag)和字段(Field)。时间戳(Time)是每条记录固有的组成部分。

对于我们的隧道风机监控系统，我们设计了如下的数据模型：

*   **数据库 (Database)**：我们创建了一个名为 `tunnel_fan_monitoring` (或根据实际项目命名)的数据库，作为所有风机监控数据的统一存储容器。

*   **表/Measurement (Table / Measurement)**：
    *   考虑到风机产生的各类传感器数据（振动、倾角、电流、温度等）通常是描述同一设备在相近时间点的状态，并且我们可能需要将这些数据一起分析，我们决定采用一个统一的表来存储这些指标。
    *   **表名**: `device_metrics`
        *   这个表将包含所有风机的各类传感器读数。如果未来有特定类型的传感器数据量极大或查询模式非常独立，也可以考虑拆分为更细粒度的表。

*   **标签 (Tags)**：
    标签用于存储元数据，是数据点的主要索引维度，常用于`WHERE`子句的过滤和`GROUP BY`子句的分组。在我们的风机监控场景中，关键的标签包括：
    *   `iotId` (String): 硬件设备在物联网平台上的唯一标识符。
    *   `productKey` (String): 设备所属的产品型号标识。
    *   `deviceName` (String): 设备的自定义名称，例如 "tunnel-A-fan-01"，这是我们系统中标识具体风机的主要业务ID。
    *   `deviceType` (String): 设备类型，例如 "FanDevice", "SensorHub"，用于区分不同类型的硬件。
    *   `(可选) location_zone` (String): 风机所在的隧道区域或更细分的地理位置标签，如果需要按区域进行聚合分析。

    **重要特性：标签集合与顺序的不可变性**
    InfluxDB 3的一个核心设计是，当数据首次写入一个新表时，该表中出现的标签键及其顺序（InfluxDB内部决定的顺序）就被固定下来了。之后，你**不能再为这个表添加新的标签键**。这意味着在设计初期，必须仔细规划好一个表需要哪些核心的、用于索引和分组的维度作为标签。如果后续确实需要新的索引维度，可能需要重新设计表结构或创建新表。

*   **字段 (Fields)**：
    字段用于存储实际的测量值或具体的属性信息。对于风机监控数据，字段将包括：
    *   `ax`, `ay`, `az` (Double): X, Y, Z轴的振动值。
    *   `roll`, `pitch`, `yaw` (Double): 翻滚角、俯仰角、偏航角。
    *   `LightCurrent` (Double): 光照传感器电流（或根据实际意义命名，如`operating_current`）。
    *   `temperature` (Double): 温度读数。
    *   `(可选) status_message` (String): 风机的详细状态描述或错误信息（如果不是主要用于过滤或聚合）。
    *   `(可选) online_status` (Boolean/Integer): 表示设备在线状态的布尔值或整数值，如果设备上下线事件也作为时序数据记录。

*   **时间戳 (Time)**：
    *   每条数据点都必须有一个时间戳，表示数据采集或事件发生的时间。InfluxDB 3支持纳秒级精度，我们在Java客户端中统一使用 `Instant` 对象，并以纳秒精度写入。

这个数据模型旨在平衡查询灵活性和InfluxDB的性能特点。通过合理的标签设计，我们可以高效地根据设备ID、类型或位置筛选数据，并通过字段获取具体的监控指标。

---

## 五、 总结与展望

将InfluxDB 3 Core集成到我们的Java (Spring Boot + RuoYi)项目中是一次富有挑战和收获的经历。InfluxDB 3凭借其对SQL的友好支持、基于Apache Arrow Flight的潜力以及简化的架构，为时序数据处理提供了一个现代且高效的解决方案。

**使用体验小结：**

*   **优点**：
    *   **SQL查询的便利性**：对于熟悉SQL的团队来说，上手速度快，查询编写直观。
    *   **安装与启动简便**：InfluxDB 3 Core的单体二进制文件和灵活的存储选项（本地文件或对象存储）使得本地开发和测试环境的搭建非常方便。
    *   **写入性能**：在我们的测试中，Line Protocol的写入表现良好。
    *   **`influxdb3-java`客户端**：作为专为InfluxDB 3设计的客户端，它提供了与Arrow Flight交互的直接途径，其 `PointValues` API在获取类型化数据时非常方便。
*   **当前阶段的挑战与注意点**：
    *   **客户端生态与文档**：`influxdb3-java` 作为一个相对较新的客户端，其API和最佳实践仍在发展和完善中。官方文档和社区示例相较于成熟的InfluxDB 2.x客户端还有待丰富。
    *   **JVM参数**：需要额外配置 `--add-opens` JVM参数，这在某些受限环境中可能需要额外协调。
    *   **标签模式的不可变性**：需要开发者在设计初期就仔细规划标签，后续修改成本较高。
    *   **`influxdb3-java` 的 `query()` API 返回 `Stream<Object[]>`**：虽然直接，但在转换为结构化对象（如 `Map`）时，需要调用者自行处理列名和顺序，不如 `queryPoints()` 返回 `Stream<PointValues>` 那样直接获取命名数据方便。

**给其他开发者的建议：**

1.  **拥抱 `influxdb3-java`**：对于新的InfluxDB 3项目，优先考虑使用官方推荐的 `influxdb3-java` 客户端。
2.  **仔细阅读客户端README和示例**：由于生态在发展，最新的官方示例和README是获取正确用法的重要途径。
3.  **理解数据模型核心**：深刻理解InfluxDB 3中Measurement、Tag（特别是其不可变性）、Field和Time的概念，这对设计高效的 schema至关重要。
4.  **优先使用 `queryPoints()`**：当需要结构化地访问查询结果中的标签和字段时，`client.queryPoints()` 返回的 `Stream<PointValues>` 是更类型安全和便捷的选择。
5.  **充分测试**：在不同场景下测试数据写入的正确性、查询的准确性和性能。
6.  **关注社区**：积极参与InfluxData社区和相关GitHub仓库的讨论，获取最新进展和寻求帮助。

**未来展望：**

随着InfluxDB 3生态的不断成熟，我们期待：

*   `influxdb3-java`客户端库功能更加完善，例如提供更便捷的参数化`queryPoints` API，以及更丰富的从查询结果到Java对象的转换工具。
*   更详尽的官方文档和最佳实践指南，特别是在Java生态集成方面。
*   InfluxDB 3 Core本身功能的进一步增强和优化。

总而言之，InfluxDB 3 Core为Java开发者提供了一个强大且有前景的时序数据解决方案。虽然在集成过程中可能会遇到一些与新技术和发展中生态相关的挑战，但其核心优势和未来的发展潜力值得我们投入和探索。

---
