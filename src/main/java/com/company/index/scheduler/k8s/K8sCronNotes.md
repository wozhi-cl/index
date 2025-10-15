# Kubernetes CronJob 运行时约定

## 并发策略配置

### 1. 并发策略选项

**Forbid（推荐）：**
- 禁止并发执行
- 如果前一个任务还在运行，新的任务会被跳过
- 适合全量索引任务

**Replace：**
- 替换正在运行的任务
- 如果前一个任务还在运行，会被新任务替换
- 适合增量索引任务

**Allow：**
- 允许并发执行
- 不推荐使用，可能导致资源竞争

### 2. 配置示例

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: index-full-cronjob
spec:
  schedule: "0 3 * * 0"  # 每周日凌晨3点
  concurrencyPolicy: Forbid  # 禁止并发
  successfulJobsHistoryLimit: 3
  failedJobsHistoryLimit: 3
```

## 分布式锁实现

### 1. 基于数据库的分布式锁

```java
@Service
public class DatabaseDistributedLock {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public boolean acquireLock(String lockName, String instanceId, int timeoutSeconds) {
        try {
            String sql = "INSERT INTO distributed_locks (lock_name, instance_id, acquired_at, expires_at) VALUES (?, ?, NOW(), DATE_ADD(NOW(), INTERVAL ? SECOND))";
            jdbcTemplate.update(sql, lockName, instanceId, timeoutSeconds);
            return true;
        } catch (DuplicateKeyException e) {
            return false; // 锁已被其他实例获取
        }
    }
    
    public void releaseLock(String lockName, String instanceId) {
        String sql = "DELETE FROM distributed_locks WHERE lock_name = ? AND instance_id = ?";
        jdbcTemplate.update(sql, lockName, instanceId);
    }
}
```

### 2. 基于 Redis 的分布式锁

```java
@Service
public class RedisDistributedLock {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    public boolean acquireLock(String lockName, String instanceId, int timeoutSeconds) {
        String key = "lock:" + lockName;
        String value = instanceId + ":" + System.currentTimeMillis();
        
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(key, value, Duration.ofSeconds(timeoutSeconds));
        
        return acquired != null && acquired;
    }
    
    public void releaseLock(String lockName, String instanceId) {
        String key = "lock:" + lockName;
        String value = instanceId + ":" + System.currentTimeMillis();
        
        // 使用 Lua 脚本确保原子性
        String luaScript = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else return 0 end";
        
        redisTemplate.execute(new DefaultRedisScript<>(luaScript, Long.class), 
            Collections.singletonList(key), value);
    }
}
```

## 资源限制配置

### 1. 全量索引任务资源限制

```yaml
resources:
  requests:
    memory: "1Gi"
    cpu: "500m"
  limits:
    memory: "2Gi"
    cpu: "1000m"
```

### 2. 增量索引任务资源限制

```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "100m"
  limits:
    memory: "512Mi"
    cpu: "250m"
```

## 故障恢复配置

### 1. 重试策略

```yaml
spec:
  template:
    spec:
      restartPolicy: OnFailure
      activeDeadlineSeconds: 3600  # 1小时超时
      backoffLimit: 3  # 最多重试3次
```

### 2. 健康检查

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 30
  timeoutSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
```

## 监控与告警

### 1. 任务执行监控

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: index-cronjob-monitor
spec:
  selector:
    matchLabels:
      app: index
  endpoints:
  - port: http
    path: /actuator/prometheus
```

### 2. 告警规则

```yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: index-cronjob-alerts
spec:
  groups:
  - name: index.cronjob
    rules:
    - alert: CronJobFailed
      expr: kube_job_status_failed > 0
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "CronJob {{ $labels.job_name }} failed"
        description: "CronJob {{ $labels.job_name }} has failed {{ $value }} times"
```

## 最佳实践

### 1. 任务设计原则

- 任务应该是幂等的
- 任务应该能够从失败点恢复
- 任务应该设置合理的超时时间
- 任务应该记录详细的执行日志

### 2. 资源管理

- 根据任务类型设置合适的资源限制
- 监控资源使用情况
- 设置合理的并发限制

### 3. 故障处理

- 实现优雅关闭
- 设置重试机制
- 记录失败原因
- 提供手动恢复机制
