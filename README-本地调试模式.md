 本地调试模式启动成功！🎉

## ✅ 当前状态

### 应用状态
- **Spring Boot 应用**: ✅ 运行中 (localhost:8080)
- **健康检查**: ✅ UP 状态
- **数据库**: ✅ H2 数据库正常
- **磁盘空间**: ✅ 充足
- **远程调试**: ✅ 已启用 (端口 5005)

### 可用的端点
- **健康检查**: http://localhost:8080/actuator/health
- **应用信息**: http://localhost:8080/actuator/info
- **指标数据**: http://localhost:8080/actuator/metrics
- **Prometheus 指标**: http://localhost:8080/actuator/prometheus ✅
- **API 端点**: http://localhost:8080/api/jobs/*

## 🔧 远程调试配置

### IDE 配置 (IntelliJ IDEA / VS Code)
1. **调试类型**: Remote JVM Debug
2. **主机**: localhost
3. **端口**: 5005
4. **传输**: Socket
5. **模式**: Attach

### VS Code 配置示例
```json
{
    "type": "java",
    "name": "Debug Index App",
    "request": "attach",
    "hostName": "localhost",
    "port": 5005
}
```

### IntelliJ IDEA 配置
1. Run → Edit Configurations
2. 添加 "Remote JVM Debug"
3. Host: localhost
4. Port: 5005
5. Command line arguments: `-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005`

## 🚀 测试功能

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
  -d '{"jobName":"fullIndexJob","dataSourceType":"h2","indexTargetType":"elasticsearch"}'
```

### 3. Prometheus 指标
```bash
curl http://localhost:8080/actuator/prometheus
```

## 📊 监控数据

应用现在暴露了以下指标：
- `application_ready_time_seconds` - 应用就绪时间
- `application_started_time_seconds` - 应用启动时间
- `disk_free_bytes` - 可用磁盘空间
- `disk_total_bytes` - 总磁盘空间
- 以及更多 Spring Boot 和自定义指标

## 🛠️ 调试技巧

### 1. 设置断点
- 在 IDE 中设置断点
- 确保断点设置在正确的类和方法上
- 使用条件断点进行更精确的调试

### 2. 热重载
- 修改代码后，应用会自动重新编译
- 某些配置更改可能需要重启应用

### 3. 日志调试
- 查看控制台输出
- 日志级别在 `application-dev.yml` 中配置
- 可以动态调整日志级别

### 4. 数据库调试
- H2 控制台: http://localhost:8080/h2-console (如果启用)
- JDBC URL: `jdbc:h2:mem:testdb`
- 用户名: `sa`
- 密码: (空)

## 🔄 重启应用

如果需要重启应用：
```bash
# 停止当前应用 (Ctrl+C)
# 然后重新运行
mvn spring-boot:run -s settings.xml \
  -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005" \
  -DskipTests=true
```

## 📝 注意事项

1. **端口冲突**: 确保端口 8080 和 5005 没有被其他应用占用
2. **Java 版本**: 当前使用 Java 19，确保 IDE 使用相同版本
3. **Maven 设置**: 使用 `settings.xml` 绕过内部镜像
4. **内存使用**: 本地运行比 Docker 模式使用更多内存

## 🎯 下一步

1. **连接调试器**: 在 IDE 中连接到 localhost:5005
2. **设置断点**: 在关键方法上设置断点
3. **测试 API**: 通过 API 调用触发代码执行
4. **查看指标**: 监控应用性能指标
5. **修改代码**: 进行实时调试和开发

---

**本地调试模式已成功启动！** 🚀

现在您可以在 IDE 中连接调试器，设置断点，并进行实时代码调试了。
