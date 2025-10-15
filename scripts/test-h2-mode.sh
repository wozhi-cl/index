#!/bin/bash

echo "=========================================="
echo "   H2 模式完整测试脚本"
echo "=========================================="
echo

# 1. 检查应用健康状态
echo "1. 检查应用健康状态..."
health=$(curl -s http://localhost:8080/actuator/health)
echo "   $health"
if [[ "$health" == *'"status":"UP"'* ]]; then
    echo "✅ 应用健康状态: UP"
else
    echo "❌ 应用健康状态异常"
    exit 1
fi
echo

# 2. 访问 H2 控制台
echo "2. H2 控制台信息:"
echo "   URL: http://localhost:8080/h2-console"
echo "   JDBC URL: jdbc:h2:mem:testdb"
echo "   用户名: sa"
echo "   密码: (空)"
echo

# 3. 测试 Batch 任务
echo "3. 触发全量索引任务..."
response=$(curl -s -X POST -H "Content-Type: application/json" http://localhost:8080/api/jobs/full)
echo "   响应: $response"
echo

# 4. 检查任务状态
if [[ "$response" == *'"status":"STARTED"'* ]] || [[ "$response" == *'"status":"COMPLETED"'* ]]; then
    echo "✅ 任务已启动"
else
    echo "❌ 任务失败"
    echo "   详细信息: $response"
fi
echo

# 5. 检查输出文件
echo "4. 检查输出文件..."
if [ -d "./output" ]; then
    file_count=$(ls -1 ./output/*.json 2>/dev/null | wc -l)
    if [ $file_count -gt 0 ]; then
        echo "✅ 找到 $file_count 个输出文件:"
        ls -lh ./output/*.json
        echo
        echo "最新文件内容:"
        latest_file=$(ls -t ./output/*.json | head -1)
        cat "$latest_file"
    else
        echo "⚠️  输出目录存在但没有文件"
    fi
else
    echo "⚠️  输出目录不存在"
fi
echo

# 6. 检查应用日志
echo "5. 检查应用日志 (最后20行)..."
if [ -f "./logs/batch.log" ]; then
    echo "--- Batch 日志 ---"
    tail -20 ./logs/batch.log
else
    echo "⚠️  Batch 日志文件不存在"
fi
echo

echo "=========================================="
echo "   测试完成"
echo "=========================================="
