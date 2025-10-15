#!/bin/bash

# 检查Docker服务状态脚本
# 用于验证MySQL和Elasticsearch是否正常运行

echo "════════════════════════════════════════════════════════════════════════════════"
echo "🔍 检查Docker服务状态"
echo "════════════════════════════════════════════════════════════════════════════════"
echo ""

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker未运行，请先启动Docker"
    exit 1
fi
echo "✅ Docker正在运行"
echo ""

# 检查MySQL容器
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 MySQL容器状态"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if docker ps | grep -q index-mysql; then
    echo "✅ MySQL容器正在运行"
    docker ps | grep index-mysql | awk '{print "   容器ID: " $1 "  状态: " $7}'
    echo ""
    
    # 测试MySQL连接
    echo "🔌 测试MySQL连接..."
    if docker exec index-mysql mysqladmin ping -h localhost -u root -proot123 2>/dev/null | grep -q "alive"; then
        echo "✅ MySQL连接正常"
        echo "   主机: localhost:3307"
        echo "   数据库: index_db"
        echo "   用户: index_user"
    else
        echo "⚠️  MySQL容器运行但连接失败"
    fi
else
    echo "❌ MySQL容器未运行"
    echo "   请执行: docker-compose up -d mysql"
fi
echo ""

# 检查Elasticsearch容器
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Elasticsearch容器状态"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if docker ps | grep -q index-elasticsearch; then
    echo "✅ Elasticsearch容器正在运行"
    docker ps | grep index-elasticsearch | awk '{print "   容器ID: " $1 "  状态: " $7}'
    echo ""
    
    # 测试ES连接
    echo "🔌 测试Elasticsearch连接..."
    if curl -s http://localhost:9200/_cluster/health > /dev/null; then
        echo "✅ Elasticsearch连接正常"
        echo "   URL: http://localhost:9200"
        
        # 获取集群健康状态
        health=$(curl -s http://localhost:9200/_cluster/health | grep -o '"status":"[^"]*"' | cut -d':' -f2 | tr -d '"')
        echo "   集群状态: $health"
        
        # 检查是否存在test_index
        if curl -s http://localhost:9200/test_index > /dev/null 2>&1; then
            doc_count=$(curl -s http://localhost:9200/test_index/_count | grep -o '"count":[0-9]*' | cut -d':' -f2)
            echo "   test_index存在，文档数: $doc_count"
        else
            echo "   test_index不存在（首次测试时会自动创建）"
        fi
    else
        echo "⚠️  Elasticsearch容器运行但连接失败"
    fi
else
    echo "❌ Elasticsearch容器未运行"
    echo "   请执行: docker-compose up -d elasticsearch"
fi
echo ""

# 检查Kibana容器（可选）
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Kibana容器状态（可选）"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if docker ps | grep -q index-kibana; then
    echo "✅ Kibana容器正在运行"
    echo "   URL: http://localhost:5601"
else
    echo "ℹ️  Kibana容器未运行（可选）"
    echo "   启动: docker-compose up -d kibana"
fi
echo ""

# 总结
echo "════════════════════════════════════════════════════════════════════════════════"
echo "📋 总结"
echo "════════════════════════════════════════════════════════════════════════════════"

mysql_ok=false
es_ok=false

if docker ps | grep -q index-mysql && docker exec index-mysql mysqladmin ping -h localhost -u root -proot123 2>/dev/null | grep -q "alive"; then
    mysql_ok=true
fi

if docker ps | grep -q index-elasticsearch && curl -s http://localhost:9200/_cluster/health > /dev/null; then
    es_ok=true
fi

if [ "$mysql_ok" = true ] && [ "$es_ok" = true ]; then
    echo "✅ 所有必需服务运行正常"
    echo ""
    echo "🚀 可以运行测试:"
    echo "   • MysqlToElasticsearchJobTest"
    echo "   • MysqlToFileJobTest"
    echo "   • AllCombinationsTestSuite"
elif [ "$mysql_ok" = true ]; then
    echo "⚠️  MySQL正常，但Elasticsearch未运行"
    echo "   请执行: docker-compose up -d elasticsearch"
elif [ "$es_ok" = true ]; then
    echo "⚠️  Elasticsearch正常，但MySQL未运行"
    echo "   请执行: docker-compose up -d mysql"
else
    echo "❌ 服务未就绪"
    echo "   请执行: docker-compose up -d mysql elasticsearch"
fi

echo "════════════════════════════════════════════════════════════════════════════════"

