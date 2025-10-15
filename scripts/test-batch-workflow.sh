#!/bin/bash

echo "=========================================="
echo "  完整 Batch 业务流程测试"
echo "=========================================="
echo

# 1. 检查应用状态
echo "1. 检查应用状态..."
APP_STATUS=$(curl -s http://localhost:8080/actuator/health 2>/dev/null | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
if [ "$APP_STATUS" != "UP" ]; then
    echo "❌ 应用未运行或不健康"
    exit 1
fi
echo "✅ 应用状态: $APP_STATUS"
echo

# 2. 清理旧的输出文件
echo "2. 清理旧的输出文件..."
rm -rf ./output/*.json 2>/dev/null
mkdir -p ./output
echo "✅ 输出目录已清理"
echo

# 3. 触发全量索引任务
echo "3. 触发全量索引任务..."
RESPONSE=$(curl -s -X POST http://localhost:8080/api/jobs/full \
    -H "Content-Type: application/json" \
    -d '{"jobType":"FULL","dataSourceType":"h2","indexTargetType":"file"}' 2>/dev/null)

echo "响应: $RESPONSE"
echo

# 4. 等待任务执行
echo "4. 等待任务执行（10秒）..."
sleep 10
echo

# 5. 检查输出文件
echo "5. 检查输出文件..."
OUTPUT_FILES=$(ls -lh ./output/*.json 2>/dev/null | wc -l)
if [ "$OUTPUT_FILES" -gt "0" ]; then
    echo "✅ 找到 $OUTPUT_FILES 个输出文件:"
    ls -lh ./output/*.json 2>/dev/null
    echo
    echo "6. 查看第一个输出文件内容:"
    FIRST_FILE=$(ls ./output/*.json 2>/dev/null | head -1)
    echo "文件: $FIRST_FILE"
    echo "---"
    cat "$FIRST_FILE" | head -50
    echo "---"
else
    echo "❌ 没有找到输出文件"
    echo "检查日志..."
    tail -100 logs/batch.log 2>/dev/null | tail -30
fi

echo
echo "=========================================="
echo "  测试完成"
echo "=========================================="

