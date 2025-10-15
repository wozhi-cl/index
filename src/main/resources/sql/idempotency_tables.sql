-- 分布式锁表
CREATE TABLE IF NOT EXISTS distributed_locks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lock_name VARCHAR(255) NOT NULL,
    instance_id VARCHAR(255) NOT NULL,
    acquired_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_lock_name (lock_name),
    INDEX idx_expires_at (expires_at)
);

-- 幂等性记录表
CREATE TABLE IF NOT EXISTS idempotency_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operation_id VARCHAR(255) NOT NULL,
    operation_type VARCHAR(100) NOT NULL,
    instance_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NULL,
    error_message TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_operation_id (operation_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_status (status),
    INDEX idx_start_time (start_time),
    INDEX idx_end_time (end_time)
);

-- 任务执行记录表
CREATE TABLE IF NOT EXISTS job_execution_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(255) NOT NULL,
    job_instance_id VARCHAR(255) NOT NULL,
    job_execution_id VARCHAR(255) NOT NULL,
    instance_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NULL,
    parameters TEXT NULL,
    error_message TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_job_execution_id (job_execution_id),
    INDEX idx_job_name (job_name),
    INDEX idx_instance_id (instance_id),
    INDEX idx_status (status),
    INDEX idx_start_time (start_time)
);

-- 清理过期记录的存储过程
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS CleanupExpiredRecords()
BEGIN
    -- 清理过期的分布式锁
    DELETE FROM distributed_locks WHERE expires_at < NOW();
    
    -- 清理过期的幂等性记录（保留7天）
    DELETE FROM idempotency_records WHERE end_time < DATE_SUB(NOW(), INTERVAL 7 DAY);
    
    -- 清理过期的任务执行记录（保留30天）
    DELETE FROM job_execution_records WHERE end_time < DATE_SUB(NOW(), INTERVAL 30 DAY);
END //
DELIMITER ;

-- 创建定时清理事件（每天凌晨2点执行）
CREATE EVENT IF NOT EXISTS cleanup_expired_records_event
ON SCHEDULE EVERY 1 DAY
STARTS TIMESTAMP(CURDATE() + INTERVAL 1 DAY + INTERVAL 2 HOUR)
DO
  CALL CleanupExpiredRecords();

-- 启用事件调度器
SET GLOBAL event_scheduler = ON;
