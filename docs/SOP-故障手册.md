# SOP 与故障手册

## 1. 常见故障场景与处置流程

### 1.1 数据源连接失败

**症状：**
- 日志中出现数据库连接异常
- 任务执行失败，错误信息包含 "Connection refused" 或 "Timeout"
- 健康检查失败

**处置流程：**
1. 检查数据源服务状态
   ```bash
   # 检查数据库服务
   kubectl get pods -n database-namespace
   kubectl logs -f <database-pod-name>
   ```

2. 验证网络连接
   ```bash
   # 从应用 Pod 测试连接
   kubectl exec -it <app-pod> -- telnet <db-host> <db-port>
   ```

3. 检查配置
   ```bash
   # 检查 Secret 配置
   kubectl get secret index-secrets -n index-system -o yaml
   ```

4. 重启应用
   ```bash
   kubectl rollout restart deployment/index-app -n index-system
   ```

**预防措施：**
- 配置连接池参数
- 设置合理的超时时间
- 监控数据源健康状态

### 1.2 索引目标不可达

**症状：**
- Elasticsearch/GetQuick 连接失败
- 写入操作超时
- 索引健康检查失败

**处置流程：**
1. 检查索引服务状态
   ```bash
   # 检查 ES 集群状态
   curl -X GET "localhost:9200/_cluster/health?pretty"
   
   # 检查 GetQuick 状态
   curl -X GET "localhost:8080/api/health"
   ```

2. 验证网络连接
   ```bash
   # 测试连接
   kubectl exec -it <app-pod> -- curl -f <index-url>/health
   ```

3. 检查凭据
   ```bash
   # 验证认证信息
   kubectl get secret index-secrets -n index-system -o yaml
   ```

4. 重启应用
   ```bash
   kubectl rollout restart deployment/index-app -n index-system
   ```

### 1.3 任务执行失败

**症状：**
- 批处理任务状态为 FAILED
- 日志中出现异常堆栈
- 指标显示错误率上升

**处置流程：**
1. 查看任务日志
   ```bash
   # 查看应用日志
   kubectl logs -f deployment/index-app -n index-system
   
   # 查看批处理日志
   kubectl logs -f deployment/index-app -n index-system | grep "BATCH"
   ```

2. 检查任务状态
   ```bash
   # 查看任务执行历史
   kubectl get jobs -n index-system
   kubectl describe job <job-name> -n index-system
   ```

3. 分析错误原因
   - 数据质量问题
   - 配置错误
   - 资源不足
   - 网络问题

4. 采取修复措施
   - 修复数据问题
   - 调整配置参数
   - 增加资源限制
   - 重试任务

### 1.4 内存溢出

**症状：**
- Pod 被 OOMKilled
- 日志中出现 OutOfMemoryError
- 应用重启频繁

**处置流程：**
1. 检查内存使用
   ```bash
   # 查看 Pod 资源使用
   kubectl top pods -n index-system
   
   # 查看详细资源信息
   kubectl describe pod <pod-name> -n index-system
   ```

2. 调整内存配置
   ```bash
   # 更新部署资源限制
   kubectl patch deployment index-app -n index-system -p '{"spec":{"template":{"spec":{"containers":[{"name":"index-app","resources":{"limits":{"memory":"4Gi"}}}]}}}}'
   ```

3. 优化应用配置
   - 调整 JVM 参数
   - 减少批处理大小
   - 优化数据处理逻辑

### 1.5 磁盘空间不足

**症状：**
- 日志写入失败
   - 应用启动失败
   - 数据写入错误

**处置流程：**
1. 检查磁盘使用
   ```bash
   # 查看节点磁盘使用
   kubectl get nodes -o wide
   df -h
   ```

2. 清理日志文件
   ```bash
   # 清理旧日志
   kubectl exec -it <pod-name> -- find /var/log/index -name "*.log" -mtime +7 -delete
   ```

3. 调整日志配置
   - 减少日志级别
   - 设置日志轮转
   - 使用外部日志收集

### 1.6 网络分区

**症状：**
- 服务间通信失败
- 任务执行超时
- 健康检查失败

**处置流程：**
1. 检查网络连接
   ```bash
   # 测试 Pod 间通信
   kubectl exec -it <pod-1> -- ping <pod-2-ip>
   ```

2. 检查服务发现
   ```bash
   # 查看服务状态
   kubectl get services -n index-system
   kubectl describe service index-service -n index-system
   ```

3. 重启相关服务
   ```bash
   # 重启应用
   kubectl rollout restart deployment/index-app -n index-system
   ```

## 2. 监控与告警

### 2.1 关键指标监控

**应用指标：**
- 任务执行成功率
- 任务执行时间
- 记录处理速率
- 错误率

**系统指标：**
- CPU 使用率
- 内存使用率
- 磁盘使用率
- 网络流量

**业务指标：**
- 索引文档数量
- 索引健康状态
- 数据源连接状态

### 2.2 告警规则

**严重告警：**
- 任务失败率 > 10%
- 索引健康检查失败
- 内存使用率 > 90%
- 磁盘使用率 > 90%

**警告告警：**
- 任务执行时间 > 正常时间 2 倍
- 记录错误率 > 5%
- CPU 使用率 > 80%

### 2.3 告警处理

**自动处理：**
- 自动重启失败的任务
- 自动扩容资源
- 自动清理日志

**人工处理：**
- 分析根本原因
- 修复配置问题
- 优化应用性能

## 3. 应急响应流程

### 3.1 故障分级

**P0 - 严重故障：**
- 服务完全不可用
- 数据丢失风险
- 安全漏洞

**P1 - 高优先级：**
- 核心功能受影响
- 性能严重下降
- 数据不一致

**P2 - 中优先级：**
- 部分功能受影响
- 性能轻微下降
- 非关键数据问题

**P3 - 低优先级：**
- 功能正常但有小问题
- 性能优化需求
- 文档更新

### 3.2 响应时间

**P0：** 15 分钟内响应，1 小时内解决
**P1：** 1 小时内响应，4 小时内解决
**P2：** 4 小时内响应，24 小时内解决
**P3：** 24 小时内响应，72 小时内解决

### 3.3 升级流程

1. **L1 支持：** 基础故障排查和修复
2. **L2 支持：** 复杂问题分析和解决
3. **L3 支持：** 架构级问题和技术决策

## 4. 预防措施

### 4.1 定期检查

**每日检查：**
- 系统健康状态
- 关键指标趋势
- 日志错误统计

**每周检查：**
- 性能基准测试
- 容量规划评估
- 安全漏洞扫描

**每月检查：**
- 架构优化评估
- 技术债务清理
- 文档更新

### 4.2 演练计划

**故障演练：**
- 模拟各种故障场景
- 测试应急响应流程
- 验证恢复时间目标

**性能演练：**
- 压力测试
- 容量测试
- 灾难恢复测试

### 4.3 持续改进

**经验总结：**
- 故障复盘会议
- 根因分析报告
- 改进措施跟踪

**知识分享：**
- 技术文档更新
- 最佳实践分享
- 培训计划执行

## 5. 联系方式

**值班人员：** 技术支持团队
**紧急联系：** +86-xxx-xxxx-xxxx
**邮件通知：** index-alerts@company.com
**Slack 频道：** #index-support

**升级联系人：**
- 技术负责人：tech-lead@company.com
- 架构师：architect@company.com
- 运维负责人：ops-lead@company.com
