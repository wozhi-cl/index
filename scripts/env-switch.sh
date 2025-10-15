#!/bin/bash

echo "=== 环境切换脚本 ==="
echo

echo "当前支持的环境："
echo "1. dev    - MySQL + Elasticsearch + Redis (Docker)"
echo "2. h2     - H2 内存数据库 + 文件输出"
echo "3. test   - MySQL + Elasticsearch (测试环境)"
echo "4. prod   - MySQL + Elasticsearch (生产环境)"
echo

echo "使用方法："
echo "1. 修改 application.yml 中的 spring.profiles.active"
echo "2. 或者在启动时指定: -Dspring.profiles.active=h2"
echo "3. 或者在 IDEA 中设置 Active profiles"
echo

echo "=== H2 环境使用指南 ==="
echo "1. 设置 Active profiles: h2"
echo "2. 启动应用"
echo "3. 访问 H2 控制台: http://localhost:8080/h2-console"
echo "4. JDBC URL: jdbc:h2:mem:testdb"
echo "5. 用户名: sa"
echo "6. 密码: (空)"
echo

echo "=== MySQL 环境使用指南 ==="
echo "1. 设置 Active profiles: dev"
echo "2. 启动 Docker 服务: docker compose up -d"
echo "3. 启动应用"
echo "4. 访问 Elasticsearch: http://localhost:9200"
echo

echo "=== 环境切换示例 ==="
echo "# H2 模式"
echo "java -jar app.jar --spring.profiles.active=h2"
echo
echo "# MySQL 模式"
echo "java -jar app.jar --spring.profiles.active=dev"
echo

echo "=== 当前配置状态 ==="
current_profile=$(grep "active:" src/main/resources/application.yml | cut -d: -f2 | tr -d ' ')
echo "当前激活环境: $current_profile"
echo

echo "要切换到 H2 模式，请执行："
echo "sed -i 's/active: dev/active: h2/' src/main/resources/application.yml"
echo
echo "要切换到 MySQL 模式，请执行："
echo "sed -i 's/active: h2/active: dev/' src/main/resources/application.yml"
