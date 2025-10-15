#!/bin/bash

echo "=== 调试 Batch 任务问题 ==="
echo

echo "1. 检查应用状态..."
curl -s http://localhost:8080/actuator/health | head -1
echo

echo "2. 检查 Elasticsearch 连接..."
curl -s http://localhost:9200/_cluster/health | head -1
echo

echo "3. 检查 MySQL 数据..."
docker exec index-mysql mysql -u root -proot123 -e "USE index_db; SELECT COUNT(*) as total FROM sample_data;" 2>/dev/null
echo

echo "4. 测试 API 端点（带详细输出）..."
echo "尝试 POST /api/jobs/full..."
curl -v -X POST -H "Content-Type: application/json" http://localhost:8080/api/jobs/full 2>&1 | head -20
echo

echo "5. 检查应用日志..."
echo "请查看 IDEA 控制台中的错误日志"
echo

echo "6. 检查 Spring Batch 表..."
docker exec index-mysql mysql -u root -proot123 -e "USE index_db; SELECT COUNT(*) as job_instances FROM BATCH_JOB_INSTANCE;" 2>/dev/null
echo

echo "=== 可能的问题 ==="
echo "1. Spring Security 认证问题"
echo "2. JobLauncherService 未正确配置"
echo "3. Elasticsearch 客户端连接问题"
echo "4. 数据源配置问题"
echo

echo "=== 建议的调试步骤 ==="
echo "1. 在 IDEA 中查看控制台日志"
echo "2. 检查是否有异常堆栈"
echo "3. 验证 JobLauncherService 是否正确注入"
echo "4. 检查 Elasticsearch 客户端配置"
