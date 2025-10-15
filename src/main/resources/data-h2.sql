-- 测试数据表和数据（H2）

-- 创建 sample_data 表
CREATE TABLE IF NOT EXISTS sample_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    data_value INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 插入测试数据
INSERT INTO sample_data (name, email, data_value) VALUES 
('John Doe', 'john@example.com', 100),
('Jane Smith', 'jane@example.com', 200),
('Bob Johnson', 'bob@example.com', 300),
('Alice Brown', 'alice@example.com', 400),
('Charlie Wilson', 'charlie@example.com', 500);

