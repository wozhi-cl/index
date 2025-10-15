#!/bin/bash

echo "=== IDEA 运行问题解决方案 ==="
echo

echo "问题：IDEA 运行时使用 H2 数据库而不是 MySQL"
echo "原因：配置文件没有正确加载"
echo

echo "=== 解决方案 ==="
echo

echo "1. 已修改 application.yml 文件，添加了数据库配置"
echo "2. 现在请按以下步骤操作："
echo

echo "步骤 1: 停止当前运行的应用"
echo "   - 在 IDEA 中点击停止按钮"
echo "   - 或者按 Ctrl+C"
echo

echo "步骤 2: 清理并重新构建项目"
echo "   - 在 IDEA 中：Build → Clean"
echo "   - 然后：Build → Rebuild Project"
echo

echo "步骤 3: 重新运行应用"
echo "   - 运行 Application.java"
echo "   - 或者使用 Run Configuration"
echo

echo "步骤 4: 验证配置"
echo "   查看启动日志，应该看到："
echo "   HikariPool-1 - Added connection conn0: url=jdbc:mysql://localhost:3307/index_db"
echo "   而不是："
echo "   HikariPool-1 - Added connection conn0: url=jdbc:h2:mem:testdb"
echo

echo "=== 如果仍然有问题 ==="
echo

echo "方案 A: 在 IDEA 中设置运行参数"
echo "   - Run → Edit Configurations"
echo "   - Program arguments: --spring.profiles.active=dev"
echo "   - VM options: -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
echo

echo "方案 B: 使用命令行运行"
echo "   mvn spring-boot:run -Dspring-boot.run.arguments=\"--spring.profiles.active=dev\""
echo

echo "方案 C: 检查 IDEA 设置"
echo "   - File → Settings → Build → Compiler"
echo "   - 确保 'Build project automatically' 已启用"
echo

echo "=== 验证成功标志 ==="
echo "✅ 看到 MySQL 连接日志"
echo "✅ 没有 Quartz 表错误"
echo "✅ 应用健康检查显示 UP"
echo "✅ 可以访问 http://localhost:8080/actuator/health"
echo

echo "=== 当前配置 ==="
echo "数据库: MySQL (localhost:3307)"
echo "Quartz: 内存模式"
echo "调试端口: 5005"
echo "应用端口: 8080"
echo

echo "如果问题仍然存在，请检查："
echo "1. Docker 服务是否正在运行"
echo "2. MySQL 是否可以连接"
echo "3. IDEA 是否有缓存问题"
