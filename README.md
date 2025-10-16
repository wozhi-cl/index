# 索引管理系统 - 使用指南

## 🚀 快速开始

### 方式一：Docker Compose 一键启动（推荐）

```bash
# 1. 构建并启动所有服务
./scripts/dev-setup.sh

# 2. 查看服务状态
docker-compose ps

# 3. 查看应用日志
docker-compose logs -f index-app

# 4. 测试 API
./scripts/test-api.sh
```

### 方式二：本地开发（支持调试）

```bash
# 1. 启动依赖服务
docker-compose up -d mysql elasticsearch redis

# 2. 本地运行应用（支持调试）
./scripts/local-dev.sh
```

### 方式三：使用 Oracle 数据库 ⭐ 新增

```bash
# 1. 启动 Oracle 服务
docker-compose up -d oracle

# 2. 等待 Oracle 启动完成（约 1-2 分钟）
docker exec index-oracle healthcheck.sh

# 3. 运行 Oracle 测试
./scripts/test-oracle.sh file  # Oracle → File 测试
./scripts/test-oracle.sh es    # Oracle → Elasticsearch 测试

# 4. 使用 Oracle 配置启动应用
java -jar app.jar --spring.profiles.active=oracle

# 详细说明请查看：docs/Oracle集成说明.md
```

## 🔧 开发调试

### IDE 调试配置

**IntelliJ IDEA:**
1. 打开 `Run/Debug Configurations`
2. 选择 `Remote JVM Debug`
3. 设置：
   - Host: `localhost`
   - Port: `5005`
   - Command line arguments: `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005`

**VS Code:**
```json
{
    "type": "java",
    "name": "Debug Index App",
    "request": "attach",
    "hostName": "localhost",
    "port": 5005
}
```

**Eclipse:**
1. 右键项目 → Debug As → Debug Configurations
2. 选择 Remote Java Application
3. 设置 Host: `localhost`, Port: `5005`

### 本地开发环境变量

```bash
export SPRING_PROFILES_ACTIVE=dev
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=index_db
export DB_USERNAME=index_user
export DB_PASSWORD=index123
export ES_URL=http://localhost:9200
export REDIS_URL=redis://localhost:6379
```

## 📊 服务访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 应用服务 | http://localhost:8080 | 主应用 |
| 健康检查 | http://localhost:8080/actuator/health | 应用健康状态 |
| 指标数据 | http://localhost:8080/actuator/prometheus | Prometheus 指标 |
| Elasticsearch | http://localhost:9200 | 搜索引擎 |
| Kibana | http://localhost:5601 | 日志分析 |
| Prometheus | http://localhost:9090 | 指标监控 |
| MySQL | localhost:3307 | MySQL 数据库 |
| Oracle | localhost:1521 | Oracle 数据库 (SID: XE) ⭐ 新增 |

## 🧪 API 测试

### 启动全量索引任务

```bash
curl -X POST http://localhost:8080/api/jobs/full \
     -H "Content-Type: application/json" \
     -d '{
       "jobType": "FULL",
       "dataSourceType": "mysql",
       "indexTargetType": "elasticsearch",
       "forceRebuild": true
     }'
```

### 启动增量索引任务

```bash
curl -X POST http://localhost:8080/api/jobs/incremental \
     -H "Content-Type: application/json" \
     -d '{
       "jobType": "INCREMENTAL",
       "dataSourceType": "mysql",
       "indexTargetType": "elasticsearch",
       "timeWindowMinutes": 5
     }'
```

### 检查任务状态

```bash
# 健康检查
curl http://localhost:8080/api/jobs/health

# 重试统计
curl http://localhost:8080/api/jobs/retry-stats

# 任务状态
curl http://localhost:8080/api/jobs/status/{jobInstanceId}
```

## 📝 常用命令

### Docker Compose 命令

```bash
# 启动所有服务
docker-compose up -d

# 启动特定服务
docker-compose up -d mysql elasticsearch redis

# 查看日志
docker-compose logs -f index-app
docker-compose logs -f mysql
docker-compose logs -f elasticsearch

# 重启应用
docker-compose restart index-app

# 停止所有服务
docker-compose down

# 停止并清理数据
docker-compose down -v
```

### 应用管理命令

```bash
# 查看应用日志
tail -f logs/application.log

# 查看批处理日志
tail -f logs/batch.log

# 查看错误日志
tail -f logs/error.log

# 清理日志
find logs -name "*.log" -mtime +7 -delete
```

## 🔍 故障排查

### 常见问题

**1. 应用启动失败**
```bash
# 检查依赖服务
docker-compose ps

# 检查应用日志
docker-compose logs index-app

# 检查端口占用
netstat -tlnp | grep :8080
```

**2. 数据库连接失败**
```bash
# 检查 MySQL 状态
docker-compose logs mysql

# 测试数据库连接
docker-compose exec mysql mysql -u index_user -p index_db
```

**3. Elasticsearch 连接失败**
```bash
# 检查 ES 状态
curl http://localhost:9200/_cluster/health

# 检查 ES 日志
docker-compose logs elasticsearch
```

**4. 调试端口连接失败**
```bash
# 检查调试端口
netstat -tlnp | grep :5005

# 检查应用是否支持调试
docker-compose exec index-app ps aux | grep java
```

### 性能调优

**JVM 参数调优：**
```bash
# 在 docker-compose.yml 中修改
environment:
  - JAVA_OPTS=-Xms1g -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication
```

**数据库连接池调优：**
```yaml
# 在 application-dev.yml 中修改
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

## 📚 开发文档

- [需求说明](docs/需求说明.md)
- [架构设计](docs/架构设计说明.md)
- [项目结构](docs/项目结构说明.md)
- [分步任务](docs/分步任务说明.md)
- [智能数据处理工具使用指南](docs/智能数据处理工具使用指南.md) ⭐ 新增 - 处理100+字段的最佳实践
- [Oracle集成说明](docs/Oracle集成说明.md) 
- [IDEA运行配置](docs/IDEA运行配置.md)
- [故障手册](docs/SOP-故障手册.md)

## 🆘 获取帮助

如果遇到问题，请：

1. 查看 [故障手册](docs/SOP-故障手册.md)
2. 检查应用日志：`docker-compose logs index-app`
3. 检查服务状态：`docker-compose ps`
4. 重启服务：`docker-compose restart index-app`
