# H2 批处理业务流程测试

## 当前状态总结

### ✅ 已完成
1. **H2 模式配置**:
   - 创建了 `application-h2.yml` 配置文件
   - H2 内存数据库配置
   - H2 控制台已启用
   - 禁用了 Redis 和 Elasticsearch（H2 模式不需要）

2. **Quartz 调度器**:
   - H2 模式使用内存 JobStore（不需要数据库表）
   - 禁用了 QuartzConfig 在 H2 模式下的加载

3. **数据初始化**:
   - 创建了 `schema-h2.sql` 包含:
     - `sample_data` 表（测试数据）
     - Spring Batch 元数据表
   - 插入了 5 条测试数据

4. **文件写入器**:
   - 创建了 `IndexFileWriter` 用于 H2 模式
   - 输出到 `./output` 目录的 JSON 文件

5. **安全配置**:
   - 完全禁用认证（方便测试）
   - 禁用 X-Frame-Options（允许 H2 控制台）

### ❌ 当前问题

**Spring Batch SQL 语法不兼容 H2**
- Spring Batch 的自动建表 SQL 与 H2 数据库语法不兼容
- 已创建手动 schema，但仍有缓存或加载顺序问题

## 解决方案

### 方案 1: 使用 MySQL 模式（推荐）
```bash
# 1. 修改 application.yml
spring:
  profiles:
    active: dev  # 改为 dev

# 2. 启动 Docker MySQL
docker compose up -d index-mysql

# 3. 启动应用
mvn spring-boot:run -s settings.xml -DskipTests=true

# 4. 触发任务
curl -X POST http://localhost:8080/api/jobs/full
```

### 方案 2: 简化 H2 配置（当前尝试）
```bash
# 已完成的步骤:
# 1. 创建 schema-h2.sql（Spring Batch + 测试数据）
# 2. 禁用 Spring Batch 自动建表
# 3. 删除重复的 data-h2.sql

# 下一步:
# 需要解决 schema 加载顺序和缓存问题
```

## 业务流程说明

完整的批处理业务流程应该包括:

1. **数据读取**:
   - `RdbFullReader` 从 `sample_data` 表读取数据
   - 使用 JDBC 分页读取（chunk size = 100）

2. **数据处理**:
   - 将 `SourceRecord` 转换为 `IndexDocument`
   - 添加元数据（timestamp, source, version）

3. **数据写入**:
   - H2 模式: `IndexFileWriter` 写入 JSON 文件到 `./output/`
   - Dev 模式: `ElasticsearchWriter` 写入到 Elasticsearch

4. **任务监控**:
   - Spring Batch 元数据表记录执行状态
   - 日志输出到 `logs/batch.log`
   - Actuator 端点提供健康检查

## 手动测试步骤

### Step 1: 确认应用启动
```bash
curl http://localhost:8080/actuator/health
# 预期: {"status":"UP"...}
```

### Step 2: 访问 H2 控制台
```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:testdb
用户名: sa
密码: (空)
```

### Step 3: 检查数据
```sql
-- 查看所有表
SHOW TABLES;

-- 查看测试数据
SELECT * FROM sample_data;

-- 查看 Spring Batch 表
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_NAME LIKE 'BATCH%';
```

### Step 4: 触发批处理任务
```bash
curl -X POST http://localhost:8080/api/jobs/full \
  -H "Content-Type: application/json" \
  -d '{
    "jobType":"FULL",
    "dataSourceType":"h2",
    "indexTargetType":"file"
  }'
```

### Step 5: 查看输出
```bash
# 查看输出文件
ls -lh ./output/

# 查看文件内容
cat ./output/index-output-*.json
```

### Step 6: 检查执行日志
```bash
# 查看批处理日志
tail -100 logs/batch.log

# 查看应用日志
tail -100 logs/application.log | grep -i "batch\|job\|step"
```

## 预期结果

成功执行后应该看到:

1. **输出文件**: `./output/index-output-{timestamp}.json`
2. **文件内容**: 包含 5 条记录的 JSON 数组
   ```json
   [
     {
       "id": "1",
       "type": "INSERT",
       "timestamp": "2025-10-14T...",
       "source": "h2",
       "version": 1,
       "data": {"id": 1, "name": "John Doe", "email": "john@example.com", ...}
     },
     ...
   ]
   ```
3. **Batch 状态**: `BATCH_JOB_EXECUTION` 表中有 `COMPLETED` 状态的记录
4. **日志输出**: 显示读取、处理、写入的记录数

## 下一步

建议暂时使用 **MySQL 模式**测试完整业务流程，因为:
1. MySQL 模式已经完全配置好
2. Spring Batch 与 MySQL 兼容性更好
3. 更接近生产环境

H2 模式的 SQL 兼容性问题需要更多时间调试。

