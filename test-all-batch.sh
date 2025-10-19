#!/bin/bash

# 批量测试脚本
# 用于一键运行所有 batch 测试

echo "========================================"
echo "开始批量测试所有 Job 配置"
echo "========================================"

# 编译项目
echo "1. 编译项目..."
mvn compile -q
if [ $? -ne 0 ]; then
    echo "❌ 编译失败"
    exit 1
fi
echo "✅ 编译成功"

# 运行 H2 测试
echo ""
echo "2. 运行 H2 到 File 测试..."
mvn test -Dtest=H2ToFileJobTest -q
if [ $? -eq 0 ]; then
    echo "✅ H2 到 File 测试通过"
else
    echo "❌ H2 到 File 测试失败"
fi

echo ""
echo "3. 运行 H2 到 Elasticsearch 测试..."
mvn test -Dtest=H2ToElasticsearchJobTest -q
if [ $? -eq 0 ]; then
    echo "✅ H2 到 Elasticsearch 测试通过"
else
    echo "❌ H2 到 Elasticsearch 测试失败"
fi

echo ""
echo "4. 运行 Oracle 到 File 测试..."
mvn test -Dtest=OracleToFileJobTest -q
if [ $? -eq 0 ]; then
    echo "✅ Oracle 到 File 测试通过"
else
    echo "❌ Oracle 到 File 测试失败"
fi

echo ""
echo "5. 运行 Oracle 到 Elasticsearch 测试..."
mvn test -Dtest=OracleToElasticsearchJobTest -q
if [ $? -eq 0 ]; then
    echo "✅ Oracle 到 Elasticsearch 测试通过"
else
    echo "❌ Oracle 到 Elasticsearch 测试失败"
fi

echo ""
echo "6. 运行简化 Job API 测试..."
mvn test -Dtest=SimpleJobApiTest -q
if [ $? -eq 0 ]; then
    echo "✅ 简化 Job API 测试通过"
else
    echo "❌ 简化 Job API 测试失败"
fi

echo ""
echo "========================================"
echo "所有测试完成"
echo "========================================"
