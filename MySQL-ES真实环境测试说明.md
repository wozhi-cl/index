# MySQL + Elasticsearch 真实环境测试说明

## 📋 概述

现在已经创建了使用**真实MySQL和Elasticsearch**（通过Docker）的集成测试，不再使用Mock。

## 🎯 新增测试组合

### 1️⃣ MySQL → Elasticsearch (真实环境)

**测试类**: `MysqlToElasticsearchJobTest.java`  
**配置文件**: `application-test-mysql-es.yml`

**测试内容**:
- ✅ 全量索引测试 (`testFullIndexJobWithMysqlToElasticsearch`)
- ✅ 增量索引测试 (`testIncrementalIndexJobWithMysqlToElasticsearch`)

**数据流**:
```
MySQL (Docker)  →  读取数据  →  Spring Batch  →  写入  →  Elasticsearch (Docker)
localhost:3307                                              localhost:9200
```

### 2️⃣ MySQL → File

**测试类**: `MysqlToFileJobTest.java`  
**配置文件**: `application-test-mysql-file.yml`

**测试内容**:
- ✅ 全量索引测试 (`testFullIndexJobWithMysqlToFile`)

**数据流**:
```
MySQL (Docker)  →  读取数据  →  Spring Batch  →  写入  →  JSON文件
localhost:3307                                        ./target/test-output/
```

---

## 🔧 环境配置

### Docker服务

#### MySQL配置
```yaml
主机: localhost:3307
数据库: index_db
用户: index_user
密码: index123
```

#### Elasticsearch配置
```yaml
URL: http://localhost:9200
索引名: test_index
批量大小: 100
```

#### Kibana配置（可选）
```yaml
URL: http://localhost:5601
用于查看ES数据
```

---

## 🚀 运行测试

### 1. 启动Docker服务

```bash
# 启动MySQL和Elasticsearch
docker-compose up -d mysql elasticsearch

# 可选：启动Kibana用于查看数据
docker-compose up -d kibana

# 检查服务状态
bash scripts/check-docker-services.sh
```

### 2. 验证服务状态

**MySQL**:
```bash
docker exec index-mysql mysqladmin ping -h localhost -u root -proot123
```

**Elasticsearch**:
```bash
curl http://localhost:9200/_cluster/health
```

### 3. 运行测试

#### 方式一：IDE运行
- 右键 `MysqlToElasticsearchJobTest` → Run
- 右键 `MysqlToFileJobTest` → Run
- 右键 `AllCombinationsTestSuite` → Run (运行所有组合)

#### 方式二：Maven命令
```bash
# 运行单个测试类
mvn test -Dtest=MysqlToElasticsearchJobTest

# 运行所有组合测试
mvn test -Dtest=AllCombinationsTestSuite
```

---

## 📊 完整测试矩阵

| 数据源 | 索引目标 | 测试类 | 说明 |
|--------|----------|--------|------|
| **H2** | File | `FullIndexJobTest` | 内存数据库 |
| **H2** | File | `IncrementalIndexJobTest` | 内存数据库 |
| **H2** | ES (Mock) | `H2ToElasticsearchJobTest` | Mock ES |
| **CSV** | File | `CsvToFileJobTest` | 文件读取 |
| **JSON** | File | `JsonToFileJobTest` | JSONL格式 |
| **MySQL (容器)** | **ES (真实)** | `MysqlToElasticsearchJobTest` | ⭐ 新增 |
| **MySQL (容器)** | File | `MysqlToFileJobTest` | ⭐ 新增 |

---

## 🔍 测试执行流程

### MySQL测试标准流程

1. **@BeforeEach - 准备测试数据**
   ```java
   • TRUNCATE sample_data 表
   • 插入20条测试数据到MySQL
   • 打印数据准备完成信息
   ```

2. **@Test - 执行Job**
   ```java
   • 创建JobParameters（带唯一ID和时间戳）
   • 启动Job（全量或增量）
   • 等待Job完成（同步执行）
   • 验证Job状态
   ```

