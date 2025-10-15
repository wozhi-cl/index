package com.company.index.scheduler.lock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * 分布式锁服务
 * 支持数据库和 Redis 两种实现
 */
@Service
public class DistributedLockService {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    @Value("${index.distributed.lock.type:database}")
    private String lockType;

    @Value("${index.distributed.lock.timeout:300}")
    private int defaultTimeout;

    /**
     * 获取分布式锁
     */
    public boolean acquireLock(String lockName, String instanceId, int timeoutSeconds) {
        switch (lockType.toLowerCase()) {
            case "redis":
                return acquireRedisLock(lockName, instanceId, timeoutSeconds);
            case "database":
            default:
                return acquireDatabaseLock(lockName, instanceId, timeoutSeconds);
        }
    }

    /**
     * 释放分布式锁
     */
    public void releaseLock(String lockName, String instanceId) {
        switch (lockType.toLowerCase()) {
            case "redis":
                releaseRedisLock(lockName, instanceId);
                break;
            case "database":
            default:
                releaseDatabaseLock(lockName, instanceId);
                break;
        }
    }

    /**
     * 尝试获取锁（带重试）
     */
    public boolean tryAcquireLock(String lockName, String instanceId, int maxRetries, int retryIntervalMs) {
        for (int i = 0; i < maxRetries; i++) {
            if (acquireLock(lockName, instanceId, defaultTimeout)) {
                return true;
            }
            try {
                Thread.sleep(retryIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * 基于数据库的分布式锁
     */
    private boolean acquireDatabaseLock(String lockName, String instanceId, int timeoutSeconds) {
        if (jdbcTemplate == null) {
            throw new IllegalStateException("JdbcTemplate not available for database lock");
        }

        try {
            // 清理过期锁
            String cleanupSql = "DELETE FROM distributed_locks WHERE lock_name = ? AND expires_at < NOW()";
            jdbcTemplate.update(cleanupSql, lockName);

            // 尝试获取锁
            String insertSql = "INSERT INTO distributed_locks (lock_name, instance_id, acquired_at, expires_at) VALUES (?, ?, NOW(), DATE_ADD(NOW(), INTERVAL ? SECOND))";
            jdbcTemplate.update(insertSql, lockName, instanceId, timeoutSeconds);
            return true;
        } catch (Exception e) {
            // 锁已被其他实例获取或数据库错误
            return false;
        }
    }

    /**
     * 释放数据库锁
     */
    private void releaseDatabaseLock(String lockName, String instanceId) {
        if (jdbcTemplate == null) {
            return;
        }

        String sql = "DELETE FROM distributed_locks WHERE lock_name = ? AND instance_id = ?";
        jdbcTemplate.update(sql, lockName, instanceId);
    }

    /**
     * 基于 Redis 的分布式锁
     */
    private boolean acquireRedisLock(String lockName, String instanceId, int timeoutSeconds) {
        if (redisTemplate == null) {
            throw new IllegalStateException("RedisTemplate not available for redis lock");
        }

        String key = "lock:" + lockName;
        String value = instanceId + ":" + System.currentTimeMillis();

        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(key, value, Duration.ofSeconds(timeoutSeconds));
            return acquired != null && acquired;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 释放 Redis 锁
     */
    private void releaseRedisLock(String lockName, String instanceId) {
        if (redisTemplate == null) {
            return;
        }

        String key = "lock:" + lockName;
        String value = instanceId + ":" + System.currentTimeMillis();

        // 使用 Lua 脚本确保原子性
        String luaScript = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else return 0 end";

        try {
            redisTemplate.execute(new DefaultRedisScript<>(luaScript, Long.class), 
                Collections.singletonList(key), value);
        } catch (Exception e) {
            // 记录错误但不抛出异常
            System.err.println("Failed to release Redis lock: " + e.getMessage());
        }
    }

    /**
     * 检查锁是否存在
     */
    public boolean isLocked(String lockName) {
        switch (lockType.toLowerCase()) {
            case "redis":
                return isRedisLocked(lockName);
            case "database":
            default:
                return isDatabaseLocked(lockName);
        }
    }

    /**
     * 检查数据库锁状态
     */
    private boolean isDatabaseLocked(String lockName) {
        if (jdbcTemplate == null) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM distributed_locks WHERE lock_name = ? AND expires_at > NOW()";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, lockName);
        return count != null && count > 0;
    }

    /**
     * 检查 Redis 锁状态
     */
    private boolean isRedisLocked(String lockName) {
        if (redisTemplate == null) {
            return false;
        }

        String key = "lock:" + lockName;
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取锁信息
     */
    public LockInfo getLockInfo(String lockName) {
        switch (lockType.toLowerCase()) {
            case "redis":
                return getRedisLockInfo(lockName);
            case "database":
            default:
                return getDatabaseLockInfo(lockName);
        }
    }

    /**
     * 获取数据库锁信息
     */
    private LockInfo getDatabaseLockInfo(String lockName) {
        if (jdbcTemplate == null) {
            return null;
        }

        String sql = "SELECT instance_id, acquired_at, expires_at FROM distributed_locks WHERE lock_name = ? AND expires_at > NOW()";
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                LockInfo info = new LockInfo();
                info.setLockName(lockName);
                info.setInstanceId(rs.getString("instance_id"));
                info.setAcquiredAt(rs.getTimestamp("acquired_at").toLocalDateTime());
                info.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
                return info;
            }, lockName);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 Redis 锁信息
     */
    private LockInfo getRedisLockInfo(String lockName) {
        if (redisTemplate == null) {
            return null;
        }

        String key = "lock:" + lockName;
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                String[] parts = value.split(":");
                if (parts.length == 2) {
                    LockInfo info = new LockInfo();
                    info.setLockName(lockName);
                    info.setInstanceId(parts[0]);
                    info.setAcquiredAt(java.time.LocalDateTime.ofEpochSecond(
                        Long.parseLong(parts[1]) / 1000, 0, java.time.ZoneOffset.UTC));
                    return info;
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return null;
    }

    /**
     * 锁信息模型
     */
    public static class LockInfo {
        private String lockName;
        private String instanceId;
        private java.time.LocalDateTime acquiredAt;
        private java.time.LocalDateTime expiresAt;

        // Getters and Setters
        public String getLockName() { return lockName; }
        public void setLockName(String lockName) { this.lockName = lockName; }

        public String getInstanceId() { return instanceId; }
        public void setInstanceId(String instanceId) { this.instanceId = instanceId; }

        public java.time.LocalDateTime getAcquiredAt() { return acquiredAt; }
        public void setAcquiredAt(java.time.LocalDateTime acquiredAt) { this.acquiredAt = acquiredAt; }

        public java.time.LocalDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(java.time.LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    }
}
