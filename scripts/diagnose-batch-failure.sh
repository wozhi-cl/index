#!/bin/bash

echo "=== Batch 任务失败诊断 ==="
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

echo "4. 检查最新执行状态..."
docker exec index-mysql mysql -u root -proot123 -e "USE index_db; SELECT STEP_NAME, STATUS, EXIT_CODE FROM BATCH_STEP_EXECUTION ORDER BY STEP_EXECUTION_ID DESC LIMIT 1;" 2>/dev/null
echo

echo "5. 检查 Elasticsearch 索引..."
curl -s "http://localhost:9200/_cat/indices?v" | head -5
echo

echo "=== 问题分析 ==="
echo "readProcessWriteStep 持续失败，可能的原因："
echo "1. ElasticsearchClient Bean 未正确注入"
echo "2. 应用需要重新启动以加载新的配置"
echo "3. ElasticsearchWriter 中的异常"
echo "4. 数据读取问题"
echo

echo "=== 建议的解决方案 ==="
echo "1. 在 IDEA 中重新启动应用"
echo "2. 检查 IDEA 控制台中的错误日志"
echo "3. 验证 ElasticsearchClient 是否正确注入"
echo "4. 检查 ElasticsearchWriter 的异常信息"
echo

echo "=== 当前配置状态 ==="
echo "✅ EsClientConfig: 已移除 @Profile 限制"
echo "✅ 数据源: MySQL (5 条记录)"
echo "✅ Elasticsearch: 运行正常"
echo "❌ Batch 任务: readProcessWriteStep 失败"
echo

echo "请重新启动应用后再次测试！"
