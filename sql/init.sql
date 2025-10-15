-- 初始化数据库脚本
USE index_db;

-- 创建分布式锁表
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

-- 创建幂等性记录表
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

-- 创建任务执行记录表
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

-- 创建示例数据表（与H2保持一致）
CREATE TABLE IF NOT EXISTS sample_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_updated_at (updated_at),
    INDEX idx_status (status)
);

-- 插入示例数据
INSERT INTO sample_data (name, description, status) VALUES
('John Doe', 'Sample data 1', 'active'),
('Jane Smith', 'Sample data 2', 'inactive'),
('Bob Johnson', 'Sample data 3', 'active'),
('Alice Brown', 'Sample data 4', 'inactive'),
('Charlie Wilson', 'Sample data 5', 'active');

-- 创建变更日志表
CREATE TABLE IF NOT EXISTS sample_data_changelog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_id BIGINT NOT NULL,
    change_type VARCHAR(20) NOT NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_record_id (record_id),
    INDEX idx_changed_at (changed_at)
);
