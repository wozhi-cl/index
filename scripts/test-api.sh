#!/bin/bash

# API 测试脚本
set -e

echo "🧪 测试索引管理系统 API"

# 等待应用启动
echo "⏳ 等待应用启动..."
sleep 10

# 检查应用健康状态
echo "🔍 检查应用健康状态..."
curl -f http://localhost:8080/actuator/health || {
    echo "❌ 应用未启动，请先启动应用"
    exit 1
}

echo "✅ 应用健康检查通过"

# 测试全量索引任务
echo "📊 测试全量索引任务..."
curl -X POST http://localhost:8080/api/jobs/full \
     -H "Content-Type: application/json" \
     -d '{
       "jobType": "FULL",
       "dataSourceType": "mysql",
       "indexTargetType": "elasticsearch",
       "forceRebuild": true
     }' || echo "❌ 全量索引任务启动失败"

echo ""

# 测试增量索引任务
echo "📈 测试增量索引任务..."
curl -X POST http://localhost:8080/api/jobs/incremental \
     -H "Content-Type: application/json" \
     -d '{
       "jobType": "INCREMENTAL",
       "dataSourceType": "mysql",
       "indexTargetType": "elasticsearch",
       "timeWindowMinutes": 5
     }' || echo "❌ 增量索引任务启动失败"

echo ""

# 检查索引健康状态
echo "🏥 检查索引健康状态..."
curl -X GET http://localhost:8080/api/jobs/health || echo "❌ 健康检查失败"

echo ""

# 检查重试统计
echo "📊 检查重试统计..."
curl -X GET http://localhost:8080/api/jobs/retry-stats || echo "❌ 重试统计获取失败"

echo ""
echo "✅ API 测试完成！"
echo ""
echo "📝 其他测试命令："
echo "  - 健康检查: curl http://localhost:8080/actuator/health"
echo "  - 指标数据: curl http://localhost:8080/actuator/prometheus"
echo "  - 应用信息: curl http://localhost:8080/actuator/info"