3. **验证结果**
   - **ES测试**: 数据写入Elasticsearch，可通过Kibana查看
   - **File测试**: 验证输出文件存在、大小、内容

4. **@AfterEach - 清理数据**
   ```java
   • TRUNCATE sample_data 表
   • 打印清理完成信息
   • ES数据保留（便于验证）
   ```

---

## 📈 查看测试结果

### Elasticsearch数据查看

#### 方式一：curl命令
```bash
# 查看索引统计
curl http://localhost:9200/test_index/_stats?pretty

# 查看所有文档
curl http://localhost:9200/test_index/_search?pretty

# 查看文档数量
curl http://localhost:9200/test_index/_count
```

#### 方式二：Kibana界面
1. 访问 http://localhost:5601
2. 创建Index Pattern: `test_index`
3. 在Discover页面查看数据

### 文件输出查看

```bash
# 查看输出文件
ls -lh target/test-output/

# 查看文件内容
cat target/test-output/mysql-to-file-output-*.json | jq
```

---

## 🛠️ 故障排查

### 问题1: MySQL连接失败

**症状**: `CommunicationsException: Communications link failure`

**解决**:
```bash
# 检查MySQL容器状态
docker ps | grep mysql

# 重启MySQL容器
docker-compose restart mysql

# 查看MySQL日志
docker logs index-mysql
```

### 问题2: Elasticsearch连接失败

**症状**: `ConnectException: Connection refused`

**解决**:
```bash
# 检查ES容器状态
docker ps | grep elasticsearch

# 重启ES容器
docker-compose restart elasticsearch

# 查看ES日志
docker logs index-elasticsearch

# 等待ES启动完成（可能需要30秒）
curl -I http://localhost:9200
```

### 问题3: 测试数据未清理

**症状**: 测试报告显示数据重复

**解决**:
```bash
# 手动清理MySQL数据
docker exec -it index-mysql mysql -u index_user -pindex123 index_db -e "TRUNCATE TABLE sample_data;"

# 删除ES索引
curl -X DELETE http://localhost:9200/test_index
```

### 问题4: Docker服务未启动

**症状**: `Connection refused`

**解决**:
```bash
# 启动所有服务
docker-compose up -d

# 检查服务状态
bash scripts/check-docker-services.sh
```

---

## 💡 最佳实践

### 1. 测试前检查
```bash
# 始终先检查Docker服务状态
bash scripts/check-docker-services.sh
```

### 2. 清理测试数据
```bash
# 定期清理ES测试索引
curl -X DELETE http://localhost:9200/test_index
```

### 3. 查看实时日志
```bash
# 在另一个终端查看应用日志
tail -f logs/application.log

# 查看ES日志
docker logs -f index-elasticsearch

# 查看MySQL日志
docker logs -f index-mysql
```

### 4. 性能测试
```bash
# 调整测试数据量（修改prepareTestData方法）
for (int i = 1; i <= 1000; i++) { ... }  // 增加到1000条

# 查看Job执行时间
grep "执行耗时" logs/application.log
```

---

## 📁 相关文件清单

### 测试配置
- `src/test/resources/application-test-mysql-es.yml` - MySQL→ES配置
- `src/test/resources/application-test-mysql-file.yml` - MySQL→File配置

### 测试类
- `src/test/java/.../MysqlToElasticsearchJobTest.java` - MySQL→ES测试
- `src/test/java/.../MysqlToFileJobTest.java` - MySQL→File测试
- `src/test/java/.../AllCombinationsTestSuite.java` - 测试套件（已更新）

### 脚本
- `scripts/check-docker-services.sh` - Docker服务检查脚本 ⭐ 新增

### Docker配置
- `docker-compose.yml` - Docker服务定义

---

## 🎉 总结

现在您有了：
- ✅ **5个不同的测试组合**
- ✅ **真实的MySQL和Elasticsearch测试**（不使用Mock）
- ✅ **完整的测试框架**（AbstractJobTest）
- ✅ **自动化测试套件**（AllCombinationsTestSuite）
- ✅ **便捷的检查脚本**（check-docker-services.sh）

可以全面测试您的Spring Batch索引系统！

