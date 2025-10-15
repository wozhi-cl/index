#!/bin/bash

echo "=========================================="
echo "  H2 数据库表检查指南"
echo "=========================================="
echo
echo "✅ 应用已启动，现在请通过 H2 控制台查看表："
echo
echo "1. 打开浏览器访问: http://localhost:8080/h2-console"
echo
echo "2. 连接信息："
echo "   JDBC URL: jdbc:h2:mem:testdb"
echo "   用户名: sa"
echo "   密码: (留空)"
echo "   点击 'Connect' 按钮"
echo
echo "3. 执行以下 SQL 查询："
echo
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "-- 查看所有表"
echo "SHOW TABLES;"
echo
echo "-- 查看 Spring Batch 表"
echo "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME LIKE 'BATCH%';"
echo
echo "-- 查看测试数据"
echo "SELECT * FROM SAMPLE_DATA;"
echo
echo "-- 查看表结构"
echo "SHOW COLUMNS FROM SAMPLE_DATA;"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo
echo "4. 如果没有看到表，可能的原因："
echo "   - 数据初始化脚本未执行"
echo "   - Spring Batch 自动建表失败"
echo "   - H2 配置问题"
echo
echo "5. 检查应用日志："
echo "   tail -f logs/application.log | grep -i 'create table\|sample_data\|batch'"
echo
echo "=========================================="
