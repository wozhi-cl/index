#!/bin/bash

echo "=== 索引管理系统 - 运行测试 ==="
echo "时间: $(date)"
echo

# 检查服务状态
echo "1. 检查 Docker 服务状态:"
docker compose ps
echo

# 检查应用健康状态
echo "2. 检查应用健康状态:"
curl -s http://localhost:8080/actuator/health | python3 -m json.tool 2>/dev/null || curl -s http://localhost:8080/actuator/health
echo
echo

# 检查应用日志
echo "3. 检查应用启动状态:"
if docker logs index-app 2>&1 | grep -q "Started Application"; then
    echo "✅ 应用已成功启动"
    START_TIME=$(docker logs index-app 2>&1 | grep "Started Application" | tail -1 | awk '{print $1}')
    echo "   启动时间: $START_TIME"
else
    echo "❌ 应用启动失败"
fi
echo

# 检查端口监听
echo "4. 检查端口监听:"
if netstat -an 2>/dev/null | grep -q ":8080.*LISTEN"; then
    echo "✅ 端口 8080 正在监听"
else
    echo "❌ 端口 8080 未监听"
fi
echo

# 测试基本连接
echo "5. 测试基本连接:"
if curl -s --connect-timeout 5 http://localhost:8080/actuator/health > /dev/null; then
    echo "✅ HTTP 连接正常"
else
    echo "❌ HTTP 连接失败"
fi
echo

# 检查 API 端点
echo "6. 测试 API 端点:"
echo "   尝试访问 /api/jobs/status:"
STATUS_RESPONSE=$(curl -s -u admin:admin123 http://localhost:8080/api/jobs/status)
if [ -n "$STATUS_RESPONSE" ]; then
    echo "✅ API 响应: $STATUS_RESPONSE"
else
    echo "❌ API 无响应"
fi
echo

# 检查 Prometheus 端点
echo "7. 测试 Prometheus 端点:"
PROM_RESPONSE=$(curl -s http://localhost:8080/actuator/prometheus | head -5)
if [ -n "$PROM_RESPONSE" ]; then
    echo "✅ Prometheus 端点有数据"
    echo "$PROM_RESPONSE"
else
    echo "❌ Prometheus 端点无数据"
fi
echo

# 总结
echo "=== 运行状态总结 ==="
echo "应用状态: $(curl -s http://localhost:8080/actuator/health | grep -o '"status":"[^"]*"' | cut -d'"' -f4)"
echo "数据库: $(curl -s http://localhost:8080/actuator/health | grep -o '"db":{"status":"[^"]*"' | cut -d'"' -f6)"
echo "磁盘空间: $(curl -s http://localhost:8080/actuator/health | grep -o '"diskSpace":{"status":"[^"]*"' | cut -d'"' -f6)"
echo

echo "=== 可用的服务 ==="
echo "• 应用: http://localhost:8080"
echo "• MySQL: localhost:3307"
echo "• Elasticsearch: http://localhost:9200"
echo "• Redis: localhost:6379"
echo "• Prometheus: http://localhost:9090"
echo "• Kibana: http://localhost:5601"
echo "• 远程调试: localhost:5005"
echo

echo "=== 下一步操作 ==="
echo "1. 查看应用日志: docker logs index-app -f"
echo "2. 测试 API: curl -u admin:admin123 http://localhost:8080/api/jobs/status"
echo "3. 触发任务: curl -u admin:admin123 -X POST http://localhost:8080/api/jobs/trigger -H 'Content-Type: application/json' -d '{\"jobName\":\"fullIndexJob\"}'"
echo "4. 访问 Prometheus: http://localhost:9090"
echo "5. 访问 Kibana: http://localhost:5601"
