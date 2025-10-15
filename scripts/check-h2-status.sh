#!/bin/bash

# H2数据库运行状态检查脚本

echo "======================================"
echo "H2 数据库运行状态检查"
echo "======================================"
echo ""

# 1. 检查应用进程
echo "1. 检查应用进程..."
PROCESS=$(ps aux | grep -i "java.*index" | grep -v grep | grep -v "jps.cmdline")
if [ -n "$PROCESS" ]; then
    echo "✅ 应用正在运行"
    echo "$PROCESS" | awk '{print "   PID: " $2 ", 内存: " $6/1024 "MB, 启动时间: " $9}'
else
    echo "❌ 应用未运行"
fi
echo ""

# 2. 检查应用健康状态
echo "2. 检查应用健康状态..."
HEALTH=$(curl -s http://localhost:8080/actuator/health 2>/dev/null)
if [ -n "$HEALTH" ]; then
    STATUS=$(echo "$HEALTH" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
    if [ "$STATUS" = "UP" ]; then
        echo "✅ 应用健康状态: UP"
        # 检查数据库状态
        DB_STATUS=$(echo "$HEALTH" | grep -o '"db":{"status":"[^"]*"' | cut -d'"' -f6)
        if [ "$DB_STATUS" = "UP" ]; then
            echo "✅ H2 数据库连接: 正常"
        else
            echo "⚠️  H2 数据库连接: $DB_STATUS"
        fi
    else
        echo "⚠️  应用健康状态: $STATUS"
    fi
else
    echo "❌ 无法访问健康检查端点"
fi
echo ""

# 3. 检查H2控制台
echo "3. 检查H2控制台..."
H2_CONSOLE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/h2-console 2>/dev/null)
if [ "$H2_CONSOLE" = "200" ]; then
    echo "✅ H2控制台可访问: http://localhost:8080/h2-console"
else
    echo "⚠️  H2控制台状态码: $H2_CONSOLE"
fi
echo ""

# 4. 检查应用端口
echo "4. 检查应用端口..."
PORT_CHECK=$(lsof -i :8080 2>/dev/null | grep LISTEN)
if [ -n "$PORT_CHECK" ]; then
    echo "✅ 端口8080正在监听"
    echo "$PORT_CHECK" | awk '{print "   进程: " $1 ", PID: " $2}'
else
    echo "❌ 端口8080未监听"
fi
echo ""

# 5. 检查最近的错误日志
echo "5. 检查最近的错误..."
ERROR_COUNT=$(grep -i "error\|exception" logs/application.log 2>/dev/null | tail -50 | wc -l | tr -d ' ')
if [ "$ERROR_COUNT" -gt 0 ]; then
    echo "⚠️  发现 $ERROR_COUNT 条错误/异常记录（最近50行）"
    echo "   最新错误："
    grep -i "error\|exception" logs/application.log 2>/dev/null | tail -3 | sed 's/^/   /'
else
    echo "✅ 未发现明显错误"
fi
echo ""

# 6. 检查H2连接池状态
echo "6. 检查数据库连接池..."
HIKARI_LOG=$(grep "HikariPool-1" logs/application.log 2>/dev/null | tail -5)
if [ -n "$HIKARI_LOG" ]; then
    CONN_COUNT=$(echo "$HIKARI_LOG" | grep "Added connection" | wc -l | tr -d ' ')
    WARN_COUNT=$(echo "$HIKARI_LOG" | grep "WARN" | wc -l | tr -d ' ')
    
    if [ "$CONN_COUNT" -gt 0 ]; then
        echo "✅ HikariCP连接池已建立连接"
    fi
    
    if [ "$WARN_COUNT" -gt 0 ]; then
        echo "⚠️  发现 $WARN_COUNT 条连接池警告"
        echo "$HIKARI_LOG" | grep "WARN" | tail -2 | sed 's/^/   /'
    fi
else
    echo "⚠️  未找到连接池日志"
fi
echo ""

# 7. 检查Spring Batch表
echo "7. 检查Spring Batch表初始化..."
BATCH_INIT=$(grep -i "batch.*table\|batch_job_instance" logs/application.log 2>/dev/null | tail -3)
if [ -n "$BATCH_INIT" ]; then
    echo "ℹ️  Batch表初始化日志："
    echo "$BATCH_INIT" | sed 's/^/   /'
else
    echo "ℹ️  未找到明确的Batch表初始化日志"
fi
echo ""

# 8. 显示配置信息
echo "8. 当前H2配置..."
echo "   数据库URL: jdbc:h2:mem:testdb"
echo "   用户名: sa"
echo "   控制台路径: /h2-console"
echo "   模式: MySQL兼容模式"
echo ""

# 9. 总结
echo "======================================"
echo "检查完成"
echo "======================================"
echo ""
echo "📝 建议："
if [ "$STATUS" = "UP" ] && [ "$H2_CONSOLE" = "200" ]; then
    echo "   ✅ H2数据库运行正常，可以通过以下方式访问："
    echo "   1. H2控制台: http://localhost:8080/h2-console"
    echo "      - JDBC URL: jdbc:h2:mem:testdb"
    echo "      - 用户名: sa"
    echo "      - 密码: (留空)"
    echo "   2. 健康检查: http://localhost:8080/actuator/health"
else
    echo "   ⚠️  建议重启应用以修复SQL语法问题"
    echo "   运行: pkill -f 'java.*index' && mvn spring-boot:run -Dspring-boot.run.profiles=h2"
fi
echo ""

