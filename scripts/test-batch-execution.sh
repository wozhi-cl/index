#!/bin/bash

echo "=== 测试 Batch 任务执行 ==="
echo

echo "1. 检查应用状态..."
curl -s http://localhost:8080/actuator/health | head -1
echo

echo "2. 检查 MySQL 数据..."
docker exec index-mysql mysql -u root -proot123 -e "USE index_db; SELECT COUNT(*) as total FROM sample_data;" 2>/dev/null
echo

echo "3. 检查 Elasticsearch 索引..."
curl -s "http://localhost:9200/_cat/indices?v" | head -5
echo

echo "4. 测试 API 端点..."
echo "尝试触发全量索引任务..."
response=$(curl -s -X POST -H "Content-Type: application/json" http://localhost:8080/api/jobs/full)
echo "响应: $response"
echo

echo "5. 等待 5 秒后检查 Elasticsearch..."
sleep 5
curl -s "http://localhost:9200/_cat/indices?v" | head -5
echo

echo "6. 检查 Spring Batch 执行记录..."
docker exec index-mysql mysql -u root -proot123 -e "USE index_db; SELECT COUNT(*) as executions FROM BATCH_JOB_EXECUTION;" 2>/dev/null
echo

echo "7. 检查最新的执行状态..."
docker exec index-mysql mysql -u root -proot123 -e "USE index_db; SELECT JOB_INSTANCE_ID, JOB_NAME, STATUS, START_TIME, END_TIME FROM BATCH_JOB_EXECUTION ORDER BY JOB_EXECUTION_ID DESC LIMIT 3;" 2>/dev/null
echo

echo "=== 如果还是没有数据，可能的问题 ==="
echo "1. JobLauncherService 注入失败"
echo "2. ReaderFactory 配置问题"
echo "3. WriterFactory 配置问题"
echo "4. Elasticsearch 客户端连接问题"
echo "5. 数据源配置问题"
echo

echo "请查看 IDEA 控制台中的错误日志"
