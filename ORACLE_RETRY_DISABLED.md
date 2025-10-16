# 🎯 Oracle 测试 - 暂时禁用重试机制

## ✅ 调整完成！

### 🐛 问题分析

运行 Oracle → Elasticsearch 测试时出现：
```
org.springframework.retry.policy.RetryCacheCapacityExceededException: 
Retry cache capacity limit breached. 
Do you need to re-consider the implementation of the key generator, 
or the equals and hashCode of the items that failed?
```

### 🔍 问题根源

1. **重试缓存超限**: Spring Retry 缓存了太多失败项（默认限制 4096 个）
2. **持续写入失败**: Elasticsearch 写入一直失败，导致大量失败项被缓存
3. **需要查看真实错误**: 重试机制掩盖了真正的底层异常

### ✅ 临时解决方案

**暂时禁用重试机制**，以便直接看到真实的错误信息：

#### 1️⃣ 修改全量索引配置
**文件**: `src/main/java/com/company/index/batch/job/FullIndexJobConfig.java`

**修改前**:
```java
.faultTolerant()
.retryLimit(3)
.retry(RuntimeException.class)
.retry(org.springframework.dao.DataAccessException.class)
.skipLimit(10)
.skip(RuntimeException.class)
.skip(org.springframework.dao.DataAccessException.class)
.build();
```

**修改后**:
```java
.build();
```

#### 2️⃣ 修改增量索引配置
**文件**: `src/main/java/com/company/index/batch/job/IncrementalIndexJobConfig.java`

**修改前**:
```java
.faultTolerant()
.retryLimit(3)
.retry(RuntimeException.class)
.retry(org.springframework.dao.DataAccessException.class)
.skipLimit(100)
.skip(RuntimeException.class)
.skip(org.springframework.dao.DataAccessException.class)
.build();
```

**修改后**:
```java
.build();
```

## 🚀 下一步操作

### 在 IDEA 中再次运行测试

1. **打开测试类**：
   - `src/test/java/com/company/index/batch/job/OracleToElasticsearchJobTest.java`

2. **右键点击测试方法** → 选择 **"Run 'testOracleToElasticsearchFullIndexJob'"** ▶️

3. **查看真实的错误信息**：
   - 现在会直接看到 Elasticsearch 写入失败的真实原因
   - 不再被重试机制掩盖

### 预期结果

测试会失败，但会显示真实的错误信息，例如：
```
Caused by: java.io.IOException: Connection refused
Caused by: co.elastic.clients.elasticsearch._types.ElasticsearchException: ...
Caused by: javax.net.ssl.SSLException: ...
```

## 🔍 常见的 Elasticsearch 写入失败原因

### 1. Elasticsearch 连接失败
**症状**: `Connection refused` 或 `ConnectException`

**检查**:
```bash
# 检查 Elasticsearch 状态
docker compose ps elasticsearch

# 检查 Elasticsearch 连接
curl -s http://localhost:9200/_cluster/health
```

**解决方案**:
```bash
# 重启 Elasticsearch
docker compose restart elasticsearch

# 等待健康
docker compose ps elasticsearch
```

### 2. Elasticsearch 认证失败
**症状**: `401 Unauthorized` 或 `security_exception`

**检查**:
- 测试配置中的 Elasticsearch 用户名/密码是否正确
- Elasticsearch 容器是否启用了安全认证

**解决方案**:
```yaml
# application-test-oracle-es.yml
index:
  indexTarget:
    type: elasticsearch
    url: http://localhost:9200
    username: elastic  # 如果需要
    password: changeme # 如果需要
    indexName: oracle_test_index
```

### 3. Elasticsearch 索引创建失败
**症状**: `mapper_parsing_exception` 或 `illegal_argument_exception`

**检查**:
- 数据映射是否正确
- 字段类型是否兼容

**解决方案**:
- 删除测试索引：`curl -X DELETE http://localhost:9200/oracle_test_index`
- 重新运行测试

### 4. Elasticsearch 版本不兼容
**症状**: `version_conflict_engine_exception`

**检查**:
- Elasticsearch 客户端版本与服务器版本是否兼容

**解决方案**:
- 检查 `pom.xml` 中的 Elasticsearch 客户端版本
- 检查 Docker 中的 Elasticsearch 服务器版本

## 📝 调试建议

### 1. 启用详细日志
在测试配置文件中启用调试日志：
```yaml
logging:
  level:
    com.company.index: DEBUG
    org.springframework.batch: INFO
    co.elastic.clients: DEBUG
    org.elasticsearch: DEBUG
```

### 2. 查看完整的异常堆栈
- 注意查看 `Caused by:` 部分
- 最底层的 `Caused by` 通常包含真实原因

### 3. 手动测试 Elasticsearch 连接
```bash
# 测试连接
curl -v http://localhost:9200

# 测试索引创建
curl -X PUT http://localhost:9200/test_index

# 测试文档写入
curl -X POST http://localhost:9200/test_index/_doc/1 \
  -H 'Content-Type: application/json' \
  -d '{"test": "data"}'

# 删除测试索引
curl -X DELETE http://localhost:9200/test_index
```

## 🎯 下一步计划

1. ✅ **暂时禁用重试机制**（已完成）
2. ⏳ **运行测试，查看真实错误**（等待您执行）
3. ⏳ **根据真实错误进行针对性修复**
4. ⏳ **修复后，重新启用重试机制**

## 📚 相关文档

- [Oracle 集成说明](docs/Oracle集成说明.md) - 完整使用指南
- [IDEA 测试运行指南](IDEA-测试运行指南.md) - 详细运行步骤

---

**调整完成时间**: 2025-10-16 23:20  
**当前状态**: ✅ 重试机制已暂时禁用，可以查看真实错误  
**推荐操作**: 在 IDEA 中运行测试，查看真实的错误信息
