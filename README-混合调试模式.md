# 混合调试模式配置成功！🎉

## ✅ 当前状态

### 服务架构
- **Java 应用**: ✅ 本地运行 (localhost:8080)
- **MySQL**: ✅ Docker 运行 (localhost:3307)
- **Elasticsearch**: ✅ Docker 运行 (localhost:9200)
- **Redis**: ✅ Docker 运行 (localhost:6379)
- **Prometheus**: ✅ Docker 运行 (localhost:9090)
- **Kibana**: ✅ Docker 运行 (localhost:5601)

### 应用状态
- **健康检查**: ✅ UP 状态
- **数据库**: ✅ MySQL 连接正常
- **磁盘空间**: ✅ 充足
- **远程调试**: ✅ 已启用 (端口 5005)

## 🔧 配置说明

### 数据库配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3307/index_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: root123
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### Redis 配置
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
```

### Elasticsearch 配置
```yaml
index:
  indexTarget:
    type: elasticsearch
    url: http://localhost:9200
    username: elastic
    password: changeme
    indexName: dev_index
```

## 🚀 启动命令

### 1. 启动 Docker 服务
```bash
# 启动除应用外的所有服务
docker compose up -d mysql elasticsearch redis prometheus kibana
```

### 2. 启动本地 Java 应用
```bash
# 启动本地应用（连接 Docker 服务）
mvn spring-boot:run -s settings.xml \
  -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005" \
  -DskipTests=true
```

## 🔍 调试配置

### IDE 远程调试
- **主机**: localhost
- **端口**: 5005
- **传输**: Socket
- **模式**: Attach

### VS Code 配置
```json
{
    "type": "java",
    "name": "Debug Index App (Mixed Mode)",
    "request": "attach",
    "hostName": "localhost",
    "port": 5005
}
```

## 📊 服务端口映射

| 服务 | 本地端口 | Docker 内部端口 | 状态 |
|------|----------|-----------------|------|
| Java 应用 | 8080 | - | ✅ 本地运行 |
| MySQL | 3307 | 3306 | ✅ Docker |
| Elasticsearch | 9200 | 9200 | ✅ Docker |
| Redis | 6379 | 6379 | ✅ Docker |
| Prometheus | 9090 | 9090 | ✅ Docker |
| Kibana | 5601 | 5601 | ✅ Docker |
| 远程调试 | 5005 | - | ✅ 本地 |

## 🧪 测试功能

### 1. 健康检查
```bash
curl http://localhost:8080/actuator/health
```

### 2. API 测试
```bash
# 查询任务状态
curl -u admin:admin123 http://localhost:8080/api/jobs/status

# 触发全量索引任务
curl -u admin:admin123 -X POST http://localhost:8080/api/jobs/trigger \
  -H "Content-Type: application/json" \
  -d '{"jobName":"fullIndexJob","dataSourceType":"mysql","indexTargetType":"elasticsearch"}'
```

### 3. 数据库连接测试
```bash
# MySQL 连接测试
mysql -h localhost -P 3307 -u root -proot123 -e "SHOW DATABASES;"
```

### 4. Elasticsearch 测试
```bash
# Elasticsearch 健康检查
curl http://localhost:9200/_cluster/health
```

### 5. Redis 测试
```bash
# Redis 连接测试
redis-cli -h localhost -p 6379 ping
```

## 🎯 优势

### 1. 调试便利性
- Java 代码在本地运行，便于设置断点和调试
- 实时修改代码，无需重新构建 Docker 镜像
- 完整的 IDE 支持（代码补全、重构等）

### 2. 服务隔离
- 数据库、缓存等服务在 Docker 中运行，环境一致
- 可以独立重启服务，不影响应用调试
- 便于模拟不同的服务状态

### 3. 性能优化
- 本地运行的应用启动更快
- 避免 Docker 容器的性能开销
- 更好的内存和 CPU 利用率

## 🔄 重启流程

### 重启应用
```bash
# 停止应用 (Ctrl+C)
# 重新启动
mvn spring-boot:run -s settings.xml \
  -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005" \
  -DskipTests=true
```

### 重启 Docker 服务
```bash
# 重启特定服务
docker compose restart mysql redis elasticsearch

# 重启所有服务
docker compose restart
```

## 📝 注意事项

1. **端口冲突**: 确保本地端口 8080 和 5005 未被占用
2. **服务依赖**: 确保 Docker 服务先启动，再启动 Java 应用
3. **网络连接**: 确保本地应用能访问 Docker 服务的端口
4. **数据持久化**: Docker 服务的数据会持久化，重启不会丢失

## 🎉 总结

混合调试模式已成功配置！

- ✅ Java 应用在本地运行，便于调试
- ✅ 其他服务在 Docker 中运行，环境一致
- ✅ 远程调试端口 5005 已启用
- ✅ 所有服务连接正常

现在您可以：
1. 在 IDE 中连接调试器
2. 设置断点进行调试
3. 实时修改代码
4. 测试各种 API 功能

享受高效的开发调试体验！🚀
