# Oracle 数据库集成说明

## 📋 概述

本项目现已支持 Oracle 数据库作为数据读取源，可以从 Oracle 数据库读取数据并索引到 Elasticsearch 或文件。

## 🚀 快速开始

### 1. 启动 Oracle 服务

使用 Docker Compose 启动 Oracle：

```bash
# 启动 Oracle 服务
docker-compose up -d oracle

# 查看 Oracle 日志
docker-compose logs -f oracle

# 检查 Oracle 状态
docker exec index-oracle healthcheck.sh
```

**注意：** Oracle 首次启动可能需要 1-2 分钟，请耐心等待。

### 2. 连接信息

| 参数 | 值 |
|------|-----|
| 主机 | localhost |
| 端口 | 1521 |
| 服务名 | XEPDB1 |
| 用户名 | system |
| 密码 | oracle123 |
| JDBC URL | jdbc:oracle:thin:@localhost:1521/XEPDB1 |

### 3. 运行应用（使用 Oracle）

```bash
# 使用 Oracle 配置启动应用
java -jar app.jar --spring.profiles.active=oracle

# 或使用 Maven
mvn spring-boot:run -Dspring-boot.run.profiles=oracle
```

## 🧪 测试

### 运行 Oracle 测试

我们提供了便捷的测试脚本：

```bash
# Oracle → File 测试
./scripts/test-oracle.sh file

# Oracle → Elasticsearch 测试
./scripts/test-oracle.sh es

# 运行所有 Oracle 测试
./scripts/test-oracle.sh all
```

### 手动运行测试

```bash
# 测试 Oracle → File
mvn test -Dtest=OracleToFileJobTest

# 测试 Oracle → Elasticsearch
mvn test -Dtest=OracleToElasticsearchJobTest
```

## 📝 配置说明

### application-oracle.yml 配置

```yaml
spring:
  datasource:
    url: jdbc:oracle:thin:@localhost:1521:XE
    username: index_user
    password: index123
    driver-class-name: oracle.jdbc.OracleDriver

index:
  dataSource:
    type: oracle
    table: SAMPLE_DATA
    deltaTable: SAMPLE_DATA_CHANGELOG
    timeColumn: UPDATED_AT
    idColumn: ID
```

### 关键配置项

- `spring.datasource.driver-class-name`: 必须设置为 `oracle.jdbc.OracleDriver`
- `index.dataSource.type`: 设置为 `oracle`
- `index.dataSource.table`: Oracle 表名（大写）
- `index.dataSource.idColumn`: 主键列名（默认 ID）
- `index.dataSource.timeColumn`: 时间戳列名（默认 UPDATED_AT）

## 🗃️ 数据库初始化

### 自动初始化

Oracle 容器启动时会自动执行 `sql/oracle_init.sql` 脚本，创建以下内容：

1. **序列 (Sequences)**
   - distributed_locks_seq
   - idempotency_records_seq
   - job_execution_records_seq
   - sample_data_seq
   - sample_data_changelog_seq

2. **表 (Tables)**
   - distributed_locks - 分布式锁表
   - idempotency_records - 幂等性记录表
   - job_execution_records - 任务执行记录表
   - SAMPLE_DATA - 示例数据表
   - SAMPLE_DATA_CHANGELOG - 变更日志表

3. **触发器 (Triggers)**
   - 自动生成主键ID
   - 自动更新时间戳

### 手动初始化

如果需要手动执行初始化脚本：

```bash
# 连接到 Oracle 容器
docker exec -it index-oracle sqlplus index_user/index123@XE

# 或从主机执行 SQL 文件
docker exec -i index-oracle sqlplus index_user/index123@XE @/container-entrypoint-initdb.d/01_init.sql
```

## 🔧 Oracle 特性支持

### 1. 全量索引

读取 Oracle 表中的所有数据：

```bash
curl -X POST http://localhost:8080/api/jobs/full \
     -H "Content-Type: application/json" \
     -d '{
       "jobType": "FULL",
       "dataSourceType": "oracle",
       "indexTargetType": "elasticsearch"
     }'
```

### 2. 增量索引

基于时间戳的增量索引：

```bash
curl -X POST http://localhost:8080/api/jobs/incremental \
     -H "Content-Type: application/json" \
     -d '{
       "jobType": "INCREMENTAL",
       "dataSourceType": "oracle",
       "indexTargetType": "elasticsearch",
       "timeWindowMinutes": 5
     }'
```

### 3. 分页读取

Oracle 使用 `ROWNUM` 进行分页：

```yaml
index:
  parallelism:
    chunkSize: 100  # 每页读取 100 条记录
    threads: 4      # 并行线程数
    partitions: 4   # 分区数
```

## 🐛 常见问题

### 1. Oracle 启动失败

**问题：** Docker 容器无法启动或持续重启

**解决方案：**
```bash
# 查看日志
docker-compose logs oracle

# 清理并重新创建
docker-compose down -v
docker-compose up -d oracle
```

### 2. 连接超时

**问题：** 应用无法连接到 Oracle

**解决方案：**
```bash
# 检查 Oracle 是否就绪
docker exec index-oracle healthcheck.sh

# 测试连接
docker exec index-oracle sqlplus index_user/index123@XE

# 检查端口
netstat -an | grep 1521
```

### 3. 表名大小写问题

**问题：** ORA-00942: table or view does not exist

**解决方案：**
- Oracle 表名默认为大写
- 配置文件中使用大写表名：`table: SAMPLE_DATA`
- 或在 SQL 中使用双引号：`"sample_data"`

### 4. 日期格式问题

**问题：** 日期格式不匹配

**解决方案：**
```yaml
spring:
  datasource:
    hikari:
      connection-init-sql: ALTER SESSION SET NLS_DATE_FORMAT='YYYY-MM-DD HH24:MI:SS'
```

### 5. 字符集问题

**问题：** 中文乱码

**解决方案：**
```yaml
spring:
  datasource:
    url: jdbc:oracle:thin:@localhost:1521:XE?oracle.jdbc.defaultNChar=true
```

## 📊 性能优化

### 1. 连接池配置

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### 2. 批量操作

```yaml
index:
  parallelism:
    chunkSize: 1000    # 增大 chunk 大小
    threads: 8         # 增加并行线程
    partitions: 8      # 增加分区数
```

### 3. 游标优化

对于大数据量，建议使用游标模式：

```java
// 在代码中配置
reader.setFetchSize(1000);
reader.setMaxRows(Integer.MAX_VALUE);
```

## 🔗 相关文档

- [Oracle JDBC 官方文档](https://docs.oracle.com/en/database/oracle/oracle-database/21/jjdbc/)
- [Spring Batch 文档](https://docs.spring.io/spring-batch/docs/current/reference/html/)
- [项目架构设计](./架构设计说明.md)
- [测试组合说明](./测试组合说明.md)

## 📞 支持

如有问题，请查看：
1. [故障手册](./SOP-故障手册.md)
2. [项目 README](../README.md)
3. 查看容器日志：`docker-compose logs oracle`

