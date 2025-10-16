# IDEA 测试运行指南

## ⚠️ 注意事项

由于 Maven 配置的私有仓库（`10.0.0.106:8081`）无法访问，建议**直接在 IDEA 中运行测试**。

## 🚀 运行 Oracle 测试（在 IDEA 中）

### 前提条件

1. **启动 Oracle Docker 容器**

```bash
cd /Users/cailiang/Desktop/java/index
docker-compose up -d oracle
```

2. **等待 Oracle 启动完成**（约 1-2 分钟）

```bash
# 检查 Oracle 健康状态
docker-compose ps oracle

# 等待状态变为 "Up (healthy)"
```

### 方法一：运行整个测试类

1. 在 IDEA 中打开测试类：
   - `src/test/java/com/company/index/batch/job/OracleToFileJobTest.java`
   - `src/test/java/com/company/index/batch/job/OracleToElasticsearchJobTest.java`

2. 右键点击类名 → 选择 **"Run 'OracleToFileJobTest'"**

### 方法二：运行单个测试方法

1. 在 IDEA 中打开测试类

2. 找到要运行的测试方法（例如 `testOracleToFileFullIndexJob`）

3. 点击方法左侧的绿色运行按钮 ▶️

### 方法三：使用 IDEA 的 Run Configuration

1. 点击 IDEA 顶部的 **Run** → **Edit Configurations...**

2. 点击左上角的 **+** → 选择 **JUnit**

3. 配置如下：
   - **Name**: `Oracle to File Test`
   - **Test kind**: `Class`
   - **Class**: `com.company.index.batch.job.OracleToFileJobTest`
   - **Module**: `index`
   - **JRE**: `Amazon Corretto 17.0.5` (或您安装的 JDK 17)

4. 点击 **OK** 保存

5. 点击顶部的运行按钮 ▶️ 运行测试

## 📊 Oracle 测试列表

### 1. OracleToFileJobTest

测试 Oracle → File 数据流：

```java
testOracleToFileFullIndexJob()      // 全量索引测试
testOracleConnectionAndQuery()      // 连接测试
```

### 2. OracleToElasticsearchJobTest

测试 Oracle → Elasticsearch 数据流：

```java
testOracleToElasticsearchFullIndexJob()  // 全量索引测试
testOracleIncrementalIndex()            // 增量索引测试
```

## ✅ 预期结果

### 成功标志

- ✅ 测试状态显示为绿色 ✓
- ✅ 控制台输出：`Job应该成功完成`
- ✅ 控制台输出：`应该至少读取了一些数据`
- ✅ 对于 File 测试：生成文件 `./target/test-output/oracle-to-file-output.json`

### Oracle → File 测试成功示例

```
准备 Oracle 测试数据...
Oracle 服务已运行且健康。

[INFO] Starting fullIndexJob...
[INFO] Step: cleanupStep COMPLETED
[INFO] Step: readProcessWriteStep COMPLETED (Read: 5, Write: 5)
[INFO] Step: fullCheckStep COMPLETED
[INFO] Step: switchStep COMPLETED

✅ 测试 Oracle → File 全量索引成功！
   - 读取记录数: 5
   - 写入记录数: 5
   - 执行时间: 1234ms
```

### Oracle → Elasticsearch 测试成功示例

```
准备 Oracle 测试数据...
Oracle 服务已运行且健康。

[INFO] Starting fullIndexJob...
[INFO] Step: cleanupStep COMPLETED
[INFO] Step: readProcessWriteStep COMPLETED (Read: 5, Write: 5)
[INFO] Step: fullCheckStep COMPLETED
[INFO] Step: switchStep COMPLETED

✅ 测试 Oracle → ES 全量索引成功！
   - 读取记录数: 5
   - 写入记录数: 5
   - ES 索引: oracle_test_index
   - 执行时间: 2345ms
```

## 🐛 常见问题

### 1. Oracle 容器未启动

**错误信息**:
```
ORA-12514: TNS:listener does not currently know of service requested
```

**解决方案**:
```bash
docker-compose up -d oracle
# 等待 1-2 分钟
docker exec index-oracle healthcheck.sh
```

### 2. Elasticsearch 未启动（仅影响 ES 测试）

**错误信息**:
```
Connection refused: localhost/127.0.0.1:9200
```

**解决方案**:
```bash
docker-compose up -d elasticsearch
```

### 3. GetQuickWriter 依赖问题

**错误信息**:
```
required a bean of type 'org.springframework.web.client.RestTemplate'
```

**解决方案**:
✅ 已修复！请确保使用最新的代码（`GetQuickWriter` 已更新 `@Profile` 注解）

### 4. 编译错误

如果 IDEA 显示编译错误，尝试：

1. **File** → **Invalidate Caches** → **Invalidate and Restart**
2. 或者手动触发重新编译：**Build** → **Rebuild Project**

## 📝 测试配置文件

### Oracle → File 测试配置

文件：`src/test/resources/application-test-oracle-file.yml`

关键配置：
- 数据源: Oracle (`jdbc:oracle:thin:@localhost:1521/XEPDB1`)
- 输出目标: File (`./target/test-output/oracle-to-file-output.json`)
- 用户名/密码: `system/oracle123`

### Oracle → ES 测试配置

文件：`src/test/resources/application-test-oracle-es.yml`

关键配置：
- 数据源: Oracle (`jdbc:oracle:thin:@localhost:1521/XEPDB1`)
- 输出目标: Elasticsearch (`http://localhost:9200`)
- 索引名: `oracle_test_index`

## 🎉 运行其他测试

同样的方法也适用于其他测试：

- `MySQLToFileJobTest` - MySQL → File
- `MySQLToElasticsearchJobTest` - MySQL → ES
- `H2ToElasticsearchJobTest` - H2 → ES
- `CsvToFileJobTest` - CSV → File
- `JsonToFileJobTest` - JSON → File

## 📚 相关文档

- [Oracle 集成说明](docs/Oracle集成说明.md)
- [测试组合说明](docs/测试组合说明.md)
- [IDEA 运行配置指南](IDEA-运行配置指南.md)
- [Oracle Writer 修复说明](ORACLE_WRITER_FIX.md)

---

**提示**: 如果您的网络环境允许访问 Maven 中央仓库或其他公共仓库，也可以修改 `settings.xml` 来使用公共仓库。

