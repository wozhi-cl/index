#!/bin/bash

echo "=== 测试 H2 数据库和 Spring Batch 表 ==="
echo

# 创建 SQL 测试脚本
cat > /tmp/test_h2.sql << 'EOF'
-- 显示所有表
SHOW TABLES;

-- 检查 Spring Batch 表
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME LIKE 'BATCH%';

-- 检查 sample_data 表
SELECT COUNT(*) as RECORD_COUNT FROM SAMPLE_DATA;
SELECT * FROM SAMPLE_DATA LIMIT 5;
EOF

echo "SQL 测试脚本已创建"
echo

# 使用 H2 客户端测试（如果安装了 H2）
# 由于 H2 是内存数据库，我们需要通过应用来测试
echo "请通过以下方式测试 H2:"
echo "1. 访问: http://localhost:8080/h2-console"
echo "2. JDBC URL: jdbc:h2:mem:testdb"
echo "3. 用户名: sa"
echo "4. 密码: (空)"
echo "5. 执行 SQL:"
echo "   SHOW TABLES;"
echo "   SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME LIKE 'BATCH%';"
echo "   SELECT * FROM SAMPLE_DATA;"
echo

# 检查应用启动日志中的表初始化信息
echo "=== 检查表初始化日志 ==="
grep -i "batch.*table\|schema\|ddl" logs/application.log | tail -20
