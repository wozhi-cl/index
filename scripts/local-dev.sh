#!/bin/bash

# 本地开发运行脚本（不使用 Docker）
set -e

echo "🚀 启动本地开发环境"

# 检查 Java 环境
if ! command -v java &> /dev/null; then
    echo "❌ Java 未安装，请先安装 Java 17+"
    exit 1
fi

# 检查 Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven 未安装，请先安装 Maven"
    exit 1
fi

# 启动依赖服务
echo "🐳 启动依赖服务..."
docker-compose up -d mysql elasticsearch redis

# 等待服务启动
echo "⏳ 等待服务启动..."
sleep 30

# 检查服务状态
echo "🔍 检查服务状态..."
docker-compose ps

# 设置环境变量
export SPRING_PROFILES_ACTIVE=dev
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=index_db
export DB_USERNAME=index_user
export DB_PASSWORD=index123
export ES_URL=http://localhost:9200
export REDIS_URL=redis://localhost:6379

# 创建日志目录
mkdir -p logs data

echo "🔨 编译应用..."
./mvnw clean compile

echo "🚀 启动应用（支持调试）..."
echo "调试端口: 5005"
echo "应用端口: 8080"
echo ""

# 启动应用（支持调试）
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
     -Dspring.profiles.active=dev \
     -Dlogging.file.name=logs/application.log \
     -jar target/*.jar
